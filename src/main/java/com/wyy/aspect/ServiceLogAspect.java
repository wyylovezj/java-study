package com.wyy.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service 层日志与耗时统计切面
 * 环绕记录业务方法调用：成功时输出返回值摘要与耗时，失败时输出异常信息与耗时
 */
public class ServiceLogAspect {

    private static final Logger logger = LoggerFactory.getLogger(ServiceLogAspect.class);

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