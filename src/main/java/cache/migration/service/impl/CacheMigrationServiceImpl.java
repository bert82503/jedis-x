/*
 * Copyright 2014 FraudMetrix.cn All right reserved. This software is the
 * confidential and proprietary information of FraudMetrix.cn ("Confidential
 * Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered
 * into with FraudMetrix.cn.
 */
package cache.migration.service.impl;

import java.util.concurrent.Future;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;

import cache.migration.service.CacheMigrationService;
import cache.migration.service.RedisServiceAdapter;
import cache.service.MemcachedService;

/**
 * "缓存迁移服务"实现，继承自{@link CacheMigrationService}。
 * 
 * @author huagang.li 2014年12月19日 下午7:33:20
 */
@Resource
public class CacheMigrationServiceImpl implements CacheMigrationService {

    /** Redis服务适配器 */
    @Autowired
    private RedisServiceAdapter redisServiceAdapter;

    /** Memcached服务 */
    @Autowired
    private MemcachedService    memcachedService;

    /** 数据从Redis服务读取开关 */
    private boolean             readRedisEnabled;

    @Override
    public void setRedisServiceAdapter(RedisServiceAdapter redisServiceAdapter) {
        this.redisServiceAdapter = redisServiceAdapter;
    }

    @Override
    public void setMemcachedService(MemcachedService memcachedService) {
        this.memcachedService = memcachedService;
    }

    @Override
    public void setEnabled(boolean enabled) {
        // 缓存迁移不用开关功能，由底层服务自身来控制
    }

    @Override
    public void setReadRedisEnabled(boolean readRedisEnabled) {
        this.readRedisEnabled = readRedisEnabled;
    }

    @Override
    public boolean getReadRedisEnabled() {
        return readRedisEnabled;
    }

    @Override
    public void close() {
        redisServiceAdapter.close();
        memcachedService.close();
    }

    /**
     * 因为Redis服务不支持该操作，所以这里也不做支持。
     */
    @Override
    public Future<Boolean> set(String key, int exp, Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Future<Boolean> set(String key, int exp, String value) {
        redisServiceAdapter.set(key, exp, value);
        return memcachedService.set(key, exp, value);
    }

    @Override
    public Object get(String key) {
        if (readRedisEnabled) {
            return redisServiceAdapter.get(key);
        } else {
            return memcachedService.get(key);
        }
    }

    @Override
    public String getString(String key) {
        if (readRedisEnabled) {
            return redisServiceAdapter.getString(key);
        } else {
            return memcachedService.getString(key);
        }
    }

    @Override
    public Future<Object> getAsync(String key) {
        if (readRedisEnabled) {
            return redisServiceAdapter.getAsync(key);
        } else {
            return memcachedService.getAsync(key);
        }
    }

    @Override
    public Future<Boolean> append(String key, int exp, String value) {
        redisServiceAdapter.append(key, exp, value);
        return memcachedService.append(key, exp, value);
    }

    @Override
    public Future<Boolean> delete(String key) {
        redisServiceAdapter.delete(key);
        return memcachedService.delete(key);
    }

}
