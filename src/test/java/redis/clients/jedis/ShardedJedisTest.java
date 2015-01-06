/*
 * Copyright 2015 FraudMetrix.cn All right reserved. This software is the
 * confidential and proprietary information of FraudMetrix.cn ("Confidential
 * Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered
 * into with FraudMetrix.cn.
 */
package redis.clients.jedis;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.testng.annotations.Test;

import redis.client.util.RedisConfigUtils;
import redis.client.util.TestConfigUtils;

/**
 * Test for {@link ShardedJedis}.
 * 
 * @author huagang.li 2015年1月6日 上午10:57:37
 */
public class ShardedJedisTest {

    @Test
    public void getAllShardInfo() {
        List<JedisShardInfo> shardInfos = RedisConfigUtils.parseRedisServerList(TestConfigUtils.getRedisServers(),
                                                                           TestConfigUtils.getTimeoutMillis());
        ShardedJedis shardedJedis = new ShardedJedis(shardInfos);
        try {
            Collection<JedisShardInfo> allClusterShardInfos = shardedJedis.getAllShardInfo();
            assertEquals(allClusterShardInfos.size(), 160 * shardInfos.size()); // 返回的集群节点数量被放大了 160 倍
            // 过滤所有重复的Shard信息
            Map<JedisShardInfo, String> shardMap = new HashMap<JedisShardInfo, String>();
            for (JedisShardInfo clusterShardInfo : allClusterShardInfos) {
                shardMap.put(clusterShardInfo, "1");
            }
            // 列表大小和所有元素都必须是完全一样的
            assertEquals(shardMap.size(), shardInfos.size());
            for (JedisShardInfo shardInfo : shardInfos) {
                assertTrue(shardMap.containsKey(shardInfo));
            }

            // 通过new操作将Set类型的Shard列表转换为List类型，其不会创建新对象
            Set<JedisShardInfo> checkedShardInfos = shardMap.keySet();
            List<JedisShardInfo> copyShardInfos = new ArrayList<JedisShardInfo>(checkedShardInfos);
            assertEquals(copyShardInfos.size(), checkedShardInfos.size());
            for (JedisShardInfo copyShardInfo : copyShardInfos) {
                assertTrue(checkedShardInfos.contains(copyShardInfo));
            }
        } finally {
            shardedJedis.close();
        }
    }

}
