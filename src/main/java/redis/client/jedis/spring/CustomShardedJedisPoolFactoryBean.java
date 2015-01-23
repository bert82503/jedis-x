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

package redis.client.jedis.spring;

import java.util.concurrent.TimeUnit;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.util.Assert;

import redis.client.jedis.CustomShardedJedisPool;
import redis.client.util.RedisConfigUtils;
import redis.clients.jedis.JedisPoolConfig;

/**
 * 基于Spring工厂Bean({@link FactoryBean})实现的自定义分片Jedis连接池({@link CustomShardedJedisPool})工厂。
 * <p>
 * 属性配置文件各字段含义说明：
 * 
 * <pre>
 * redis.server.list：Redis服务器列表信息，配置格式如下：
 *     redisServers："redisServer[, redisServer ...]"
 *     redisServer："host:port:name[:weight]"
 * redis.timeout.millis：链接套接字的连接超时时间和读取超时时间
 * redis.max.total.num：在给定的时间可以由连接池分配的对象的数量上限
 * redis.max.idle.num：连接池中空闲实例的数量上限
 * redis.min.idle.num：连接池维护的空闲对象的最小数量
 * redis.pool.behaviour：对象池管理池对象的行为（LIFO：堆栈 - 后进先出(last in, first out)；FIFO：队列 - 先进先出(first in, first out)）
 * redis.time.between.eviction.runs.seconds："空闲池对象有效性驱逐检测线程"的调度运行间隔时间
 * redis.num.tests.per.eviction.run："驱逐检测线程"每次运行有效性检测的"空闲池对象"数量
 * redis.min.evictable.idle.time.minutes：池对象的最小可驱逐的空闲时间（当池对象的空闲时间超过该属性值时，就被纳入到驱逐检测对象的范围里）
 * redis.max.evictable.idle.time.minutes：池对象的最大可驱逐的空闲时间（当池对象的空闲时间超过该属性值时，会被立刻驱逐并销毁）
 * redis.block.when.exhausted：是否在池对象耗尽时阻塞借用
 * redis.test.on.borrow：是否在池对象借用时检测
 * redis.test.on.return：是否在池对象返回时检测
 * redis.test.while.idle：是否当池对象空闲时检测
 * redis.server.state.check.time.between.runs.seconds："Redis服务器状态检测"定时任务的运行间隔时间
 * redis.server.state.check.ping.retry.times：PING命令的失败重试次数
 * </pre>
 * 
 * 【配置示例】<br>
 * Properties：
 * 
 * <pre>
 * redis.server.list=127.0.0.1:6379:Shard-01,127.0.0.1:6380:Shard-02,127.0.0.1:6381:Shard-03
 * redis.timeout.millis=100
 * redis.max.total.num=32768
 * redis.max.idle.num=32768
 * redis.min.idle.num=30
 * redis.pool.behaviour=LIFO
 * redis.time.between.eviction.runs.seconds=1
 * redis.num.tests.per.eviction.run=10
 * redis.min.evictable.idle.time.minutes=5
 * redis.max.evictable.idle.time.minutes=30
 * # Internal default configurations (not to change)
 * redis.block.when.exhausted=false
 * redis.test.on.borrow=false
 * redis.test.on.return=false
 * redis.test.while.idle=true
 * redis.server.state.check.time.between.runs.seconds=1
 * redis.server.state.check.ping.retry.times=2
 * </pre>
 * 
 * XML：
 * 
 * <pre>
 * {@literal
 * <bean id="shardedJedisPool" class="redis.client.jedis.spring.CustomShardedJedisPoolFactoryBean">
 * }
 *         &lt;property name="redisServers" value="${redis.server.list}" />
 *         &lt;property name="timeoutMillis" value="${redis.timeout.millis}" />
 *         &lt;property name="maxTotalNum" value="${redis.max.total.num}" />
 *         &lt;property name="maxIdleNum" value="${redis.max.idle.num}" />
 *         &lt;property name="minIdleNum" value="${redis.min.idle.num}" />
 *         &lt;property name="poolBehaviour" value="${redis.pool.behaviour}" />
 *         &lt;property name="timeBetweenEvictionRunsSeconds" value="${redis.time.between.eviction.runs.seconds}" />
 *         &lt;property name="numTestsPerEvictionRun" value="${redis.num.tests.per.eviction.run}" />
 *         &lt;property name="minEvictableIdleTimeMinutes" value="${redis.min.evictable.idle.time.minutes}" />
 *         &lt;property name="maxEvictableIdleTimeMinutes" value="${redis.max.evictable.idle.time.minutes}" />
 *         &lt;property name="blockWhenExhausted" value="${redis.block.when.exhausted}" />
 *         &lt;property name="testOnBorrow" value="${redis.test.on.borrow}" />
 *         &lt;property name="testOnReturn" value="${redis.test.on.return}" />
 *         &lt;property name="testWhileIdle" value="${redis.test.while.idle}" />
 *         &lt;property name="timeBetweenServerStateCheckRunsSeconds" value="${redis.server.state.check.time.between.runs.seconds}" />
 *         &lt;property name="pingRetryTimes" value="${redis.server.state.check.ping.retry.times}" />
 * {@literal
 * </bean>
 * }
 * </pre>
 * 
 * @author huagang.li 2014年12月13日 上午9:25:52
 */
