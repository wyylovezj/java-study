package com.wyy.controllerservlet;

import com.wyy.entity.SysUser;
import com.wyy.service.UserService;
import com.wyy.service.impl.UserServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
//import org.springframework.web.context.WebApplicationContext;
//import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

public class LoginServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(LoginServlet.class);
    // 不引入 Spring，自行创建 Service 实例
    // 无状态 Service 可作为字段复用，不必每个请求都创建；
    // controller 只依赖 service 接口，不越层触碰 dao 与 JDBC
    // private UserService userService = new UserServiceImpl();

    // 引入 Spring，通过依赖注入获取 Service 实例
    private UserService userService;
    // Servlet 初始化时注入 Service，避免每次请求都创建
    @Override
    public void init() throws ServletException {

        // Spring 或 Spring MVC 环境下，通过spring-web(Spring MVC的依赖包)包工具类获取根容器 WebApplicationContext,
        WebApplicationContext ctx =
                WebApplicationContextUtils.getWebApplicationContext(getServletContext());
        //  纯 Spring 环境下，获取 Spring IOC 根容器，全局唯一
        //  ApplicationContext ctx = (ApplicationContext) getServletContext().getAttribute(SpringContextListener.SPRING_CONTEXT_ATTRIBUTE);

        // 在未被IOC容器管理的类中，通过WebApplicationContext的getBean()方法获取Bean实例
        // 通过类型获取Bean实例
        // this.userService = ctx.getBean(UserServiceImpl.class);
        // 通过名称+类型获取Bean实例
        // this.userService = ctx.getBean("userService", UserServiceImpl.class);
        // 通过名称获取Bean实例，需要强制类型转换
        this.userService = (UserServiceImpl) ctx.getBean("userService");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // GET 方式访问登录接口时直接跳转登录页，登录必须 POST 提交
        resp.sendRedirect(req.getContextPath() + "/login.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");
        // 参数非空校验，避免空值参与 SQL 查询与密码比对（password 为 null 时 equals 会 NPE）
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            logger.warn("LoginServlet 缺少必要参数, username: {}", username);
            resp.sendRedirect(req.getContextPath() + "/login.jsp?error=empty");
            return;
        }
        try {
            // 校验通过返回用户对象，失败统一返回 null（不区分用户不存在/密码错误，防用户名枚举）
            SysUser user = userService.login(username, password);
            if (user != null) {
                // 用户对象写入 session，AuthFilter 依赖该属性放行后续请求。
                // req.getSession() 是全站唯一创建会话的入口（login.jsp/error.jsp 已 session="false"）：
                // 登录成功才建会话，访客浏览登录页、登录失败重试都不会产生空会话，
                // 在线人数也就不会被这些 30 分钟后才销毁的会话虚增
                req.getSession().setAttribute("user", user);
                logger.info("LoginServlet 登录成功, userId: {}, username: {}", user.getId(), user.getUsername());
                resp.sendRedirect(req.getContextPath() + "/index.jsp");
            } else {
                logger.warn("LoginServlet 登录校验失败, username: {}", username);
                resp.sendRedirect(req.getContextPath() + "/login.jsp?error=wrong");
            }
        } catch (SQLException e) {
            // 数据库异常：记录完整堆栈后跳转登录页提示系统繁忙，不向用户暴露异常细节
            logger.error("LoginServlet 登录查询数据库异常, username: {}", username, e);
            resp.sendRedirect(req.getContextPath() + "/login.jsp?error=db");
        }
    }
}
