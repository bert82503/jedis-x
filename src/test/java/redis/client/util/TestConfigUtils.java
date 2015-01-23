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

package redis.client.util;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import redis.client.jedis.spring.CustomShardedJedisPoolFactoryBean.PoolBehaviour;

/**
 * 处理Test属性配置文件的工具类。
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
        return Integer.parseInt(configList.getProperty("redis.max.total.num", "10000")); // 8 - 默认值
    }

    public static int getMaxIdleNum() {
        return Integer.parseInt(configList.getProperty("redis.max.idle.num", "10000")); // 8
    }

    public static int getMinIdleNum() {
        return Integer.parseInt(configList.getProperty("redis.min.idle.num", "30")); // 0
    }

    public static PoolBehaviour getPoolBehaviour() {
        return PoolBehaviour.valueOf(configList.getProperty("redis.pool.behaviour", "LIFO")); // LIFO
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
        return Long.parseLong(configList.getProperty("redis.max.evictable.idle.time.minutes", "30")); // 30 - 默认值
    }

    // ---------------------- 内部默认配置属性(不可随意更改) ----------------------
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
     * 获取"Redis服务器状态检测"定时任务的运行间隔时间。
     * 
     * @return
     */
    public static int getTimeBetweenServerStateCheckRunsSeconds() {
        return Integer.parseInt(configList.getProperty("redis.server.state.check.time.between.runs.seconds", "1"));
    }

    /**
     * 获取Redis PING命令的失败重试次数。
     * 
     * @return
     */
    public static int getPingRetryTimes() {
        return Integer.parseInt(configList.getProperty("redis.server.state.check.ping.retry.times", "2"));
    }

}
