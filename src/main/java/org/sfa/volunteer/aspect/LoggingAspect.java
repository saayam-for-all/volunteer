package org.sfa.volunteer.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

    @Around("execution(* org.sfa.volunteer.service.*.*(..))")
    public Object logServiceMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        log.info("Entering method: {}", methodName);
        for (Object arg : args) {
            log.info("Argument: {}", arg);
        }

        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Throwable throwable) {
            log.error("Exception in method: {} with message: {}", methodName, throwable.getMessage(), throwable);
            throw throwable;
        }

        log.info("Exiting method: {} with result: {}", methodName, result);
        return result;
    }
}
