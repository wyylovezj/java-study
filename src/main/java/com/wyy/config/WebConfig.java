package com.wyy.config;

import com.wyy.interceptor.LogInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Spring MVC 子容器配置类，逐项替代
 * {@code src/main/resources/spring/spring-mvc-xml.xml}。
 * <p>
 * 【spring-mvc-xml.xml → 本类 对应关系】
 * <pre>
 *   spring-mvc-xml.xml                                  →  本类
 *   <context:component-scan base-package="com.wyy.controllerxml"/>
 *                                                       →  类上的 {@code @ComponentScan}
 *   <mvc:annotation-driven/>                           →  类上的 {@code @EnableWebMvc}
 *   <mvc:resources mapping="/css/**" location="/css/" cache-period="3600"/>
 *                                                       →  {@link #addResourceHandlers}
 *   <mvc:view-controller path="/" view-name="index"/>  →  {@link #addViewControllers}
 *   <bean class="InternalResourceViewResolver">         →  {@link #internalResourceViewResolver()}
 *   <bean id="logInterceptor"> + <mvc:interceptors>     →  LogInterceptor 上的 @Component
 *                                                            + {@link #addInterceptors}
 *   <bean class="GlobalExceptionHandlerResolver">        →  该类上的 @Component
 *   <bean name="/login" class="LoginXmlController">      →  controller 包下新的 @Controller
 *   <bean name="/getWeekConfig" ...>                    （见 LoginController / WeekConfigController）
 * </pre>
 * 注意：MVC 子容器只扫 controller / interceptor / resolver，
 * service、mapper、事务、AOP 全部留在根容器（{@link RootConfig}），
 * 与原 XML 的父子容器分工保持一致，避免同一个 bean 被注册两次。
 * 子容器可按类型向父容器查找 bean，所以 Controller 能直接注入到根容器里的 Service。
 *
 * @author 魏阳阳
 */
@Configuration
@EnableWebMvc
/*
 * 替代 spring-mvc-xml.xml 第 9 行：
 * <context:component-scan base-package="com.wyy.controllerxml"/>
 * 原包里的 Controller 是「老式 Controller 接口 + XML 里 name="/login" 映射」的写法，无法被扫描生效；
 * 注解化后新增 controller 包（与 controllerxml / controllerservlet 并列的风格包）放 @Controller。
 * 同时把 interceptor、resolver 两个 MVC 层组件也纳入子容器扫描，
 * 取代 XML 里的 <bean id="logInterceptor"> 和 <bean class="GlobalExceptionHandlerResolver">。
 */
@ComponentScan(basePackages = {
        "com.wyy.controller",   // @Controller + @RequestMapping
        "com.wyy.interceptor",  // 替代 <bean id="logInterceptor" class="com.wyy.interceptor.LogInterceptor"/>
        "com.wyy.resolver"      // 替代 <bean class="com.wyy.resolver.GlobalExceptionHandlerResolver"/>
})
public class WebConfig implements WebMvcConfigurer {

    /**
     * 替代 spring-mvc-xml.xml 第 37 行的拦截器 bean 声明：
     * <bean id="logInterceptor" class="com.wyy.interceptor.LogInterceptor"/>
     * 现由 LogInterceptor 类上的 {@code @Component} 注册，这里按类型注入进来用于配置映射路径。
     */
    private final LogInterceptor logInterceptor;

    public WebConfig(LogInterceptor logInterceptor) {
        this.logInterceptor = logInterceptor;
    }

    /**
     * 替代 spring-mvc-xml.xml 第 20~22 行：
     * <pre>
     *   <mvc:resources mapping="/css/**"    location="/css/"    cache-period="3600"/>
     *   <mvc:resources mapping="/js/**"     location="/js/"     cache-period="3600"/>
     *   <mvc:resources mapping="/images/**" location="/images/" cache-period="3600"/>
     * </pre>
     * 静态资源仍由 Spring MVC 自己处理（不走容器 DefaultServlet），AuthFilter 白名单已放行这些前缀。
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/css/**").addResourceLocations("/css/").setCachePeriod(3600);
        registry.addResourceHandler("/js/**").addResourceLocations("/js/").setCachePeriod(3600);
        registry.addResourceHandler("/images/**").addResourceLocations("/images/").setCachePeriod(3600);
    }

    /**
     * 替代 spring-mvc-xml.xml 第 28 行：
     * <mvc:view-controller path="/" view-name="index"/>
     * <p>
     * 存在的原因不变：DispatcherServlet 映射 "/" 后，web.xml 的 welcome-file 机制失效
     * （welcome-file 由容器 DefaultServlet 负责），访问 "/" 会 404，
     * 因此仍需把 "/" 直接转发到 index 视图。
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("index");
    }

    /**
     * 替代 spring-mvc-xml.xml 第 31~34 行：
     * <pre>
     *   <bean class="org.springframework.web.servlet.view.InternalResourceViewResolver">
     *       <property name="prefix" value="/"/>
     *       <property name="suffix" value=".jsp"/>
     *   </bean>
     * </pre>
     * 现有 JSP 都在 webapp 根目录，prefix 保持 "/"。
     * <p>
     * 【与 @EnableWebMvc 自带解析器的共存关系】
     * {@code WebMvcConfigurationSupport} 在 Spring 5.3 里注册的默认解析器 bean 名是
     * {@code mvcViewResolver}（不是 4.x 的 {@code viewResolver}），它内部是
     * {@code ViewResolverComposite}。只有当容器里一个其他 ViewResolver 都没有时，
     * 它才会自己 new 一个空 prefix/suffix 的 InternalResourceViewResolver；
     * 本 bean 一旦存在，composite 里的列表就保持为空，{@code resolveView} 直接返回 null，
     * 然后才轮到本解析器生效，因此不冲突，方法名也不需要非得叫 viewResolver。
     * （注：这里的结论基于 spring-webmvc 5.3.31 的 mvcViewResolver 字节码验证）
     */
    @Bean
    public InternalResourceViewResolver internalResourceViewResolver() {
        InternalResourceViewResolver resolver = new InternalResourceViewResolver();
        resolver.setPrefix("/");
        resolver.setSuffix(".jsp");
        return resolver;
    }

    /**
     * 替代 spring-mvc-xml.xml 第 39~47 行：
     * <pre>
     *   <mvc:interceptors>
     *       <mvc:interceptor>
     *           <mvc:mapping path="/**"/>
     *           <mvc:exclude-mapping path="/login"/>
     *           <ref bean="logInterceptor"/>
     *       </mvc:interceptor>
     *   </mvc:interceptors>
     * </pre>
     * addPathPatterns ← <mvc:mapping>，excludePathPatterns ← <mvc:exclude-mapping>。
     * 放行 /login 避免登录请求被拦截，语义与 XML 完全一致。
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(logInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/login");
    }

    /**
     * 给 {@code @EnableWebMvc}（替代 {@code <mvc:annotation-driven/>}）默认注册的
     * Jackson 消息转换器统一设置日期格式。
     * <p>
     * 【为什么要多这一步】原 XML 版本里 {@code WeekConfigXmlController} 是用 fastjson2
     * 手写输出 JSON 的；改成 {@code @ResponseBody} 后序列化交给 Jackson，
     * 而 {@code java.sql.Date} 字段在 Jackson 默认配置下会输出成 epoch 毫秒数，
     * 响应体与原行为不一致。这里用 extendMessageConverters（而不是
     * configureMessageConverters）只调属性、不替换整个转换器列表，避免丢掉默认转换器。
     * 格式沿用 WeekConfig 自身定义的 DATETIME_FORMATTER。
     */
    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        for (HttpMessageConverter<?> converter : converters) {
            if (converter instanceof MappingJackson2HttpMessageConverter) {
                MappingJackson2HttpMessageConverter jackson =
                        (MappingJackson2HttpMessageConverter) converter;
                // 该转换器本身不暴露 setDateFormat，需直接配它持有的 ObjectMapper
                jackson.getObjectMapper().setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
            }
        }
    }

    /*
     * 【关于全局异常处理器】spring-mvc-xml.xml 第 53 行：
     * <bean class="com.wyy.resolver.GlobalExceptionHandlerResolver"/>
     * 不再用 <bean> 声明，改由该类上的 @Component + 上面的 @ComponentScan("com.wyy.resolver") 注册。
     * <p>
     * 两个必须保留的隐性约束（由类本身保证，这里无需额外配置）：
     * 1. bean 名不能叫 handlerExceptionResolver，否则 DispatcherServlet 会用它替换掉整个默认解析链；
     * 2. 它实现了 Ordered 并返回 -1，保证优先于 @EnableWebMvc
     *    （即原 <mvc:annotation-driven/>）注册的默认 resolver 执行。
     */
}
