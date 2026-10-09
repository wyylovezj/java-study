package com.wyy.controller;

import com.wyy.entity.SysUser;
import com.wyy.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.sql.SQLException;

/**
 * 注解式登录控制器，替代「老式 Controller + XML bean 映射」的
 * {@code com.wyy.controllerxml.LoginXmlController}。
 * <p>
 * 【被替代的 XML 配置（spring-mvc-xml.xml 第 62~64 行）】
 * <pre>
 *   <bean name="/login" class="com.wyy.controllerxml.LoginXmlController">
 *       <property name="userService" ref="userService"/>
 *   </bean>
 * </pre>
 * 逐点对应：
 * <ul>
 *   <li>{@code <bean name="/login">} 中的 <b>name 以 / 开头</b> 是配合
 *       {@code BeanNameUrlHandlerMapping} 完成 URL 映射的老机制（且要求 XML 显式声明 bean，
 *       因为实现 {@code Controller} 接口的类不带任何注解，组件扫描认不出来）
 *       → 现由 {@code @RequestMapping("/login")} 直接声明 URL 映射，
 *       由 {@code @Controller} + {@code @ComponentScan("com.wyy.controller")} 完成 bean 注册。</li>
 *   <li>{@code <property name="userService" ref="userService"/>} → {@code @Autowired} 字段注入。
 *       原注释提到「必须显式声明 property，否则 service 为 null 抛 NPE」，
 *       注解化后由 {@code @Autowired} 保证注入，按类型向父容器（{@code RootConfig}）查找 userService。</li>
 *   <li>{@code <mvc:annotation-driven/>}（spring-mvc-xml.xml 第 13 行）→ {@code WebConfig} 上的
 *       {@code @EnableWebMvc}，正是它让 {@code @RequestMapping} 系列注解生效。</li>
 * </ul>
 * 方法内部的业务逻辑、日志、Session 写入时机、异常处理策略均与原实现保持一致。
 *
 * @author 魏阳阳
 */
@Controller
@RequestMapping("/login")
public class LoginController {

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    /**
     * 替代 spring-mvc-xml.xml 第 63 行：
     * {@code <property name="userService" ref="userService"/>}
     * 字段注入是注解特有的能力（XML 只能 setter / 构造器注入），反射直接赋值，无需 setter。
     */
    @Autowired
    private UserService userService;

    /**
     * 替代原 {@code handleRequest} 里的 {@code if ("GET".equalsIgnoreCase(method))} 分支。
     * <p>
     * 【一处行为细化】原实现用 if 判断 method 字符串，其他请求方法（PUT/DELETE 等）走
     * {@code else return null;}（视为响应已处理完毕）。
     * 注解化后 {@code @GetMapping}/{@code @PostMapping} 由框架做方法匹配，
     * 未声明的请求方法会得到 405 Method Not Allowed —— 这是更规范的语义，也与老行为不同。
     */
    @GetMapping
    public String loginPage() {
        return "redirect:/login.jsp";
    }

    /**
     * 替代原 {@code handleRequest} 里的 {@code else if ("POST")} 分支。
     * <p>
     * {@code @RequestParam(required = false)} 对应原来的 {@code request.getParameter(...)}：
     * parameter 不存在时拿到 null，交由下面的空值校验返回 error=empty。
     * 若用默认的 {@code required = true}，缺参会直接抛
     * MissingServletRequestParameterException（返回 500 JSON），与原行为不符，所以必须关掉。
     *
     * @param username 用户名，缺失为 null
     * @param password 密码，缺失为 null
     * @param request  用于登录成功后创建 Session
     * @return 视图名，以 {@code redirect:} 前缀开头表示 302 重定向
     * @throws IOException Service 声明的 IO 异常，方法内未处理，向上冒泡由统一异常处理兜底
     */
    @PostMapping
    public String login(@RequestParam(value = "username", required = false) String username,
                        @RequestParam(value = "password", required = false) String password,
                        HttpServletRequest request) throws IOException {
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            logger.warn("LoginController 缺少必要参数, username: {}", username);
            return "redirect:/login.jsp?error=empty";
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
                logger.info("LoginController 登录成功, userId: {}, username: {}", user.getId(), user.getUsername());
                logger.info("request.getContextPath: {}", request.getContextPath());
                return "redirect:/index.jsp";
            }
            logger.warn("LoginController 登录校验失败, username: {}", username);
            // 原写法：modelAndView.addObject("error", "wrong") 挂在 redirect: 视图上，
            // 效果等同于把模型属性拼到重定向 URL 后面，故直接写成查询参数，语义一致
            return "redirect:/login.jsp?error=wrong";
        } catch (SQLException e) {
            // 数据库异常：记录完整堆栈后跳转登录页提示系统繁忙，不向用户暴露异常细节
            logger.error("LoginController 登录查询数据库异常, username: {}", username, e);
            return "redirect:/login.jsp?error=db";
        }
    }
}
