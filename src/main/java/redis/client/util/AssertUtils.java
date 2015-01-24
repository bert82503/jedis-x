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
import java.util.Map;

/**
 * 断言工具类，协助验证参数。<br>
 * 有助于在运行时的早期就明确地确定编程错误。
 * <p>
 * Assertion utility class that assists in validating arguments.<br>
 * Useful for identifying programmer errors early and clearly at runtime.
 * <p>
 * <font color="red">#ThreadSafe# (线程安全)</font>
 * 
 * @author huagang.li
 * @see org.springframework.util.Assert
 * @since 1.0
 */
public abstract class AssertUtils {

    /**
     * Assert a boolean expression, throwing {@code IllegalArgumentException} if the test result is {@code false}.
     * 
     * <pre>
     * AssertUtils.isTrue(i > 0, "'i' must be greater than 0");
     * </pre>
     * 
     * @param expression a boolean expression
     * @param message the exception message to use if the assertion fails
     * @throws IllegalArgumentException if expression is {@code false}
     */
    public static void isTrue(boolean expression, String message) {
        if (!expression) {
            throw new IllegalArgumentException(message);
        }
    }

    // Empty checks (空检查)
    // -----------------------------------------------------------------------
    /**
     * Assert that a {@link String} is not empty; that is, it must not be {@code null} and not the empty String.
     * 
     * <pre>
     * AssertUtils.notEmpty(name, "'name' must not be empty");
     * </pre>
     * 
     * @param str the String to check
     * @param message the exception message to use if the assertion fails
     */
    public static void notEmpty(String str, String message) {
        if (StringUtils.isEmpty(str)) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Assert that a collection has elements; that is, it must not be {@code null} and must have at least one element.
     * 
     * <pre>
     * AssertUtils.notEmpty(collection, "'collection' must have elements");
     * </pre>
     * 
     * @param coll the collection to check
     * @param message the exception message to use if the assertion fails
     * @throws IllegalArgumentException if the collection is {@code null} or has no elements
     */
    public static void notEmpty(Collection<?> coll, String message) {
        if (CollectionUtils.isEmpty(coll)) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Assert that a {@link Map} has entries; that is, it must not be {@code null} and must have at least one entry.
     * 
     * <pre>
     * AssertUtils.notEmpty(map, "'map' must have entries");
     * </pre>
     * 
     * @param map the map to check
     * @param message the exception message to use if the assertion fails
     * @throws IllegalArgumentException if the map is {@code null} or has no entries
     */
    public static void notEmpty(Map<?, ?> map, String message) {
        if (MapUtils.isEmpty(map)) {
            throw new IllegalArgumentException(message);
        }
    }

}
