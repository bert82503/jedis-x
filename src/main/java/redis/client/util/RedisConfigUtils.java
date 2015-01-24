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

package redis.client.util;

import java.util.ArrayList;
import java.util.List;

import redis.clients.jedis.JedisShardInfo;

/**
 * 处理Redis配置信息的工具类。
 * 
 * @author huagang.li 2014年12月13日 下午2:25:52
 */
public abstract class RedisConfigUtils {

    /** 服务器信息的分隔符 */
    private static final String SERVER_INFO_SETPARATOR       = ",";

    /** 服务器信息中各属性的分隔符 */
    private static final String SERVER_INFO_FIELD_SETPARATOR = ":";

    /**
     * 根据给定的{@code redisServers}来解析并返回{@link JedisShardInfo}节点信息列表。
     * 
     * <pre>
     * {@code redisServer}格式：
     *     host:port:name[:weight]
     * </pre>
     * 
     * @param redisServers Redis集群分片节点配置信息
     * @param timeoutMillis 超时时间(ms)
     * @return
     */
    public static List<JedisShardInfo> parseRedisServerList(String redisServers, int timeoutMillis) {
        AssertUtils.notEmpty(redisServers, "'redisServers' param must not be null and empty");

        String[] shardInfoArray = redisServers.split(SERVER_INFO_SETPARATOR);
        List<JedisShardInfo> shards = new ArrayList<JedisShardInfo>(shardInfoArray.length);
        JedisShardInfo shard = null;
        for (String shardInfo : shardInfoArray) {
            if (StringUtils.isNotEmpty(shardInfo)) {
                shardInfo = shardInfo.trim();
                String[] shardFieldArray = shardInfo.split(SERVER_INFO_FIELD_SETPARATOR);
                AssertUtils.isTrue(3 <= shardFieldArray.length && shardFieldArray.length <= 4,
                                   "'redisServers' param does not meet the 'host:port:name[:weight] [, ...]' format : "
                                           + shardInfo);

                String host = shardFieldArray[0];
                AssertUtils.notEmpty(host, "'host' field must not be null and empty : " + shardInfo);
                int port = Integer.parseInt(shardFieldArray[1]);
                String name = shardFieldArray[2];
                AssertUtils.notEmpty(name, "'name' field must not be null and empty : " + shardInfo);

                if (3 == shardFieldArray.length) { // 未定义"节点权重"属性
                    shard = new JedisShardInfo(host, port, timeoutMillis, name);
                } else {
                    shard = new JedisShardInfo(host, port, timeoutMillis, name);
                    // int weight = Integer.parseInt(shardFieldArray[3]);
                    // AssertUtils.isTrue(weight > 0,
                    // "'weight' field of 'redisServers' property must be greater than 0 : "
                    // + weight);
                    // shard.setWeight(weight); // FIXME 该方法现在还不支持，所以现在权重只能使用默认值(1)！
                }
                shards.add(shard);
            }
        }
        return shards;
    }

}
