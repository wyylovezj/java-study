package com.wyy.controllerxml;


import com.alibaba.fastjson2.JSON;
import com.wyy.entity.WeekConfig;
import com.wyy.service.WeekConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.List;

public class WeekConfigXmlController implements Controller {

    private static final Logger logger = LoggerFactory.getLogger(WeekConfigXmlController.class);

    private WeekConfigService weekConfigService;
    public void setWeekConfigService(WeekConfigService weekConfigService) {
        this.weekConfigService = weekConfigService;
    }
    @Override
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String method = request.getMethod();
        if ("GET".equalsIgnoreCase(method)) {
            try {
                List<WeekConfig> weekConfigs = weekConfigService.listWeekConfigs();
                // 必须先设置响应编码再获取 Writer，否则中文乱码
                response.setContentType("application/json;charset=UTF-8");
                PrintWriter out = response.getWriter();
                out.print(JSON.toJSONString(weekConfigs));
                logger.info("查询结果: {}", weekConfigs);
            } catch (SQLException e) {
                // 记录完整堆栈后向上抛出，由 ExceptionFilter 统一返回错误响应，绝不静默吞异常
                logger.error("WeekConfigServlet 数据库查询失败", e);
                throw new ServletException("查询周配置失败", e);
            }
        } else if ("POST".equalsIgnoreCase(method)) {
            return null;
        }
        return null;
    }

}
