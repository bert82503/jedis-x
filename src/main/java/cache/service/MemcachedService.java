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

package cache.service;

import java.util.concurrent.Future;

/**
 * 封装<a href="http://memcached.org">Memcached</a>客户端，并增加开关功能。
 * 
 * @author huagang.li 2014年12月27日 下午5:12:15
 */
public interface MemcachedService extends SwitchService {

    Future<Boolean> set(String key, int exp, Object value);

    Future<Boolean> set(String key, int exp, String value);

    Object get(String key);

    String getString(String key);

    Future<Object> getAsync(String key);

    Future<Boolean> append(String key, int exp, String value);

    Future<Boolean> delete(String key);

}
