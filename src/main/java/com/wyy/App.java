package com.wyy;

import com.wyy.pojo.WeekConfig;
import com.wyy.utils.DbUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

/**
 * Hello world!
 *
 */
public class App {
    private static final Logger logger = LoggerFactory.getLogger(App.class);


//    public static void main( String[] args ) throws SQLException {
//        String URL = "jdbc:mysql://localhost:3306/rbac?useUnicode=true&characterEncoding=utf-8&zeroDateTimeBehavior=convertToNull&transformedBitIsBoolean=true&allowMultiQueries=true&allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=Asia/Shanghai";
//        String user = "root";
//        String password = "940429";
//        String sql = "select * from week_config";
//        WeekConfig weekConfig = new WeekConfig();
//        try {
//            Class.forName("com.mysql.cj.jdbc.Driver");
//        } catch (ClassNotFoundException e) {
//            logger.error("加载 MySQL 驱动失败", e);
//        }
//        try (Connection conn = DriverManager.getConnection(URL, user, password);
//            Statement stmt = conn.createStatement();
//            ResultSet rs = stmt.executeQuery(sql);){
//            if (rs.next()) {
//                weekConfig.setId(rs.getInt("id"));
//                weekConfig.setYear(rs.getInt("year"));
//                weekConfig.setWeekNum(rs.getInt("week_num"));
//                weekConfig.setStartDate(rs.getDate("start_date"));
//                weekConfig.setEndDate(rs.getDate("end_date"));
//                weekConfig.setCreatedTime(rs.getTimestamp("created_time").toLocalDateTime());
//                weekConfig.setUpdatedTime(rs.getTimestamp("updated_time").toLocalDateTime());
//            }
//            logger.info("查询结果: {}", weekConfig);
//        }
//        catch (SQLException e) {
//            logger.error("数据库操作失败", e);
//        }
//    }
    public static void main(String[] args) throws SQLException {
        String sql = "select * from week_config";
        WeekConfig weekConfig = new WeekConfig();
        try (Connection conn = DbUtils.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql);) {
            if (rs.next()) {
                weekConfig.setId(rs.getInt("id"));
                weekConfig.setYear(rs.getInt("year"));
                weekConfig.setWeekNum(rs.getInt("week_num"));
                weekConfig.setStartDate(rs.getDate("start_date"));
                weekConfig.setEndDate(rs.getDate("end_date"));
                weekConfig.setCreatedTime(rs.getTimestamp("created_time").toLocalDateTime());
                weekConfig.setUpdatedTime(rs.getTimestamp("updated_time").toLocalDateTime());
            }
            logger.info("查询结果: {}", weekConfig);
        }
        catch (SQLException e) {
            logger.error("数据库操作失败", e);
        }
    }
}
