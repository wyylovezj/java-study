package com.wyy;

import com.wyy.entity.WeekConfig;
import com.wyy.utils.DbUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Unit test for simple App.
 */
public class AppTest {

    private static final Logger log = LoggerFactory.getLogger(AppTest.class);

    @Test
    public void testQueryWeekConfig() throws Exception {
        String sql = "select * from week_config";
        WeekConfig weekConfig = new WeekConfig();
        try (Connection conn = DbUtils.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                weekConfig.setId(rs.getInt("id"));
                weekConfig.setYear(rs.getInt("year"));
                weekConfig.setWeekNum(rs.getInt("week_num"));
                weekConfig.setStartDate(rs.getDate("start_date"));
                weekConfig.setEndDate(rs.getDate("end_date"));
                // created_time/updated_time 已从实体中移除，不再封装
            }
            log.info("Query result: {}", weekConfig);
        }
    }
}
