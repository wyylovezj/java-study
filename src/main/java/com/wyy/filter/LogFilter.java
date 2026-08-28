package com.wyy.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


@WebFilter(
        filterName = "logFilter",
        urlPatterns = "/*"
)
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
        chain.doFilter(request, response);
        long endTime = System.currentTimeMillis();
        logger.info("[LogFilter]: after chain: duration {}", endTime - startTime);
    }

    @Override
    public void destroy() {
        logger.info("LogFilter destroy");
    }
}
