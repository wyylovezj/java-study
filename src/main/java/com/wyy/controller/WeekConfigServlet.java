package com.wyy.controller;

import com.alibaba.fastjson2.JSON;
import com.wyy.entity.WeekConfig;
import com.wyy.listener.SpringContextListener;
import com.wyy.service.WeekConfigService;
import com.wyy.service.impl.WeekConfigServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
//import org.springframework.web.context.WebApplicationContext;
//import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.List;

public class WeekConfigServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(WeekConfigServlet.class);

    // 无状态 Service 可作为字段复用，不必每个请求都创建；
    // controller 只依赖 service 接口，不越层触碰 dao 与 JDBC
    //    private final WeekConfigService weekConfigService = new WeekConfigServiceImpl();

    private WeekConfigService weekConfigService;
    @Override
    public void init() throws ServletException {
//        WebApplicationContext ctx =
//                WebApplicationContextUtils.getWebApplicationContext(getServletContext());
        ApplicationContext ctx = (ApplicationContext) getServletContext().getAttribute(SpringContextListener.SPRING_CONTEXT_ATTRIBUTE);
        this.weekConfigService = ctx.getBean(WeekConfigService.class);
    }
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        try {
            List<WeekConfig> weekConfigs = weekConfigService.listWeekConfigs();
            // 必须先设置响应编码再获取 Writer，否则中文乱码
            resp.setContentType("application/json;charset=UTF-8");
            PrintWriter out = resp.getWriter();
            out.print(JSON.toJSONString(weekConfigs));
            logger.info("查询结果: {}", weekConfigs);
        } catch (SQLException e) {
            // 记录完整堆栈后向上抛出，由 ExceptionFilter 统一返回错误响应，绝不静默吞异常
            logger.error("WeekConfigServlet 数据库查询失败", e);
            throw new ServletException("查询周配置失败", e);
        }
    }
}
