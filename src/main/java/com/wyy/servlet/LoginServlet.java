package com.wyy.servlet;

import com.wyy.pojo.SysUser;
import com.wyy.utils.DbUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.*;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoginServlet.class);

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
            LOGGER.warn("LoginServlet 缺少必要参数, username: {}", username);
            resp.sendRedirect(req.getContextPath() + "/login.jsp?error=empty");
            return;
        }
        // 只查询登录需要的列，不使用 SELECT *
        String sql = "SELECT id, username, password FROM sys_user WHERE username = ?";
        LOGGER.info("LoginServlet username: {}", username);
        try (Connection conn = DbUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            // 绑定占位符参数，必须在 executeQuery 之前完成
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    // 将查询结果封装为用户对象，供密码比对与会话保存使用
                    SysUser user = new SysUser();
                    user.setId(rs.getInt("id"));
                    user.setUsername(rs.getString("username"));
                    user.setPassword(rs.getString("password"));
                    if (password.equals(user.getPassword())) {
                        // 登录成功：用户对象写入 session，AuthFilter 依赖该属性放行后续请求
                        req.getSession().setAttribute("user", user);
                        LOGGER.info("LoginServlet 登录成功, userId: {}, username: {}", user.getId(), user.getUsername());
                        // 直接向浏览器输出登录成功文本，不跳转页面；必须设置响应编码，否则中文乱码
                        resp.setContentType("text/html;charset=UTF-8");
                        resp.getWriter().write("登录成功，欢迎你：" + user.getUsername());
                    } else {
                        LOGGER.warn("LoginServlet 密码错误, username: {}", username);
                        resp.sendRedirect(req.getContextPath() + "/login.jsp?error=pwd");
                    }
                } else {
                    LOGGER.warn("LoginServlet 用户不存在, username: {}", username);
                    resp.sendRedirect(req.getContextPath() + "/login.jsp?error=noUser");
                }
            }
        } catch (SQLException e) {
            LOGGER.error("LoginServlet 登录查询异常", e);
            throw new RuntimeException(e);
        }
    }
}
