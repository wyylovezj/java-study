package com.wyy.service.impl;

import com.wyy.dao.WeekConfigDao;
import com.wyy.dao.impl.WeekConfigDaoImpl;
import com.wyy.entity.WeekConfig;
import com.wyy.mapper.WeekConfigMapper;
import com.wyy.service.WeekConfigService;
import com.wyy.utils.MybatisUtils;
import org.apache.ibatis.session.SqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;

public class WeekConfigServiceImpl implements WeekConfigService {

    private static final Logger logger = LoggerFactory.getLogger(WeekConfigServiceImpl.class);

    // 无状态依赖在字段声明处初始化并加 final，实例生命周期内复用，不必每次调用都 new
    //    private final WeekConfigDao weekConfigDao = new WeekConfigDaoImpl();
    //  使用HikariDataSource，但未使用 mybatis
    //    @Override
    //    public List<WeekConfig> listWeekConfigs() throws SQLException {
    //        return weekConfigDao.findWeekConfigAll();
    //    }

    @Override
    public List<WeekConfig> listWeekConfigs() throws SQLException {

        try( SqlSession session = MybatisUtils.getSqlSessionFactory().openSession(true)) {
            WeekConfigMapper mapper = session.getMapper(WeekConfigMapper.class);
            List<WeekConfig> weekConfig = mapper.findWeekConfigAll();
            if (weekConfig != null) {
                return weekConfig;
            }
            return null;
        } catch (Exception e) {
            logger.error("WeekConfigServiceImpl listWeekConfigs error", e);
            throw new SQLException("Failed to list week configs", e);
        }
    }
}
