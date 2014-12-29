package cache.service;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.Random;
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
        set("foo", 0, "bar"); // item 永不过期
        // delete
        delete("foo");

        set("foo", TIME_7_DAY, "bar"); // 相对当前时间，过期时间为7天
        set("foo", TIME_30_DAY, "bar"); // 相对当前时间，过期时间为30天
        delete("foo");

        // 相对当前时间，过期时间为-1秒
        // 插入是成功的
        // 对于"二进制协议"
        // 插入后，item 不过期，是有效的
        // 对于"文本协议"
        // 但插入后，item 就过期了，因为其过期时间是插入操作的前1秒钟
        set("foo", -1, "bar");

        // 过期时间被设置为大于30天，实际的过期时间是系统的绝对时间
        // 插入是成功的
        // 但插入后，item 就过期了，因为其过期时间是操作系统相对(January 1, 1970)的时间，系统取出的当前时间肯定比它大
        set("foo", TIME_30_DAY + 1, "bar");
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

    private static final String ONLINE_EVENT_CONTENT = "{\"accountLogin\":\"181380\",\"create\":1414774385000,\"deliverAddressStreet\":\"浙江省|金华市|婺城区|宾虹路|865号\","
                                                       + "\"eventId\":\"trade\",\"eventType\":\"Trade\",\"ext_IMEI\":\"99000522667636\",\"ipAddress\":\"115.210.9.165\",\"location\":\"金华市\","
                                                       + "\"payeeUserid\":\"hpayZZT@w13758984588\",\"tradingAmount\":21400}";

    @Test(enabled = false, description = "性能测试")
    public void benchmark() {
        for (int j = 1; j <= 7; j++) {
            String key = "zset:global.smartId.smartdev123";

            Random random = new Random(System.currentTimeMillis());
            // 预热缓存数据
            int sampleNum = 3000;
            int capacity = (ONLINE_EVENT_CONTENT.length() + 20) * sampleNum;
            StringBuilder buffer = new StringBuilder(capacity);
            for (int i = 0; i < sampleNum; i++) {
                String member = ONLINE_EVENT_CONTENT + random.nextLong();
                buffer.append(member).append(';');
            }
            memcachedService.set(key, TIME_7_DAY, buffer.toString());

            // 测试有序集合较长情况下，zadd 的性能
            int totalTime = 0;
            int size = 3;
            for (int i = 0; i < size; i++) {
                long startTime = System.currentTimeMillis();
                String newValue = ONLINE_EVENT_CONTENT + random.nextLong();
                String originalValue = memcachedService.getString(key);
                memcachedService.set(key, TIME_7_DAY, originalValue + newValue + ';');
                long runTime = System.currentTimeMillis() - startTime;
                totalTime += runTime;
                logger.info("Time of 'append': {}", runTime);
            }
            logger.info("Total time of 'append': {}", totalTime);

            long startTime = System.currentTimeMillis();
            String val = memcachedService.getString(key);
            long runTime = System.currentTimeMillis() - startTime;
            logger.info("Time of 'get': {}, size of value: {}, length of value: {}", runTime, sampleNum + size,
                        val.length());

            // 清空缓存数据
            memcachedService.delete(key);

            logger.info("Complete time: {}\n", Integer.valueOf(j));
        }
    }

    @AfterClass
    public void destroy() {
        if (memcachedService != null) {
            memcachedService.close();
        }
    }

}
