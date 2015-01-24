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

/**
 * 字符串({@link String})工具类，其是{@code null}安全的。
 * <p>
 * Operations on {@link java.lang.String} that are {@code null} safe.
 * <p>
 * <font color="red">#ThreadSafe# (线程安全)</font>
 * 
 * @author huagang.li
 * @see java.lang.String
 * @see org.apache.commons.lang3.StringUtils
 * @see org.springframework.util.StringUtils
 * @since 1.0
 */
public abstract class StringUtils {

    // Empty checks (空检查)
    // -----------------------------------------------------------------------
    /**
     * Null-safe check if a {@link CharSequence} is null or empty ("").
     * <p>
     * Null returns {@code true}.
     * 
     * <pre>
     * StringUtils.isEmpty(null)      = true
     * StringUtils.isEmpty("")        = true
     * StringUtils.isEmpty(" ")       = false
     * StringUtils.isEmpty("bob")     = false
     * StringUtils.isEmpty("  bob  ") = false
     * </pre>
     * 
     * @param cs the CharSequence to check, may be null
     * @return {@code true} if the CharSequence is null or empty
     */
    public static boolean isEmpty(final CharSequence cs) {
        return cs == null || cs.length() == 0;
    }

    /**
     * Null-safe check if a {@link CharSequence} is not null and not empty ("").
     * <p>
     * Null returns {@code false}.
     * 
     * <pre>
     * StringUtils.isNotEmpty(null)      = false
     * StringUtils.isNotEmpty("")        = false
     * StringUtils.isNotEmpty(" ")       = true
     * StringUtils.isNotEmpty("bob")     = true
     * StringUtils.isNotEmpty("  bob  ") = true
     * </pre>
     * 
     * @param cs the CharSequence to check, may be null
     * @return {@code true} if the CharSequence is non-null and non-empty
     */
    public static boolean isNotEmpty(final CharSequence cs) {
        return !isEmpty(cs);
    }

}
