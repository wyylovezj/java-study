package com.wyy.mapper;

import com.wyy.entity.SysUser;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.sql.SQLException;
import java.util.List;

public interface SysUserMapper {

    List<SysUser> findUserAll();

    @Select("SELECT id, username, password FROM sys_user where username = #{username}")
    SysUser findByUsername(@Param("username") String username);
}
