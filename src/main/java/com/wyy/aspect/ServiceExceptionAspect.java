package com.wyy.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

/**
 * Service 层异常统一处理切面
 * 捕获 service 方法抛出的所有异常：记录 error 日志后包装为 SQLException 重新抛出，
 * 与 service 接口的 throws SQLException 声明保持兼容（调用方无需改动）
 */
public class ServiceExceptionAspect {

    private static final Logger logger = LoggerFactory.getLogger(ServiceExceptionAspect.class);

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