/*
 * Copyright 2014 FraudMetrix.cn All right reserved. This software is the
 * confidential and proprietary information of FraudMetrix.cn ("Confidential
 * Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered
 * into with FraudMetrix.cn.
 */
package redis.client.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import net.spy.memcached.ConnectionFactoryBuilder.Locator;
import net.spy.memcached.ConnectionFactoryBuilder.Protocol;
import net.spy.memcached.DefaultHashAlgorithm;
import net.spy.memcached.FailureMode;
import net.spy.memcached.HashAlgorithm;
import net.spy.memcached.transcoders.SerializingTranscoder;
import net.spy.memcached.transcoders.Transcoder;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import redis.client.jedis.spring.CustomShardedJedisPoolFactoryBean.PoolBehaviour;
import redis.clients.jedis.JedisShardInfo;

/**
 * 处理Redis配置信息的工具类。
 * 
 * @author huagang.li 2014年12月13日 下午2:25:52
 */
public class ConfigUtils {

    private static final Logger logger               = LoggerFactory.getLogger(ConfigUtils.class);

    private static final String BIZ_SERVICE_CONFIG   = "properties" + File.separator + "biz.service.properties";

    private static final String DEFAULT_REDIS_CONFIG = "properties" + File.separator + "redis.default.properties";

    private static Properties   configList;

    static {
        configList = loadPropertyFile(DEFAULT_REDIS_CONFIG, BIZ_SERVICE_CONFIG);
    }

    /**
     * 加载属性配置文件。
     * 
     * @param fileName 属性配置文件名
     * @return
     * @throws IOException
     */
    public static Properties loadPropertyFile(String... fileNames) {
        Properties configs = new Properties();
        try {
            for (String fileName : fileNames) {
                configs.load(ConfigUtils.class.getClassLoader().getResourceAsStream(fileName));
            }
        } catch (IOException ioe) {
            String errMsg = String.format("File '%s' does not exist", BIZ_SERVICE_CONFIG);
            logger.error(errMsg, ioe);
        }
        return configs;
    }

    // 内部默认配置属性(外部不可随意更改)
    public static boolean getBlockWhenExhausted() {
        return Boolean.parseBoolean(configList.getProperty("redis.block.when.exhausted", "true"));
    }

    public static boolean getTestOnBorrow() {
        return Boolean.parseBoolean(configList.getProperty("redis.test.on.borrow", "false"));
    }

    public static boolean getTestOnReturn() {
        return Boolean.parseBoolean(configList.getProperty("redis.test.on.return", "false"));
    }

    public static boolean getTestWhileIdle() {
        return Boolean.parseBoolean(configList.getProperty("redis.test.while.idle", "false"));
    }

    /**
     * 获取"Redis服务器状态检测"定时任务的运行间隔时间(ms)。
     * 
     * @return
     */
    public static long getTimeBetweenServerStateCheckRunsMillis() {
        return TimeUnit.SECONDS.toMillis(Long.parseLong(configList.getProperty("redis.server.state.check.time.between.runs.seconds",
                                                                               "1")));
    }

    /**
     * 获取Redis PING命令的失败重试次数。
     * 
     * @return
     */
    public static int getPingRetryTimes() {
        return Integer.parseInt(configList.getProperty("redis.server.state.check.ping.retry.times", "2"));
    }

    // 公开配置属性
    /**
     * 获取Redis服务器列表。
     * 
     * @return
     */
    public static String getRedisServers() {
        String redisServers = configList.getProperty("redis.server.list");
        Assert.notNull(redisServers, "'redis.server.list' is not configured in '" + BIZ_SERVICE_CONFIG + "' file");
        return redisServers;
    }

    public static int getTimeoutMillis() {
        return Integer.parseInt(configList.getProperty("redis.timeout.millis", "100"));
    }

    public static int getMaxTotalNum() {
        return Integer.parseInt(configList.getProperty("redis.max.total.num", "10000")); // 8
    }

    public static int getMaxIdleNum() {
        return Integer.parseInt(configList.getProperty("redis.max.idle.num", "10000")); // 8
    }

    public static int getMinIdleNum() {
        return Integer.parseInt(configList.getProperty("redis.min.idle.num", "30")); // 0
    }

    public static PoolBehaviour getPoolBehaviour() {
        return PoolBehaviour.valueOf(configList.getProperty("redis.pool.behaviour", "FIFO")); // LIFO
    }

    public static long getTimeBetweenEvictionRunsSeconds() {
        return Long.parseLong(configList.getProperty("redis.time.between.eviction.runs.seconds", "1")); // -1
    }

    public static int getNumTestsPerEvictionRun() {
        return Integer.parseInt(configList.getProperty("redis.num.tests.per.eviction.run", "10")); // 3
    }

