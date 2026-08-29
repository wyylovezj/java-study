package com.wyy.servlet;

import com.wyy.utils.DbUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServlet;

public class InitServlet extends HttpServlet {

    private static Logger logger = LoggerFactory.getLogger(InitServlet.class);

    @Override
    public void init() {
        try {
            DbUtils.getDataSource();
            logger.info("InitServlet 初始化完成，连接池已就绪");
        } catch (Exception e) {
            // 连接池初始化失败直接终止应用启动（fail-fast），避免带病运行
            logger.error("InitServlet 初始化连接池失败", e);
            throw new RuntimeException(e);
        }
    }
}
