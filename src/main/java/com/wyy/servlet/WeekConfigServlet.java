package com.wyy.servlet;

import com.alibaba.fastjson2.JSON;
import com.wyy.pojo.WeekConfig;
import com.wyy.utils.DbUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;

@WebServlet("/getWeekConfig")
public class WeekConfigServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(WeekConfigServlet.class);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String sql = "select * from week_config";
        WeekConfig weekConfig = new WeekConfig();
        try (Connection conn = DbUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery();) {
            if (rs.next()) {
                weekConfig.setId(rs.getInt("id"));
                weekConfig.setYear(rs.getInt("year"));
                weekConfig.setWeekNum(rs.getInt("week_num"));
                weekConfig.setStartDate(rs.getDate("start_date"));
                weekConfig.setEndDate(rs.getDate("end_date"));
                // created_time/updated_time 可能为 NULL，判空避免自动拆箱 NPE
                Timestamp createdTime = rs.getTimestamp("created_time");
                if (createdTime != null) {
                    weekConfig.setCreatedTime(createdTime.toLocalDateTime());
                }
                Timestamp updatedTime = rs.getTimestamp("updated_time");
                if (updatedTime != null) {
                    weekConfig.setUpdatedTime(updatedTime.toLocalDateTime());
                }
            } else {
                logger.warn("week_config 表无数据");
            }
            // 必须先设置响应编码再获取 Writer，否则中文乱码
            resp.setContentType("application/json;charset=UTF-8");
            PrintWriter out = resp.getWriter();
            out.print(JSON.toJSONString(weekConfig));
            logger.info("查询结果: {}", weekConfig);
        } catch (SQLException e) {
            // 记录完整堆栈后向上抛出，由 ExceptionFilter 统一返回错误响应，绝不静默吞异常
            logger.error("WeekConfigServlet 数据库查询失败", e);
            throw new ServletException("查询周配置失败", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String username = req.getParameter("username");
        if (username == null || username.trim().isEmpty()) {
            logger.warn("WeekConfigServlet 缺少参数 username");
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.setContentType("application/json;charset=UTF-8");
            resp.getWriter().write("{\"code\":400,\"message\":\"参数 username 不能为空\"}");
            return;
        }
        logger.info("username: {}", username);
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        out.print(username);
    }
}
