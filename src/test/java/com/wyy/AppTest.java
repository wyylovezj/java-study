package com.wyy;

import com.wyy.config.RootConfig;
import com.wyy.entity.SysUser;
import com.wyy.entity.WeekConfig;
import com.wyy.service.UserService;
import com.wyy.service.WeekConfigService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

/**
 * Spring + JUnit 集成测试：由 Spring 容器加载配置类并注入 Bean
 * <p>
 * 【替代的那行 XML 式写法】
 * 原为 @ContextConfiguration(locations = "classpath:spring/spring-xml.xml")。
 * 项目转为纯注解配置后，测试容器改由 {@link RootConfig} 这个 Java 配置类引导（locations → classes）。
 * 因为 spring-xml.xml 作对照保留且仍可加载，把上面那行换回来依旧能跑，
 * 两种方式都能验证数据源/MyBatis/事务/切面配置是否正确。
 * 注意：本测试需要真实 MySQL（rbac 库）才能跑通，因为它直接查库。
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = RootConfig.class)
public class AppTest {

    private static final Logger log = LoggerFactory.getLogger(AppTest.class);


    @Autowired
    private UserService userService;

    @Autowired
    private WeekConfigService weekConfigService;

    @Test
    public void testLogin() throws Exception {
        // 真实数据库查询，事务 AOP 对 service 方法同样生效
        SysUser user = userService.login("admin", "admin");
        log.info("login result: {}", user);
    }

    @Test
    public void testListWeekConfigs() throws Exception {
        List<WeekConfig> list = weekConfigService.listWeekConfigs();
        log.info("weekConfigs size: {}", list.size());
    }
}