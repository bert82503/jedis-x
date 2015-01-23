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

import redis.client.jedis.CustomShardedJedisPool;
import redis.client.jedis.spring.CustomShardedJedisPoolFactoryBean;

/**
 * 缓存工具类。
 * 
 * @author huagang.li 2014年12月29日 下午2:52:07
 */
public class TestCacheUtils {

    /**
     * 获取数据分片的Jedis连接池。
     * 
     * @return
     * @throws Exception
     */
    public static CustomShardedJedisPool getShardedJedisPool() throws Exception {
        CustomShardedJedisPoolFactoryBean shardedJedisPoolFactory = new CustomShardedJedisPoolFactoryBean();
        shardedJedisPoolFactory.setRedisServers(TestConfigUtils.getRedisServers());
        shardedJedisPoolFactory.setTimeoutMillis(TestConfigUtils.getTimeoutMillis());
        shardedJedisPoolFactory.setMaxTotalNum(TestConfigUtils.getMaxTotalNum());
        shardedJedisPoolFactory.setMaxIdleNum(TestConfigUtils.getMaxIdleNum());
        shardedJedisPoolFactory.setMinIdleNum(TestConfigUtils.getMinIdleNum());
        shardedJedisPoolFactory.setPoolBehaviour(TestConfigUtils.getPoolBehaviour());
        shardedJedisPoolFactory.setTimeBetweenEvictionRunsSeconds(TestConfigUtils.getTimeBetweenEvictionRunsSeconds());
        shardedJedisPoolFactory.setNumTestsPerEvictionRun(TestConfigUtils.getNumTestsPerEvictionRun());
        shardedJedisPoolFactory.setMinEvictableIdleTimeMinutes(TestConfigUtils.getMinEvictableIdleTimeMinutes());
        shardedJedisPoolFactory.setMaxEvictableIdleTimeMinutes(TestConfigUtils.getMaxEvictableIdleTimeMinutes());
        shardedJedisPoolFactory.setBlockWhenExhausted(TestConfigUtils.getBlockWhenExhausted());
        shardedJedisPoolFactory.setTestOnBorrow(TestConfigUtils.getTestOnBorrow());
        shardedJedisPoolFactory.setTestOnReturn(TestConfigUtils.getTestOnReturn());
        shardedJedisPoolFactory.setTestWhileIdle(TestConfigUtils.getTestWhileIdle());
        shardedJedisPoolFactory.setTimeBetweenServerStateCheckRunsSeconds(TestConfigUtils.getTimeBetweenServerStateCheckRunsSeconds());
        shardedJedisPoolFactory.setPingRetryTimes(TestConfigUtils.getPingRetryTimes());

        return shardedJedisPoolFactory.getObject();
    }

}
