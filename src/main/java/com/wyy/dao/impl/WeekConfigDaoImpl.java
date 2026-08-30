package com.wyy.dao.impl;

import com.wyy.dao.WeekConfigDao;
import com.wyy.entity.WeekConfig;
import com.wyy.utils.JdbcUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class WeekConfigDaoImpl implements WeekConfigDao {

    @Override
    public List<WeekConfig> findWeekConfigAll() throws SQLException {
        // 明确列清单，不使用 SELECT *：避免表结构变更带来的隐式耦合与无效数据传输
        String sql = "select id, year, week_num, start_date, end_date from week_config";
        List<WeekConfig> weekConfigs = new ArrayList<>();
        try(Connection conn = JdbcUtils.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                // 每行 new 一个新对象，保证列表元素各自独立、互不覆盖
                WeekConfig weekConfig = new WeekConfig();
                weekConfig.setId(rs.getInt("id"));
                weekConfig.setYear(rs.getInt("year"));
                weekConfig.setWeekNum(rs.getInt("week_num"));
                weekConfig.setStartDate(rs.getDate("start_date"));
                weekConfig.setEndDate(rs.getDate("end_date"));
                weekConfigs.add(weekConfig);
            }
        }
        return weekConfigs;
    }
}
