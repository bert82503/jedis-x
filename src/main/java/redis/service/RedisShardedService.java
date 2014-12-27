/*
 * Copyright 2014 FraudMetrix.cn All right reserved. This software is the
 * confidential and proprietary information of FraudMetrix.cn ("Confidential
 * Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered
 * into with FraudMetrix.cn.
 */
package redis.service;

import java.io.Closeable;

/**
 * Redis数据分片服务定义，增加开关功能和资源关闭功能。
 * <p>
 * 服务继承{@link Closeable}接口，以便使用JDK 7的 <a
 * href="http://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html">try-with-resources</a>
 * 语法，同时允许Spring容器能在关闭时释放其持有的资源。
 * 
 * @author huagang.li 2014年12月19日 上午10:42:30
 */
public interface RedisShardedService extends Closeable {

    /**
     * 设置是否启用Redis服务。
     * 
     * @param enabled {@code true}：启用Redis服务；{@code false}：关闭Redis服务
     */
    void setEnabled(boolean enabled);

    /**
     * 关闭Redis连接池中所有客户端的链接。
     * <p>
     * {@inheritDoc}
     */
    @Override
    void close();

}
