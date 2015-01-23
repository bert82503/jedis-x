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

package cache.service;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import redis.client.util.RedisConfigUtils;
import redis.client.util.TestCacheUtils;
import redis.client.util.TestConfigUtils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisShardInfo;
import cache.service.impl.JedisServiceImpl;

/**
 * Tests for {@link RedisService}.
 * 
 * @author huagang.li 2014年12月15日 下午6:10:01
 */
public class RedisServiceTest {

    private RedisService redisService;

    @BeforeClass
    public void init() throws Exception {
        JedisServiceImpl jedisServiceImpl = new JedisServiceImpl();
        jedisServiceImpl.setShardedJedisPool(TestCacheUtils.getShardedJedisPool());
        jedisServiceImpl.setEnabled(true);

        redisService = jedisServiceImpl;
    }

    @Test(description = "检查每一台Redis服务器是否运行正常")
    public void checkEachRedisServerRunOk() {
        List<JedisShardInfo> shards = RedisConfigUtils.parseRedisServerList(TestConfigUtils.getRedisServers(),
                                                                            TestConfigUtils.getTimeoutMillis());
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
        String stringKey = "foo";
        this.set(stringKey, "bar"); // key 永不过期
        this.del(stringKey, 1);
        // 数据已被删除，连续第二次delete时，删除操作会失败！
        this.del(stringKey, 0);
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
    private void del(String key, int removedKeyNum) {
        // DEL
        int rkn = redisService.del(key);
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
        this.setex("str:7day", TIME_7_DAY, "7 day"); // 相对当前时间，过期时间为7天
        this.del("str:7day", 1);

        // Redis的过期时间没有最大30天限制，可以是任何的正整数，与Memcached不同！
        this.setex("str:30day.1s", TIME_30_DAY + 1, "30 day + 1s");
        this.del("str:30day.1s", 1);

        this.setex("str:1", 1, "1"); // 1秒钟后自动过期

        // 当seconds参数不合法(<= 0)时，返回 null
        this.setex("str:0", 0, "0");
        this.setex("str:-1", -1, "-1");
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
        String listKey = "list";

        // 清空缓存数据
        redisService.del(listKey);

        // lpush - 左端Push
        int pushedListLength = redisService.lpush(listKey, "foo");
        assertEquals(pushedListLength, 1L);
        pushedListLength = redisService.lpush(listKey, "bar");
        assertEquals(pushedListLength, 2L);
        // lrange - 获取所有元素
        List<String> list = redisService.lrange(listKey, 0, -1);
        assertEquals(list.toString(), "[bar, foo]");
        // 允许重复元素
        pushedListLength = redisService.lpush(listKey, "foo", "bar");
        assertEquals(pushedListLength, 4L);

        // lrange
        list = redisService.lrange(listKey, 0, -1);
        assertEquals(list.toString(), "[bar, foo, bar, foo]");
        // 获取若干个元素
        list = redisService.lrange(listKey, 0, 1);
        assertEquals(list.toString(), "[bar, foo]");

        // ltrim
        // 限制List的长度
        String ret = redisService.ltrim(listKey, 0, 2);
        assertEquals(ret, RET_OK);
        int len = redisService.llen(listKey);
        assertEquals(len, 3L);
        list = redisService.lrange(listKey, 0, -1);
        assertEquals(list.toString(), "[bar, foo, bar]");
        // 截断只剩下表头元素
        ret = redisService.ltrim(listKey, 0, 0);
        assertEquals(ret, RET_OK);
        list = redisService.lrange(listKey, 0, -1);
        assertEquals(list.toString(), "[bar]");

        // rpop - 右端Pop
        String value = redisService.rpop(listKey);
        assertEquals(value, "bar");
        // 列表为空
        len = redisService.llen(listKey);
        assertEquals(len, 0L);
        list = redisService.lrange(listKey, 0, -1);
        assertEquals(list.toString(), "[]");
        value = redisService.rpop(listKey);
        assertEquals(value, null);
    }

    @Test(description = "验证 有序集合(Sorted Set) 的 ZADD、ZRANGEBYSCORE、ZREVRANGEBYSCORE、ZREMRANGEBYSCORE、ZCARD 命令", dependsOnMethods = "zadd")
    public void sortedSet() {
        String zsetKey = "zset";

        // 清空缓存数据
        redisService.del(zsetKey);

        // zadd
        int newElementNum = redisService.zadd(zsetKey, 3.0, "3.0");
        assertEquals(newElementNum, 1L);
        // 添加"重复元素"失败
        newElementNum = redisService.zadd(zsetKey, 3.0, "3.0");
        assertEquals(newElementNum, 0L);
        newElementNum = redisService.zadd(zsetKey, 1.0, "1.0");
        assertEquals(newElementNum, 1L);
        redisService.zadd(zsetKey, 2.0, "2.0");
        redisService.zadd(zsetKey, 2.0, "2.01");

        Set<String> zset = null;
        // zcard - 获取元素数量
        int zsetElementNum = redisService.zcard(zsetKey);
        assertEquals(zsetElementNum, 4L);
        // zrange - 按分数递增(从小到大)排序
        zset = redisService.zrange(zsetKey, 0, 2);
        assertEquals(zset.toString(), "[1.0, 2.0, 2.01]");
        // zrevrange - 按分数递减(从大到小)排序
        zset = redisService.zrevrange(zsetKey, 0, 2);
        assertEquals(zset.toString(), "[3.0, 2.01, 2.0]");
        // zrangeByScore - 按分数递增(从小到大)排序
        zset = redisService.zrangeByScore(zsetKey, Double.MIN_VALUE, Double.MAX_VALUE);
        assertEquals(zset.toString(), "[1.0, 2.0, 2.01, 3.0]");
        zset = redisService.zrangeByScore(zsetKey, Double.MIN_VALUE, Double.MAX_VALUE, 0, 3);
        assertEquals(zset.toString(), "[1.0, 2.0, 2.01]");
        // zrevrangeByScore - 按分数递减(从大到小)排序
        zset = redisService.zrevrangeByScore(zsetKey, 2.3, 1);
        assertEquals(zset.toString(), "[2.01, 2.0, 1.0]");
        zset = redisService.zrevrangeByScore(zsetKey, Double.MAX_VALUE, Double.MIN_VALUE, 0, 2);
        assertEquals(zset.toString(), "[3.0, 2.01]");

        // zremrangeByScore - 根据分数排序过滤元素
        int removedElementNum = redisService.zremrangeByScore(zsetKey, 0, 2); // 把"2.01"也过滤掉了，与浮点数在机器的真实表示为准
        assertEquals(removedElementNum, 3L);
        zsetElementNum = redisService.zcard(zsetKey);
        assertEquals(zsetElementNum, 1L);
        zset = redisService.zrangeByScore(zsetKey, Double.MIN_VALUE, Double.MAX_VALUE);
        assertEquals(zset.toString(), "[3.0]");
        // 又添加2个元素
        redisService.zadd(zsetKey, 5, "5");
        redisService.zadd(zsetKey, 2, "2");
        zsetElementNum = redisService.zcard(zsetKey);
        assertEquals(zsetElementNum, 3L);
        zset = redisService.zrangeByScore(zsetKey, Double.MIN_VALUE, Double.MAX_VALUE);
        assertEquals(zset.toString(), "[2, 3.0, 5]");
        // zremrangeByRank - 按分数递增(从小到大)排序过滤元素
        removedElementNum = redisService.zremrangeByRank(zsetKey, 0, 1);
        assertEquals(removedElementNum, 2L);
        zsetElementNum = redisService.zcard(zsetKey);
        assertEquals(zsetElementNum, 1L);
        zset = redisService.zrange(zsetKey, 0, -1);
        assertEquals(zset.toString(), "[5]");

        // 最大、最小分数参数传反了
        redisService.zrangeByScore(zsetKey, Double.MAX_VALUE, Double.MIN_VALUE);
        redisService.zrevrangeByScore(zsetKey, Double.MIN_VALUE, Double.MAX_VALUE);
    }

    @Test(description = "验证'EXPIRE'命令结合'zadd'的效果")
    public void zsetExpire() {
        String key = "zsetExpire";

        // 清空缓存数据
        redisService.del(key);

        int newElementNum = redisService.zadd(key, 10, "10");
        assertEquals(newElementNum, 1L);
        // expire (设置Key过期时间)
        int ret = redisService.expire(key, TIME_7_DAY);
        assertEquals(ret, 1);
        // ttl (key的剩余生存时间)
        long liveTimeSeconds = redisService.ttl(key);
        assertTrue(liveTimeSeconds > TIME_7_DAY - 1L);
        try {
            TimeUnit.SECONDS.sleep(1L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // 添加新数据，ttl 并未失效
        newElementNum = redisService.zadd(key, 23, "23");
        assertEquals(newElementNum, 1L);
        liveTimeSeconds = redisService.ttl(key);
        assertTrue(liveTimeSeconds > TIME_7_DAY - 2L);
        // 移除若干老数据，ttl 也并未失效
        int removedElementNum = redisService.zremrangeByRank(key, 0, 0);
        assertEquals(removedElementNum, 1);
        liveTimeSeconds = redisService.ttl(key);
        assertTrue(liveTimeSeconds > TIME_7_DAY - 2L);

        // 设置Key过期时间为 1s
        ret = redisService.expire(key, 1);
        assertEquals(ret, 1);
        try {
            TimeUnit.SECONDS.sleep(1L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // Key已过期不存在
        liveTimeSeconds = redisService.ttl(key);
        assertEquals(liveTimeSeconds, -2);
    }

    @Test
    public void zcard() {
        // key 不存在
        int zsetElementNum = redisService.zcard("non_exists_key");
        assertEquals(zsetElementNum, 0L);
    }

    @Test(enabled = false, description = "验证'列表缩容的操作实现'", dependsOnMethods = "zadd")
    public void zremrangeByRank() {
        String key = "zremrangeByRank";

        // 清空缓存数据
        redisService.del(key);

        int size = 3050;
        for (int i = 1; i <= size; i++) {
            // 在未加入"有序集合自动异步缩容"功能前尝试
            redisService.zadd(key, i, Integer.toString(i));
        }

        int elementNum = redisService.zcard(key);
        int maxLength = 3000;
        if (elementNum >= maxLength + 50) {
            int stop = elementNum - maxLength - 1;
            int removedElementNum = redisService.zremrangeByRank(key, 0, stop);
            assertEquals(removedElementNum, 50);
        }
        Set<String> zset = redisService.zrange(key, 0, -1);
        assertEquals(zset.size(), maxLength);
        String[] result = zset.toArray(new String[zset.size()]);
        assertEquals(result[0], "51");
        assertEquals(result[maxLength - 1], "3050");
    }

    /**
     * 通过输出zadd操作中zremrangeByRank操作的运行时间，得知一次删除100条数据的时间在 1ms之内。
     */
    @Test(description = "验证'有序集合在zadd后的长度超过阈值后，会自动进行异步缩容'的功能")
    public void zadd() {
        String key = "zadd";
        int maxLength = 100;

        // 清空缓存数据
        redisService.del(key);

        int size = maxLength + RedisService.LENGTH_THRESHOLD;
        for (int i = 1; i < size; i++) {
            redisService.zadd(key, i, Integer.toString(i), maxLength);
        }
        int elementNum = redisService.zcard(key);
        assertEquals(elementNum, 149);

        redisService.zadd(key, 150, "150", maxLength);
        // 因为插入超过列表长度阈值后，会删除超过长度的元素进行列表缩容
        try {
            TimeUnit.SECONDS.sleep(1L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        elementNum = redisService.zcard(key);
        assertEquals(elementNum, maxLength);
        // 头元素
        Set<String> elements = redisService.zrange(key, 0, 0);
        assertEquals(elements.size(), 1);
        assertEquals(elements.toArray(new String[1])[0], "51");
        // 尾元素
        elements = redisService.zrevrange(key, 0, 0);
        assertEquals(elements.size(), 1);
        assertEquals(elements.toArray(new String[1])[0], "150");

        // 测试批量接口
        int initialCapacity = RedisService.LENGTH_THRESHOLD * 4 / 3 + 1;
        Map<String, Double> scoreMembers = new HashMap<String, Double>(initialCapacity);
        for (int i = size + 1, len = size + RedisService.LENGTH_THRESHOLD; i <= len; i++) {
            scoreMembers.put(Integer.toString(i), new Double(System.currentTimeMillis()));
        }
        int newElementNum = redisService.zadd(key, scoreMembers, maxLength);
        assertEquals(newElementNum, RedisService.LENGTH_THRESHOLD);
        try {
            TimeUnit.SECONDS.sleep(1L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        elementNum = redisService.zcard(key);
        assertEquals(elementNum, maxLength);
        // 头元素
        elements = redisService.zrange(key, 0, 0);
        assertEquals(elements.size(), 1);
        assertEquals(elements.toArray(new String[1])[0], "101");
        // 尾元素
        elements = redisService.zrevrange(key, 0, 0);
        assertEquals(elements.size(), 1);
        assertEquals(elements.toArray(new String[1])[0], "200");
    }

    private static final Logger   logger          = LoggerFactory.getLogger(RedisServiceTest.class);

    /** 异步任务执行器 */
    private final ExecutorService executorService = new ThreadPoolExecutor(30, 10000, 60L, TimeUnit.SECONDS,
                                                                           new LinkedBlockingQueue<Runnable>(50));

    /**
     * <pre>
     * redis.pool.behaviour=LIFO
     * ...
     * redis.min.evictable.idle.time.minutes=5
     * redis.max.evictable.idle.time.minutes=30
     * ...
     * 
     * Redis客户端的连接数，随着请求量的大小而上下波动，属于<b>正常情况</b>。
     * 因为对象池行为使用 LIFO (后进先出，栈) 行为，对象池会重用最热的池对象服务于请求。<br>
     * 这样，当某一时刻的请求总量不大时，过多的空闲时间超过 5分钟的池对象就会被回收，尽量保证对象池都是热点对象。
     * </pre>
     * 
     * <pre>
     * redis.pool.behaviour=FIFO
     * ...
     * redis.min.evictable.idle.time.minutes=5
     * redis.max.evictable.idle.time.minutes=1440
     * ...
     * 
     * Redis客户端的连接数，随着时间的推移只增不减，即使减少也是很小量的，属于<font color="red"><b>不正常情况</b></font>。
     * 因为对象池行为使用 FIFO (先进先出，队列) 行为，对象池内的对象在 5分钟内 都可能被使用过一次，无法被后台"驱逐者线程"释放。<br>
     * 这样，其连接数就与服务 5分钟内的请求总量成正相关。
     * </pre>
     */
    @Test(enabled = false, description = "模拟并发请求，并观察Redis客户端的连接数")
    public void concurrentRequest() throws IOException, InterruptedException {
        String key = "concurrentRequest";
        String value = "Concurrent Request";
        Random rand = new Random(System.currentTimeMillis());

        for (int j = 1; j <= 31; j++) {
            // 模拟并发请求
            int requestNum = Math.abs(rand.nextInt() % 100);
            for (int i = 0; i < requestNum; i++) {
                executorService.submit(new SetRunnable(key, value));
            }

            // 输出已连接的客户端数
            String info = redisService.info(key, "clients");
            BufferedReader reader = new BufferedReader(new StringReader(info));
            String line = null;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("connected_clients")) {
                    logger.info(line);
                }
            }

            logger.info("Complete time: {}, request number: {}", j, requestNum);
            TimeUnit.MINUTES.sleep(1L);
        }
    }

    /**
     * 异步执行 set 命令。
     */
    private class SetRunnable implements Runnable {

        private final String key;
        private final String value;

        public SetRunnable(String key, String value){
            this.key = key;
            this.value = value;
        }

        @Override
        public void run() {
            redisService.set(key, value);
        }

    }

    /**
     * <pre>
     * # Clients
     * connected_clients:65
     * client_longest_output_list:0
     * client_biggest_input_buf:0
     * blocked_clients:0
     * </pre>
     * 
     * @throws IOException
     */
    @Test(enabled = false, description = "查看'INFO命令'的内容格式")
    public void info() throws IOException {
        String key = "info";
        String section = "clients";

        String info = redisService.info(key, section);
        logger.info(info);

        BufferedReader reader = new BufferedReader(new StringReader(info));
        String line = null;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("connected_clients")) {
                logger.info(line);
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
