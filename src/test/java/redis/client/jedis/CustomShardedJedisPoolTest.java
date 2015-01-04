/*
 * Copyright 2014 FraudMetrix.cn All right reserved. This software is the
 * confidential and proprietary information of FraudMetrix.cn ("Confidential
 * Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered
 * into with FraudMetrix.cn.
 */
package redis.client.jedis;

import static org.testng.Assert.assertEquals;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.pool2.impl.AbandonedConfig;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import redis.client.util.ConfigUtils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.exceptions.JedisException;

/**
 * Test for {@link CustomShardedJedisPool}.
 * 
 * @author huagang.li 2014年12月9日 上午9:33:00
 */
public class CustomShardedJedisPoolTest {

    private static final Logger    logger = LoggerFactory.getLogger(CustomShardedJedisPoolTest.class);

    private CustomShardedJedisPool pool;

    @BeforeClass
    public void init() throws InterruptedException {
        List<JedisShardInfo> shards = ConfigUtils.parseRedisServerList(ConfigUtils.getRedisServers(),
                                                                        ConfigUtils.getTimeoutMillis());

        GenericObjectPoolConfig poolConfig = new JedisPoolConfig();
        // 高并发压测
        poolConfig.setMaxTotal(ConfigUtils.getMaxTotalNum());
        poolConfig.setMaxIdle(ConfigUtils.getMaxIdleNum());
        // poolConfig.setMinIdle(ConfigUtils.getMinIdleNum());
        poolConfig.setMinIdle(3); // local test
        // 非阻塞
        poolConfig.setBlockWhenExhausted(false);
        // 阻塞等待一段时间
        // poolConfig.setBlockWhenExhausted(true);
        // poolConfig.setMaxWaitMillis(TimeUnit.MILLISECONDS.toMillis(10L));

        // 关闭"在借用或返回池对象时，检测其有效性"（因为这样对性能影响较大）
        poolConfig.setTestOnBorrow(false);
        poolConfig.setTestOnReturn(false);

        /*
         * "EvictionTimer守护线程"相关配置，用它来检测"空闲对象"
         */
        poolConfig.setTestWhileIdle(true);
        // 每隔5秒钟执行一次，保证异常节点被及时探测到（具体隔多久调度一次，根据业务需求来定）
        // poolConfig.setTimeBetweenEvictionRunsMillis(TimeUnit.SECONDS.toMillis(ConfigUtils.getTimeBetweenEvictionRunsSeconds()));
        poolConfig.setTimeBetweenEvictionRunsMillis(TimeUnit.SECONDS.toMillis(2L)); // local test
        // 模拟关闭后台EvictionTimer守护线程
        // poolConfig.setTimeBetweenEvictionRunsMillis(TimeUnit.SECONDS.toMillis(500L)); // local test
        // 每次检测10个空闲对象
        poolConfig.setNumTestsPerEvictionRun(3);
        // 当池对象的空闲时间超过该值时，就被纳入到驱逐检测范围
        poolConfig.setSoftMinEvictableIdleTimeMillis(TimeUnit.MINUTES.toMillis(ConfigUtils.getMinEvictableIdleTimeMinutes()));
        // 池的最小驱逐空闲时间(空闲驱逐时间)
        // 当池对象的空闲时间超过该值时，立马被驱逐
        poolConfig.setMinEvictableIdleTimeMillis(TimeUnit.MINUTES.toMillis(ConfigUtils.getMaxEvictableIdleTimeMinutes()));

        this.pool = new CustomShardedJedisPool(poolConfig, shards);

        // 池对象废弃策略
        AbandonedConfig abandonedConfig = new AbandonedConfig();
        abandonedConfig.setRemoveAbandonedOnMaintenance(true);
        abandonedConfig.setRemoveAbandonedTimeout((int) TimeUnit.MINUTES.toSeconds(ConfigUtils.getRemoveAbandonedTimeoutMinutes()));
        // abandonedConfig.setRemoveAbandonedTimeout((int) TimeUnit.SECONDS.toSeconds(20L)); // local test
        abandonedConfig.setLogAbandoned(true);
        abandonedConfig.setRemoveAbandonedOnBorrow(false);
        this.pool.setAbandonedConfig(abandonedConfig);
    }

    private static final String DEFAUL_VALUE = "1";

    private static final String RET_OK       = "OK";

