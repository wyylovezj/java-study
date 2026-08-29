package com.wyy.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;


/**
 * 鉴权过滤器
 * 检查用户是否已登录，未登录则重定向到登录页面
 * 放行登录页面、静态资源等公共路径
 */

public class AuthFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(AuthFilter.class);
    private String loginPage;       // 登录页面路径
    private String[] excludePaths;  // 不需要鉴权的路径

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // 从web.xml过滤器配置中获取初始化参数
        this.loginPage = filterConfig.getInitParameter("loginPage");
        if (this.loginPage == null) {
            this.loginPage = "/login.jsp";
        }
        // 获取不需要鉴权的路径
        String excludes = filterConfig.getInitParameter("excludePaths");
        if (excludes != null && !excludes.isEmpty()) {
            this.excludePaths = excludes.split(",");
        } else {
            this.excludePaths = new String[]{"/login.jsp", "/css/", "/js/", "/images/"};
        }
        logger.info("[AuthFilter] init");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws ServletException, IOException {
        // 向下转型为 HttpServletRequest 和 HttpServletResponse，以便使用专有的方法
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        logger.info("[AuthFilter] before chain, ContextPath: {}, RequestURI: {}", req.getContextPath(), req.getRequestURI() );
        // 获取请求路径，去除上下文路径
        String path = req.getRequestURI().substring(req.getContextPath().length());
        // 遍历不需要鉴权的路径，如果请求路径以某个不需要鉴权的路径开头，则放行
        for (String excludePath : excludePaths) {
            if (path.startsWith(excludePath)) {
                logger.info("[AuthFilter] before chain, excludePath: {}", excludePath);
                chain.doFilter(request, response);
                logger.info("[AuthFilter] after chain, excludePath: {}", excludePath);
                // 白名单命中后必须 return，否则会继续执行登录检查：
                // 未登录时 login.jsp 会重定向到自己，造成无限重定向循环
                return;
            }
        }

        // 检查用户是否已登录（session 中是否有 user 属性）
        HttpSession session = req.getSession(false);
        boolean isLoggedIn = (session != null && session.getAttribute("user") != null);
        if (isLoggedIn) {
            logger.info("[AuthFilter] before chain, isLoggedIn: {}", true);
            chain.doFilter(request, response);
            logger.info("[AuthFilter] after chain, isLoggedIn: {}", true);
        } else {
            logger.info("[AuthFilter] redirect to loginPage: {}", loginPage);
            // 重定向路径必须携带上下文路径，否则浏览器会跳到 http://host:8080/login.jsp 导致 404
            resp.sendRedirect(req.getContextPath() + loginPage);
        }

    }

    @Override
    public void destroy() {
        logger.info("[AuthFilter] destroy");
    }
}
