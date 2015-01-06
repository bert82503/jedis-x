/*
 * Copyright 2014 FraudMetrix.cn All right reserved. This software is the
 * confidential and proprietary information of FraudMetrix.cn ("Confidential
 * Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered
 * into with FraudMetrix.cn.
 */
package redis.client.jedis.spring;

import static org.testng.Assert.assertEquals;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import redis.client.jedis.CustomShardedJedisPool;
import redis.client.util.CacheUtils;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.exceptions.JedisException;

/**
 * Test for {@link CustomShardedJedisPoolFactoryBean}.
 * 
 * @author huagang.li 2014年12月13日 下午3:22:36
 */
public class CustomShardedJedisPoolFactoryBeanTest {

    private static final Logger    logger = LoggerFactory.getLogger(CustomShardedJedisPoolFactoryBeanTest.class);

    private CustomShardedJedisPool pool;

    @BeforeClass
    public void init() throws Exception {
        pool = CacheUtils.getShardedJedisPool();
    }

    private static final String DEFAUL_VALUE = "bar";

    private static final String RET_OK       = "OK";

    @Test(description = "验证SET操作")
    public void set() {
        ShardedJedis jedis = null;
        JedisShardInfo shardInfo = null;
        String key = null;

        int size = 7;
        for (int i = 1; i <= size; i++) {
            key = "foo_" + i;

            try {
                // 获取一个Jedis集群池对象
                jedis = this.pool.getResource();

                // log Shard info
                shardInfo = jedis.getShardInfo(key);
                logger.debug("Key: {}, Shard Info: {}", key, shardInfo);

                String statusCode = jedis.set(key, DEFAUL_VALUE);
                assertEquals(statusCode, RET_OK);
                String value = jedis.get(key);
                assertEquals(value, DEFAUL_VALUE);
                long removedElementNum = jedis.del(key);
                assertEquals(removedElementNum, 1L);

                // 返回Jedis集群池对象到连接池
                jedis.close();
            } catch (JedisException je) {
                String errorMsg = String.format("Failed to operate on '%s' Jedis Client", shardInfo);
                logger.warn(errorMsg, je);
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
