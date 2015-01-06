/*
 * Copyright 2014 FraudMetrix.cn All right reserved. This software is the
 * confidential and proprietary information of FraudMetrix.cn ("Confidential
 * Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered
 * into with FraudMetrix.cn.
 */
package redis.client.jedis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.client.pool.GenericTimer;
import redis.client.pool.JedisServerStateCheckTimerTask;
import redis.client.util.RedisConfigUtils;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.ShardedJedis;
import redis.clients.util.Hashing;

/**
 * "数据分片的Jedis连接池对象工厂"自定义实现，继承自{@link PooledObjectFactory<ShardedJedis>}。
 * 
 * @author huagang.li 2014年12月8日 下午6:58:03
 */
public class CustomShardedJedisFactory implements PooledObjectFactory<ShardedJedis> {

    private static final Logger            logger                    = LoggerFactory.getLogger(CustomShardedJedisFactory.class);

    /** 正常活跃的Jedis分片节点信息列表 */
    private List<JedisShardInfo>           shards;
    /** 哈希算法 */
    private final Hashing                  algo;
    /** 键标记模式 */
    private final Pattern                  keyTagPattern;

    /*
     * Redis服务器状态检测
     */
    /** "服务器状态检测"同步对象 */
    private final Object                   serverStateCheckLock      = new Object();
    /** "Redis服务器状态检测"定时任务 */
    // @GuardedBy("serverStateCheckLock")
    private JedisServerStateCheckTimerTask serverStateCheckTimerTask = null;

    /**
     * 创建一个"数据分片的Jedis工厂"实例。
     * 
     * @param shards Jedis分片节点信息列表
     * @param algo 哈希算法
     * @param keyTagPattern 键标记模式
     */
    public CustomShardedJedisFactory(List<JedisShardInfo> shards, Hashing algo, Pattern keyTagPattern){
        this.shards = shards;
        this.algo = algo;
        this.keyTagPattern = keyTagPattern;

        this.startServerStateCheckTimerTask(RedisConfigUtils.getTimeBetweenServerStateCheckRunsMillis());
    }

    /**
     * 启动"Redis服务器状态检测"定时任务。
     */
    private final void startServerStateCheckTimerTask(long delay) {
        synchronized (serverStateCheckLock) { // 同步锁
            if (null != serverStateCheckTimerTask) {
                // 先释放申请的资源
                GenericTimer.cancel(serverStateCheckTimerTask);
                serverStateCheckTimerTask = null;
            }
            if (delay > 0) {
                serverStateCheckTimerTask = new JedisServerStateCheckTimerTask(shards, RedisConfigUtils.getPingRetryTimes());
                GenericTimer.schedule(serverStateCheckTimerTask, delay, delay);
            }
        }
    }

    /**
     * 创建一个{@link ShardedJedis}资源实例，并将它包装在{@link PooledObject}里便于连接池管理。
     * <p>
     * {@inheritDoc}
     */
    @Override
    public PooledObject<ShardedJedis> makeObject() throws Exception {
        ShardedJedis jedis = new ShardedJedis(shards, algo, keyTagPattern);
        return new DefaultPooledObject<ShardedJedis>(jedis);
    }

    /**
     * 销毁这个{@link PooledObject<ShardedJedis>}池对象。
     * <p>
     * {@inheritDoc}
     */
    @Override
    public void destroyObject(PooledObject<ShardedJedis> pooledShardedJedis) throws Exception {
        final ShardedJedis shardedJedis = pooledShardedJedis.getObject();
        shardedJedis.disconnect();
    }

    /**
     * 校验整个{@link ShardedJedis}集群中所有的Jedis链接是否正常。
     * <p>
     * <font color="red">这个操作是挺耗时的！</font>
     * <p>
     * {@inheritDoc}
     */
    @Override
    public boolean validateObject(PooledObject<ShardedJedis> pooledShardedJedis) {
        ShardedJedis shardedJedis = pooledShardedJedis.getObject();
        // FIXME "Sharded.getAllShardInfo() returns 160*shards info list not returns the original shards list"
        // https://github.com/xetorthio/jedis/issues/837
        Collection<JedisShardInfo> allClusterShardInfos = shardedJedis.getAllShardInfo(); // 返回的集群节点数量被放大了160倍，详见ShardedJedisTest.getAllShardInfo()测试用例
        // 过滤所有重复的Shard信息
        Map<JedisShardInfo, String> clusterShardInfoMap = new HashMap<JedisShardInfo, String>();
        for (JedisShardInfo clusterShardInfo : allClusterShardInfos) {
            if (!clusterShardInfoMap.containsKey(clusterShardInfo)) {
                clusterShardInfoMap.put(clusterShardInfo, "1");
            }
        }
        Set<JedisShardInfo> checkedShards = clusterShardInfoMap.keySet();
        logger.debug("Active Shard list for current validated sharded Jedis: {}", checkedShards);

        Set<JedisShardInfo> activeShards = serverStateCheckTimerTask.getAllActiveJedisShards();
        if (checkedShards.size() != activeShards.size()) { // 节点数不一样
            logger.debug("Find a pooled sharded Jedis is updated");

            shards = new ArrayList<JedisShardInfo>(activeShards);
            logger.debug("Active Shard list after updated: {}", shards);
            return false;
        } else { // 尽管节点数相同，但可能真实的节点列表是不同的(如，一台节点恢复正常了，正好另一台节点出现了异常)
            for (JedisShardInfo checkedShard : checkedShards) {
                if (!activeShards.contains(checkedShard)) {
                    logger.debug("Find a pooled sharded Jedis is updated");

                    shards = new ArrayList<JedisShardInfo>(activeShards);
                    logger.debug("Active Shard list after updated: {}", shards);
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * 重新初始化{@link ShardedJedis}连接池对象，并返回给连接池。
     * <p>
     * {@inheritDoc}
     */
    @Override
    public void activateObject(PooledObject<ShardedJedis> p) throws Exception {
        //
    }

    /**
     * 不初始化{@link ShardedJedis}连接池对象，并返回到空闲对象池(idleObjects)。
     * <p>
     * {@inheritDoc}
     */
    @Override
    public void passivateObject(PooledObject<ShardedJedis> p) throws Exception {
        //
    }

}
