package com.wyy.config;

import com.wyy.filter.AuthFilter;
import com.wyy.filter.EncodingFilter;
import com.wyy.filter.ExceptionFilter;
import com.wyy.filter.LogFilter;
import com.wyy.listener.RequestListener;
import com.wyy.listener.SessionListener;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import java.util.EnumSet;

/**
 * 纯注解配置的 Web 入口，完全替代 {@code src/main/webapp/WEB-INF/web.xml}。
 * <p>
 * Servlet 3.0+ 容器启动时会通过 ServletContainerInitializer 机制自动发现本接口
 * （Spring 的 {@code SpringServletContainerInitializer} 负责收集所有 WebApplicationInitializer 实现），
 * 因此不再需要 web.xml；原 web.xml 已改名为 web.xml.bak 仅作对照保留，容器不会加载它。
 * <p>
 * 【本类与 web.xml 的逐项对应关系】
 * <pre>
 *   web.xml                                            →  本类
 *   <listener>ContextLoaderListener                   →  registerRootContext()
 *   <context-param>contextConfigLocation=spring-xml.xml →  rootContext.register(RootConfig.class)
 *   <servlet>DispatcherServlet + <init-param>
 *       contextConfigLocation=spring-mvc-xml.xml       →  registerDispatcherServlet() + WebConfig.class
 *   <servlet-mapping>url-pattern=/                     →  registration.addMapping("/")
 *   <load-on-startup>1                                →  registration.setLoadOnStartup(1)
 *   <filter>/<filter-mapping>/<init-param>             →  registerFilters()
 * <listener>SessionListener / RequestListener        →  registerListeners()（按原顺序，不用 @WebListener）
 * <welcome-file-list>                                →  WebConfig#addViewControllers（原 XML 已注释掉）
 * <session-config><session-timeout>30</...>      →  无注解等价物，见下方【遗留项】
 * <error-page>                                     →  无注解等价物，见下方【遗留项】
 * <display-name>                                   →  纯展示用，无影响，直接略过
 * </pre>
 * <p>
 * 【遗留项：两项 Servlet 容器级配置没有标准注解写法】
 * <ol>
 *   <li><b>session-timeout = 30 分钟</b>：{@code ServletContext} API 只能配 Cookie
 *       （{@code getSessionCookieConfig()}），配不了会话超时。
 *       好消息是 <b>Tomcat 默认会话超时就是 30 分钟</b>，所以删掉它行为不变；
 *       若今后要改成其他值，可写 Tomcat 专有的 {@code META-INF/context.xml}
 *       （{@code <Context sessionTimeout="30">}），注意那不再是 web.xml。</li>
 *   <li><b>error-page（404/500/Throwable → /error.jsp）</b>：错误页映射是 web.xml 独有能力，
 *       无编程式/注解式入口。现状：
 *       <ul>
 *         <li>500 / 未捕获异常：本来就被 {@code ExceptionFilter} +
 *             {@code GlobalExceptionHandlerResolver} 拦截返回 JSON，不经过 error.jsp（原 XML 下也如此）；</li>
 *         <li>纯 404（无任何 handler 命中）：ExceptionFilter 会调用
 *             {@code response.sendError(404)}，没有 error-page 配置时由 <b>容器默认 404 页</b>接手，
 *             不再跳 /error.jsp。若必须保留跳页效果，可选：
 *             给 {@code WebConfig#addViewControllers} 加兜底转发，
 *             或在 {@code registerDispatcherServlet} 里设
 *             {@code registration.setInitParameter("throwExceptionIfNoHandlerFound", "true")}
 *             后由异常处理器统一返回 JSON。</li>
 *       </ul>
 *   </li>
 * </ol>
 * 以上两点是有意的取舍，不是遗漏；如需保留原行为请告知，我再补对应方案。
 *
 * @author 魏阳阳
 */
public class WebAppInitializer implements WebApplicationInitializer {

    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {
        // 调用顺序严格照搬原 web.xml 的声明顺序：
        //   <listener>ContextLoaderListener → <listener>SessionListener → <listener>RequestListener
        //   → <filter> x4 → <servlet>DispatcherServlet
        registerRootContext(servletContext);   // 对应 web.xml 第 23~25 行（声明在最前面）
        registerListeners(servletContext);     // 对应 web.xml 第 45~52 行
        registerFilters(servletContext);       // 对应 web.xml 第 55~106 行
        registerDispatcherServlet(servletContext); // 对应 web.xml 第 137~149 行
    }

