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

package redis.client.jedis;

import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.ShardedJedis;
import redis.clients.util.Hashing;
import redis.clients.util.Pool;

/**
 * "数据分片的Jedis连接池"自定义实现，继承自{@link Pool<ShardedJedis>}。
 * 
 * @author huagang.li 2014年12月8日 下午6:06:09
 */
public class CustomShardedJedisPool extends Pool<ShardedJedis> {

    /**
     * 创建一个"数据分片的Jedis连接池"实例。
     * 
     * @param poolConfig 连接池配置信息
     * @param shards Jedis节点分片信息列表
     * @param timeBetweenServerStateCheckRunsMillis "Redis服务器状态检测"定时任务的运行间隔时间
     * @param pingRetryTimes Redis PING命令的失败重试次数
     */
    public CustomShardedJedisPool(GenericObjectPoolConfig poolConfig, List<JedisShardInfo> shards,
                                  int timeBetweenServerStateCheckRunsMillis, int pingRetryTimes){
        this(poolConfig, shards, Hashing.MURMUR_HASH, timeBetweenServerStateCheckRunsMillis, pingRetryTimes);
    }

    public CustomShardedJedisPool(GenericObjectPoolConfig poolConfig, List<JedisShardInfo> shards, Hashing algo,
                                  int timeBetweenServerStateCheckRunsMillis, int pingRetryTimes){
        this(poolConfig, shards, algo, null, timeBetweenServerStateCheckRunsMillis, pingRetryTimes);
    }

    public CustomShardedJedisPool(GenericObjectPoolConfig poolConfig, List<JedisShardInfo> shards,
                                  Pattern keyTagPattern, int timeBetweenServerStateCheckRunsMillis, int pingRetryTimes){
        this(poolConfig, shards, Hashing.MURMUR_HASH, keyTagPattern, timeBetweenServerStateCheckRunsMillis,
             pingRetryTimes);
    }

    /**
     * 创建一个"数据分片的Jedis连接池"实例，使用自定义实现的{@link CustomShardedJedisFactory}。
     * 
     * @param poolConfig 连接池配置信息
     * @param shards Jedis节点分片信息列表
     * @param algo 哈希算法
     * @param keyTagPattern 键标记模式
     * @param timeBetweenServerStateCheckRunsMillis "Redis服务器状态检测"定时任务的运行间隔时间
     * @param pingRetryTimes Redis PING命令的失败重试次数
     */
    public CustomShardedJedisPool(GenericObjectPoolConfig poolConfig, List<JedisShardInfo> shards, Hashing algo,
                                  Pattern keyTagPattern, int timeBetweenServerStateCheckRunsMillis, int pingRetryTimes){
        super(poolConfig, new CustomShardedJedisFactory(shards, algo, keyTagPattern,
                                                        timeBetweenServerStateCheckRunsMillis, pingRetryTimes));
    }

    /**
     * 创建一个"数据分片的Jedis连接池"实例，使用自定义实现的{@link PooledObjectFactory}。
     * 
     * @param poolConfig 连接池配置信息
     * @param factory 池对象工厂
     */
    public CustomShardedJedisPool(GenericObjectPoolConfig poolConfig, PooledObjectFactory<ShardedJedis> factory){
        super(poolConfig, factory);
    }

    /**
     * 获取"Jedis连接池"中的一个{@link ShardedJedis}资源。
     * 
     * <pre>
     * 分2个步骤：
     *  1. 从Pool<ShardedJedis>中获取一个{@link ShardedJedis}资源；
     *  2. 设置{@link ShardedJedis}资源所在的连接池数据源。
     * </pre>
     * 
     * 可能抛出"Could not get a resource from the pool"的{@link redis.clients.jedis.exceptions.JedisConnectionException
     * JedisConnectionException}异常。
     */
    @Override
    public ShardedJedis getResource() {
        ShardedJedis jedis = super.getResource();
        jedis.setDataSource(this);
        return jedis;
    }

    /**
     * 将正常的{@link ShardedJedis}资源返回给"连接池"。
     * <p>
     * 可能抛出"Could not return the resource to the pool"的{@link redis.clients.jedis.exceptions.JedisException
     * JedisException}异常。
     */
    @Override
    public void returnResource(ShardedJedis jedis) {
        if (jedis != null) {
            jedis.resetState();
            this.returnResourceObject(jedis);
        }
    }

    /**
     * 将出现异常的{@link ShardedJedis}资源返回给"连接池"。
     * <p>
     * 可能抛出"Could not return the broken resource to the pool"的{@link redis.clients.jedis.exceptions.JedisException
     * JedisException}异常。
     */
    @Override
    public void returnBrokenResource(ShardedJedis jedis) {
        if (jedis != null) {
            this.returnBrokenResourceObject(jedis);
        }
    }

}
