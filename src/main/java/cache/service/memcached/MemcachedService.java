/*
 * Copyright 2014 FraudMetrix.cn All right reserved. This software is the
 * confidential and proprietary information of FraudMetrix.cn ("Confidential
 * Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered
 * into with FraudMetrix.cn.
 */
package cache.service.memcached;

import java.util.concurrent.Future;

import cache.service.ShardedService;

/**
 * 封装<a href="http://memcached.org">Memcached</a>客户端，并增加开关功能。
 * 
 * @author huagang.li 2014年12月27日 下午5:12:15
 */
public interface MemcachedService extends ShardedService {

    Future<Boolean> set(String key, int exp, Object value);

    Future<Boolean> set(String key, int exp, String value);

    Object get(String key);

    String getString(String key);

    Future<Object> getAsync(String key);

    Future<Boolean> append(String key, int exp, String value);

    Future<Boolean> delete(String key);

}
