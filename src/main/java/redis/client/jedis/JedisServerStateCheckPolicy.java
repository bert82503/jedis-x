/*
 * Copyright 2002-2015 the original author or authors.
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;

/**
 * 基于Jedis实现的"Redis服务器状态检测策略"。
 * 
 * @author huagang.li 2015年1月4日 下午6:34:44
 */
public class JedisServerStateCheckPolicy {

    private static final Logger logger                    = LoggerFactory.getLogger(JedisServerStateCheckPolicy.class);

    /**
     * <a href="http://redis.io/commands/ping">PING命令</a>的正常返回值
     */
    private static final String PING_COMMAND_RETURN_VALUE = "PONG";

    /**
     * 一台Redis服务器是否应该"在出现异常后(无响应、宕机)被自动摘除，在恢复正常后被自动添加"，调用此方法来判定。
     * 
     * @param redisClient Redis链接客户端
     * @param pingRetryTimes PING命令的失败重试次数
     * @return {@code true}：表示服务器是正常的；{@code false}：表示服务器出现了异常。
     */
    public static boolean detect(Jedis redisClient, int pingRetryTimes) {
        try {
            if (redisClient.ping().equals(PING_COMMAND_RETURN_VALUE)) {
                return true;
            } else { // ping失败，可能是由于服务器负载过大引起，使用"重试机制"
                while (pingRetryTimes > 0) {
                    if (redisClient.ping().equals(PING_COMMAND_RETURN_VALUE)) {
                        return true;
                    }
                    pingRetryTimes--;
                }
                return false; // 重试若干次后，还是ping失败，当作"服务器出现异常"处理
            }
        } catch (JedisConnectionException jce) { // 客户端连接不上服务器
            // 当服务器异常时，抛出该异常属于正常行为
            return false;
        } catch (Exception e) {
            logger.warn("unknown exception", e);
            return false;
        }
    }

}
