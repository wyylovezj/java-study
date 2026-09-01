package com.wyy.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class SpringContextListener implements ServletContextListener {

    private static final Logger logger = LoggerFactory.getLogger(SpringContextListener.class);

    // 定义存储Spring根容器上下文的属性名称
    public static final String SPRING_CONTEXT_ATTRIBUTE = "springContext";
    @Override
    public void contextInitialized(ServletContextEvent sce) {

        // 读取配置文件中的contextConfigLocation参数
        String location = sce.getServletContext().getInitParameter("contextConfigLocation");
        logger.info("Spring context initialized with location: {}", location);
        // 创建Spring根容器
        ApplicationContext ctx = new ClassPathXmlApplicationContext(location);
        // 将Spring根容器存储到ServletContext中，全局唯一
        sce.getServletContext().setAttribute(SPRING_CONTEXT_ATTRIBUTE, ctx);
        logger.info("Spring 根容器初始化成功, 配置: {}", location);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // close() 会回调 bean 的 destroy-method（如 dataSource 的 close），
        // 连接池释放职责由容器接管，ServletListener 里的 MybatisUtils.close() 可退役
        ApplicationContext ctx = (ApplicationContext) sce.getServletContext().getAttribute(SPRING_CONTEXT_ATTRIBUTE);
        if (ctx != null) {
            ((ClassPathXmlApplicationContext) ctx).close();
        }
    }
}
