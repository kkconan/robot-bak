package com.money.game.robot.aspect;

import com.money.game.core.aspect.BaseExceptionAspect;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Component
@Aspect
@Slf4j
public class ExceptionAspect {

//    @Pointcut("execution(* com.money.game.robot..*(..))")
    public void aspect() {

    }

//    @Around("aspect()")
    public Object around(JoinPoint joinPoint) {
        return BaseExceptionAspect.printExcLogAndThrow(joinPoint);
    }
}
