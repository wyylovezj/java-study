package com.wyy.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 请求监听器
 * 记录请求进入/结束日志（含耗时），并基于"环形秒级窗口"统计 QPS（每秒请求数）：
 * 窗口为最近 60 秒，每秒一个计数槽，请求到达时累加"当前秒"对应的槽；
 * 槽被复用时通过归属秒校验识别过期数据（读为 0、写前清零），因此无需后台线程定期清理
 * <p>
 * 线程安全说明：容器以线程池并发回调监听器——
 * - 计数使用 AtomicLong，避免 i++ 的"读-改-写"竞争丢更新；
 * - "过期重置 + 累加"必须原子完成，故对单个槽加锁；锁按 60 个槽分散，竞争可忽略
 */
public class RequestListener implements ServletRequestListener {

    private static final Logger logger = LoggerFactory.getLogger(RequestListener.class);

    /** 环形窗口长度（秒）：窗口越长统计越平滑，内存开销为 O(窗口长度) */
    private static final int WINDOW_SECONDS = 60;
    /** 记录进入时间用的请求属性名，带全限定类名前缀避免与其他属性冲突 */
    private static final String START_TIME_ATTR = RequestListener.class.getName() + ".startTime";

    private static final WindowSlot[] WINDOW = new WindowSlot[WINDOW_SECONDS];

    /** 累计请求数 */
    private static final AtomicLong totalRequests = new AtomicLong();

    static {
        for (int i = 0; i < WINDOW_SECONDS; i++) {
            WINDOW[i] = new WindowSlot();
        }
    }

    /**
     * 单个时间槽：second 记录该槽当前归属的 epoch 秒，count 为该秒内的请求计数。
     * 60 秒后槽会被循环复用，second 不匹配说明是上一轮的过期数据
     */
    private static class WindowSlot {
        volatile long second;
        final AtomicLong count = new AtomicLong();
    }

    /** 请求到达时累加当前秒的计数槽 */
    private static void recordArrival() {
        long now = System.currentTimeMillis() / 1000;
        WindowSlot slot = WINDOW[(int) (now % WINDOW_SECONDS)];
        synchronized (slot) {
            if (slot.second != now) {
                slot.second = now;
                slot.count.set(0);
            }
            slot.count.incrementAndGet();
        }
    }

    /** 读取指定 epoch 秒的请求数，秒不匹配（过期数据）返回 0 */
    private static long countOf(long second) {
        WindowSlot slot = WINDOW[(int) (second % WINDOW_SECONDS)];
        synchronized (slot) {
            return slot.second == second ? slot.count.get() : 0;
        }
    }

    @Override
    public void requestInitialized(ServletRequestEvent sre) {
        if (!(sre.getServletRequest() instanceof HttpServletRequest)) {
            return;
        }
        HttpServletRequest req = (HttpServletRequest) sre.getServletRequest();
        // Listener 的两个回调是两次独立调用，无法像 Filter 那样用局部变量传值，
        // 因此借助请求属性把进入时间带到 requestDestroyed
        sre.getServletRequest().setAttribute(START_TIME_ATTR, System.currentTimeMillis());
        recordArrival();
        logger.info("[RequestListener] 请求进入: {} {} {}", req.getMethod(), req.getRequestURI(), req.getRemoteAddr());
    }

    @Override
    public void requestDestroyed(ServletRequestEvent sre) {
        if (!(sre.getServletRequest() instanceof HttpServletRequest)) {
            return;
        }
        HttpServletRequest req = (HttpServletRequest) sre.getServletRequest();
        Object start = sre.getServletRequest().getAttribute(START_TIME_ATTR);
        long duration = start == null ? -1L : System.currentTimeMillis() - (long) start;
        // 读"上一完整秒"而非当前秒：当前秒仍在累加中，读出来必然偏小
        long lastSecondQps = countOf(System.currentTimeMillis() / 1000 - 1);
        logger.info("[RequestListener] 请求结束: {} {} 耗时 {}ms, 累计 {} 次, 上一秒 QPS {}",
                req.getMethod(), req.getRequestURI(), duration, totalRequests.incrementAndGet(), lastSecondQps);
    }
}
