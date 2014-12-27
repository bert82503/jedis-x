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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import redis.clients.jedis.JedisShardInfo;

/**
 * 处理Redis配置信息的工具类。
 * 
 * @author huagang.li 2014年12月13日 下午2:25:52
 */
public class RedisConfigUtils {

    private static final Logger logger       = LoggerFactory.getLogger(RedisConfigUtils.class);

    private static final String REDIS_CONFIG = "properties" + File.separator + "biz.service.properties";

    private static Properties   configs;

    static {
        try {
            configs = RedisConfigUtils.loadPropertyFile(REDIS_CONFIG);
        } catch (IOException e) {
            logger.error("Not found Redis config file: {}", REDIS_CONFIG);
            configs = new Properties();
        }
    }

    /**
     * 获取Redis服务器列表。
     * 
     * @return
     */
    public static String getRedisServers() {
        return configs.getProperty("redis.server.list");
    }

    /**
     * 获取Memcached服务器列表。
     * 
     * @return
     */
    public static String getMemcachedServers() {
        return configs.getProperty("memcache.server.list");
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
    public static List<JedisShardInfo> parserRedisServerList(String redisServers, int timeoutMillis) {
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

    /**
     * 加载属性配置文件。
     * 
     * @param fileName 属性配置文件名
     * @return
     * @throws IOException
     */
    public static Properties loadPropertyFile(String fileName) throws IOException {
        Properties configs = new Properties();
        configs.load(RedisConfigUtils.class.getClassLoader().getResourceAsStream(fileName));
        return configs;
    }

}
