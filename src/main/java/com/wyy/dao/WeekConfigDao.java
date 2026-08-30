package com.wyy.dao;

import com.wyy.entity.WeekConfig;

import java.sql.SQLException;
import java.util.List;

public interface WeekConfigDao {

    List<WeekConfig> findWeekConfigAll() throws SQLException;
}
