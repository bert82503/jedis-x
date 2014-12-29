/*
 * Copyright 2014 FraudMetrix.cn All right reserved. This software is the
 * confidential and proprietary information of FraudMetrix.cn ("Confidential
 * Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered
 * into with FraudMetrix.cn.
 */
package cache.service;

import static org.testng.Assert.assertEquals;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import redis.client.jedis.CustomShardedJedisPool;
import redis.client.jedis.spring.CustomShardedJedisPoolFactoryBean;
import redis.client.util.ConfigUtils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisShardInfo;
import cache.service.impl.JedisServiceImpl;

/**
 * Test for {@link RedisService}.
 * 
 * @author huagang.li 2014年12月15日 下午6:10:01
 */
public class RedisServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(RedisServiceTest.class);

    private RedisService        redisService;

    @BeforeClass
    public void init() throws Exception {
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

        JedisServiceImpl jedisServiceImpl = new JedisServiceImpl();
        CustomShardedJedisPool shardedJedisPool = shardedJedisPoolFactory.getObject();
        jedisServiceImpl.setShardedJedisPool(shardedJedisPool);
        jedisServiceImpl.setEnabled(true);
        redisService = jedisServiceImpl;
    }

    private static final int DEFAULT_TIMEOUT = (int) TimeUnit.MILLISECONDS.toMillis(100L);

    @Test(description = "检查每一台Redis服务器是否运行正常")
    public void checkEachRedisServerRunOk() {
        List<JedisShardInfo> shards = ConfigUtils.parserRedisServerList(ConfigUtils.getRedisServers(), DEFAULT_TIMEOUT);
        for (JedisShardInfo shardInfo : shards) {
            // try-with-resources, in Java SE 7 and later
            try (JedisPool pool = new JedisPool(new JedisPoolConfig(), shardInfo.getHost(), shardInfo.getPort(),
                                                shardInfo.getTimeout())) {
                // Jedis implements Closeable. Hence, the jedis instance will be auto-closed after the last statement.
                try (Jedis jedis = pool.getResource()) {
                    String ret = jedis.set("foo", "bar");
                    assertEquals(ret, "OK");
                    String value = jedis.get("foo");
                    assertEquals(value, "bar");
                }
            }
        }
    }

    private static final String RET_OK = "OK";

    @Test(description = "验证 String 的 SET、GET、DEL 命令")
    public void setAndGetAndDel() {
        String ret = redisService.set("foo", "bar");
        assertEquals(ret, RET_OK);

        String value = redisService.get("foo");
        assertEquals(value, "bar");

        long removedKeyNum = redisService.del("foo");
        assertEquals(removedKeyNum, 1L);

        value = redisService.get("foo");
        assertEquals(value, null);
    }

    @Test(description = "验证 String 的 SETEX 命令")
    public void setex() {
        String ret = redisService.setex("str:1", 1, "1");
        assertEquals(ret, RET_OK);

        // 当seconds参数不合法时，返回 null (后台抛出"JedisDataException: ERR invalid expire time in setex")
        ret = redisService.setex("str:0", 0, "0");
        assertEquals(ret, null);

        ret = redisService.setex("str:-1", -1, "-1");
        assertEquals(ret, null);
    }

    @Test(description = "验证 List 的 LPUSH、LRANGE、LTRIM、LLEN、RPOP 命令")
    public void list() {
        // lpush - 左端Push
        long pushedListLength = redisService.lpush("list", "foo");
        assertEquals(pushedListLength, 1L);
        pushedListLength = redisService.lpush("list", "bar");
        assertEquals(pushedListLength, 2L);
        // lrange - 获取所有元素
        List<String> list = redisService.lrange("list", 0, -1);
        assertEquals(list.toString(), "[bar, foo]");
        // 允许重复元素
        pushedListLength = redisService.lpush("list", "foo", "bar");
        assertEquals(pushedListLength, 4L);

        // lrange
        list = redisService.lrange("list", 0, -1);
        assertEquals(list.toString(), "[bar, foo, bar, foo]");
        // 获取若干个元素
        list = redisService.lrange("list", 0, 1);
        assertEquals(list.toString(), "[bar, foo]");

        // ltrim
        // 限制List的长度
        String ret = redisService.ltrim("list", 0, 2);
        assertEquals(ret, RET_OK);
        long len = redisService.llen("list");
        assertEquals(len, 3L);
        list = redisService.lrange("list", 0, -1);
        assertEquals(list.toString(), "[bar, foo, bar]");
        // 截断只剩下表头元素
        ret = redisService.ltrim("list", 0, 0);
        assertEquals(ret, RET_OK);
        list = redisService.lrange("list", 0, -1);
        assertEquals(list.toString(), "[bar]");

        // rpop - 右端Pop
        String value = redisService.rpop("list");
        assertEquals(value, "bar");
        // 列表为空
        len = redisService.llen("list");
        assertEquals(len, 0L);
        list = redisService.lrange("list", 0, -1);
        assertEquals(list.toString(), "[]");
        value = redisService.rpop("list");
        assertEquals(value, null);

        // 清空缓存数据
        redisService.ltrim("list", 0, 0);
        redisService.rpop("list");
    }

    @Test(enabled = true, description = "List(列表) 相关操作的性能测试")
    public void listBenchmark() {
        for (int j = 1; j <= 7; j++) {
            String key = "list:global.smartId.smartdev123";

            Random random = new Random(System.currentTimeMillis());
            // 预热缓存数据
            int sampleNum = 3000;
            for (int i = 0; i < sampleNum; i++) {
                String value = ONLINE_EVENT_CONTENT + random.nextLong();
                redisService.lpush(key, value);
            }

            // 测试列表较长情况下，lpush 的性能
            int totalTime = 0;
            int size = 3;
            for (int i = 0; i < size; i++) {
                long startTime = System.currentTimeMillis();
                String value = ONLINE_EVENT_CONTENT + random.nextLong();
                redisService.lpush(key, value);
                long runTime = System.currentTimeMillis() - startTime;
                totalTime += runTime;
                logger.info("Time of 'lpush': {}", runTime);
            }
            logger.info("Total time of 'lpush': {}", totalTime);

            long startTime = System.currentTimeMillis();
            List<String> list = redisService.lrange(key, 0, -1);
            long runTime = System.currentTimeMillis() - startTime;
            logger.info("Time of 'lrange': {}, size of List: {}", runTime, list.size());

            // 清空缓存数据
            redisService.ltrim(key, 0, 0);
            redisService.rpop(key);

            logger.info("Complete time: {}\n", Integer.valueOf(j));
        }
    }

    @Test(description = "验证 有序集合(Sorted Set) 的 ZADD、ZRANGEBYSCORE、ZREVRANGEBYSCORE、ZREMRANGEBYSCORE、ZCARD 命令")
    public void sortedSet() {
        // zadd
        long newElementNum = redisService.zadd("zset", 3.0, "3.0");
        assertEquals(newElementNum, 1L);
        // 添加"重复元素"失败
        newElementNum = redisService.zadd("zset", 3.0, "3.0");
        assertEquals(newElementNum, 0L);
        newElementNum = redisService.zadd("zset", 1.0, "1.0");
        assertEquals(newElementNum, 1L);
        redisService.zadd("zset", 2.0, "2.0");
        redisService.zadd("zset", 2.0, "2.01");

        // zcard - 获取元素数量
        long zsetElementNum = redisService.zcard("zset");
        assertEquals(zsetElementNum, 4L);
        // zrangeByScore - 按分数正序
        Set<String> zset = redisService.zrangeByScore("zset", Double.MIN_VALUE, Double.MAX_VALUE);
        assertEquals(zset.toString(), "[1.0, 2.0, 2.01, 3.0]");
        zset = redisService.zrangeByScore("zset", Double.MIN_VALUE, Double.MAX_VALUE, 0, 3);
        assertEquals(zset.toString(), "[1.0, 2.0, 2.01]");
        // zrevrangeByScore - 按分数逆序
        zset = redisService.zrevrangeByScore("zset", 2.3, 1);
        assertEquals(zset.toString(), "[2.01, 2.0, 1.0]");
        zset = redisService.zrevrangeByScore("zset", Double.MAX_VALUE, Double.MIN_VALUE, 0, 2);
        assertEquals(zset.toString(), "[3.0, 2.01]");

        // zremrangeByScore - 根据分数排序过滤元素
        long removedElementNum = redisService.zremrangeByScore("zset", 0, 2); // 把"2.01"也过滤掉了，与浮点数在机器的真实表示为准
        assertEquals(removedElementNum, 3L);
        zsetElementNum = redisService.zcard("zset");
        assertEquals(zsetElementNum, 1L);
        zset = redisService.zrangeByScore("zset", Double.MIN_VALUE, Double.MAX_VALUE);
        assertEquals(zset.toString(), "[3.0]");

        // 最大、最小分数参数传反了
        redisService.zrangeByScore("zset", Double.MAX_VALUE, Double.MIN_VALUE);
        redisService.zrevrangeByScore("zset", Double.MIN_VALUE, Double.MAX_VALUE);

        // 清空缓存数据
        redisService.zremrangeByScore("zset", Double.MIN_VALUE, Double.MAX_VALUE);
    }

    private static final String ONLINE_EVENT_CONTENT = "{\"accountLogin\":\"181380\",\"create\":1414774385000,\"deliverAddressStreet\":\"浙江省|金华市|婺城区|宾虹路|865号\","
                                                       + "\"eventId\":\"trade\",\"eventType\":\"Trade\",\"ext_IMEI\":\"99000522667636\",\"ipAddress\":\"115.210.9.165\",\"location\":\"金华市\","
                                                       + "\"payeeUserid\":\"hpayZZT@w13758984588\",\"tradingAmount\":21400}";

    @Test(enabled = true, description = "Sorted Set(有序集合) 相关操作的性能测试")
    public void sortedSetBenchmark() {
        for (int j = 1; j <= 7; j++) {
            String key = "zset:global.smartId.smartdev123";

            Random random = new Random(System.currentTimeMillis());
            // 预热缓存数据
            int sampleNum = 3000;
            for (int i = 0; i < sampleNum; i++) {
                String member = ONLINE_EVENT_CONTENT + random.nextLong();
                redisService.zadd(key, System.currentTimeMillis(), member);
            }

            // 测试有序集合较长情况下，zadd 的性能
            int totalTime = 0;
            int size = 3;
            for (int i = 0; i < size; i++) {
                long startTime = System.currentTimeMillis();
                String value = ONLINE_EVENT_CONTENT + random.nextLong();
                long score = System.currentTimeMillis();
                redisService.zadd(key, score, value);
                long runTime = System.currentTimeMillis() - startTime;
                totalTime += runTime;
                logger.info("Time of 'zadd': {}", runTime);
            }
            logger.info("Total time of 'zadd': {}", totalTime);

            long startTime = System.currentTimeMillis();
            Set<String> zset = redisService.zrevrangeByScore(key, Double.MAX_VALUE, Double.MIN_VALUE);
            long runTime = System.currentTimeMillis() - startTime;
            logger.info("Time of 'zrevrangeByScore': {}, size of ZSet: {}", runTime, zset.size());

            // 清空缓存数据
            redisService.zremrangeByScore(key, Double.MIN_VALUE, Double.MAX_VALUE);

            logger.info("Complete time: {}\n", Integer.valueOf(j));
        }
    }

    @Test(enabled = false, description = "验证\"自动摘除异常(宕机)的Redis服务器，自动添加恢复正常的Redis服务器\"功能")
    public void autoDetectBrokenRedisServer() throws InterruptedException {
        String key = null;

        int size = 7;
        for (int i = 1; i <= size; i++) {
            key = "st_" + i;
            String ret = redisService.set(key, "1");
            assertEquals(ret, RET_OK);

            logger.info("Complete time: {}", Integer.valueOf(i));
            if (i < size) {
                TimeUnit.SECONDS.sleep(3L);
            }
        }
    }

    @AfterClass
    public void destroy() {
        redisService.close();
    }

}