    /**
     * <font color="red">注意：</font>将{@link GenericObjectPoolConfig#setTimeBetweenEvictionRunsMillis(long)}设置为500L。
     * 
     * @throws InterruptedException
     */
    @Test(enabled = false, description = "验证\"当集群中的某些节点出现异常(宕机)时，不影响其它节点数据的正常访问\"功能")
    public void accessOtherShardsNormally() throws InterruptedException {
        ShardedJedis jedis = null;
        JedisShardInfo shardInfo = null;
        String key = null;

        int size = 5;
        for (int i = 1; i <= size; i++) {
            key = "st_" + i;

            try {
                // 获取一条Redis连接
                jedis = this.pool.getResource();

                // log Shard info
                shardInfo = jedis.getShardInfo(key);
                logger.info("Shard Info: " + shardInfo);

                String statusCode = jedis.set(key, DEFAUL_VALUE);
                assertEquals(statusCode, RET_OK);
                // 返回连接到连接池
                jedis.close();

                // 关闭第一个被访问到的Redis服务器，模拟Redis服务器宕机的场景
                if (1 == i) {
                    Jedis client = jedis.getShard(key);
                    statusCode = client.shutdown();
                    assertEquals(statusCode, null); // 不返回任何响应信息
                }

            } catch (JedisException je) {
                String errorMsg = String.format("Failed to operate on '%s' Jedis Client", shardInfo);
                logger.warn(errorMsg, je);
            }

            logger.info("Complete time: {}", Integer.valueOf(i));
            if (i < size) {
                TimeUnit.SECONDS.sleep(3L);
            }
        }
    }

    /**
     * 验证"自动摘除异常(宕机)的Redis服务器，自动添加恢复正常的Redis服务器"功能。
     * 
     * @throws InterruptedException
     */
    @Test(description = "验证\"自动摘除异常(宕机)的Redis服务器，自动添加恢复正常的Redis服务器\"功能")
    public void autoDetectBrokenRedisServer() throws InterruptedException {
        ShardedJedis jedis = null;
        JedisShardInfo shardInfo = null;
        String key = null;

        int size = 4;
        for (int i = 1; i <= size; i++) {
            key = "auto_detect_broken_redis_server_" + i;

            try {
                // 获取一条Redis连接
                jedis = this.pool.getResource();

                // log Shard info
                shardInfo = jedis.getShardInfo(key);
                logger.info("Shard Info: " + shardInfo);

                String statusCode = jedis.set(key, DEFAUL_VALUE);
                assertEquals(statusCode, RET_OK);
                // 返回连接到连接池
                jedis.close();

                // 关闭处理节点的服务端连接，模拟Redis服务器出现异常(宕机)的场景，便于驱逐者定时器自动摘除异常(宕机)的Redis服务器
                // 但只要请求一次命令又会重新连接上，模拟异常Redis服务器恢复正常的场景，便于驱逐者定时器自动添加恢复正常的Redis服务器
                if (1 == i) {
                    clientKill(jedis.getShard(key));
                }
            } catch (JedisException je) {
                String errorMsg = String.format("Failed to operate on '%s' Jedis Client", shardInfo);
                logger.warn(errorMsg, je);
            }

            logger.info("Complete time: {}", Integer.valueOf(i));
            if (i < size) {
                TimeUnit.SECONDS.sleep(2L);
            }
        }
    }

    private static final String DEAD_SERVER_CLIENT_NAME = "DEAD";

    /**
     * 关闭一个给定的客户端连接。
     * 
     * @param jedis
     */
    public static void clientKill(Jedis jedis) {
        jedis.clientSetname(DEAD_SERVER_CLIENT_NAME);

        // CLIENT LIST (返回连接到这台服务器的所有客户端的信息和统计数据) - http://redis.io/commands/client-list
        // CLIENT LIST (多了name字段):
        // "id=4 addr=127.0.0.1:50946 fd=6 name=DEAD age=0 idle=0 flags=N db=0 sub=0 psub=0 multi=-1 qbuf=0 qbuf-free=32768 obl=0 oll=0 omem=0 events=r cmd=client"
        for (String clientInfo : jedis.clientList().split("\n")) {
            if (clientInfo.contains(DEAD_SERVER_CLIENT_NAME)) {
                for (String field : clientInfo.split(" ")) {
                    if (field.contains("addr")) {
                        String hostAndPort = field.split("=")[1];
                        // It would be better if we kill the client by Id (CLIENT KILL ID client-id) as it's safer but
                        // Jedis doesn't implement the command yet.
                        // CLIENT KILL (关闭一个给定的客户端连接) - http://redis.io/commands/client-kill
                        jedis.clientKill(hostAndPort);
                    }
                }
            }
        }
    }

    @AfterClass
    public void destroy() {
        if (null != pool) {
            pool.close();
        }
    }

}
