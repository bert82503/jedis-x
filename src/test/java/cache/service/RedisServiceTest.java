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
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import redis.client.util.CacheUtils;
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
        JedisServiceImpl jedisServiceImpl = new JedisServiceImpl();
        jedisServiceImpl.setShardedJedisPool(CacheUtils.getShardedJedisPool());
        jedisServiceImpl.setEnabled(true);

        redisService = jedisServiceImpl;
    }

    private static final int DEFAULT_TIMEOUT = (int) TimeUnit.MILLISECONDS.toMillis(100L);

    @Test(description = "检查每一台Redis服务器是否运行正常")
    public void checkEachRedisServerRunOk() {
        List<JedisShardInfo> shards = ConfigUtils.parseRedisServerList(ConfigUtils.getRedisServers(), DEFAULT_TIMEOUT);
        for (JedisShardInfo shardInfo : shards) {
            // try-with-resources, in Java SE 7 and later
            try (JedisPool pool = new JedisPool(new JedisPoolConfig(), shardInfo.getHost(), shardInfo.getPort(),
                                                shardInfo.getTimeout())) {
                // Jedis implements Closeable. Hence, the jedis instance will be auto-closed after the last statement.
                try (Jedis jedis = pool.getResource()) {
                    assertEquals(jedis.ping(), "PONG");
                }
            }
        }
    }

    @Test(description = "验证 String 的 SET、GET、DEL 命令")
    public void setAndGetAndDel() {
        set("foo", "bar"); // key 永不过期
        del("foo", 1L);
        // 数据已被删除，连续第二次delete时，删除操作会失败！
        del("foo", 0L);
    }

    private static final String RET_OK = "OK";

    /**
     * 更新(set)缓存数据。
     */
    private void set(String key, String value) {
        // SET
        String ret = redisService.set(key, value);
        assertEquals(ret, RET_OK);
        // GET
        String val = redisService.get(key);
        assertEquals(val, value);
    }

    /**
     * 删除(delete)缓存数据。
     */
    private void del(String key, long removedKeyNum) {
        // DEL
        long rkn = redisService.del(key);
        assertEquals(rkn, removedKeyNum);
        // GET
        String val = redisService.get(key);
        assertEquals(val, null);
    }

    /** 7天 */
    private static final int TIME_7_DAY  = (int) TimeUnit.DAYS.toSeconds(7L);
    /** 30天 */
    private static final int TIME_30_DAY = (int) TimeUnit.DAYS.toSeconds(30L);

    @Test(description = "验证 String 的 SETEX 命令")
    public void setex() {
        setex("str:7day", TIME_7_DAY, "7 day"); // 相对当前时间，过期时间为7天
        del("str:7day", 1L);

        // Redis的过期时间没有最大30天限制，可以是任何的正整数，与Memcached不同！
        setex("str:30day.1s", TIME_30_DAY + 1, "30 day + 1s");
        del("str:30day.1s", 1L);

        setex("str:1", 1, "1"); // 1秒钟后自动过期

        // 当seconds参数不合法(<= 0)时，返回 null
        setex("str:0", 0, "0");
        setex("str:-1", -1, "-1");
    }

    /**
     * 更新(set)缓存数据及其过期时间。
     */
    private void setex(String key, int seconds, String value) {
        // SETEX
        String ret = redisService.setex(key, seconds, value);
        if (seconds > 0) {
            assertEquals(ret, RET_OK);
        } else {
            assertEquals(ret, null); // 更新失败
        }
        // GET
        String val = redisService.get(key);
        if (seconds > 0) {
            assertEquals(val, value);
        } else {
            assertEquals(val, null);
        }
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
        if (null != redisService) {
            redisService.close();
        }
    }

}
