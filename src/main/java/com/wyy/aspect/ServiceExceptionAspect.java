package com.wyy.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.sql.SQLException;

/**
 * Service 层异常统一处理切面
 * 捕获 service 方法抛出的所有异常：记录 error 日志后包装为 SQLException 重新抛出，
 * 与 service 接口的 throws SQLException 声明保持兼容（调用方无需改动）
 * <p>
 * 【替代 spring-aop-xml.xml 的哪几行】
 * <pre>
 *   第 10 行 <bean id="serviceExceptionAspect" class="com.wyy.aspect.ServiceExceptionAspect"/>
 *           → 本类上的 {@code @Component}（由 RootConfig 的 @ComponentScan("com.wyy.aspect") 扫到）
 *   第 14 行 <aop:pointcut id="serviceLayerPointcut"
 *                        expression="execution(* com.wyy.service..*.*(..))"/>
 *           → 方法上 {@code @Around} 里内联同一个切点表达式
 *   第 17~19 行 <aop:aspect ref="serviceExceptionAspect" order="1">
 *                  <aop:around method="handleException" pointcut-ref="serviceLayerPointcut"/>
 *                </aop:aspect>
 *           → 本类上的 {@code @Aspect} + {@code @Order(1)} + 方法上的 {@code @Around}
 * </pre>
 * {@code @Order(1)} 对应 XML 的 {@code order="1"}：数值越小越先进入，
 * 因此本切面仍在最外层，包裹住日志切面与事务切面。
 * <p>
 * 注意：XML 里切面类是 POJO（不带 @Aspect），注解方式下必须显式声明
 * {@code @Aspect}，同时由 RootConfig 上的 {@code @EnableAspectJAutoProxy} 开启自动代理。
 */
@Aspect
@Component
@Order(1)
public class ServiceExceptionAspect {

    private static final Logger logger = LoggerFactory.getLogger(ServiceExceptionAspect.class);

    /**
     * 对应 XML 的 {@code <aop:around method="handleException" pointcut-ref="serviceLayerPointcut"/>}：
     * method 属性由「被注解的方法本身」表达，pointcut-ref 改为直接写切点表达式。
     */
    @Around("execution(* com.wyy.service..*.*(..))")
    public Object handleException(ProceedingJoinPoint pjp) throws Throwable {
        try {
            return pjp.proceed();
        } catch (Exception e) {
            logger.error("{} 调用异常, args: {}", pjp.getSignature().toShortString(), pjp.getArgs(), e);
            // 必须重新抛出：吞掉异常会导致事务管理器感知不到失败，事务不会回滚
            throw new SQLException(pjp.getSignature().toShortString() + " error", e);
        }
    }
}