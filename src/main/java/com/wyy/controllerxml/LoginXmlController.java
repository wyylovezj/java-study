package com.wyy.controllerxml;

import com.wyy.entity.SysUser;
import com.wyy.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.sql.SQLException;


/**
 * LoginXmlController
 * @author 张三
 * @date 2026-09-14 15:20:49
 * @since 
 */
public class LoginXmlController implements Controller {
    private static final Logger logger = LoggerFactory.getLogger(LoginXmlController.class);


    /**
     * 用户服务
     * 字段说明：
     *
     * @see UserService：
     */
    private UserService userService;
    public void setUserService(UserService userService) {
        this.userService = userService;
    }


/**
 * 
 * @author 张三
 * @date 2026-09-14 15:22
 * 
 * @param null
 * 
 * @return null
 * @throws 
 * @throws null
 */
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String method = request.getMethod();
        if ("GET".equalsIgnoreCase(method)) {
            return new ModelAndView("redirect:/login.jsp");
        } else if ("POST".equalsIgnoreCase(method)) {
            String username = request.getParameter("username");
            String password = request.getParameter("password");
            if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
                logger.warn("LoginServlet 缺少必要参数, username: {}", username);
                return new ModelAndView("redirect:/login.jsp?error=empty");
            }
            logger.info("username: {}, password: {}", username, password);
            try {
                // 校验通过返回用户对象，失败统一返回 null（不区分用户不存在/密码错误，防用户名枚举）
                SysUser user = userService.login(username, password);
                if (user != null) {
                    // 用户对象写入 session，AuthFilter 依赖该属性放行后续请求。
                    // req.getSession() 是全站唯一创建会话的入口（login.jsp/error.jsp 已 session="false"）：
                    // 登录成功才建会话，访客浏览登录页、登录失败重试都不会产生空会话，
                    // 在线人数也就不会被这些 30 分钟后才销毁的会话虚增
                    request.getSession().setAttribute("user", user);
                    logger.info("LoginServlet 登录成功, userId: {}, username: {}", user.getId(), user.getUsername());
                    logger.info("request.getContextPath: {}", request.getContextPath()); // 打印请求的上下文路径
                    ModelAndView modelAndView = new ModelAndView("redirect:/index.jsp");
                    return modelAndView;
                } else {
                    logger.warn("LoginServlet 登录校验失败, username: {}", username);
                    ModelAndView modelAndView = new ModelAndView("redirect:/login.jsp");
                    modelAndView.addObject("error", "wrong");
                    return modelAndView;
                }
            } catch (SQLException e) {
                // 数据库异常：记录完整堆栈后跳转登录页提示系统繁忙，不向用户暴露异常细节
                logger.error("LoginServlet 登录查询数据库异常, username: {}", username, e);
                ModelAndView modelAndView = new ModelAndView("redirect:/login.jsp");
                modelAndView.addObject("error", "db");
                return modelAndView;
            }
        } else {
            return null;
        }
    }
}
