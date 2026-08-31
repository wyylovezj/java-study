package com.wyy.utils;

import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;

public class MybatisUtils {

    // 日志对象，用于记录日志
    private static final Logger logger = LoggerFactory.getLogger(MybatisUtils.class);
    // 仅在静态块中赋值一次，应用生命周期内复用（工厂线程安全，可被所有请求共享）
    private static final SqlSessionFactory sqlSessionFactory;

    // 私有构造器，防止外部实例化
    private MybatisUtils() {}

    static {
        // build(InputStream) 内部 finally 会自动关流，但 try-with-resources 显式关闭不依赖版本实现
        try (InputStream in = Resources.getResourceAsStream("mybatis-config.xml")) {
            // ★ 必须赋给静态字段：若像局部变量一样接收，getSqlSessionFactory() 将永远返回 null
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(in);
            logger.info("SqlSessionFactory 初始化成功");
        } catch (IOException e) {
            throw new ExceptionInInitializerError("MyBatis 初始化失败: " + e.getMessage());
        }
    }

    // SqlSessionFactory：全局唯一，应用启动时创建一次（接口本身无 close，无需也无法关闭）
    public static SqlSessionFactory getSqlSessionFactory() {
        return sqlSessionFactory;
    }

    /**
     * 每次业务操作开一个新会话；调用方必须用 try-with-resources 关闭：
     * try (SqlSession session = MybatisUtils.openSession()) { ... }
     * SqlSession 不关会长期占住池内连接，这是 MyBatis 最常见的资源泄漏点
     */
    public static SqlSession openSession() {
        return sqlSessionFactory.openSession();
    }

    /**
     * 关闭 MyBatis 管理的数据源（HikariCP 连接池）：
     * SqlSessionFactory 没有把手，连接池要从 Configuration 里取出来再关；
     * 由 ServletListener 在应用销毁时调用，与 DbUtils.close() 同一职责
     */
    public static void close() {
        if (sqlSessionFactory == null) {
            return;
        }
        DataSource ds = sqlSessionFactory.getConfiguration().getEnvironment().getDataSource();
        if (ds instanceof HikariDataSource && !((HikariDataSource) ds).isClosed()) {
            ((HikariDataSource) ds).close();
            logger.info("MyBatis HikariCP 连接池已关闭");
        }
    }


}
