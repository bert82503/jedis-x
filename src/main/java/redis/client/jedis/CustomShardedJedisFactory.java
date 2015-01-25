/*
 * Copyright 2002-2014 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.client.util.GenericTimer;
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
     * @param timeBetweenServerStateCheckRunsMillis "Redis服务器状态检测"定时任务的运行间隔时间
     * @param pingRetryTimes Redis PING命令的失败重试次数
     */
    public CustomShardedJedisFactory(List<JedisShardInfo> shards, Hashing algo, Pattern keyTagPattern,
                                     int timeBetweenServerStateCheckRunsMillis, int pingRetryTimes){
        this.shards = shards;
        this.algo = algo;
        this.keyTagPattern = keyTagPattern;

        this.startServerStateCheckTimerTask(timeBetweenServerStateCheckRunsMillis, pingRetryTimes);
    }

    /**
     * 启动"Redis服务器状态检测"定时任务。
     */
    private final void startServerStateCheckTimerTask(long delay, int pingRetryTimes) {
        synchronized (serverStateCheckLock) { // 同步锁
            if (null != serverStateCheckTimerTask) {
                // 先释放申请的资源
                GenericTimer.cancel(serverStateCheckTimerTask);
                serverStateCheckTimerTask = null;
            }
            if (delay > 0) {
                serverStateCheckTimerTask = new JedisServerStateCheckTimerTask(shards, pingRetryTimes);
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
        Set<JedisShardInfo> checkedShards = new HashSet<JedisShardInfo>(allClusterShardInfos);
        logger.debug("Active Shard list for current validated sharded Jedis: {}", checkedShards);

        Set<JedisShardInfo> activeShards = serverStateCheckTimerTask.getAllActiveJedisShards();
        if (checkedShards.size() != activeShards.size()) { // 节点数不一样
            logger.debug("Find a pooled sharded Jedis is updated");

            shards = new ArrayList<JedisShardInfo>(activeShards);
            logger.debug("Active Shard list after updated: {}", shards);
            return false;
        } else { // 尽管节点数相同，但可能真实的节点列表是不同的(如，一台节点恢复正常了，正好另一台节点出现了异常)
            if (!activeShards.containsAll(checkedShards)) {
                logger.debug("Find a pooled sharded Jedis is updated");

                shards = new ArrayList<JedisShardInfo>(activeShards);
                logger.debug("Active Shard list after updated: {}", shards);
                return false;
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
