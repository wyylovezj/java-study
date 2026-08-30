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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;

public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

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

    // 使用HikariDataSource + mybatis
    @Override
    public SysUser login(String username, String password) throws SQLException {

        try (SqlSession session = MybatisUtils.getSqlSessionFactory().openSession(true)){
            SysUserMapper mapper = session.getMapper(SysUserMapper.class);
            SysUser user = mapper.findByUsername(username);
            // 常量在前：即使数据库 password 列为 NULL 也不会抛 NPE
            if (user == null || !password.equals(user.getPassword())) {
                return null;
            }
            return user;
        } catch (Exception e) {
            logger.error("UserServiceImpl login error", e);
            throw new SQLException("UserServiceImpl login error", e);
        }
    }
}
