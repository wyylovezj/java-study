package com.wyy.controller;

import com.wyy.entity.WeekConfig;
import com.wyy.service.WeekConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.ServletException;
import java.sql.SQLException;
import java.util.List;

/**
 * 注解式周配置控制器，替代「老式 Controller + XML bean 映射」的
 * {@code com.wyy.controllerxml.WeekConfigXmlController}。
 * <p>
 * 【被替代的 XML 配置（spring-mvc-xml.xml 第 65~67 行）】
 * <pre>
 *   <bean name="/getWeekConfig" class="com.wyy.controllerxml.WeekConfigXmlController">
 *       <property name="weekConfigService" ref="weekConfigService"/>
 *   </bean>
 * </pre>
 * 对应关系与 {@link LoginController} 完全相同：
 * <ul>
 *   <li>bean name 以 / 开头配合 BeanNameUrlHandlerMapping → {@code @RequestMapping("/getWeekConfig")}</li>
 *   <li>{@code <property name="weekConfigService" ref="weekConfigService"/>} → {@code @Autowired}</li>
 *   <li>老式实现里手写 {@code response.setContentType} + {@code PrintWriter} 输出 JSON
 *       → {@code @ResponseBody}，序列化交给 {@code @EnableWebMvc}
 *       （替代 {@code <mvc:annotation-driven/>}）注册的消息转换器</li>
 * </ul>
 *
 * @author 魏阳阳
 */
@Controller
@RequestMapping("/getWeekConfig")
public class WeekConfigController {

    private static final Logger logger = LoggerFactory.getLogger(WeekConfigController.class);

    /**
     * 替代 spring-mvc-xml.xml 第 66 行：
     * {@code <property name="weekConfigService" ref="weekConfigService"/>}
     */
    @Autowired
    private WeekConfigService weekConfigService;

    /**
     * 替代原 {@code handleRequest} 中 GET 分支的手写 JSON 输出。
     * <p>
     * 【@ResponseBody 取代的那段手写代码】
     * <pre>
     *   response.setContentType("application/json;charset=UTF-8");
     *   PrintWriter out = response.getWriter();
     *   out.print(JSON.toJSONString(weekConfigs));
     * </pre>
     * 现在直接返回对象，由 Jackson（pom 中的 jackson-databind）完成序列化，
     * 这正是 {@code <mvc:annotation-driven/>} / {@code @EnableWebMvc} 提供的能力。
     * <p>
     * 【一处行为差异，需注意】序列化库由 fastjson2 换成了 Jackson，
     * {@code WeekConfig.startDate/endDate} 是 {@code java.sql.Date}，
     * Jackson 默认会输出成 epoch 毫秒数。为尽量贴近原来的字符串效果，
     * 已在 {@code WebConfig#extendMessageConverters} 里把日期格式统一为
     * {@code yyyy-MM-dd HH:mm:ss}；若要求与 fastjson2 输出逐字节一致，
     * 可改回 {@code @ResponseBody String} 并返回 {@code JSON.toJSONString(...)}。
     * <p>
     * 【一处行为细化】原实现对 POST 返回 {@code null}（无输出、状态 200），
     * 现在只声明了 {@code @GetMapping}，POST 会得到 405 Method Not Allowed。
     *
     * @return 周配置列表
     * @throws ServletException 数据库查询失败时包装后向上抛出，交由统一异常处理返回 500 JSON
     */
    @GetMapping(produces = "application/json;charset=UTF-8")
    @ResponseBody
    public List<WeekConfig> getWeekConfig() throws ServletException {
        try {
            List<WeekConfig> weekConfigs = weekConfigService.listWeekConfigs();
            logger.info("查询结果: {}", weekConfigs);
            return weekConfigs;
        } catch (SQLException e) {
            // 记录完整堆栈后向上抛出，由 GlobalExceptionHandlerResolver / ExceptionFilter 统一返回错误响应，绝不静默吞异常
            // 与原实现一致：抛 ServletException，会命中 GlobalExceptionHandlerResolver 的 else 分支（"系统繁忙，请稍后重试"）
            logger.error("WeekConfigController 数据库查询失败", e);
            throw new ServletException("查询周配置失败", e);
        }
    }
}
