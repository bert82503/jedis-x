package cache.service;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import redis.client.util.CacheUtils;
import cache.service.impl.MemcachedServiceImpl;

/**
 * Test for {@link MemcachedService}.
 * 
 * @author huagang.li 2014年12月29日 下午2:47:49
 */
public class MemcachedServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(MemcachedServiceTest.class);

    private MemcachedService    memcachedService;

    @BeforeClass
    public void init() throws Exception {
        MemcachedServiceImpl memcachedServiceImpl = new MemcachedServiceImpl();
        memcachedServiceImpl.setMemcachedClient(CacheUtils.getMemcachedClient());
        memcachedServiceImpl.setEnabled(true);

        memcachedService = memcachedServiceImpl;
    }

    /** 7天 */
    private static final int TIME_7_DAY  = (int) TimeUnit.DAYS.toSeconds(7L);
    /** 30天 */
    private static final int TIME_30_DAY = (int) TimeUnit.DAYS.toSeconds(30L);

    @Test(description = "验证 get、set、delete 命令")
    public void getAndSetAndDelete() throws InterruptedException, ExecutionException {
        // set
        this.set("foo", 0, "bar"); // item 永不过期
        // delete
        this.delete("foo");

        this.set("foo", TIME_7_DAY, "bar"); // 相对当前时间，过期时间为7天
        this.set("foo", TIME_30_DAY, "bar"); // 相对当前时间，过期时间为30天
        this.delete("foo");

        // 相对当前时间，过期时间为-1秒
        // 插入是成功的
        // 对于"二进制协议"
        // 插入后，item 不过期，是有效的
        // 对于"文本协议"
        // 但插入后，item 就过期了，因为其过期时间是插入操作的前1秒钟
        this.set("foo", -1, "bar");

        // 过期时间被设置为大于30天，实际的过期时间是系统的绝对时间
        // 插入是成功的
        // 但插入后，item 就过期了，因为其过期时间是操作系统相对(January 1, 1970)的时间，系统取出的当前时间肯定比它大
        this.set("foo", TIME_30_DAY + 1, "bar");
    }

    /**
     * 更新(set)缓存数据及其过期时间。
     */
    private void set(String key, int timeoutSeconds, String value) throws InterruptedException, ExecutionException {
        // set
        Future<Boolean> ret = memcachedService.set(key, timeoutSeconds, value);
        assertTrue(ret.get().booleanValue()); // 成功更新
        // get
        String val = memcachedService.getString("foo");
        // 0 < expire time <= 30天，0 表示永不过期
        // if (timeoutSeconds >= 0 && timeoutSeconds <= TIME_30_DAY) { // 文本协议
        if (timeoutSeconds <= TIME_30_DAY) { // 二进制协议
            assertEquals(val, "bar");
        } else {
            assertEquals(val, null);
        }
    }

    /**
     * 删除(delete)缓存数据。
     */
    private void delete(String key) throws InterruptedException, ExecutionException {
        // delete
        Future<Boolean> ret = memcachedService.delete("foo");
        assertTrue(ret.get().booleanValue());
        // get
        String val = memcachedService.getString("foo");
        assertEquals(val, null);
    }

    /**
     * 客户端使用二进制协议(Binary Protocol)存储数据时，照样可以在命令行模式下使用文本形式的键获取。
     * 
     * <pre>
     * telnet localhost 11211
     * 
     * get protocol_binary
     * VALUE protocol_binary 0 11
     * Binary Data
     * END
     * </pre>
     */
    @Test(description = "验证二进制协议(Binary Protocol)的数据存储方式")
    public void useBinaryProtocol() {
        String key = "protocol_binary";
        String val = "Binary Data";
        memcachedService.set(key, TIME_7_DAY, val);
        assertEquals(memcachedService.get(key), val);
    }

    /**
     * <pre>
     * 启动Memcached服务
     *      /usr/apps/memcached/bin/memcached -p 11211 -B auto -m 1 -n 48 -f 1.25 -c 1024 -C -vv
     *      
     *      slab class   1: chunk size        96 perslab   10922
     *      slab class   2: chunk size       120 perslab    8738
     *      slab class   3: chunk size       152 perslab    6898
     *      slab class   4: chunk size       192 perslab    5461
     * 查看 Item 分布
     *      stats items
     *      
     *      STAT items:1:number 10922
     *      STAT items:1:age 74
     *      STAT items:1:evicted 19078
     *      STAT items:1:evicted_nonzero 19078
     *      STAT items:1:evicted_time 29
     *      STAT items:1:outofmemory 0
     *      STAT items:1:tailrepairs 0
     *      STAT items:1:reclaimed 0
     *      STAT items:1:expired_unfetched 0
     *      STAT items:1:evicted_unfetched 1078
     *      STAT items:1:crawler_reclaimed 0
     *      STAT items:1:lrutail_reflocked 0
     * 其中，number属性表示该"STAT items"可以保存的Item总量。
     * </pre>
     * 
     * @throws InterruptedException
     * @throws ExecutionException
     */
    @Test(enabled = false, description = "测试Memcached的LRU删除机制")
    public void lruDelete() throws InterruptedException, ExecutionException {
        String keyPrefix = "global.account.";
        for (int i = 0, size = 1000; i < size; i++) {
            Future<Boolean> ret = memcachedService.set(keyPrefix + i, TIME_7_DAY, "zhangsan");
            if (!ret.get().booleanValue()) {
                logger.warn("Set key failed: {}", keyPrefix + i);
            }
        }

        for (int i = 2000, size = 12000; i < size; i++) {
            String key = keyPrefix + i;
            Future<Boolean> ret = memcachedService.set(key, TIME_7_DAY, "zhangsan");
            if (!ret.get().booleanValue()) {
                logger.warn("Set key failed: {}", keyPrefix + i);
            }
            memcachedService.getString(key);
            memcachedService.getString(key);
        }

        int unhitCounter = 0;
        for (int i = 0, size = 1000; i < size; i++) {
            String val = memcachedService.getString(keyPrefix + i);
            if (val == null) {
                unhitCounter += 1;
            }
        }
        logger.debug("Number of un-hit key: {}", Integer.valueOf(unhitCounter));
    }

    @AfterClass
    public void destroy() {
        if (null != memcachedService) {
            memcachedService.close();
        }
    }

}
