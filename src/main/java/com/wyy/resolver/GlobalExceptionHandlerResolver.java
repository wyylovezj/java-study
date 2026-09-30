package com.wyy.resolver;

import com.alibaba.fastjson2.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * MVC 全局异常处理器
 * 在 DispatcherServlet 内部、ExceptionFilter 之前拦截 Controller 抛出的异常：
 * 记录完整堆栈日志，按异常类型返回统一 JSON 错误响应，不向用户暴露堆栈/SQL 等内部信息。
 * 返回 null 表示本处理器不处理（如已响应的重定向场景），交由后续 resolver 或冒泡到 ExceptionFilter 兜底。
 */
public class GlobalExceptionHandlerResolver implements HandlerExceptionResolver, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandlerResolver.class);

    /** order 设小，先于 <mvc:annotation-driven/> 注册的默认 resolver 执行 */
    @Override
    public int getOrder() {
        return -1;
    }

    @Override
    public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // handler 是抛出异常的目标（老式 Controller 实例 或 HandlerMethod），便于定位
        logger.error("[全局异常处理] uri: {}, method: {}, handler: {} 抛出异常",
                request.getRequestURI(), request.getMethod(),
                handler == null ? "null" : handler.getClass().getSimpleName(), ex);

        // 响应已提交则无法再写，放弃处理交容器/ExceptionFilter 兜底，避免 IllegalStateException
        if (response.isCommitted()) {
            logger.warn("[全局异常处理] 响应已提交，跳过统一处理");
            return null;
        }

        try {
            response.reset();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json;charset=UTF-8");

            // 按异常类型给不同的友好提示，绝不回传堆栈/SQL 细节
            String message;
            if (ex instanceof SQLException) {
                message = "数据库操作失败，请稍后重试";
            } else if (ex instanceof IllegalArgumentException) {
                message = "请求参数不合法";
            } else {
                message = "系统繁忙，请稍后重试";
            }

            Map<String, Object> body = new HashMap<>();
            body.put("code", 500);
            body.put("message", message);

            PrintWriter out = response.getWriter();
            out.write(JSON.toJSONString(body));
            out.flush();
        } catch (Exception writeEx) {
            // 写错误响应本身又失败时，返回 null 让 ExceptionFilter 兜底
            logger.error("[全局异常处理] 写出统一错误响应失败", writeEx);
            return null;
        }

        // 返回空 ModelAndView（非 null）表示"异常已处理完成，无需再渲染视图、无需向外抛"
        return new ModelAndView();
    }
}