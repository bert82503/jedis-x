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

package redis.client.jedis.spring;

import static org.testng.Assert.assertEquals;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import redis.client.jedis.CustomShardedJedisPool;
import redis.client.util.TestCacheUtils;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.exceptions.JedisException;

/**
 * Tests for {@link CustomShardedJedisPoolFactoryBean}.
 * 
 * @author huagang.li 2014年12月13日 下午3:22:36
 */
public class CustomShardedJedisPoolFactoryBeanTest {

    private static final Logger    logger = LoggerFactory.getLogger(CustomShardedJedisPoolFactoryBeanTest.class);

    private CustomShardedJedisPool shardedJedisPool;

    @BeforeClass
    public void init() throws Exception {
        shardedJedisPool = TestCacheUtils.getShardedJedisPool();
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
                jedis = shardedJedisPool.getResource();

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
        if (shardedJedisPool != null) {
            shardedJedisPool.close();
        }
    }

}
