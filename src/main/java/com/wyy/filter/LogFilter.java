package com.wyy.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


public class LogFilter implements Filter {

    private static Logger logger = LoggerFactory.getLogger(LogFilter.class);

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("LogFilter init");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        String ip = req.getRemoteAddr();
        String uri = req.getRequestURI();
        String method = req.getMethod();

        long startTime = System.currentTimeMillis();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        logger.info("[LogFilter]: before chain: {} {} {} {}", timestamp, ip, method, uri);
        try {
            chain.doFilter(request, response);
        } finally {
            // finally 保证异常路径也输出对称的 after 日志，并记录响应状态码，便于全链路排查
            long endTime = System.currentTimeMillis();
            int status = ((HttpServletResponse) response).getStatus();
            logger.info("[LogFilter]: after chain: duration {}ms, status {}", endTime - startTime, status);
        }
    }

    @Override
    public void destroy() {
        logger.info("LogFilter destroy");
    }
}
