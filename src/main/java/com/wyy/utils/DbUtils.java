package com.wyy.utils;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public class DbUtils {

    private static final Logger logger = LoggerFactory.getLogger(DbUtils.class);
    private static final HikariDataSource dataSource;

    static {
        try {
            Properties props = new Properties();
            InputStream in = DbUtils.class.getClassLoader().getResourceAsStream("jdbc.properties");
            props.load(in);
            HikariConfig config = new HikariConfig();
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
        } catch (Exception e) {
            logger.error("加载 配置失败 驱动失败", e);
            throw new RuntimeException(e);
        }
    }

    public static HikariDataSource getDataSource() {
        return dataSource;
    }

    public static Connection getConnection() throws SQLException {
        logger.info("获取数据库连接成功");
        return dataSource.getConnection();
    }
}
