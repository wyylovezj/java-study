package com.wyy.utils;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.datasource.DataSourceFactory;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * MyBatis 数据源工厂：桥接 MyBatis 与 HikariCP
 * <p>
 * MyBatis 解析 {@code <dataSource type="..."/>} 时会强转
 * {@code (DataSourceFactory) newInstance()}，而 HikariDataSource 没有实现该接口，
 * 直接写它的全限定类名会在构建 SqlSessionFactory 时抛 ClassCastException，
 * 所以必须由本工厂代为创建连接池。
 * <p>
 * 键名差异：MyBatis 配置习惯用 driver/url，Hikari 需要的是
 * driverClassName/jdbcUrl，setProperties 里做了转换
 */
public class HikariDataSourceFactory implements DataSourceFactory {

    private HikariDataSource dataSource;

    @Override
    public void setProperties(Properties props) {
        HikariConfig config = new HikariConfig();
        // 键名转换：driver → driverClassName，url → jdbcUrl
        config.setDriverClassName(props.getProperty("driver"));
        config.setJdbcUrl(props.getProperty("url"));
        config.setUsername(props.getProperty("username"));
        config.setPassword(props.getProperty("password"));
        // 池参数可选：mybatis-config 配了才生效，未配置走默认值（语义与 jdbc.properties 一致）
        config.setPoolName(props.getProperty("poolName", "mybatis-hikari-pool"));
        config.setMaximumPoolSize(optInt(props, "maximumPoolSize", 10));
        config.setMinimumIdle(optInt(props, "minimumIdle", 5));
        config.setConnectionTimeout(optLong(props, "connectionTimeout", 30000L));
        config.setIdleTimeout(optLong(props, "idleTimeout", 600000L));
        config.setMaxLifetime(optLong(props, "maxLifetime", 1800000L));

        this.dataSource = new HikariDataSource(config);
    }

    @Override
    public DataSource getDataSource() {

        return dataSource;
    }

    // 读取 int 属性，未配置返回默认值
    private static int optInt(Properties props, String key, int def) {
        String v = props.getProperty(key);
        return v == null || v.isEmpty() ? def : Integer.parseInt(v.trim());
    }

    // 读取 long 属性，未配置返回默认值
    private static long optLong(Properties props, String key, long def) {
        String v = props.getProperty(key);
        return v == null || v.isEmpty() ? def : Long.parseLong(v.trim());
    }
}