    /**
     * 替代 web.xml 中的：
     * <pre>
     *   <context-param>
     *       <param-name>contextConfigLocation</param-name>
     *       <param-value>classpath:spring/spring-xml.xml</param-value>
     *   </context-param>
     *   <listener>
     *       <listener-class>org.springframework.web.context.ContextLoaderListener</listener-class>
     *   </listener>
     * </pre>
     * contextConfigLocation 指向的 XML 由 {@link RootConfig} 这个 Java 配置类取代。
     */
    private void registerRootContext(ServletContext servletContext) {
        AnnotationConfigWebApplicationContext rootContext = new AnnotationConfigWebApplicationContext();
        rootContext.register(RootConfig.class);
        // ContextLoaderListener 负责创建根容器并写入 ServletContext，与 web.xml 声明的监听器行为完全一致
        servletContext.addListener(new ContextLoaderListener(rootContext));
    }

    /**
     * 替代 web.xml 中的：
     * <pre>
     *   <servlet>
     *       <servlet-name>dispatcherServlet</servlet-name>
     *       <servlet-class>org.springframework.web.servlet.DispatcherServlet</servlet-class>
     *       <init-param>
     *           <param-name>contextConfigLocation</param-name>
     *           <param-value>classpath:spring/spring-mvc-xml.xml</param-value>
     *       </init-param>
     *       <load-on-startup>1</load-on-startup>
     *   </servlet>
     *   <servlet-mapping>
     *       <servlet-name>dispatcherServlet</servlet-name>
     *       <url-pattern>/</url-pattern>
     *   </servlet-mapping>
     * </pre>
     * init-param 指向的 MVC XML 由 {@link WebConfig} 这个 Java 配置类取代。
     */
    private void registerDispatcherServlet(ServletContext servletContext) {
        AnnotationConfigWebApplicationContext mvcContext = new AnnotationConfigWebApplicationContext();
        mvcContext.register(WebConfig.class);

        ServletRegistration.Dynamic registration =
                servletContext.addServlet("dispatcherServlet", new DispatcherServlet(mvcContext));
        registration.setLoadOnStartup(1);
        registration.addMapping("/");
    }

    /**
     * 替代 web.xml 中的：
     * <pre>
     *   <listener>
     *       <listener-class>com.wyy.listener.SessionListener</listener-class>   （第 45~47 行）
     *   </listener>
     *   <listener>
     *       <listener-class>com.wyy.listener.RequestListener</listener-class>   （第 50~52 行）
     *   </listener>
     * </pre>
     * 【为什么不用 @WebListener 而改成编程式】两个原因，与过滤器同一个理由：
     * <ol>
     *   <li><b>@WebListener 不保证多个监听器之间的先后顺序</b>（由容器实现决定），
     *       无法复现 web.xml 的"声明顺序即注册顺序"。</li>
     *   <li><b>注解发现的时机早于 ServletContainerInitializer</b>。如果这里用 @WebListener，
     *       SessionListener / RequestListener 会比 {@link #registerRootContext} 里编程式注册的
     *       ContextLoaderListener <b>更早</b>注册，整体相对原 web.xml 变成反序；
     *       "注解 + 编程式"混用更会让真实顺序变成隐式推导，而非显式声明。</li>
     * </ol>
     * 全部改用 {@code addListener} 后顺序完全确定：Tomcat 的
     * {@code ApplicationContext.addListener()} → {@code StandardContext.addApplicationLifecycleListener()}
     * 是<b>按调用顺序追加</b>到列表尾部（注意：这里跟 filter-mapping 不同，
     * filter 那边 {@code isMatchAfter=false} 会插到链首，所以两者不能套用直觉）。
     * <p>
     * 另外：原先给这两个类加的 {@code @WebListener} 注解必须同时删掉，
     * 否则容器会先按注解注册一次、{@code onStartup} 再注册一次，导致同一监听器收到双份事件
     * （在线人数 / 登录人数 / QPS 会直接翻倍）。
     * <p>
     * 【一个必须知道的约束】{@code ServletContext} 不保证 ServletContextListener 之间的顺序，
     * 所以 {@code SessionListener} / {@code RequestListener} 绝不能在这里（或在它们自己的
     * {@code contextInitialized} 里）依赖 Spring 根容器已就绪；
     * 确实需要容器时，应把它们改写成 {@code ServletContextListener} 并放在
     * {@link #registerRootContext} 之后注册。
     */
    private void registerListeners(ServletContext servletContext) {
        // 对应 <listener>SessionListener（统计在线人数与登录人数）
        servletContext.addListener(SessionListener.class);
        // 对应 <listener>RequestListener（请求日志与 QPS 统计）
        servletContext.addListener(RequestListener.class);
    }

