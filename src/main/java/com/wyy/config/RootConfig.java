package com.wyy.config;

import com.zaxxer.hikari.HikariDataSource;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.aop.Advisor;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.interceptor.NameMatchTransactionAttributeSource;
import org.springframework.transaction.interceptor.RollbackRuleAttribute;
import org.springframework.transaction.interceptor.RuleBasedTransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Spring 根容器（IOC 主容器）配置类，逐项替代
 * {@code src/main/resources/spring/spring-xml.xml}。
 * <p>
 * 【spring-xml.xml → 本类 对应关系】
 * <pre>
 *   spring-xml.xml                                        →  本类
 *   <context:property-placeholder location="jdbc.properties"/>
 *                                                       →  {@code @PropertySource} + {@link #propertyConfigurer()}
 *   <bean id="dataSource" class="HikariDataSource" destroy-method="close">
 *                                                       →  {@code @Bean(destroyMethod = "close")} dataSource(...)
 *   <bean id="sqlSessionFactory" class="SqlSessionFactoryBean">
 *                                                       →  {@code @Bean} sqlSessionFactory(...)
 *   <bean class="MapperScannerConfigurer">              →  类上的 {@code @MapperScan}
 *   <bean id="transactionManager">                     →  {@code @Bean} transactionManager(...)
 *   <tx:advice>                                          →  {@code @Bean} txAdvice(...)（{@code TransactionInterceptor}）
 *   <aop:config><aop:pointcut> + <aop:advisor>         →  {@code @Bean} txAdvisor(...)（{@code DefaultPointcutAdvisor}）
 *                                                            通配规则原样保留，不再使用 @Transactional
 *   <bean id="userService"/><bean id="weekConfigService"> →  类上的 {@code @ComponentScan} 扫 service 包
 *                                                            + ServiceImpl 上的 {@code @Service}/{@code @Autowired}
 *   <import resource="spring-aop-xml.xml"/>             →  类上的 {@code @EnableAspectJAutoProxy}
 *                                                            + 切面类上的 {@code @Aspect}
 * </pre>
 * 本容器是「根容器」，只管 service / dao / mapper / 事务 / AOP；
 * Controller 归 MVC 子容器（{@link WebConfig}）管，扫描范围严格分开，
 * 避免父子容器重复注册同一个 bean —— 这一点与原 XML 的分层约定完全一致。
 *
 * @author 魏阳阳
 */
@Configuration
/*
 * 替代 spring-xml.xml 第 9 行：
 * <context:property-placeholder location="classpath:jdbc.properties"/>
 * 该标签把 properties 挂到 Environment 上；注解里 @PropertySource 负责挂载，
 * ${} 的解析能力则由下面的 PropertySourcesPlaceholderConfigurer 提供（见其方法注释）。
 */
@PropertySource("classpath:jdbc.properties")
/*
 * 替代 spring-xml.xml 第 29~32 行：
 * <bean class="org.mybatis.spring.mapper.MapperScannerConfigurer">
 *     <property name="basePackage" value="com.wyy.mapper"/>
 *     <property name="sqlSessionFactoryBeanName" value="sqlSessionFactory"/>
 * </bean>
 * basePackages     ← basePackage
 * sqlSessionFactoryRef ← sqlSessionFactoryBeanName（按名字引用，避免与 dataSource 形成循环依赖）
 */
@MapperScan(basePackages = "com.wyy.mapper", sqlSessionFactoryRef = "sqlSessionFactory")
/*
 * 替代 spring-xml.xml 第 52~57 行两个 <bean> 的注册职责：
 * <bean id="userService" class="com.wyy.service.impl.UserServiceImpl">
 * <bean id="weekConfigService" class="com.wyy.service.impl.WeekConfigServiceImpl">
 * 改由组件扫描识别 ServiceImpl 类上的 @Service 完成注册（bean 名变为 userServiceImpl / weekConfigServiceImpl，
 * 注入按类型匹配，不影响使用）。
 * 只扫 service 包：controller 归子容器，mapper 归 @MapperScan，切面单独扫 aspect 包。
 */
@ComponentScan(basePackages = {
        "com.wyy.service",   // @Service
        "com.wyy.aspect"     // 替代 spring-aop-xml.xml 里的两个通知类 <bean>，见 @EnableAspectJAutoProxy 说明
})
/*
 * 注意：这里不加 @EnableTransactionManagement。
 * 那个注解对应的是“在方法上标 @Transactional”的注解驱动事务；
 * 而本项目保留了 spring-xml.xml 第 40~49 行的“按方法名通配”声明式事务。
 * 两者同时开启会让同一个方法被套上两层事务 advice（注解 advisor + 通配 advisor），
 * 纯属浪费且容易误导。通配规则由下面的 txAdvice + txAdvisor 两个 bean 实现。
 */
/*
 * 替代被 spring-xml.xml 第 60 行 <import resource="spring-aop-xml.xml"/> 引入的
 * spring-aop-xml.xml 第 13~25 行 <aop:config>：
 * <aop:config> 默认使用 JDK 动态代理，与 @EnableAspectJAutoProxy 的默认行为（proxyTargetClass=false）一致，
 * 因此无需额外设置 proxyTargetClass。切点与环绕通知改为写在切面类上。
 * <p>
 * 它同时也负责把容器里所有 {@code Advisor} bean 纳入代理链，下面的 {@link #txAdvisor} 就是靠这一点生效的
 * （相当于 XML 里一个 <aop:config> 同时挂了 <aop:advisor> 和两个 <aop:aspect>）。
 */
@EnableAspectJAutoProxy
public class RootConfig {

    /**
     * 替代 spring-xml.xml 第 9 行 {@code <context:property-placeholder>} 的「占位符解析」职责。
     * <p>
     * {@code <context:property-placeholder>} 底层注册的就是这个 BeanFactoryPostProcessor，
     * 注解方式下 {@code @PropertySource} 只负责把文件读进 Environment，
     * 仍需显式声明本处理器，{@code ${jdbc.url}} 这类占位符才能在 bean 定义中被解析。
     * <p>
     * 必须声明为 {@code static}：它是 BeanFactoryPostProcessor，需要在普通 bean 创建之前就绪，
     * 若非 static，会提前实例化 @Configuration 类本身，导致本类其他 @Bean 方法脱离代理管理。
     */
    @Bean
    public static PropertySourcesPlaceholderConfigurer propertyConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    /**
     * 替代 spring-xml.xml 第 12~19 行的 dataSource bean。
     * <p>
     * {@code destroy-method="close"} 直接写在 @Bean 上：容器关闭（如 Listener 销毁、应用 undeploy）时
     * 自动调用 {@code HikariDataSource.close()} 释放连接池，与原 XML 的接管语义一致。
     * <p>
     * 注意 maximumPoolSize=20 / minimumIdle=5 是沿用 XML 里硬编码的值（并非 jdbc.properties 中的
     * pool.maximumPoolSize=10），为保持行为不变这里继续硬编码；如需改由属性驱动，替换为
     * {@code @Value("${pool.maximumPoolSize}")} 即可。
     * <p>
     * 返回类型写具体的 {@code HikariDataSource} 而不是 {@code DataSource}：
     * {@code close()} 只在 HikariDataSource 上有（javax.sql.DataSource 接口没有），
     * 声明为接口会让 {@code destroyMethod = "close"} 无法静态校验；
     * 下游 bean 仍按 {@code DataSource} 类型注入，不受影响。
     */
    @Bean(destroyMethod = "close")
    public HikariDataSource dataSource(
            @Value("${jdbc.url}") String jdbcUrl,
            @Value("${jdbc.username}") String username,
            @Value("${jdbc.password}") String password,
            @Value("${jdbc.driver}") String driverClassName) {

        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(jdbcUrl);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setDriverClassName(driverClassName);
        dataSource.setMaximumPoolSize(20);
        dataSource.setMinimumIdle(5);
        return dataSource;
    }

    /**
     * 替代 spring-xml.xml 第 22~26 行的 sqlSessionFactory bean。
     * <ul>
     *   <li>{@code <property name="dataSource" ref="dataSource"/>} → 方法参数直接引用同容器内的 dataSource；
     *       Service 与 Mapper 共用这一个 DataSource，事务同步机制才能生效（项目既有约束）。</li>
     *   <li>{@code <property name="configLocation" value="classpath:mybatis/mybatis-config.xml"/>}
     *       → {@code setConfigLocation(new ClassPathResource(...))}，mybatis-config.xml 继续保留并被引用。</li>
     *   <li>{@code <property name="mapperLocations" value="classpath:mapper/*.xml"/>}
     *       → {@code setMapperLocations(...)}，用 PathMatchingResourcePatternResolver 复现通配符匹配。</li>
     * </ul>
     * 返回类型写 {@code SqlSessionFactoryBean}（FactoryBean 本身），容器取出的是它生产的
     * SqlSessionFactory，与 XML 中 class 属性指向 FactoryBean 的行为一致。
     */
    @Bean
    public SqlSessionFactoryBean sqlSessionFactory(DataSource dataSource) throws IOException {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        // 对应 <property name="configLocation" value="classpath:mybatis/mybatis-config.xml"/>
        factoryBean.setConfigLocation(new ClassPathResource("mybatis/mybatis-config.xml"));
        // 对应 <property name="mapperLocations" value="classpath:mapper/*.xml"/>
        factoryBean.setMapperLocations(
                new PathMatchingResourcePatternResolver().getResources("classpath:mapper/*.xml"));
        return factoryBean;
    }

    /**
     * 替代 spring-xml.xml 第 35~37 行的 transactionManager bean：
     * <bean id="transactionManager" class="org.springframework.jdbc.datasource.DataSourceTransactionManager">
     *     <property name="dataSource" ref="dataSource"/>
     * </bean>
     * bean 名保持 {@code transactionManager}，便于与留档的 XML 对照；
     * 下面的 {@link #txAdvice} 直接按类型把它注入进来，相当于 XML 里的
     * {@code transaction-manager="transactionManager"} 引用。
     */
    @Bean
    public DataSourceTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    /**
     * 替代 spring-xml.xml 第 40~45 行的 {@code <tx:advice>}
     * （XML 的 {@code <tx:advice>} 解析后注册的 bean 本来就是
     * {@code TransactionInterceptor} + {@code NameMatchTransactionAttributeSource}，
     * 所以这里是最直接的 1:1 等价写法）：
     * <pre>
     *   <tx:advice id="txAdvice" transaction-manager="transactionManager">
     *       <tx:attributes>
     *           <tx:method name="get*|find*|list*" read-only="true"/>
     *           <tx:method name="*" propagation="REQUIRED"/>
     *       </tx:attributes>
     *   </tx:advice>
     * </pre>
     * 【通配规则逐条对应】
     * <ul>
     *   <li>{@code name="get*|find*|list*" read-only="true"}：XML 用 {@code |} 写在一个条目里，
     *       {@code NameMatchTransactionAttributeSource} 的 nameMap 不支持 {@code |}，
     *       因此拆成 get* / find* / list* 三个 entry，匹配效果完全等价。</li>
     *   <li>{@code name="*" propagation="REQUIRED"}：对应 * 这个 entry。
     *       命中优先级由 NameMatchTransactionAttributeSource 保证：先精确名、再前缀 {@code xxx*}、
     *       再后缀 {@code *xxx}、再中间 {@code *x*}，最后才落到 {@code *}，
     *       所以 {@code listWeekConfigs} 仍先命中 {@code list*}（只读）而不是 {@code *}，与 XML 一致。</li>
     *   <li>read-only 条目未写 propagation → 默认 REQUIRED；{@code *} 条目未写 read-only → 默认 false。</li>
     *   <li>两个 {@code <tx:method>} 都没写 {@code <tx:rollback-for>} → 回滚规则用 Spring 默认的
     *       “只有 RuntimeException/Error 回滚”，因此这里也不加 rollbackRules，保持原行为。</li>
     * </ul>
     */
    @Bean
    public TransactionInterceptor txAdvice(DataSourceTransactionManager transactionManager) {
        // 对应 <tx:method name="get*|find*|list*" read-only="true"/>
        RuleBasedTransactionAttribute readOnlyTx = new RuleBasedTransactionAttribute();
        readOnlyTx.setReadOnly(true);
        readOnlyTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);

        // 对应 <tx:method name="*" propagation="REQUIRED"/>
        RuleBasedTransactionAttribute requiredTx = new RuleBasedTransactionAttribute(
                TransactionDefinition.PROPAGATION_REQUIRED, Collections.<RollbackRuleAttribute>emptyList());

        // 用 LinkedHashMap 保持与 XML 中 <tx:method> 的书写顺序一致，便于对照阅读
        // （注：真正决定优先级的是上面的 NameMatch 匹配规则，不是 map 顺序）
        Map<String, TransactionAttribute> txAttributes = new LinkedHashMap<>();
        txAttributes.put("get*", readOnlyTx);
        txAttributes.put("find*", readOnlyTx);
        txAttributes.put("list*", readOnlyTx);
        txAttributes.put("*", requiredTx);

        NameMatchTransactionAttributeSource source = new NameMatchTransactionAttributeSource();
        source.setNameMap(txAttributes);

        // 构造参数里的 transactionManager 等价于 XML 的 transaction-manager="transactionManager"
        return new TransactionInterceptor(transactionManager, source);
    }

    /**
     * 替代 spring-xml.xml 第 46~49 行：
     * <pre>
     *   <aop:config>
     *       <aop:pointcut id="serviceLayer" expression="execution(* com.wyy.service..*.*(..))"/>
     *       <aop:advisor advice-ref="txAdvice" pointcut-ref="serviceLayer"/>
     *   </aop:config>
     * </pre>
     * {@code DefaultPointcutAdvisor(pointcut, advice)} 就是 {@code <aop:advisor>} 的编程式等价物：
     * <ul>
     *   <li>advice-ref="txAdvice" → 第二个参数直接传入上面的 {@link #txAdvice}；</li>
     *   <li>pointcut-ref="serviceLayer" → {@code AspectJExpressionPointcut} 用同一条切点表达式；</li>
     *   <li>XML 里 {@code <aop:advisor>} 没写 order，默认就是 {@code Ordered.LOWEST_PRECEDENCE}，
     *       {@code DefaultPointcutAdvisor} 的默认 order 也是它，所以下面无需显式 setOrder；
     *       代理顺序依旧是：异常切面(1) → 日志切面(2) → 事务(最内层)，与原 XML 完全一致。</li>
     * </ul>
     * 【它为什么能被应用】本类上的 {@code @EnableAspectJAutoProxy} 注册的
     * {@code AnnotationAwareAspectJAutoProxyCreator} 会收集容器里所有 {@code Advisor} 类型的 bean，
     * 本 bean 因而自动进入代理拦截链，不需要像 XML 那样把它们写进同一个 {@code <aop:config>}。
     * 正因如此，@Bean 方法返回类型必须是 {@code Advisor}（或其子类型）而不能是 Object：
     * 自动代理创建器要按类型筛选 Advisor bean，它靠方法返回类型在未实例化前就识别出本 bean。
     */
    @Bean
    public Advisor txAdvisor(TransactionInterceptor txAdvice) {
        AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
        // 对应 <aop:pointcut id="serviceLayer" expression="..."/>
        pointcut.setExpression("execution(* com.wyy.service..*.*(..))");
        return new DefaultPointcutAdvisor(pointcut, txAdvice);
    }
}