public class CustomShardedJedisPoolFactoryBean implements FactoryBean<CustomShardedJedisPool> {

    /** 对象池的配置信息 */
    private final GenericObjectPoolConfig poolConfig = new JedisPoolConfig();

    /** Redis集群节点列表信息 */
    private String                        redisServers;

    /** 链接套接字的连接超时时间、读取超时时间 */
    private int                           timeoutMillis;

    /** "Redis服务器状态检测"定时任务的运行间隔时间 */
    private int                           timeBetweenServerStateCheckRunsMillis;

    /** Redis PING命令的失败重试次数 */
    private int                           pingRetryTimes;

    @Override
    public CustomShardedJedisPool getObject() throws Exception {
        CustomShardedJedisPool shardedJedisPool = new CustomShardedJedisPool(
                                                                             poolConfig,
                                                                             RedisConfigUtils.parseRedisServerList(redisServers,
                                                                                                                   timeoutMillis),
                                                                             timeBetweenServerStateCheckRunsMillis,
                                                                             pingRetryTimes);
        return shardedJedisPool;
    }

    @Override
    public Class<?> getObjectType() {
        return CustomShardedJedisPool.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    /**
     * 设置Redis集群的节点列表信息。
     * <p>
     *
     * <pre>
     * Redis节点列表的配置格式：
     *     redisServers："redisServer[, redisServer ...]"
     *     redisServer："host:port:name[:weight]"
     * 
     * 示例：
     *     "127.0.0.1:6379:Shard-01,127.0.0.1:6380:Shard-02,127.0.0.1:6381:Shard-03"
     *     "127.0.0.1:6379:Shard-01:1,127.0.0.1:6380:Shard-02:1,127.0.0.1:6381:Shard-03:1"
     * </pre>
     *
     * @param redisServers Redis集群节点列表信息
     */
    public final void setRedisServers(String redisServers) {
        Assert.notNull(redisServers, "'redisServers' property must not be null");

        this.redisServers = redisServers;
    }

    /**
     * 设置链接套接字的连接超时时间和读取超时时间(毫秒数)。
     * 
     * @param timeoutMillis 套接字的连接超时时间和读取超时时间(ms)
     */
    public final void setTimeoutMillis(int timeoutMillis) {
        Assert.isTrue(timeoutMillis > 0, "'timeoutMillis' property must be greater than 0 : " + timeoutMillis);

        this.timeoutMillis = timeoutMillis;
    }

    /**
     * 设置在给定的时间可以由连接池分配的对象的数量上限。
     * <p>
     * 默认值是 8个。
     * 
     * @param maxTotalNum 由连接池同时管理的对象实例的总数上限
     * @see GenericObjectPoolConfig#setMaxTotal(int)
     * @see org.apache.commons.pool2.impl.GenericObjectPool#setMaxTotal(int)
     */
    public final void setMaxTotalNum(int maxTotalNum) {
        Assert.isTrue(maxTotalNum > 0, "'maxTotalNum' property must be greater than 0 : " + maxTotalNum);

        poolConfig.setMaxTotal(maxTotalNum);
    }

    /**
     * 设置连接池中空闲实例的数量上限。
     * <p>
     * 如果在负载较重的系统中{@code maxIdle}被设置得太低，这可能会看到对象被销毁时，立即又有新对象被创建。<br>
     * 这样就会引起"活跃线程返回对象的速度比请求对象还快"，造成空闲对象的数量超过最大空闲对象数量({@code maxIdle})。<br>
     * 负载较重系统的{@code maxIdle}的最佳值会有所不同，但默认值是一个很好的起点。
     * <p>
     * 默认值是 8个，该属性值必须大于{@link #setMinIdleNum(int)}设置的"空闲对象的最小数量"。
     * 
     * @param maxIdleNum 连接池中空闲实例的数量上限
     * @see GenericObjectPoolConfig#setMaxIdle(int)
     * @see org.apache.commons.pool2.impl.GenericObjectPool#setMaxIdle(int)
     */
    public final void setMaxIdleNum(int maxIdleNum) {
        Assert.isTrue(maxIdleNum > 0, "'maxIdleNum' property must be greater than 0 : " + maxIdleNum);

        poolConfig.setMaxIdle(maxIdleNum);
    }

    /**
     * 设置连接池维护的空闲对象的最小数量。
     * <p>
     * 默认值是 0个，即不维护空闲对象。
     * 
     * @param minIdleNum
     * @see GenericObjectPoolConfig#setMinIdle(int)
     * @see org.apache.commons.pool2.impl.GenericObjectPool#setMinIdle(int)
     */
    public final void setMinIdleNum(int minIdleNum) {
        Assert.isTrue(minIdleNum > 0, "'minIdleNum' property must be greater than 0 : " + minIdleNum);

        poolConfig.setMinIdle(minIdleNum);
    }

    /**
     * 对象池管理池对象的行为表示类。
     */
    public static enum PoolBehaviour {
        /** 堆栈 - 后进先出(last in, first out) */
        LIFO,
        /** 队列 - 先进先出(first in, first out) */
        FIFO;
    }

    /**
     * 设置对象池管理池对象的行为。
     * <p>
     * 默认管理池对象的行为是使用"后进先出({@link PoolBehaviour#LIFO})"方式，还可以使用"先进先出( {@link PoolBehaviour#FIFO})"方式。
     * 
     * @param poolBehaviour
     */
    public final void setPoolBehaviour(PoolBehaviour poolBehaviour) {
        switch (poolBehaviour) {
            case LIFO:
                poolConfig.setLifo(true);
                break;
            case FIFO:
                poolConfig.setLifo(false);

            default:
                break;
        }
    }

    /**
     * 设置"空闲池对象有效性驱逐检测线程"的调度运行间隔时间(秒数)。
     * <p>
     * <font color="red"><b>注意：</b></font>该值设置地越小，越能保证异常节点被及时自动摘除！
     * 
     * @param timeBetweenEvictionRunsSeconds
     * @see GenericObjectPoolConfig#setTimeBetweenEvictionRunsMillis(long)
     * @see org.apache.commons.pool2.impl.GenericObjectPool#setTimeBetweenEvictionRunsMillis(long)
     */
    public final void setTimeBetweenEvictionRunsSeconds(long timeBetweenEvictionRunsSeconds) {
        Assert.isTrue(timeBetweenEvictionRunsSeconds > 0,
                      "'timeBetweenEvictionRunsSeconds' property must be greater than 0 : "
                              + timeBetweenEvictionRunsSeconds);

        poolConfig.setTimeBetweenEvictionRunsMillis(TimeUnit.SECONDS.toMillis(timeBetweenEvictionRunsSeconds));
    }

    /**
     * 设置"驱逐检测线程"每次运行有效性检测的"空闲池对象"数量。
     * 
     * @param numTestsPerEvictionRun
     * @see GenericObjectPoolConfig#setNumTestsPerEvictionRun(int)
     * @see org.apache.commons.pool2.impl.GenericObjectPool#setNumTestsPerEvictionRun(int)
     */
    public final void setNumTestsPerEvictionRun(int numTestsPerEvictionRun) {
        Assert.isTrue(numTestsPerEvictionRun > 0, "'numTestsPerEvictionRun' property must be greater than 0 : "
                                                  + numTestsPerEvictionRun);

        poolConfig.setNumTestsPerEvictionRun(numTestsPerEvictionRun);
    }

    /**
     * 设置池对象的最小可驱逐的空闲时间(分钟数)。
     * <p>
     * 默认值是 30分钟。
     * <p>
     * 当池对象的空闲时间超过该属性值时，就被纳入到驱逐检测对象的范围里。
     * 
     * @param minEvictableIdleTimeMinutes
     * @see GenericObjectPoolConfig#setSoftMinEvictableIdleTimeMillis(long)
     * @see org.apache.commons.pool2.impl.GenericObjectPool#setSoftMinEvictableIdleTimeMillis(long)
     */
    public final void setMinEvictableIdleTimeMinutes(long minEvictableIdleTimeMinutes) {
        Assert.isTrue(minEvictableIdleTimeMinutes > 0,
                      "'minEvictableIdleTimeMinutes' property must be greater than 0 : " + minEvictableIdleTimeMinutes);

        poolConfig.setSoftMinEvictableIdleTimeMillis(TimeUnit.MINUTES.toMillis(minEvictableIdleTimeMinutes));
    }

    /**
     * 设置池对象的最大可驱逐的空闲时间(分钟数)。
     * <p>
     * 默认值是 30分钟，该属性值必须大于{@link #setMinEvictableIdleTimeMinutes(long)}设置的"最小可驱逐的空闲时间"。
     * <p>
     * 当池对象的空闲时间超过该属性值时，会被立刻驱逐并销毁。
     * 
     * @param maxEvictableIdleTimeMinutes
     * @see GenericObjectPoolConfig#setMinEvictableIdleTimeMillis(long)
     * @see org.apache.commons.pool2.impl.GenericObjectPool#setMinEvictableIdleTimeMillis(long)
     */
    public final void setMaxEvictableIdleTimeMinutes(long maxEvictableIdleTimeMinutes) {
        Assert.isTrue(maxEvictableIdleTimeMinutes > 0,
                      "'maxEvictableIdleTimeMinutes' property must be greater than 0 : " + maxEvictableIdleTimeMinutes);

        poolConfig.setMinEvictableIdleTimeMillis(TimeUnit.MINUTES.toMillis(maxEvictableIdleTimeMinutes));
    }

    // ------------------ 内部默认配置属性(不可随意更改) ------------------
    /**
     * 设置"在池对象耗尽时，借用池对象的方法(ObjectPool#borrowObject())调用"是非阻塞的。
     * 
     * @param blockWhenExhausted 是否在池对象耗尽时阻塞借用(false)
     */
    public final void setBlockWhenExhausted(boolean blockWhenExhausted) {
        poolConfig.setBlockWhenExhausted(blockWhenExhausted);
    }

    /**
     * 关闭"在借用池对象时，检测其有效性"。（因为这样对性能影响较大）
     * 
     * @param testOnBorrow 是否在池对象借用时检测(false)
     */
    public final void setTestOnBorrow(boolean testOnBorrow) {
        poolConfig.setTestOnBorrow(testOnBorrow);
    }

    /**
     * 关闭"在返回池对象时，检测其有效性"。（因为这样对性能影响较大）
     * 
     * @param testOnReturn 是否在池对象返回时检测(false)
     */
    public final void setTestOnReturn(boolean testOnReturn) {
        poolConfig.setTestOnReturn(testOnReturn);
    }

    /**
     * "Evictor驱逐者守护线程"的相关配置，用它来检测"空闲对象"的有效性。
     * 
     * @param testWhileIdle 是否当池对象空闲时检测(true)
     */
    public final void setTestWhileIdle(boolean testWhileIdle) {
        poolConfig.setTestWhileIdle(testWhileIdle);
    }

    /**
     * 设置"Redis服务器状态检测"定时任务的运行间隔时间。
     * 
     * @param timeBetweenServerStateCheckRunsSeconds "Redis服务器状态检测"定时任务的运行间隔时间(1)
     */
    public final void setTimeBetweenServerStateCheckRunsSeconds(int timeBetweenServerStateCheckRunsSeconds) {
        timeBetweenServerStateCheckRunsMillis = (int) TimeUnit.SECONDS.toMillis(timeBetweenServerStateCheckRunsSeconds);
    }

    /**
     * 设置Redis PING命令的失败重试次数。
     * 
     * @param pingRetryTimes PING命令的失败重试次数(2)
     */
    public final void setPingRetryTimes(int pingRetryTimes) {
        this.pingRetryTimes = pingRetryTimes;
    }

}
