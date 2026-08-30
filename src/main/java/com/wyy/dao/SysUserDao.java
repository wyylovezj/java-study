package com.wyy.dao;

import com.wyy.entity.SysUser;

import java.sql.SQLException;
import java.util.List;

public interface SysUserDao {

    List<SysUser> findUserAll() throws SQLException;

    SysUser findByUsername(String username) throws SQLException;
}
