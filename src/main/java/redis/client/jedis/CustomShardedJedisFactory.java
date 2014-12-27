/*
 * Copyright 2014 FraudMetrix.cn All right reserved. This software is the
 * confidential and proprietary information of FraudMetrix.cn ("Confidential
 * Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered
 * into with FraudMetrix.cn.
 */
package redis.client.jedis;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Client;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.util.Hashing;

/**
 * "数据分片的Jedis连接池对象工厂"自定义实现，继承自{@link PooledObjectFactory<ShardedJedis>}。
 * 
 * @author huagang.li 2014年12月8日 下午6:58:03
 */
public class CustomShardedJedisFactory implements PooledObjectFactory<ShardedJedis> {

    private static final Logger                   logger = LoggerFactory.getLogger(CustomShardedJedisFactory.class);

    /** 正常活跃的Jedis分片节点信息列表 */
    private List<JedisShardInfo>                  shards;
    /** 哈希算法 */
    private Hashing                               algo;
    /** 键标记模式 */
    private Pattern                               keyTagPattern;

    /*
     * "异常节点的自动摘除和恢复添加"维护表
     */
    /** 正常活跃的Jedis分片节点信息映射表(<host:port, JedisShardInfo>) */
    private ConcurrentMap<String, JedisShardInfo> activeShardMap;
    /** 异常的Jedis分片节点列表 */
    private ConcurrentMap<Jedis, JedisShardInfo>  brokenShardMap;

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

        if (logger.isDebugEnabled()) {
            logger.debug("Initial Shard List: {}", this.shards);
        }

        int initialCapacity = shards.size() * 4 / 3 + 1;
        activeShardMap = new ConcurrentHashMap<String, JedisShardInfo>(initialCapacity);
        for (JedisShardInfo shardInfo : this.shards) {
            String shardKey = generateShardKey(shardInfo.getHost(), shardInfo.getPort());
            activeShardMap.put(shardKey, shardInfo);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Initial active Shard map: {}", activeShardMap);
        }

        brokenShardMap = new ConcurrentHashMap<Jedis, JedisShardInfo>(3);
    }

    /**
     * 生成Shard键，key格式是 "host:port"。
     */
    private static String generateShardKey(String host, int port) {
        return host + ':' + port;
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

    /** PING命令的正常返回值 */
    private static final String PING_COMMAND_RETURN_VALUE = "PONG";

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
        List<Jedis> activeShards = new ArrayList<Jedis>(shardedJedis.getAllShards());
        if (logger.isDebugEnabled()) {
            logger.debug("Active Shard List for current validated sharded Jedis: {}", listShardsToString(activeShards));
        }

        // 1. broken server 自动探测"是否已恢复正常"，并自动添加恢复正常的Redis节点
        if (!brokenShardMap.isEmpty()) {
            AtomicInteger brokenToActiveCounter = new AtomicInteger(0);
            for (Jedis shard : brokenShardMap.keySet()) {
                try {
                    if (shard.ping().equals(PING_COMMAND_RETURN_VALUE)) {
                        // 探测到一个异常节点现在恢复正常了
                        JedisShardInfo activeShard = brokenShardMap.remove(shard);
                        if (null != activeShard) {
                            logger.warn("Broken Redis server now is active: {}", activeShard);

                            shards.add(activeShard);
                            String shardKey = generateShardKey(activeShard.getHost(), activeShard.getPort());
                            activeShardMap.put(shardKey, activeShard);
                            brokenToActiveCounter.incrementAndGet();

                            if (logger.isDebugEnabled()) {
                                logger.debug("Active Shard list after a return to normal node added: {}", shards);
                                logger.debug("Active Shard map after a return to normal node added: {}", activeShardMap);
                            }
                        }
                    }
                } catch (JedisConnectionException e) {
                    // 探测异常的节点，抛出异常是正常行为，忽略之
                }
            }
            if (brokenToActiveCounter.get() > 0) {
                // 有节点恢复正常了
                if (logger.isDebugEnabled()) {
                    logger.debug("Find a pooled sharded Jedis is updated");
                }
                return false;
            }
        }

        // 2. 检测是否有恢复正常的节点添加进来了
        if (activeShardMap.size() > activeShards.size()) {
            if (logger.isDebugEnabled()) {
                logger.debug("Find a pooled sharded Jedis is updated");
            }
            return false;
        }

        // 3. 每次校验"Jedis集群池对象"有效性时，都会对所有当前正常的Redis服务器进行"PING命令"请求，这样是很耗时的！
        Jedis jedis = null;
        try {
            int size = activeShards.size();
            for (int i = 0; i < size; i++) {
                jedis = activeShards.get(i);
                if (!jedis.ping().equals(PING_COMMAND_RETURN_VALUE)) {
                    // FIXME 增加重试机制
                    // 3.1 自动摘除出现异常的Redis节点
                    // this.removeShard(jedis);
                    return false;
                }
            }
            return true;
        } catch (JedisConnectionException ex) {
            // 4. 自动摘除出现异常的Redis节点
            this.removeShard(jedis);
            return false;
        }
    }

    /**
     * 从集群中摘除异常的"Redis节点"，且只会被摘除一次。
     */
    private void removeShard(Jedis jedis) {
        // 1. 关闭Jedis客户端链接
        Client redisClient = jedis.getClient();
        redisClient.close();

        // 2. 将异常节点从活跃的Shard列表中移除，并放入到异常的Shard列表中，等待恢复后添加
        String shardKey = generateShardKey(redisClient.getHost(), redisClient.getPort());
        JedisShardInfo shard = activeShardMap.remove(shardKey);
        if (null != shard) { // 节点已不在活跃节点列表中，这样保证只会被移除一次
            logger.warn("Remove a broken Redis server: {}", shardKey);

            shards = new ArrayList<JedisShardInfo>(activeShardMap.values());
            brokenShardMap.put(jedis, shard);

            if (logger.isDebugEnabled()) {
                logger.debug("Active Shard List after a broken Redis server removed: {}", shards);
            }
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("Find a pooled sharded Jedis is broken");
            }
        }
    }

    private static String listShardsToString(List<Jedis> shards) {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        for (Jedis jedis : shards) {
            Client client = jedis.getClient();
            sb.append(client.getHost()).append(':').append(client.getPort()).append(',');
        }
        sb.append('}');
        return sb.toString();
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
