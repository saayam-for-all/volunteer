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

    @Around("execution(* org.sfa.volunteer.service.*.*(..))"
            + " && !execution(* org.sfa.volunteer.service.VolunteerService*.updateVolunteerStep4(..))"
            + " && !execution(* org.sfa.volunteer.service.VolunteerService*.getOnboardingAvailability(..))"
            + " && !execution(* org.sfa.volunteer.service.VolunteerService*.requireOnboardingAvailabilityReady(..))"
            + " && !execution(* org.sfa.volunteer.service.VolunteerService*.updateVolunteerCompletion(..))"
            + " && !execution(* org.sfa.volunteer.service.VolunteerService*.updateVolunteer(..))")
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
