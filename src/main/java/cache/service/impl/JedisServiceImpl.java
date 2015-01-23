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

package cache.service.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import redis.client.jedis.CustomShardedJedisPool;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Response;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPipeline;
import redis.clients.jedis.exceptions.JedisException;
import cache.service.RedisService;

/**
 * "数据分片的Jedis连接池"服务实现，继承自{@link RedisService}。
 * 
 * @author huagang.li 2014年12月12日 下午4:59:38
 */
@Resource
public class JedisServiceImpl implements RedisService {

    private static final Logger    logger          = LoggerFactory.getLogger(JedisServiceImpl.class);

    /** Redis连接池 */
    @Autowired
    private CustomShardedJedisPool shardedJedisPool;

    /** Redis服务启用标识 */
    private boolean                enabled;

    /** 异步任务执行器 */
    private final ExecutorService  executorService = new ThreadPoolExecutor(30, 10000, 60L, TimeUnit.SECONDS,
                                                                            new LinkedBlockingQueue<Runnable>(50));

    /**
     * 用于单元测试(UT, Unit Test)。
     * 
     * @param shardedJedisPool
     */
    public void setShardedJedisPool(CustomShardedJedisPool shardedJedisPool) {
        this.shardedJedisPool = shardedJedisPool;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean getEnabled() {
        return enabled;
    }

    /**
     * 关闭Redis连接池中所有客户端的链接。
     */
    @Override
    public void close() {
        if (shardedJedisPool != null) {
            shardedJedisPool.close();
        }
    }

    // ----------------------- Redis Command -----------------------
    /**
     * 将使用完成的"分片Jedis池对象"返回给"对象池"。
     * 
     * @param jedis
     */
    private static void close(ShardedJedis jedis) {
        if (jedis != null) {
            try {
                jedis.close();
            } catch (JedisException e) {
                logger.error("ShardedJedis close fail", e);
            }
        }
    }

    // ---------------- Key (键) ----------------
    @Override
    public int expire(String key, int seconds) {
        if (enabled && StringUtils.isNotEmpty(key)) {
            ShardedJedis jedis = null;
            try {
                jedis = shardedJedisPool.getResource();
                int ret = jedis.expire(key, seconds).intValue();
                return ret;
            } catch (JedisException e) {
                logger.error("'expire' key fail, key: {}, seconds: {}", key, seconds);
                logger.error(e.getMessage(), e);
            } finally {
                close(jedis);
            }
        }
        return 0;
    }

    @Override
    public long ttl(String key) {
        if (enabled && StringUtils.isNotEmpty(key)) {
            ShardedJedis jedis = null;
            try {
                jedis = shardedJedisPool.getResource();
                long liveTimeSeconds = jedis.ttl(key).longValue();
                return liveTimeSeconds;
            } catch (JedisException e) {
                logger.error("'ttl' key fail, key: {}", key);
                logger.error(e.getMessage(), e);
            } finally {
                close(jedis);
            }
        }
        return -2L;
    }

    @Override
    public int del(String key) {
        if (enabled && StringUtils.isNotEmpty(key)) {
            ShardedJedis jedis = null;
            try {
                jedis = shardedJedisPool.getResource();
                int removedKeyNum = jedis.del(key).intValue();
                return removedKeyNum;
            } catch (JedisException e) {
                logger.error("'del' key fail, key: {}", key);
                logger.error(e.getMessage(), e);
            } finally {
                close(jedis);
            }
        }
        return 0;
    }

    // ---------------- String (字符串) ----------------
    @Override
    public String get(String key) {
        if (enabled && StringUtils.isNotEmpty(key)) {
            ShardedJedis jedis = null;
            try {
                jedis = shardedJedisPool.getResource();
                String value = jedis.get(key);
                return value;
            } catch (JedisException e) {
                logger.error("'get' key fail, key: {}", key);
                logger.error(e.getMessage(), e);
            } finally {
                close(jedis);
            }
        }
        return null;
    }

    @Override
    public String set(String key, String value) {
        if (enabled && StringUtils.isNotEmpty(key)) {
            ShardedJedis jedis = null;
            try {
                jedis = shardedJedisPool.getResource();
                String ret = jedis.set(key, value);
                return ret;
            } catch (JedisException e) {
                logger.error("'set' key fail, key: {}, value: {}", key, value);
                logger.error(e.getMessage(), e);
            } finally {
                close(jedis);
            }
        }
        return null;
    }

    @Override
    public String setex(String key, int seconds, String value) {
        if (enabled && StringUtils.isNotEmpty(key) && null != value) {
            if (seconds > 0) {
                ShardedJedis jedis = null;
                try {
                    jedis = shardedJedisPool.getResource();
                    String ret = jedis.setex(key, seconds, value);
                    return ret;
                } catch (JedisException e) {
                    logger.error("'setex' key fail, key: {}, seconds: {}, value: {}", key, seconds, value);
                    logger.error(e.getMessage(), e);
                } finally {
                    close(jedis);
                }
            } // 当seconds参数不合法(<= 0)时，后端会返回一个错误 ("JedisDataException: ERR invalid expire time in setex")，即操作失败
        }
        return null;
    }

    // ---------------- List (列表) ----------------
    @Override
    public int llen(String key) {
        if (enabled && StringUtils.isNotEmpty(key)) {
            ShardedJedis jedis = null;
            try {
                jedis = shardedJedisPool.getResource();
                int listLength = jedis.llen(key).intValue();
                return listLength;
            } catch (JedisException e) {
                logger.error("'llen' key fail, key: {}", key);
                logger.error(e.getMessage(), e);
            } finally {
                close(jedis);
            }
        }
        return 0;
    }

    @Override
    public int lpush(String key, String... values) {
        if (enabled && StringUtils.isNotEmpty(key)) {
            ShardedJedis jedis = null;
            try {
                jedis = shardedJedisPool.getResource();
                int pushedListLength = jedis.lpush(key, values).intValue();
                return pushedListLength;
            } catch (JedisException e) {
                logger.error("'lpush' key fail, key: {}, values: {}", key, Arrays.toString(values));
                logger.error(e.getMessage(), e);
            } finally {
                close(jedis);
            }
        }
        return 0;
    }

    @Override
    public String rpop(String key) {
        if (enabled && StringUtils.isNotEmpty(key)) {
            ShardedJedis jedis = null;
            try {
                jedis = shardedJedisPool.getResource();
                String value = jedis.rpop(key);
                return value;
            } catch (JedisException e) {
                logger.error("'rpop' key fail, key: {}", key);
                logger.error(e.getMessage(), e);
            } finally {
                close(jedis);
            }
        }
        return null;
    }

    @Override
    public List<String> lrange(String key, int start, int stop) {
        if (enabled && StringUtils.isNotEmpty(key)) {
            ShardedJedis jedis = null;
            try {
                jedis = shardedJedisPool.getResource();
                List<String> list = jedis.lrange(key, start, stop);
                return list;
            } catch (JedisException e) {
                logger.error("'lrange' key fail, key: {}", key);
                logger.error(e.getMessage(), e);
            } finally {
                close(jedis);
            }
        }
        return Collections.emptyList();
    }

    @Override
    public String ltrim(String key, int start, int stop) {
        if (enabled && StringUtils.isNotEmpty(key)) {
            ShardedJedis jedis = null;
            try {
                jedis = shardedJedisPool.getResource();
                String ret = jedis.ltrim(key, start, stop);
                return ret;
            } catch (JedisException e) {
                logger.error("'ltrim' key fail, key: {}, start: {}, stop: {}", key, start, stop);
                logger.error(e.getMessage(), e);
            } finally {
                close(jedis);
            }
        }
        return null;
    }

    // ---------------- Sorted Set (有序集合) ----------------
    @Override
    public int zadd(String key, double score, String member) {
        return this.zadd(key, score, member, DEFAULT_MAX_LENGTH);
    }

    @Override
    public int zadd(String key, double score, String member, int maxLength) {
        if (enabled && StringUtils.isNotEmpty(key)) {
            ShardedJedis jedis = null;
            try {
                int newElementNum = 0;
                int elementNum = 0;

                jedis = shardedJedisPool.getResource();
                ShardedJedisPipeline pipeline = jedis.pipelined();
                Response<Long> zaddResponse = pipeline.zadd(key, score, member);
                Response<Long> zcardResponse = pipeline.zcard(key);
                pipeline.sync();

                Long zaddRes = zaddResponse.get();
                if (zaddRes != null) {
                    newElementNum = zaddRes.intValue();
                }
                Long zcardRes = zcardResponse.get();
                if (zcardRes != null) {
                    elementNum = zcardRes.intValue();
                }
                if (newElementNum > 0 && elementNum > 0) {
                    this.asynShrinkZset(key, elementNum, maxLength);
                }

                return newElementNum;
            } catch (JedisException e) {
                logger.error("'zadd' key fail, key: {}, score: {}, member: {}, maxLength: {}", key, score, member,
                             maxLength);
                logger.error(e.getMessage(), e);
            } finally {
                close(jedis);
            }
        }
        return 0;
    }

    @Override
    public int zadd(String key, Map<String, Double> scoreMembers) {
        return this.zadd(key, scoreMembers, DEFAULT_MAX_LENGTH);
    }

    @Override
    public int zadd(String key, Map<String, Double> scoreMembers, int maxLength) {
        if (enabled && StringUtils.isNotEmpty(key)) {
            ShardedJedis jedis = null;
            try {
                int newElementNum = 0;
                int elementNum = 0;

                jedis = shardedJedisPool.getResource();
                ShardedJedisPipeline pipeline = jedis.pipelined();
                Response<Long> zaddResponse = pipeline.zadd(key, scoreMembers);
                Response<Long> zcardResponse = pipeline.zcard(key);
                pipeline.sync();

                Long zaddRes = zaddResponse.get();
                if (zaddRes != null) {
                    newElementNum = zaddRes.intValue();
                }
                Long zcardRes = zcardResponse.get();
                if (zcardRes != null) {
                    elementNum = zcardRes.intValue();
                }
                if (newElementNum > 0 && elementNum > 0) {
                    this.asynShrinkZset(key, elementNum, maxLength);
                }

                return newElementNum;
            } catch (JedisException e) {
                logger.error("'zadd' key fail, key: {}, scoreMembers: {}, maxLength: {}", key, scoreMembers, maxLength);
                logger.error(e.getMessage(), e);
            } finally {
                close(jedis);
            }
        }
        return 0;
    }

    /**
     * 只有当"有序集合"长度超过阈值时，才会进行"异步缩容"操作。
     * 
     * @param key 键
     * @param maxLength 有序集合最大长度
     */
    private void asynShrinkZset(String key, int elementNum, int maxLength) {
        if (elementNum >= maxLength + LENGTH_THRESHOLD) {
            executorService.submit(new ZremrangeByRankRunnable(this, key, elementNum, maxLength));
        }
    }

    /**
     * 通过 zremrangeByRank 命令进行"异步缩容"操作。
     */
    private class ZremrangeByRankRunnable implements Runnable {

        private final RedisService redisService;
        private final String       key;
        private final int          elementNum;
        private final int          maxLength;

        public ZremrangeByRankRunnable(RedisService redisService, String key, int elementNum, int maxLength){
            this.redisService = redisService;
            this.key = key;
            this.elementNum = elementNum;
            this.maxLength = maxLength;
        }

        /**
         * 对"有序集合"进行"缩容"操作。
         */
        @Override
        public void run() {
            long startTime = System.currentTimeMillis();

            int stop = elementNum - maxLength - 1;
            int removedElementNum = redisService.zremrangeByRank(key, 0, stop);
            if (removedElementNum != elementNum - maxLength) {
                logger.warn("Failed to 'zremrangeByRank' of key: {}, elementNum: {}, maxLength: {}", key, elementNum,
                            maxLength);
            }

            long runTime = System.currentTimeMillis() - startTime;
            logger.debug("'zremrangeByRank' of Sorted Set key: {}, removed number: {}, time: {}", key,
                         removedElementNum, runTime);
        }

    }

    @Override
    public Set<String> zrange(String key, int start, int stop) {
        if (enabled && StringUtils.isNotEmpty(key)) {
            ShardedJedis jedis = null;
            try {
                jedis = shardedJedisPool.getResource();
                Set<String> zset = jedis.zrange(key, start, stop);
                return zset;
            } catch (JedisException e) {
                logger.error("'zrange' key fail, key: {}", key);
                logger.error(e.getMessage(), e);
            } finally {
                close(jedis);
            }
        }
        return Collections.emptySet();
    }

    @Override
    public Set<String> zrevrange(String key, int start, int stop) {
        if (enabled && StringUtils.isNotEmpty(key)) {
            ShardedJedis jedis = null;
            try {
                jedis = shardedJedisPool.getResource();
                Set<String> zset = jedis.zrevrange(key, start, stop);
                return zset;
            } catch (JedisException e) {
                logger.error("'zrevrange' key fail, key: {}", key);
                logger.error(e.getMessage(), e);
            } finally {
                close(jedis);
            }
        }
        return Collections.emptySet();
    }

    @Override
    public Set<String> zrangeByScore(String key, double min, double max) {
        if (enabled && StringUtils.isNotEmpty(key)) {
            ShardedJedis jedis = null;
            try {
                jedis = shardedJedisPool.getResource();
                Set<String> zset = jedis.zrangeByScore(key, min, max);
                return zset;
            } catch (JedisException e) {
                logger.error("'zrangeByScore' key fail, key: {}", key);
                logger.error(e.getMessage(), e);
            } finally {
                close(jedis);
            }
        }
        return Collections.emptySet();
    }

    @Override
    public Set<String> zrangeByScore(String key, double min, double max, int offset, int count) {
        if (enabled && StringUtils.isNotEmpty(key)) {
            ShardedJedis jedis = null;
            try {
                jedis = shardedJedisPool.getResource();
                Set<String> zset = jedis.zrangeByScore(key, min, max, offset, count);
                return zset;
            } catch (JedisException e) {
                logger.error("'zrangeByScore' key fail, key: {}", key);
                logger.error(e.getMessage(), e);
            } finally {
                close(jedis);
            }
        }
        return Collections.emptySet();
    }

    @Override
    public Set<String> zrevrangeByScore(String key, double max, double min) {
        if (enabled && StringUtils.isNotEmpty(key)) {
            ShardedJedis jedis = null;
            try {
                jedis = shardedJedisPool.getResource();
                Set<String> zset = jedis.zrevrangeByScore(key, max, min);
                return zset;
            } catch (JedisException e) {
                logger.error("'zrevrangeByScore' key fail, key: {}", key);
                logger.error(e.getMessage(), e);
            } finally {
                close(jedis);
            }
        }
        return Collections.emptySet();
    }

    @Override
    public Set<String> zrevrangeByScore(String key, double max, double min, int offset, int count) {
        if (enabled && StringUtils.isNotEmpty(key)) {
            ShardedJedis jedis = null;
            try {
                jedis = shardedJedisPool.getResource();
                Set<String> zset = jedis.zrevrangeByScore(key, max, min, offset, count);
                return zset;
            } catch (JedisException e) {
                logger.error("'zrevrangeByScore' key fail, key: {}", key);
                logger.error(e.getMessage(), e);
            } finally {
                close(jedis);
            }
        }
        return Collections.emptySet();
    }

    @Override
    public int zcard(String key) {
        if (enabled && StringUtils.isNotEmpty(key)) {
            ShardedJedis jedis = null;
            try {
                jedis = shardedJedisPool.getResource();
                int zsetElementNum = jedis.zcard(key).intValue();
                return zsetElementNum;
            } catch (JedisException e) {
                logger.error("'zcard' key fail, key: {}", key);
                logger.error(e.getMessage(), e);
            } finally {
                close(jedis);
            }
        }
        return 0;
    }

    @Override
    public int zremrangeByScore(String key, double min, double max) {
        if (enabled && StringUtils.isNotEmpty(key)) {
            ShardedJedis jedis = null;
            try {
                jedis = shardedJedisPool.getResource();
                int removedElementNum = jedis.zremrangeByScore(key, min, max).intValue();
                return removedElementNum;
            } catch (JedisException e) {
                logger.error("'zremrangeByScore' key fail, key: {}, min: {}, max: {}", key, min, max);
                logger.error(e.getMessage(), e);
            } finally {
                close(jedis);
            }
        }
        return 0;
    }

    @Override
    public int zremrangeByRank(String key, int start, int stop) {
        if (enabled && StringUtils.isNotEmpty(key)) {
            ShardedJedis jedis = null;
            try {
                jedis = shardedJedisPool.getResource();
                int removedElementNum = jedis.zremrangeByRank(key, start, stop).intValue();
                return removedElementNum;
            } catch (JedisException e) {
                logger.error("'zremrangeByRank' key fail, key: {}, start: {}, stop: {}", key, start, stop);
                logger.error(e.getMessage(), e);
            } finally {
                close(jedis);
            }
        }
        return 0;
    }

    @Override
    public String info(String key, String section) {
        ShardedJedis shardedJedis = null;
        try {
            shardedJedis = shardedJedisPool.getResource();
            Jedis jedis = shardedJedis.getShard(key);
            String info = jedis.info(section);
            return info;
        } catch (JedisException e) {
            logger.error("'info' key fail, key: {}", key);
            logger.error(e.getMessage(), e);
        } finally {
            close(shardedJedis);
        }
        return "";
    }

}
