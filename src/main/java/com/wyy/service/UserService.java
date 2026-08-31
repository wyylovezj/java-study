package com.wyy.service;

import com.wyy.entity.SysUser;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;

public interface UserService {

    /**
     * 用户登录校验
     *
     * @return 校验通过返回用户对象，供 Controller 写入 Session；
     *         用户不存在与密码错误统一返回 null，不向外部区分两种失败原因（防用户名枚举）
     */
    SysUser login(String username, String password) throws SQLException, IOException;
}
