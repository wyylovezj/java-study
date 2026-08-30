package com.wyy.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 会话监听器
 * 统计两类指标并发布到 ServletContext（application 作用域），JSP/Servlet 可直接读取：
 * 1. onlineCount 在线人数：活跃 session 数，由 sessionCreated/sessionDestroyed 驱动，
 *    包含访问 login.jsp 但尚未登录的访客（JSP 默认会创建 session），属于"近似在线人数"
 * 2. loginCount 登录人数：以 session 中是否存在 "user" 属性为准（LoginServlet 登录成功后写入），
 *    通过 HttpSessionAttributeListener 监听该属性的增删，语义上更精确
 * <p>
 * 一个监听器类可以同时实现多个监听器接口，web.xml 注册一次即可收到全部事件；
 * 计数使用 AtomicInteger 而非 int++：多个请求可能并发创建/销毁会话，
 * i++ 是"读-改-写"三步操作，并发下会互相覆盖导致计数偏小
 */
public class SessionListener implements HttpSessionListener, HttpSessionAttributeListener {

    private static final Logger logger = LoggerFactory.getLogger(SessionListener.class);

    /** 登录成功标记的 session 属性名，与 LoginServlet/AuthFilter 的约定一致 */
    private static final String USER_ATTR = "user";

    /** 当前在线人数（活跃 session 数） */
    private static final AtomicInteger onlineCount = new AtomicInteger(0);

    /** 当前登录人数（session 中带有 "user" 属性的会话数） */
    private static final AtomicInteger loginCount = new AtomicInteger(0);

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        int count = onlineCount.incrementAndGet();
        publish(se.getSession().getServletContext());
        logger.info("[SessionListener] 会话创建, sessionId: {}, 当前在线人数: {}", se.getSession().getId(), count);
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        // 超时（web.xml session-timeout=30 分钟）或显式 invalidate 时触发
        int count = onlineCount.decrementAndGet();
        publish(se.getSession().getServletContext());
        logger.info("[SessionListener] 会话销毁, sessionId: {}, 当前在线人数: {}", se.getSession().getId(), count);
    }

    @Override
    public void attributeAdded(HttpSessionBindingEvent event) {
        if (USER_ATTR.equals(event.getName())) {
            // 登录成功：LoginServlet 写入 "user" 属性时触发，getValue() 即登录用户
            int count = loginCount.incrementAndGet();
            publish(event.getSession().getServletContext());
            logger.info("[SessionListener] 用户登录, {}, 当前登录人数: {}", event.getValue(), count);
        }
    }

    @Override
    public void attributeRemoved(HttpSessionBindingEvent event) {
        if (USER_ATTR.equals(event.getName())) {
            // 会话超时销毁时容器会先移除全部属性再回调 sessionDestroyed，
            // 因此"注销"与"超时"两条路径都会经过这里，登录人数都能正确递减
            int count = loginCount.decrementAndGet();
            publish(event.getSession().getServletContext());
            logger.info("[SessionListener] 登录状态移除(注销或超时), {}, 当前登录人数: {}", event.getValue(), count);
        }
    }

    @Override
    public void attributeReplaced(HttpSessionBindingEvent event) {
        // 同一会话内重复登录（不注销再登录一次）走"替换"而非"新增"，
        // 会话内仍只有一个登录用户，登录人数不变，无需处理
    }

    /** 将统计指标发布到 ServletContext，页面可通过 ${onlineCount} / ${loginCount} 读取 */
    private static void publish(ServletContext servletContext) {
        servletContext.setAttribute("onlineCount", onlineCount.get());
        servletContext.setAttribute("loginCount", loginCount.get());
    }
}
