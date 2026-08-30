package com.wyy.service.impl;

import com.wyy.dao.WeekConfigDao;
import com.wyy.dao.impl.WeekConfigDaoImpl;
import com.wyy.entity.WeekConfig;
import com.wyy.service.WeekConfigService;

import java.sql.SQLException;
import java.util.List;

public class WeekConfigServiceImpl implements WeekConfigService {

    // 无状态依赖在字段声明处初始化并加 final，实例生命周期内复用，不必每次调用都 new
    private final WeekConfigDao weekConfigDao = new WeekConfigDaoImpl();

    @Override
    public List<WeekConfig> listWeekConfigs() throws SQLException {
        return weekConfigDao.findWeekConfigAll();
    }
}
