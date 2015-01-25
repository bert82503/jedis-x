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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Tests for {@link AssertUtils}.
 * 
 * @author huagang.li 2015年1月24日 下午1:50:19
 */
public class AssertUtilsTest {

    @Test(dataProvider = "notEmptyString", expectedExceptions = IllegalArgumentException.class)
    public void notEmptyString(String str) {
        AssertUtils.notEmpty(str, "'str' must not be null and empty");
    }

    @DataProvider(name = "notEmptyString")
    protected static final Object[][] notEmptyStringTestData() {
        Object[][] testData = new Object[][] {//
                                              //
                { null },//
                { "" },//
        };
        return testData;
    }

    @Test(dataProvider = "notEmptyCollection", expectedExceptions = IllegalArgumentException.class)
    public void notEmptyCollection(Collection<?> coll) {
        AssertUtils.notEmpty(coll, "'coll' must not be null and empty");
    }

    @DataProvider(name = "notEmptyCollection")
    protected static final Object[][] notEmptyCollectionTestData() {
        Object[][] testData = new Object[][] {//
                                              //
                { null },//
                { Collections.emptyList() },//
                { Collections.emptySet() },//
                { new ArrayList<Object>() },// 最常用
                { new LinkedList<Object>() },//
                { new CopyOnWriteArrayList<Object>() },// "写复制"线程安全
                { new HashSet<Object>() },// 最常用
                { new LinkedHashSet<Object>() },// 按"元素添加顺序"排序
                { new TreeSet<Object>() },// 按"元素"排序
                { new ConcurrentSkipListSet<Object>() },// 按"元素"排序，且线程安全
        };
        return testData;
    }

    @Test(dataProvider = "notEmptyMap", expectedExceptions = IllegalArgumentException.class)
    public void notEmptyMap(Map<?, ?> map) {
        AssertUtils.notEmpty(map, "'map' must not be null and empty");
    }

    @DataProvider(name = "notEmptyMap")
    protected static final Object[][] notEmptyMapTestData() {
        Object[][] testData = new Object[][] {//
                                              //
                { null },//
                { Collections.emptyMap() },//
                { new HashMap<String, Object>() },// 最常用
                { new LinkedHashMap<String, Object>() },// 按"元素添加顺序"排序
                { new WeakHashMap<String, Object>() },// "弱引用(WeakReference)"映射表
                { new ConcurrentHashMap<String, Object>() },// 并发线程安全
                { new ConcurrentSkipListMap<String, Object>() },// 按"键(key)"排序，且线程安全
                { new TreeMap<String, Object>() },// 按"键(key)"排序
        };
        return testData;
    }

}
