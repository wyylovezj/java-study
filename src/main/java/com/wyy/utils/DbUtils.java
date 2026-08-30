package com.wyy.utils;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;


/**
 * 数据库连接池工具类
 * 通过 HikariCP 连接池获取数据库连接
 */
public class DbUtils {

    private static final Logger logger = LoggerFactory.getLogger(DbUtils.class);
    private static final HikariDataSource dataSource;

    static {
        // try-with-resources 确保配置文件流用完即关
        try (InputStream in = DbUtils.class.getClassLoader().getResourceAsStream("jdbc.properties")) {
            Properties props = new Properties();
            props.load(in);
            HikariConfig config = new HikariConfig();
            // 命名连接池，日志与监控中可定位到具体池
            config.setPoolName(props.getProperty("pool.poolName", "rbac-pool"));
            config.setJdbcUrl(props.getProperty("db.url"));
            config.setUsername(props.getProperty("db.username"));
            config.setPassword(props.getProperty("db.password"));
            config.setDriverClassName(props.getProperty("db.driver"));
            config.setMaximumPoolSize(Integer.parseInt(props.getProperty("pool.maximumPoolSize")));
            config.setMinimumIdle(Integer.parseInt(props.getProperty("pool.minimumIdle")));
            config.setMaxLifetime(Integer.parseInt(props.getProperty("pool.maxLifetime")));
            config.setConnectionTimeout(Integer.parseInt(props.getProperty("pool.connectionTimeout")));
            config.setIdleTimeout(Integer.parseInt(props.getProperty("pool.idleTimeout")));
            dataSource = new HikariDataSource(config);
            logger.info("HikariCP 连接池初始化成功");
        } catch (Exception e) {
            logger.error("加载 jdbc.properties / 初始化连接池失败", e);
            throw new RuntimeException(e);
        }
    }

    public static HikariDataSource getDataSource() {
        return dataSource;
    }

    /**
     * 关闭连接池：由 ServletListener 在应用销毁时调用，避免热部署/停机时连接泄漏
     */
    public static void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("HikariCP 连接池已关闭");
        }
    }

    public static Connection getConnection() throws SQLException {
        // 高频调用：仅从池中借出连接，不打 info 日志避免刷屏，链路耗时由 LogFilter 统一记录
        return dataSource.getConnection();
    }
}
