/*
 * Copyright 2015 FraudMetrix.cn All right reserved. This software is the
 * confidential and proprietary information of FraudMetrix.cn ("Confidential
 * Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered
 * into with FraudMetrix.cn.
 */
package redis.client.pool;

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
