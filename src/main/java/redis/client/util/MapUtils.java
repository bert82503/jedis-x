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

package redis.client.util;

import java.util.Map;

/**
 * 映射表({@link Map})工具类，其是{@code null}安全的。
 * <p>
 * Provides utility methods and decorators for {@link java.util.Map} and {@link java.util.SortedMap} instances that are
 * {@code null} safe.
 * <p>
 * <font color="red">#ThreadSafe# (线程安全)</font>
 * 
 * @author huagang.li
 * @see java.util.Map
 * @see org.apache.commons.collections4.MapUtils
 * @since 1.0
 */
public abstract class MapUtils {

    // Empty checks (空检查)
    // -----------------------------------------------------------------------
    /**
     * Null-safe check if a {@link Map} is null or empty.
     * <p>
     * Null returns {@code true}.
     * 
     * <pre>
     * MapUtils.isEmpty(null)                    == true
     * MapUtils.isEmpty(Collections.emptyMap())  == true
     * </pre>
     * 
     * @param map the Map to check, may be null
     * @return {@code true} if the Map is null or empty
     */
    public static boolean isEmpty(final Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    /**
     * Null-safe check if a {@link Map} is not null and not empty.
     * <p>
     * Null returns {@code false}.
     * 
     * <pre>
     * MapUtils.isNotEmpty(null)                    == false
     * MapUtils.isNotEmpty(Collections.emptyMap())  == false
     * </pre>
     * 
     * @param map the map to check, may be null
     * @return {@code true} if the Map is non-null and non-empty
     */
    public static boolean isNotEmpty(final Map<?, ?> map) {
        return !isEmpty(map);
    }

}
