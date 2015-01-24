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

import java.util.Collection;

/**
 * 集合({@link Collection})工具类，其是{@code null}安全的。
 * <p>
 * Provides utility methods and decorators for {@link java.util.Collection} or {@link java.lang.Iterable} instances that
 * are {@code null} safe.
 * <p>
 * <font color="red">#ThreadSafe# (线程安全)</font>
 * 
 * @author huagang.li
 * @see java.util.Collection
 * @see org.apache.commons.collections4.CollectionUtils
 * @see org.springframework.util.CollectionUtils
 * @since 1.0
 */
public abstract class CollectionUtils {

    // Empty checks (空检查)
    // -----------------------------------------------------------------------
    /**
     * Null-safe check if a {@link Collection} is null or empty.
     * <p>
     * Null returns {@code true}.
     * 
     * <pre>
     * CollectionUtils.isEmpty(null)                     == true
     * CollectionUtils.isEmpty(Collections.emptyList())  == true
     * </pre>
     * 
     * @param coll the Collection to check, may be null
     * @return {@code true} if the Collection is null or empty
     */
    public static boolean isEmpty(final Collection<?> coll) {
        return coll == null || coll.isEmpty();
    }

    /**
     * Null-safe check if a {@link Collection} is not null and not empty.
     * <p>
     * Null returns {@code false}.
     * 
     * <pre>
     * CollectionUtils.isNotEmpty(null)                     == false
     * CollectionUtils.isNotEmpty(Collections.emptyList())  == false
     * </pre>
     * 
     * @param coll the Collection to check, may be null
     * @return {@code true} if the Collection is non-null and non-empty
     */
    public static boolean isNotEmpty(final Collection<?> coll) {
        return !isEmpty(coll);
    }

}
