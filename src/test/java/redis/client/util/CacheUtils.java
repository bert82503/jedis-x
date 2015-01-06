package redis.client.util;

import net.spy.memcached.MemcachedClient;
import net.spy.memcached.spring.MemcachedClientFactoryBean;
import redis.client.jedis.CustomShardedJedisPool;
import redis.client.jedis.spring.CustomShardedJedisPoolFactoryBean;

/**
 * 缓存工具类。
 * 
 * @author huagang.li 2014年12月29日 下午2:52:07
 */
public class CacheUtils {

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

        return shardedJedisPoolFactory.getObject();
    }

    /**
     * 获取Memcached客户端。
     * 
     * @return
     * @throws Exception
     */
    public static MemcachedClient getMemcachedClient() throws Exception {
        MemcachedClientFactoryBean memcachedClientFactory = new MemcachedClientFactoryBean();
        memcachedClientFactory.setServers(TestConfigUtils.getMemcachedServers());
        memcachedClientFactory.setProtocol(TestConfigUtils.getProtocol());
        memcachedClientFactory.setTranscoder(TestConfigUtils.getTranscoder());
        memcachedClientFactory.setOpTimeout(TestConfigUtils.getOpTimeout());
        memcachedClientFactory.setTimeoutExceptionThreshold(TestConfigUtils.getTimeoutExceptionThreshold());
        memcachedClientFactory.setHashAlg(TestConfigUtils.getHashAlgorithm());
        memcachedClientFactory.setLocatorType(TestConfigUtils.getLocatorType());
        memcachedClientFactory.setFailureMode(TestConfigUtils.getFailureMode());
        memcachedClientFactory.setUseNagleAlgorithm(TestConfigUtils.getUseNagleAlgorithm());

        return (MemcachedClient) memcachedClientFactory.getObject();
    }

}
