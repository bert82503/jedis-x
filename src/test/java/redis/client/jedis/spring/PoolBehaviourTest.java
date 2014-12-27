/*
 * Copyright (c)
 */
package redis.client.jedis.spring;

import static org.testng.Assert.assertEquals;

import java.util.Arrays;

import org.testng.annotations.Test;

import redis.client.jedis.spring.CustomShardedJedisPoolFactoryBean.PoolBehaviour;

/**
 * Test for {@link PoolBehaviour}.
 * 
 * @author huagang.li 2014年12月20日 上午11:18:59
 */
public class PoolBehaviourTest {

    @Test
    public void values() {
        assertEquals(Arrays.toString(PoolBehaviour.values()), "[LIFO, FIFO]");
    }

}
