package com.money.game.robot.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Aspect
@Slf4j
public class LogAspect {

    @Pointcut("execution(* com.money.game.robot.schedule..*(..))")
    public void aspect() {

    }

    @Around("aspect()")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        try {
            String logId = UUID.randomUUID().toString();
            MDC.put("logId", logId);
        } catch (Throwable e) {
            log.error("e={}", e);
        }
        return pjp.proceed();
    }
}
