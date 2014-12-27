/*
 * Copyright 2014 FraudMetrix.cn All right reserved. This software is the
 * confidential and proprietary information of FraudMetrix.cn ("Confidential
 * Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered
 * into with FraudMetrix.cn.
 */
package cache.migration.service;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * 简单实现{@link Future}接口，仅用于传递返回结果，为了兼容{@link cache.service.memcached.MemcachedService MemcachedService}接口的返回结果。
 * 
 * @author huagang.li 2014年12月17日 上午11:38:17
 */
public class RedisFuture<V> implements Future<V> {

    private V value;

    public RedisFuture(V value){
        this.value = value;
    }

    @Override
    public V get() {
        return value;
    }

    @Override
    public V get(long timeout, TimeUnit unit) {
        return value;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return true;
    }

}