    /**
     * 替代 web.xml 中的 4 组 {@code <filter> + <filter-mapping>}（含 {@code <init-param>}）。
     * <p>
     * 【为什么用编程式注册而不是 @WebFilter 注解】
     * 原 web.xml 注释明确写了"声明顺序即执行顺序"，链条必须是
     * ExceptionFilter → LogFilter → EncodingFilter → AuthFilter。
     * 而 {@code @WebFilter} 规范不保证多个过滤器之间的先后顺序（由容器实现决定），
     * 一旦顺序变化，ExceptionFilter 的响应缓冲兜底、EncodingFilter 的编码前置都会失效。
     * 编程式注册的先后完全可控，可稳定复现 web.xml 的语义。
     * <p>
     * 【关键：{@code addMappingForUrlPatterns} 的第二个参数 isMatchAfter 必须传 true】
     * 这个参数的含义是“本映射是否排在声明式（web.xml）映射之后”，而不是“是否排在最后”，
     * 很容易误读，规范原文的取值含义是：
     * <ul>
     *   <li>{@code true}  → 本映射排在容器已声明（web.xml）的 filter-mapping 之后；</li>
     *   <li>{@code false} → 本映射排在已声明的 filter-mapping 之前。</li>
     * </ul>
     * Tomcat 的具体实现是：{@code true} 走 {@code addFilterMap()}（追加到映射链尾部），
     * {@code false} 走 {@code addFilterMapBefore()}（插入到映射链头部 index 0）。
     * 本项目已经没有 web.xml，没有任何“声明式”映射需要让位，因此传 {@code false} 时
     * 每次调用都插到最前面，四次调用的结果是把链条整体反转为
     * AuthFilter → EncodingFilter → LogFilter → ExceptionFilter，
     * 本该在最外层的 ExceptionFilter 反而跑到最里层，全局异常兜底与响应缓冲全部失效。
     * 所以这里必须全部传 {@code true}，让追加顺序等于调用顺序，
     * 即 ExceptionFilter → LogFilter → EncodingFilter → AuthFilter，与原 web.xml 一致。
     * <p>
     * 第一个参数显式传 {@code EnumSet.of(DispatcherType.REQUEST)}，等价于 web.xml 的
     * {@code <filter-mapping>} 不写 {@code <dispatcher>} 时的默认值（仅拦截 REQUEST）。
     * <p>
     * 【如何验证顺序真的对了】Tomcat 的 {@code StandardContext.filterStart()} 是按生效的
     * filter-mapping 顺序逐个调 {@code Filter.init()} 的，所以启动日志里四条 init 日志
     * 依次是 [ExceptionFilter] init → LogFilter init → [EncodingFilter] init → [AuthFilter] init
     * 才算正确；若发现顺序反了，先查这里是否又改回了 {@code false}。
     */
    private void registerFilters(ServletContext servletContext) {
        EnumSet<DispatcherType> requestOnly = EnumSet.of(DispatcherType.REQUEST);
        // isMatchAfter=true：追加到映射链尾部，使调用顺序 == 生效顺序（与原 web.xml 声明顺序一致）
        boolean appendToEnd = true;

        // 对应 <filter>exceptionFilter + <filter-mapping>url-pattern=/*（web.xml 第 55~62 行）
        FilterRegistration.Dynamic exceptionFilter =
                servletContext.addFilter("exceptionFilter", ExceptionFilter.class);
        exceptionFilter.addMappingForUrlPatterns(requestOnly, appendToEnd, "/*");

        // 对应 <filter>logFilter + <filter-mapping>url-pattern=/*（web.xml 第 65~72 行）
        FilterRegistration.Dynamic logFilter =
                servletContext.addFilter("logFilter", LogFilter.class);
        logFilter.addMappingForUrlPatterns(requestOnly, appendToEnd, "/*");

        // 对应 <filter>encodingFilter 及其 <init-param>encoding=UTF-8（web.xml 第 75~87 行）
        FilterRegistration.Dynamic encodingFilter =
                servletContext.addFilter("encodingFilter", EncodingFilter.class);
        encodingFilter.setInitParameter("encoding", "UTF-8");
        encodingFilter.addMappingForUrlPatterns(requestOnly, appendToEnd, "/*");

        // 对应 <filter>authFilter 及其 <init-param>loginPage / excludePaths（web.xml 第 90~106 行）
        // 白名单必须放行 /login 登录接口，否则登录请求会被自己拦截重定向，永远登不进去
        FilterRegistration.Dynamic authFilter =
                servletContext.addFilter("authFilter", AuthFilter.class);
        authFilter.setInitParameter("loginPage", "/login.jsp");
        authFilter.setInitParameter("excludePaths", "/login.jsp,/login,/css/,/js/,/images/");
        authFilter.addMappingForUrlPatterns(requestOnly, appendToEnd, "/*");
    }
}
