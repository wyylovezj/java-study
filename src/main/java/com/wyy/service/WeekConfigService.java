package com.wyy.service;

import com.wyy.entity.WeekConfig;

import java.sql.SQLException;
import java.util.List;

public interface WeekConfigService {

    /**
     * 查询全部周配置
     *
     * @return 周配置列表，表无数据时返回空列表
     */
    List<WeekConfig> listWeekConfigs() throws SQLException;
}
