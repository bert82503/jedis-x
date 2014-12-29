/*
 * Copyright 2014 FraudMetrix.cn All right reserved. This software is the
 * confidential and proprietary information of FraudMetrix.cn ("Confidential
 * Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered
 * into with FraudMetrix.cn.
 */
package cache.migration.service;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import redis.client.util.CacheUtils;
import cache.migration.service.impl.CacheMigrationServiceImpl;
import cache.service.impl.JedisServiceImpl;
import cache.service.impl.MemcachedServiceImpl;

/**
 * Test for {@link CacheMigrationService}.
 * 
 * @author huagang.li 2014年12月29日 下午2:15:47
 */
public class CacheMigrationServiceTest {

    private CacheMigrationService cacheMigrationService;

    @BeforeClass
    public void init() throws Exception {
        cacheMigrationService = new CacheMigrationServiceImpl();
        cacheMigrationService.setReadRedisEnabled(false);

        RedisServiceAdapter redisServiceAdapter = new RedisServiceAdapter();
        JedisServiceImpl jedisServiceImpl = new JedisServiceImpl();
        jedisServiceImpl.setShardedJedisPool(CacheUtils.getShardedJedisPool());
        jedisServiceImpl.setEnabled(true);
        redisServiceAdapter.setRedisService(jedisServiceImpl);
        cacheMigrationService.setRedisServiceAdapter(redisServiceAdapter);

        MemcachedServiceImpl memcachedServiceImpl = new MemcachedServiceImpl();
        memcachedServiceImpl.setMemcachedClient(CacheUtils.getMemcachedClient());
        memcachedServiceImpl.setEnabled(true);
        cacheMigrationService.setMemcachedService(memcachedServiceImpl);
    }

    /** 7天 */
    private static final int TIME_7_DAY = (int) TimeUnit.DAYS.toSeconds(7L);

    @Test(description = "验证'缓存迁移的双写功能'")
    public void doubleWrite() throws InterruptedException, ExecutionException {
        this.doubleSet("key:double:write", 0, "double write"); // item 永不过期
        this.doubleDelete("key:double:write");

        this.doubleSet("key:double:write", TIME_7_DAY, "double write"); // 相对当前时间，过期时间为7天
        this.doubleDelete("key:double:write");
    }

    /**
     * 双更新(set)缓存数据。
     */
    private void doubleSet(String key, int timeoutSeconds, String value) throws InterruptedException,
                                                                        ExecutionException {
        // set
        Future<Boolean> ret = cacheMigrationService.set(key, timeoutSeconds, value);
        assertTrue(ret.get().booleanValue());
        // 验证双写功能(get)
        // 从Memcached读取数据
        cacheMigrationService.setReadRedisEnabled(false);
        String val = cacheMigrationService.getString(key);
        assertEquals(val, value);
        // 从Redis读取数据
        cacheMigrationService.setReadRedisEnabled(true);
        val = cacheMigrationService.getString(key);
        assertEquals(val, value);
    }

    /**
     * 双删除(delete)缓存数据。
     */
    private void doubleDelete(String key) throws InterruptedException, ExecutionException {
        // delete
        Future<Boolean> ret = cacheMigrationService.delete(key);
        assertTrue(ret.get().booleanValue());
        // 验证双写功能(get)
        // 从Memcached读取数据
        cacheMigrationService.setReadRedisEnabled(false);
        String val = cacheMigrationService.getString(key);
        assertEquals(val, null);
        // 从Redis读取数据
        cacheMigrationService.setReadRedisEnabled(true);
        val = cacheMigrationService.getString(key);
        assertEquals(val, null);
    }

    @AfterClass
    public void destroy() {
        if (cacheMigrationService != null) {
            cacheMigrationService.close();
        }
    }

}
