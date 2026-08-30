package com.wyy.dao.impl;

import com.wyy.dao.SysUserDao;
import com.wyy.entity.SysUser;
import com.wyy.utils.DbUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SysUserDaoImpl implements SysUserDao {

    private static final Logger logger = LoggerFactory.getLogger(SysUserDaoImpl.class);

    @Override
    public List<SysUser> findUserAll() throws SQLException {
        String sql = "select id, username, password from sys_user";
        // 实体是数据快照：每行结果必须 new 一个新对象，复用成员变量会导致
        // 列表元素全部指向同一实例（只剩最后一行数据），且并发请求下互相覆盖
        List<SysUser> users = new ArrayList<>();
        try (Connection conn = DbUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                SysUser user = new SysUser();
                user.setId(rs.getInt("id"));
                user.setUsername(rs.getString("username"));
                user.setPassword(rs.getString("password"));
                users.add(user);
            }
        } catch (Exception e) {
            logger.error("SysUserDaoImpl findUserAll error", e);
            throw new SQLException("SysUserDaoImpl findUserAll error", e);
        }
        return users;
    }

    @Override
    public SysUser findByUsername(String username) throws SQLException {
        String sql = "select id, username, password from sys_user where username = ?";
        try (Connection conn = DbUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                // 唯一键查询最多命中一行，用 if 即可；查无此人返回 null，由上层判断
                if (rs.next()) {
                    SysUser user = new SysUser();
                    user.setId(rs.getInt("id"));
                    user.setUsername(rs.getString("username"));
                    user.setPassword(rs.getString("password"));
                    return user;
                }
            }
        } catch (Exception e) {
            logger.error("SysUserDaoImpl findByUsername error", e);
            throw new SQLException("SysUserDaoImpl findByUsername error", e);
        }
        return null;
    }
}
