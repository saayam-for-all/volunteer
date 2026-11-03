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

    // Availability arguments/results contain personal schedules; keep them out of generic payload logging.
    @Around("execution(* org.sfa.volunteer.service.*.*(..))"
            + " && !target(org.sfa.volunteer.service.AvailabilityScheduleService)"
            + " && !target(org.sfa.volunteer.service.OnboardingAvailabilityService)"
            + " && !target(org.sfa.volunteer.service.LegacyAvailabilityProjection)"
            + " && !execution(* org.sfa.volunteer.service.UserService+.getAvailability(..))"
            + " && !execution(* org.sfa.volunteer.service.UserService+.updateAvailability(..))"
            + " && !execution(org.sfa.volunteer.dto.response.VolunteerResponse org.sfa.volunteer.service.VolunteerService+.*(..))"
            + " && !execution(* org.sfa.volunteer.service.VolunteerService+.findAllVolunteersWithPagination(..))"
            + " && !execution(* org.sfa.volunteer.service.StewardService+.getVolunteers(..))")
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
