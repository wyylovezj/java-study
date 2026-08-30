package com.wyy.listener;

import com.wyy.utils.DbUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class ServletListener implements ServletContextListener {

    private static Logger logger = LoggerFactory.getLogger(ServletListener.class);

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // 仅触发 DbUtils 类加载：静态块完成连接池初始化；
        // 初始化失败时 JVM 会以 ExceptionInInitializerError（Error 体系）向上抛出，直接导致 Tomcat 启动失败（fail-fast），
        // 此处无需也无法用 catch (Exception) 捕获，错误日志已在静态块中记录
        DbUtils.getDataSource();
        logger.info("ServletListener 初始化完成，连接池已就绪");
        // 应用启动时执行
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // 应用关闭/热部署时释放连接池，避免连接泄漏
        DbUtils.close();
        logger.info("ServletListener 已销毁，连接池已释放");
    }
}
