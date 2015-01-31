/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package redis.client.jedis;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.client.util.AssertUtils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisShardInfo;

/**
 * 基于Jedis实现的"Redis服务器状态检测"定时任务。
 * 
 * @author huagang.li 2015年1月4日 下午6:36:44
 */
public class JedisServerStateCheckTimerTask extends TimerTask {

    private static final Logger                        logger = LoggerFactory.getLogger(JedisServerStateCheckTimerTask.class);

    /** 活跃的Jedis分片节点信息列表 */
    private final Set<JedisShardInfo>                  jedisShardSet;

    /** PING命令的失败重试次数 */
    private final int                                  pingRetryTimes;

    /*
     * "异常节点的自动摘除和恢复添加"维护表
     */
    /** 活跃的Jedis分片资源节点映射表 */
    private final ConcurrentMap<Jedis, JedisShardInfo> activeShardMap;
    /** 异常的Jedis分片资源节点映射表 */
    private final ConcurrentMap<Jedis, JedisShardInfo> brokenShardMap;
    /** "活跃的节点列表是否有更新"标识 */
    private final AtomicBoolean                        activeShardListUpdated;

    /**
     * 创建一个"Redis服务器状态检测"定时任务对象。
     * 
     * @param jedisShards Jedis实现的Redis分片节点信息列表
     * @param pingRetryTimes PING命令的失败重试次数
     */
    public JedisServerStateCheckTimerTask(List<JedisShardInfo> jedisShards, int pingRetryTimes){
        AssertUtils.notEmpty(jedisShards, "'jedisShards' must not be null and empty");
        logger.debug("Initial Shard List: {}", jedisShards);

        jedisShardSet = new HashSet<JedisShardInfo>(jedisShards.size());
        jedisShardSet.addAll(jedisShards);
        this.pingRetryTimes = pingRetryTimes;

        activeShardMap = new ConcurrentHashMap<Jedis, JedisShardInfo>(jedisShards.size());
        for (JedisShardInfo jedisShard : jedisShards) {
            Jedis jedis = jedisShard.createResource();
            activeShardMap.put(jedis, jedisShard);
        }
        logger.debug("Initial active Shard map: {}", activeShardMap.values());

        brokenShardMap = new ConcurrentHashMap<Jedis, JedisShardInfo>(4);
        activeShardListUpdated = new AtomicBoolean(false);
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
                    activeShardListUpdated.compareAndSet(false, true);

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
                    activeShardListUpdated.compareAndSet(false, true);

                    logger.debug("Active Shard list after a broken Redis server removed: {}", jedisShardSet);
                    logger.debug("Active Shard map after a broken Redis server removed: {}", activeShardMap.values());
                }
            }
        }
    }

    /**
     * 探测"正常活跃的节点列表是否有更新"。
     * 
     * @return
     */
    public boolean isActiveShardListUpdated() {
        return activeShardListUpdated.get();
    }

    /**
     * 获取所有正常活跃的Jedis分片节点信息列表。
     * 
     * @return
     */
    public Set<JedisShardInfo> getAllActiveJedisShards() {
        activeShardListUpdated.compareAndSet(true, false);
        return Collections.unmodifiableSet(jedisShardSet);
    }

}
