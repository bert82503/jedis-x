/*
 * Copyright 2015 FraudMetrix.cn All right reserved. This software is the
 * confidential and proprietary information of FraudMetrix.cn ("Confidential
 * Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered
 * into with FraudMetrix.cn.
 */
package redis.client.pool;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisShardInfo;

/**
 * 基于Jedis实现的"Redis服务器状态检测"定时任务。
 * 
 * @author huagang.li 2015年1月4日 下午6:36:44
 */
public class JedisServerStateCheckTimerTask extends TimerTask {

    private static final Logger                        logger = LoggerFactory.getLogger(JedisServerStateCheckTimerTask.class);

    /** 正常活跃的Jedis分片节点信息列表 */
    private final Set<JedisShardInfo>                  jedisShardSet;

    /** PING命令的失败重试次数 */
    private final int                                  pingRetryTimes;

    /*
     * "异常节点的自动摘除和恢复添加"维护表
     */
    /** 正常可用的Jedis分片资源节点映射表 */
    private final ConcurrentMap<Jedis, JedisShardInfo> activeShardMap;
    /** 异常的Jedis分片资源节点映射表 */
    private final ConcurrentMap<Jedis, JedisShardInfo> brokenShardMap;

    /**
     * 创建一个"Redis服务器状态检测"定时任务对象。
     * 
     * @param jedisShards Jedis实现的Redis分片节点信息列表
     * @param pingRetryTimes PING命令的失败重试次数
     */
    public JedisServerStateCheckTimerTask(List<JedisShardInfo> jedisShards, int pingRetryTimes){
        Assert.notEmpty(jedisShards, "'jedisShards' must be not empty");
        logger.debug("Initial Shard List: {}", jedisShards);

        this.jedisShardSet = new HashSet<JedisShardInfo>(jedisShards);
        this.pingRetryTimes = pingRetryTimes;

        int initialCapacity = jedisShards.size() * 4 / 3 + 1;
        activeShardMap = new ConcurrentHashMap<Jedis, JedisShardInfo>(initialCapacity);
        for (JedisShardInfo jedisShard : jedisShards) {
            Jedis jedis = jedisShard.createResource();
            activeShardMap.put(jedis, jedisShard);
        }
        logger.debug("Initial active Shard map: {}", activeShardMap.values());

        brokenShardMap = new ConcurrentHashMap<Jedis, JedisShardInfo>(4);
    }

    /**
     * 每次调度都会对整个Redis集群中的所有节点(正常、异常)进行有效性探测。
     */
    @Override
    public void run() {
        logger.debug("All active Redis server list for current check run: {}", jedisShardSet);

        // 1. 探测Redis异常节点是否已恢复正常
        for (Jedis jedis : brokenShardMap.keySet()) {
            if (JedisServerStateCheckPolicy.detect(jedis, 0)) { // 异常节点恢复正常了
                // 将恢复正常的节点从"阻塞映射表"移到"活跃映射表"
                JedisShardInfo activeShard = brokenShardMap.remove(jedis);
                if (null != activeShard) { // 保证在并发环境下，只会被移除一次
                    logger.warn("Broken Redis server now is active: {}", activeShard);

                    jedisShardSet.add(activeShard);
                    activeShardMap.put(jedis, activeShard);

                    logger.debug("Active Shard list after a normal Redis server added: {}", jedisShardSet);
                    logger.debug("Active Shard map after a normal Redis server added: {}", activeShardMap.values());
                }
            }
        }

        // 2. 探测Redis正常节点是否出现异常
        for (Jedis jedis : activeShardMap.keySet()) {
            if (!JedisServerStateCheckPolicy.detect(jedis, pingRetryTimes)) { // 正常节点出现异常了
                // 将出现异常的节点从"活跃映射表"移到"阻塞映射表"
                JedisShardInfo brokenShard = activeShardMap.remove(jedis);
                if (null != brokenShard) { // 保证在并发环境下，只会被移除一次
                    logger.warn("Active Redis server now is broken: {}", brokenShard);

                    jedis.close();
                    jedisShardSet.remove(brokenShard);
                    brokenShardMap.put(jedis, brokenShard);

                    logger.debug("Active Shard list after a broken Redis server removed: {}", jedisShardSet);
                    logger.debug("Active Shard map after a broken Redis server removed: {}", activeShardMap.values());
                }
            }
        }
    }

    /**
     * 获取所有正常活跃的Jedis分片节点信息列表。
     * 
     * @return
     */
    public Set<JedisShardInfo> getAllActiveJedisShards() {
        return Collections.unmodifiableSet(jedisShardSet);
    }

}
