package com.wyy;

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
 * Spring + JUnit 集成测试：由 Spring 容器加载 applicationContext.xml 并注入 Bean
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:spring/applicationContext.xml")
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