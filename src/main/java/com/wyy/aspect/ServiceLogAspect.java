package com.wyy.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Service 层日志与耗时统计切面
 * 环绕记录业务方法调用：成功时输出返回值摘要与耗时，失败时输出异常信息与耗时
 * <p>
 * 【替代 spring-aop-xml.xml 的哪几行】
 * <pre>
 *   第 11 行 <bean id="serviceLogAspect" class="com.wyy.aspect.ServiceLogAspect"/>
 *           → 本类上的 {@code @Component}（由 RootConfig 的 @ComponentScan("com.wyy.aspect") 扫到）
 *   第 14 行 <aop:pointcut id="serviceLayerPointcut"
 *                        expression="execution(* com.wyy.service..*.*(..))"/>
 *           → 方法上 {@code @Around} 里内联同一个切点表达式
 *           （XML 中两个切面共用一个命名切点，注解化后各自写一遍；
 *            若想去重，可用 {@code @Around("com.wyy.aspect.ServiceExceptionAspect.xxx")}
 *            引用另一个切面里用 {@code @Pointcut} 声明的命名切点）
 *   第 22~24 行 <aop:aspect ref="serviceLogAspect" order="2">
 *                  <aop:around method="logAround" pointcut-ref="serviceLayerPointcut"/>
 *                </aop:aspect>
 *           → 本类上的 {@code @Aspect} + {@code @Order(2)} + 方法上的 {@code @Around}
 * </pre>
 * {@code @Order(2)} 对应 XML 的 {@code order="2"}：大于异常切面的 1，
 * 因此位于内层，包裹在最里面的事务切面之外（事务 advisor 默认 LOWEST_PRECEDENCE，
 * 与 XML 中无 order 的 {@code <aop:advisor>} 位置一致），日志仍先于事务执行。
 */
@Aspect
@Component
@Order(2)
public class ServiceLogAspect {

    private static final Logger logger = LoggerFactory.getLogger(ServiceLogAspect.class);

    /**
     * 对应 XML 的 {@code <aop:around method="logAround" pointcut-ref="serviceLayerPointcut"/>}
     */
    @Around("execution(* com.wyy.service..*.*(..))")
    public Object logAround(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            Object result = pjp.proceed();
            logger.info("[ServiceLogAspect] {} 调用成功, 耗时 {}ms, 结果: {}",
                    pjp.getSignature().toShortString(), System.currentTimeMillis() - start, result);
            return result;
        } catch (Throwable e) {
            logger.warn("[ServiceLogAspect] {} 调用失败, 耗时 {}ms, 异常: {}",
                    pjp.getSignature().toShortString(), System.currentTimeMillis() - start, e.getMessage());
            throw e;   // 只记录不拦截，继续向上传播
        }
    }
}