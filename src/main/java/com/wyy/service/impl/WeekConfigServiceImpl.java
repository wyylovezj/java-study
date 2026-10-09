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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;

/*
 * 替代 spring-xml.xml 第 55~57 行：
 * <bean id="weekConfigService" class="com.wyy.service.impl.WeekConfigServiceImpl">
 *     <property name="weekConfigMapper" ref="weekConfigMapper"/>
 * </bean>
 * bean 注册由 RootConfig 上的 @ComponentScan("com.wyy.service") 识别本注解完成。
 */
@Service
public class WeekConfigServiceImpl implements WeekConfigService {

    private static final Logger logger = LoggerFactory.getLogger(WeekConfigServiceImpl.class);

    private WeekConfigMapper weekConfigMapper;
    // 无状态依赖在字段声明处初始化并加 final，实例生命周期内复用，不必每次调用都 new
    //    private final WeekConfigDao weekConfigDao = new WeekConfigDaoImpl();
    //  使用HikariDataSource，但未使用 mybatis
    //    @Override
    //    public List<WeekConfig> listWeekConfigs() throws SQLException {
    //        return weekConfigDao.findWeekConfigAll();
    //    }

    /*
     * 【为什么本方法没有 @Transactional】
     * 本项目保留 spring-xml.xml 第 41~44 行的通配规则：
     * <tx:method name="get*|find*|list*" read-only="true"/>
     * 它已由 RootConfig 的 txAdvice + txAdvisor 两个 bean 等价重建（get / find / list 三个前缀 → 只读 + REQUIRED，
     * 其余任意方法名 → REQUIRED）；方法名 listWeekConfigs 命中 list 前缀，因此自动走 readOnly=true 的事务，
     * 无需在本方法上做任何标注。
     */
    @Override
    public List<WeekConfig> listWeekConfigs() throws SQLException {

//        try( SqlSession session = MybatisUtils.getSqlSessionFactory().openSession(true)) {
//            WeekConfigMapper mapper = session.getMapper(WeekConfigMapper.class);
//            List<WeekConfig> weekConfig = mapper.findWeekConfigAll();
        try {
            List<WeekConfig> weekConfig = weekConfigMapper.findWeekConfigAll();
            if (weekConfig != null) {
                return weekConfig;
            }
            return null;
        } catch (Exception e) {
            logger.error("WeekConfigServiceImpl listWeekConfigs error", e);
            throw new SQLException("Failed to list week configs", e);
        }
    }


    /*
     * 替代 spring-xml.xml 第 56 行：<property name="weekConfigMapper" ref="weekConfigMapper"/>
     * 加 @Autowired 后即不再依赖 XML 也能完成注入；保留 setter 是为了让留档的 XML 描述与代码一致。
     * 注入的 weekConfigMapper 由 @MapperScan("com.wyy.mapper")（取代 MapperScannerConfigurer）生成。
     */
    @Autowired
    public void setWeekConfigMapper(WeekConfigMapper weekConfigMapper) {
        this.weekConfigMapper = weekConfigMapper;
    }
}
