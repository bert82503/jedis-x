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

package redis.clients.jedis;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.testng.annotations.Test;

import redis.client.util.RedisConfigUtils;
import redis.client.util.TestConfigUtils;

/**
 * Tests for {@link ShardedJedis}.
 * 
 * @author huagang.li 2015年1月6日 上午10:57:37
 */
public class ShardedJedisTest {

    @Test(description = "验证'ShardedJedis.getAllShardInfo()'方法的功能")
    public void getAllShardInfo() {
        List<JedisShardInfo> shardInfos = RedisConfigUtils.parseRedisServerList(TestConfigUtils.getRedisServers(),
                                                                                TestConfigUtils.getTimeoutMillis());
        ShardedJedis shardedJedis = new ShardedJedis(shardInfos);
        try {
            // FIXME "Sharded.getAllShardInfo() returns 160*shards info list not returns the original shards list"
            // https://github.com/xetorthio/jedis/issues/837
            Collection<JedisShardInfo> allClusterShardInfos = shardedJedis.getAllShardInfo();
            assertEquals(allClusterShardInfos.size(), 160 * shardInfos.size()); // 返回的集群节点数量被放大了 160 倍
            // 过滤所有重复的Shard信息
            Set<JedisShardInfo> checkedShards = new HashSet<>(allClusterShardInfos);
            // 列表大小和所有元素都必须是完全一样的
            assertEquals(checkedShards.size(), shardInfos.size());
            assertTrue(checkedShards.containsAll(shardInfos));

            // 通过new操作将Set类型的Shard列表转换为List类型，其不会创建新对象
            List<JedisShardInfo> copyShardInfos = new ArrayList<JedisShardInfo>(checkedShards);
            assertEquals(copyShardInfos.size(), checkedShards.size());
            assertTrue(checkedShards.containsAll(copyShardInfos));
        } finally {
            shardedJedis.close();
        }
    }

}
