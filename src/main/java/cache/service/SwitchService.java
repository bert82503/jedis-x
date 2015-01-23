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

import java.io.Closeable;

/**
 * 开关服务，包括资源关闭功能。
 * <p>
 * 服务继承{@link Closeable}接口，以便使用JDK 7的 <a
 * href="http://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html">try-with-resources</a>
 * 语法，同时允许Spring容器能在关闭时释放其持有的资源。
 * 
 * @author huagang.li 2014年12月19日 上午10:42:30
 */
public interface SwitchService extends Closeable {

    /**
     * 设置"服务启用开关的状态"。
     * 
     * @param enabled {@code true}：启用服务；{@code false}：关闭服务
     */
    void setEnabled(boolean enabled);

    /**
     * 返回"服务启用开关的状态"。
     * 
     * @return {@code true}：启用服务；{@code false}：关闭服务。
     */
    boolean getEnabled();

    /**
     * 释放服务持有的所有资源。
     * <p>
     * {@inheritDoc}
     */
    @Override
    void close();

}
