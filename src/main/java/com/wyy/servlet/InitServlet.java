package com.wyy.servlet;

import com.wyy.utils.DbUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServlet;

public class InitServlet extends HttpServlet {

    private static Logger logger = LoggerFactory.getLogger(InitServlet.class);

    @Override
    public void init() {
        DbUtils.getDataSource();
        logger.info("InitServlet 初始化完成，连接池已就绪");
    }
}
