/*
 * Copyright 2014 FraudMetrix.cn All right reserved. This software is the
 * confidential and proprietary information of FraudMetrix.cn ("Confidential
 * Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered
 * into with FraudMetrix.cn.
 */
package cache.migration.service;

import cache.service.MemcachedService;

/**
 * 缓存迁移服务，继承自{@link MemcachedService}。
 * <p>
 * 缓存迁移方案：双写Memcached和Redis，读取通过开关从Memcached切换到Redis，最后固化Redis读取开关。
 * <p>
 * 【XML配置示例】
 * 
 * <pre>
 * {@literal
 * <bean id="cacheMigrationService" class="cache.migration.service.impl.CacheMigrationServiceImpl">
 * }
 *    &lt;property name="readRedisEnabled" value="${cache.migration.read.redis.enabled}" />
 * {@literal
 * </bean>
 * }
 * </pre>
 * 
 * @author huagang.li 2014年12月19日 下午7:27:33
 */
public interface CacheMigrationService extends MemcachedService {

    /**
     * 设置Redis服务适配器。
     * 
     * @param redisServiceAdapter
     */
    void setRedisServiceAdapter(RedisServiceAdapter redisServiceAdapter);

    /**
     * 设置Memcached服务。
     * 
     * @param memcachedService
     */
    void setMemcachedService(MemcachedService memcachedService);

    /**
     * 设置"是否从Redis服务读取数据"开关。
     * 
     * @param enabled {@code true}：从Redis服务读取数据；{@code false}：从Memcached服务读取数据
     */
    void setReadRedisEnabled(boolean readRedisEnabled);

    /**
     * 获取"是否从Redis服务读取数据"开关的状态。
     * 
     * @return
     */
    boolean getReadRedisEnabled();

}
