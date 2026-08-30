package com.wyy.service.impl;

import com.wyy.dao.SysUserDao;
import com.wyy.dao.impl.SysUserDaoImpl;
import com.wyy.entity.SysUser;
import com.wyy.service.UserService;

import java.sql.SQLException;

public class UserServiceImpl implements UserService {

    // 无状态依赖在字段声明处初始化并加 final，实例生命周期内复用，不必每次调用都 new
    private final SysUserDao sysUserDao = new SysUserDaoImpl();

    @Override
    public SysUser login(String username, String password) throws SQLException {
        SysUser user = sysUserDao.findByUsername(username);
        // 常量在前：即使数据库 password 列为 NULL 也不会抛 NPE
        if (user == null || !password.equals(user.getPassword())) {
            return null;
        }
        return user;
    }
}