    public static long getMinEvictableIdleTimeMinutes() {
        return Long.parseLong(configList.getProperty("redis.min.evictable.idle.time.minutes", "5")); // 30
    }

    public static long getMaxEvictableIdleTimeMinutes() {
        return Long.parseLong(configList.getProperty("redis.max.evictable.idle.time.minutes", "1440")); // 30
    }

    /**
     * 获取Memcached服务器列表。
     * 
     * @return
     */
    public static String getMemcachedServers() {
        String memcachedServers = configList.getProperty("memcache.server.list");
        Assert.notNull(memcachedServers, "'memcache.server.list' is not configured in '" + BIZ_SERVICE_CONFIG
                                         + "' file");
        return memcachedServers;
    }

    public static Protocol getProtocol() {
        return Protocol.valueOf(configList.getProperty("memcache.protocol", "BINARY"));
    }

    public static Transcoder<Object> getTranscoder() {
        SerializingTranscoder transcoder = new SerializingTranscoder();
        transcoder.setCompressionThreshold(Integer.parseInt(configList.getProperty("memcache.compress.threshold",
                                                                                   "1024")));
        return transcoder;
    }

    public static long getOpTimeout() {
        return Long.parseLong(configList.getProperty("memcache.operation.timeout", "500"));
    }

    public static int getTimeoutExceptionThreshold() {
        return Integer.parseInt(configList.getProperty("memcache.exception.timeout", "500"));
    }

    public static HashAlgorithm getHashAlgorithm() {
        return DefaultHashAlgorithm.valueOf(configList.getProperty("memcache.hash.algorithm", "KETAMA_HASH"));
    }

    public static Locator getLocatorType() {
        return Locator.valueOf(configList.getProperty("memcache.locator.type", "CONSISTENT"));
    }

    public static FailureMode getFailureMode() {
        return FailureMode.valueOf(configList.getProperty("memcache.failure.mode", "Redistribute"));
    }

    public static boolean getUseNagleAlgorithm() {
        return Boolean.parseBoolean(configList.getProperty("memcache.use.nagle.algorithm", "false"));
    }

    /** 服务器信息的分隔符 */
    private static final String SERVER_INFO_SETPARATOR       = ",";

    /** 服务器信息中各属性的分隔符 */
    private static final String SERVER_INFO_FIELD_SETPARATOR = ":";

    /**
     * 根据给定的{@code redisServers}来解析并返回{@link JedisShardInfo}节点信息列表。
     * 
     * <pre>
     * {@code redisServer}格式：
     *     host:port:name[:weight]
     * </pre>
     * 
     * @param redisServers Redis集群分片节点配置信息
     * @param timeoutMillis 超时时间(ms)
     * @return
     */
    public static List<JedisShardInfo> parseRedisServerList(String redisServers, int timeoutMillis) {
        Assert.notNull(redisServers, "'redisServers' param must not be null");

        String[] shardInfoArray = redisServers.split(SERVER_INFO_SETPARATOR);
        List<JedisShardInfo> shards = new ArrayList<JedisShardInfo>(shardInfoArray.length);
        JedisShardInfo shard = null;
        for (String shardInfo : shardInfoArray) {
            if (StringUtils.isNotBlank(shardInfo)) {
                shardInfo = shardInfo.trim();
                String[] shardFieldArray = shardInfo.split(SERVER_INFO_FIELD_SETPARATOR);
                Assert.isTrue(3 <= shardFieldArray.length && shardFieldArray.length <= 4,
                              "'redisServers' param does not meet the 'host:port:name[:weight] [, ...]' format : "
                                      + shardInfo);

                String host = shardFieldArray[0];
                Assert.isTrue(StringUtils.isNotBlank(host), "'host' field must not be empty : " + shardInfo);
                int port = Integer.parseInt(shardFieldArray[1]);
                String name = shardFieldArray[2];
                Assert.isTrue(StringUtils.isNotBlank(name), "'name' field must not be empty : " + shardInfo);

                if (3 == shardFieldArray.length) { // 未定义"节点权重"属性
                    shard = new JedisShardInfo(host, port, timeoutMillis, name);
                } else {
                    shard = new JedisShardInfo(host, port, timeoutMillis, name);
                    // int weight = Integer.parseInt(shardFieldArray[3]);
                    // Assert.isTrue(weight > 0, "'weight' field of 'redisServers' property must be greater than 0 : "
                    // + weight);
                    // shard.setWeight(weight); // FIXME 该方法现在还不支持，所以现在权重只能使用默认值(1)！
                }
                shards.add(shard);
            }
        }
        return shards;
    }

}
