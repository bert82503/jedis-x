/*
 * Copyright 2014 FraudMetrix.cn All right reserved. This software is the
 * confidential and proprietary information of FraudMetrix.cn ("Confidential
 * Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered
 * into with FraudMetrix.cn.
 */
package redis.client.util;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test for {@link ConfigUtils}.
 * 
 * @author huagang.li 2014年12月13日 下午3:23:26
 */
public class ConfigUtilsTest {

    @Test
    public void getRedisServers() {
        assertEquals(ConfigUtils.getRedisServers(),
                     "127.0.0.1:6379:Shard-01,127.0.0.1:6380:Shard-02,127.0.0.1:6381:Shard-03");
    }

    @Test
    public void getMemcachedServers() {
        assertEquals(ConfigUtils.getMemcachedServers(), "127.0.0.1:11211");
    }

    @Test(dataProvider = "parserRedisServerList")
    public void parserRedisServerList(String redisServers, int timeoutMillis, String serverInfoStr) {
        assertEquals(ConfigUtils.parserRedisServerList(redisServers, timeoutMillis).toString(), serverInfoStr);
    }

    @DataProvider(name = "parserRedisServerList")
    protected static final Object[][] parserRedisServerListTestData() {
        Object[][] testData = new Object[][] {//
        // 未定义节点权重
                { "192.168.6.189:6379:Shard-01, 192.168.6.189:6380:Shard-02, 192.168.6.189:6381:Shard-03", 100,
                        "[192.168.6.189:6379*1, 192.168.6.189:6380*1, 192.168.6.189:6381*1]" },// 节点配置信息之间包含若干个空格
                //
                { " 127.0.0.1:6377:Shard-01,  ,  127.0.0.1:6375:Shard-02,   127.0.0.1:6373:Shard-03,  ", 500,
                        "[127.0.0.1:6377*1, 127.0.0.1:6375*1, 127.0.0.1:6373*1]" },// 节点配置信息之间包含若干个空格和无用逗号
                // Redis开发环境
                {
                        "192.168.6.35:6379:Shard-01,192.168.6.36:6379:Shard-02,192.168.6.37:6379:Shard-03,192.168.6.38:6379:Shard-04",
                        300, "[192.168.6.35:6379*1, 192.168.6.36:6379*1, 192.168.6.37:6379*1, 192.168.6.38:6379*1]" },// 节点配置信息之间不包含任何空格
                // 定义节点权重
                { "127.0.0.1:6379:Shard-01:1, 127.0.0.1:6380:Shard-02:1, 127.0.0.1:6381:Shard-03:1", 100,
                        "[127.0.0.1:6379*1, 127.0.0.1:6380*1, 127.0.0.1:6381*1]" },// 节点配置信息之间包含若干个空格
        };
        return testData;
    }

    @Test(dataProvider = "parserRedisServerListExp", expectedExceptions = { IllegalArgumentException.class })
    public void parserRedisServerListExp(String redisServers, int timeoutMillis) {
        ConfigUtils.parserRedisServerList(redisServers, timeoutMillis);
    }

    @DataProvider(name = "parserRedisServerListExp")
    protected static final Object[][] parserRedisServerListExpTestData() {
        Object[][] testData = new Object[][] {//
                                              //
                { null, 100 },//
                { "127.0.0.1:6379", 300 },// 不满足"host:port:name[:weight]"格式
                { ":6379:Shard-01", 400 },// host is empty
                { " 127.0.0.1:6379: ", 401 },// name is empty
        };
        return testData;
    }

}
