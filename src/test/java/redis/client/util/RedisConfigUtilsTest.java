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

import static org.testng.Assert.assertEquals;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Tests for {@link RedisConfigUtils}.
 * <p>
 * 通过单元测试来防止Redis的一些默认设置不被随意更改。
 * 
 * @author huagang.li 2014年12月13日 下午3:23:26
 */
public class RedisConfigUtilsTest {

    @Test(dataProvider = "parseRedisServerList")
    public void parseRedisServerList(String redisServers, int timeoutMillis, String serverInfoStr) {
        assertEquals(RedisConfigUtils.parseRedisServerList(redisServers, timeoutMillis).toString(), serverInfoStr);
    }

    @DataProvider(name = "parseRedisServerList")
    protected static final Object[][] parseRedisServerListTestData() {
        Object[][] testData = new Object[][] {//
        // 未定义节点权重
                { "192.168.6.189:6379:Shard-01, 192.168.6.189:6380:Shard-02, 192.168.6.189:6381:Shard-03", 100,
                        "[192.168.6.189:6379*1, 192.168.6.189:6380*1, 192.168.6.189:6381*1]" },// 节点配置信息之间包含若干个空格
                //
                { " 192.168.6.189:6377:Shard-01,  ,  192.168.6.189:6375:Shard-02,   192.168.6.189:6373:Shard-03,  ",
                        500, "[192.168.6.189:6377*1, 192.168.6.189:6375*1, 192.168.6.189:6373*1]" },// 节点配置信息之间包含若干个空格和无用逗号
                // Redis开发环境
                {
                        "192.168.6.35:6379:Shard-01,192.168.6.36:6379:Shard-02,192.168.6.37:6379:Shard-03,192.168.6.38:6379:Shard-04",
                        300, "[192.168.6.35:6379*1, 192.168.6.36:6379*1, 192.168.6.37:6379*1, 192.168.6.38:6379*1]" },// 节点配置信息之间不包含任何空格
                // 定义节点权重
                { "192.168.6.189:6379:Shard-01:1, 192.168.6.189:6380:Shard-02:1, 192.168.6.189:6381:Shard-03:1", 100,
                        "[192.168.6.189:6379*1, 192.168.6.189:6380*1, 192.168.6.189:6381*1]" },// 节点配置信息之间包含若干个空格
        };
        return testData;
    }

    @Test(dataProvider = "parseRedisServerListExp", expectedExceptions = { IllegalArgumentException.class })
    public void parseRedisServerListExp(String redisServers, int timeoutMillis) {
        RedisConfigUtils.parseRedisServerList(redisServers, timeoutMillis);
    }

    @DataProvider(name = "parseRedisServerListExp")
    protected static final Object[][] parseRedisServerListExpTestData() {
        Object[][] testData = new Object[][] {//
                                              //
                { null, 100 },//
                { "192.168.6.189:6379", 300 },// 不满足"host:port:name[:weight]"格式
                { ":6379:Shard-01", 400 },// host is empty
                { " 192.168.6.189:6379: ", 401 },// name is empty
        };
        return testData;
    }

}
