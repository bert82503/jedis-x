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

import static org.testng.Assert.assertEquals;

import java.util.Arrays;

import org.testng.annotations.Test;

import redis.client.jedis.spring.CustomShardedJedisPoolFactoryBean.PoolBehaviour;

/**
 * Tests for {@link PoolBehaviour}.
 * 
 * @author huagang.li 2014年12月20日 上午11:18:59
 */
public class PoolBehaviourTest {

    @Test
    public void values() {
        assertEquals(Arrays.toString(PoolBehaviour.values()), "[LIFO, FIFO]");
    }

}
