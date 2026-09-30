package com.wyy.interceptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * MVC 请求日志拦截器（示范）
 * preHandle → Controller 执行 → postHandle → 视图渲染 → afterCompletion
 */
public class LogInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(LogInterceptor.class);

    /** 存进 request 的起始时间戳的 key，供 afterCompletion 计算耗时 */
    private static final String ATTR_START_TIME = "mvcInterceptorStartTime";

    /**
     * Controller 执行【之前】调用。
     * 返回 true = 放行，继续往下走；返回 false = 中断请求，Controller 不执行。
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        request.setAttribute(ATTR_START_TIME, System.currentTimeMillis());
        // handler 就是匹配到的 Controller 对象（老式接口）或 HandlerMethod（注解式）
        logger.info("[拦截器] preHandle 进入: {} {} -> handler={}",
                request.getMethod(), request.getRequestURI(), handler.getClass().getSimpleName());
        return true;
    }

    /**
     * Controller 执行【之后】、视图渲染【之前】调用。
     * 可在此修改 ModelAndView（例如统一往模型里塞公共数据）。
     */
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        logger.info("[拦截器] postHandle Controller 返回, 待渲染视图: {}", modelAndView);
    }

    /**
     * 整个请求【全部结束后】（视图也渲染完）调用，无论是否抛异常都会执行，常用于释放资源/统计耗时。
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        Object start = request.getAttribute(ATTR_START_TIME);
        long cost = (start == null) ? -1 : System.currentTimeMillis() - (long) start;
        logger.info("[拦截器] afterCompletion 结束, 总耗时 {} ms, 异常: {}", cost, ex == null ? "无" : ex.getMessage());
    }
}