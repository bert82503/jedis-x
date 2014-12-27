/*
 * Copyright 2014 FraudMetrix.cn All right reserved. This software is the
 * confidential and proprietary information of FraudMetrix.cn ("Confidential
 * Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered
 * into with FraudMetrix.cn.
 */
package cache.service.memcached.impl;

import java.util.concurrent.Future;

import net.spy.memcached.MemcachedClient;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import cache.service.memcached.MemcachedService;

/**
 * 实现Memcached服务。
 * 
 * @author huagang.li 2014年12月27日 下午5:39:40
 */
public class MemcachedServiceImpl implements MemcachedService {

    private final static Logger logger = LoggerFactory.getLogger(MemcachedServiceImpl.class);

    /** Memcached客户端 */
    @Autowired
    private MemcachedClient     memcachedClient;

    /** Memcached服务启用标识 */
    private boolean             enabled;

    public void setMemcachedClient(MemcachedClient memcachedClient) {
        this.memcachedClient = memcachedClient;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void close() {
        // Memcached 客户端不需要使用方显示关闭
    }

    @Override
    public Future<Boolean> set(String key, int exp, Object value) {
        if (enabled && StringUtils.isNotBlank(key) && value != null) {
            key = trim(key);
            try {
                return memcachedClient.set(key, exp, value);
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }
        return null;
    }

    @Override
    public Future<Boolean> set(String key, int exp, String value) {
        if (enabled && StringUtils.isNotBlank(key) && value != null) {
            key = trim(key);
            try {
                return memcachedClient.set(key, exp, value);
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }
        return null;
    }

    @Override
    public Object get(String key) {
        if (enabled && StringUtils.isNotBlank(key)) {
            key = trim(key);
            try {
                return memcachedClient.get(key);
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }

        return null;
    }

    @Override
    public String getString(String key) {
        if (enabled && StringUtils.isNotBlank(key)) {
            key = trim(key);
            try {
                Object obj = memcachedClient.get(key);
                if (obj != null) {
                    return (String) obj;
                }
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }
        return null;
    }

    @Override
    public Future<Object> getAsync(String key) {
        if (enabled && StringUtils.isNotBlank(key)) {
            key = trim(key);
            try {
                return memcachedClient.asyncGet(key);
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }
        return null;
    }

    @Override
    public Future<Boolean> append(String key, int exp, String value) {
        if (enabled && StringUtils.isNotBlank(key) && value != null) {
            key = trim(key);
            try {
                String origin = getString(key);
                String newValue = origin == null ? value : (origin + value);
                return memcachedClient.set(key, exp, newValue);
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }
        return null;
    }

    @Override
    public Future<Boolean> delete(String key) {
        if (enabled && StringUtils.isNotBlank(key)) {
            key = trim(key);
            try {
                return memcachedClient.delete(key);
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }

        return null;
    }

    /**
     * 过滤除英文字母、数字、中文、点号(.)、中横线(-)和下划线(_)以外的所有字符。
     * <p>
     * 因为Memcached的键(key)不能包含空格等字符。
     * 
     * @param key
     * @return
     */
    private final static String trim(String key) {
        return key.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5\\.\\-\\_]", "");
    }

}
