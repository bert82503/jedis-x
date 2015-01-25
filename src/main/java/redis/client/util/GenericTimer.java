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

import java.util.Timer;
import java.util.TimerTask;

/**
 * 通用"定时任务"调度的定时器。<br>
 * 此类包装标准的定时器({@link Timer})。<br>
 * 如果没有对象池使用这个定时器，它会被取消。这样可以防止线程一直运行着 (这会导致内存泄漏)，防止应用程序关闭或重新加载。
 * <p>
 * <font color="red">此类是线程安全的！</font>
 * 
 * @author huagang.li 2015年1月4日 下午6:38:42
 */
public class GenericTimer {

    /** 定时器实例 */
    private static Timer timer;     // @GuardedBy("this")

    /** 使用计数追踪器 */
    private static int   usageCount; // @GuardedBy("this")

    /** 防止实例化 */
    private GenericTimer(){
        // Hide the default constructor
    }

    /**
     * 添加指定的驱逐任务到这个定时器。<br>
     * 任务，通过调用该方法添加的，必须调用{@link #cancel(TimerTask)}来取消这个任务，以防止内存或消除泄漏。
     * 
     * @param task 要调度的任务
     * @param delay 任务执行前的等待时间(ms)
     * @param period 执行间隔时间(ms)
     */
    public static synchronized void schedule(TimerTask task, long delay, long period) {
        if (null == timer) {
            timer = new Timer("redis-pool-GenericTimer", true);
        }
        usageCount++;
        timer.schedule(task, delay, period);
    }

    /**
     * 从定时器中删除指定的驱逐者任务。
     * 
     * @param task 要调度的任务
     */
    public static synchronized void cancel(TimerTask task) {
        task.cancel(); // 1. 将任务的状态标记为"取消(CANCELLED)"状态
        usageCount--;
        if (usageCount == 0) { // 2. 如果没有对象池使用这个定时器，定时器就会被取消
            timer.cancel();
            timer = null;
        }
    }

}
