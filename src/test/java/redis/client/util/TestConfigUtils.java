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
import java.util.Arrays;
import java.util.Properties;

import net.spy.memcached.ConnectionFactoryBuilder.Locator;
import net.spy.memcached.ConnectionFactoryBuilder.Protocol;
import net.spy.memcached.DefaultHashAlgorithm;
import net.spy.memcached.FailureMode;
import net.spy.memcached.HashAlgorithm;
import net.spy.memcached.transcoders.SerializingTranscoder;
import net.spy.memcached.transcoders.Transcoder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import redis.client.jedis.spring.CustomShardedJedisPoolFactoryBean.PoolBehaviour;

/**
 * 处理属性配置信息文件的工具类。
 * 
 * @author huagang.li 2014年12月13日 下午2:25:52
 */
public class TestConfigUtils {

    private static final Logger logger             = LoggerFactory.getLogger(TestConfigUtils.class);

    private static final String BIZ_SERVICE_CONFIG = "properties" + File.separator + "biz.service.properties";

    private static Properties   configList;

    static {
        configList = loadPropertyFile(BIZ_SERVICE_CONFIG);
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
                configs.load(TestConfigUtils.class.getClassLoader().getResourceAsStream(fileName));
            }
        } catch (IOException ioe) {
            String errMsg = String.format("File '%s' does not exist", Arrays.toString(fileNames));
            logger.error(errMsg, ioe);
        }
        return configs;
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

}
