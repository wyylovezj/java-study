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
                weekConfig.setCreatedTime(rs.getTimestamp("created_time").toLocalDateTime());
                weekConfig.setUpdatedTime(rs.getTimestamp("updated_time").toLocalDateTime());
            }
            PrintWriter out = resp.getWriter();
            resp.setContentType("application/json;charset=UTF-8");
            out.print(JSON.toJSONString(weekConfig));
            logger.info("查询结果: {}", weekConfig);
        }
        catch (SQLException e) {
            logger.error("数据库操作失败", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String username = req.getParameter("username");
        logger.info("username: {}", username);
        System.out.printf("username:%s", username);
        PrintWriter out = resp.getWriter();
        resp.setContentType("application/json;charset=UTF-8");
        out.print(username);

    }
}
