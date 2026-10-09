package com.wyy.service.impl;

import com.wyy.dao.SysUserDao;
import com.wyy.dao.impl.SysUserDaoImpl;
import com.wyy.entity.SysUser;
import com.wyy.mapper.SysUserMapper;
import com.wyy.service.UserService;
import com.wyy.utils.MybatisUtils;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;

/*
 * 替代 spring-xml.xml 第 52~54 行：
 * <bean id="userService" class="com.wyy.service.impl.UserServiceImpl">
 *     <property name="sysUserMapper" ref="sysUserMapper"/>
 * </bean>
 * bean 注册由 RootConfig 上的 @ComponentScan("com.wyy.service") 识别本注解完成。
 */
@Service
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    private SysUserMapper sysUserMapper;

    /*
     * 替代 spring-xml.xml 第 53 行：<property name="sysUserMapper" ref="sysUserMapper"/>
     * <p>
     * 【为什么保留 setter 而不是改成 @Autowired 字段注入】
     * 原项目走的是纯 XML setter 注入（XML 只能做 setter / 构造器注入，字段注入是注解独有能力）。
     * 把 @Autowired 加在 setter 上，既能取代 <property>，又保留了 setter 方法本身，
     * 使被替代但仍旧留档的 spring-xml.xml 描述与代码实际一致；
     * 若不在意 XML 归档的可运行性，也可以改为直接在本字段上写 @Autowired 省略 setter。
     * <p>
     * 注入的 sysUserMapper 不是 XML 里的 bean，而是 @MapperScan("com.wyy.mapper")
     * （取代 <bean class="MapperScannerConfigurer">）动态生成的代理 bean。
     */
    @Autowired
    public void setSysUserMapper(SysUserMapper sysUserMapper) {
        this.sysUserMapper = sysUserMapper;
    }
    // 无状态依赖在字段声明处初始化并加 final，实例生命周期内复用，不必每次调用都 new
    //    private final SysUserDao sysUserDao = new SysUserDaoImpl();

    // 使用HikariDataSource，但未使用 mybatis
    //    @Override
    //    public SysUser login(String username, String password) throws SQLException {
    //        SysUser user = sysUserDao.findByUsername(username);
    //        // 常量在前：即使数据库 password 列为 NULL 也不会抛 NPE
    //        if (user == null || !password.equals(user.getPassword())) {
    //            return null;
    //        }
    //        return user;
    //    }

    // 使用HikariDataSource + mybatis + spring
    //    @Override
    //    public SysUser login(String username, String password) throws SQLException {
    //
    //        try (SqlSession session = MybatisUtils.getSqlSessionFactory().openSession(true)){
    //            SysUserMapper mapper = session.getMapper(SysUserMapper.class);
    //        try {
    //            SysUser user = sysUserMapper.findByUsername(username);
    //            // 常量在前：即使数据库 password 列为 NULL 也不会抛 NPE
    //            if (user == null || !password.equals(user.getPassword())) {
    //                return null;
    //            }
    //            return user;
    //        } catch (Exception e) {
    //            logger.error("UserServiceImpl login error", e);
    //            throw new SQLException("UserServiceImpl login error", e);
    //        }
    //    }

    // 使用HikariDataSource + mybatis + Spring + aop + transaction
    /*
     * 【为什么本方法没有 @Transactional】
     * 本项目保留 spring-xml.xml 第 40~49 行那种“按方法名通配”的声明式事务，
     * 它已由 RootConfig 的 txAdvice（TransactionInterceptor + NameMatchTransactionAttributeSource）
     * 加上 txAdvisor（DefaultPointcutAdvisor，切点 execution(* com.wyy.service..*.*(..))）等价重建，
     * 事务在代理链上自动施加，方法本身不需要任何事务注解。
     * 本方法名 login 不匹配 get / find / list 三个前缀规则，命中 name="*" 那条：
     * propagation=REQUIRED、readOnly=false，与原 XML 完全一致。
     * 新增 service 方法时无需手写事务注解，只要方法名命中通配规则就自动纳入事务，
     * 与原 XML 一样不再需要人工逐个标注。
     */
    @Override
    public SysUser login(String username, String password) throws SQLException {
        SysUser user = sysUserMapper.findByUsername(username);
        // 常量在前：即使数据库 password 列为 NULL 也不会抛 NPE
        if (user == null || !password.equals(user.getPassword())) {
            return null;
        }
        return user;
    }

}
