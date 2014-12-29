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
        shardedJedisPoolFactory.setRedisServers(ConfigUtils.getRedisServers());
        shardedJedisPoolFactory.setTimeoutMillis(ConfigUtils.getTimeoutMillis());
        shardedJedisPoolFactory.setMaxTotalNum(ConfigUtils.getMaxTotalNum());
        shardedJedisPoolFactory.setMaxIdleNum(ConfigUtils.getMaxIdleNum());
        shardedJedisPoolFactory.setMinIdleNum(ConfigUtils.getMinIdleNum());
        shardedJedisPoolFactory.setPoolBehaviour(ConfigUtils.getPoolBehaviour());
        shardedJedisPoolFactory.setTimeBetweenEvictionRunsSeconds(ConfigUtils.getTimeBetweenEvictionRunsSeconds());
        shardedJedisPoolFactory.setNumTestsPerEvictionRun(ConfigUtils.getNumTestsPerEvictionRun());
        shardedJedisPoolFactory.setMinEvictableIdleTimeMinutes(ConfigUtils.getMinEvictableIdleTimeMinutes());
        shardedJedisPoolFactory.setMaxEvictableIdleTimeMinutes(ConfigUtils.getMaxEvictableIdleTimeMinutes());
        shardedJedisPoolFactory.setRemoveAbandonedTimeoutMinutes(ConfigUtils.getRemoveAbandonedTimeoutMinutes());

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
        memcachedClientFactory.setServers(ConfigUtils.getMemcachedServers());
        memcachedClientFactory.setProtocol(ConfigUtils.getProtocol());
        memcachedClientFactory.setTranscoder(ConfigUtils.getTranscoder());
        memcachedClientFactory.setOpTimeout(ConfigUtils.getOpTimeout());
        memcachedClientFactory.setTimeoutExceptionThreshold(ConfigUtils.getTimeoutExceptionThreshold());
        memcachedClientFactory.setHashAlg(ConfigUtils.getHashAlgorithm());
        memcachedClientFactory.setLocatorType(ConfigUtils.getLocatorType());
        memcachedClientFactory.setFailureMode(ConfigUtils.getFailureMode());
        memcachedClientFactory.setUseNagleAlgorithm(ConfigUtils.getUseNagleAlgorithm());

        return (MemcachedClient) memcachedClientFactory.getObject();
    }

}
