package com.rizzo.trifle.aop;

import org.apache.log4j.MDC;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StopWatch;

@Aspect
public class PerformanceLogAspect {

    private static final Logger LOGGER = LoggerFactory.getLogger(PerformanceLogAspect.class);

    @Around("@annotation(logPerformance)")
    public Object doContextualLogging(final ProceedingJoinPoint proceedingJoinPoint,
                                      LogPerformance logPerformance) throws Throwable {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try {
            return proceedingJoinPoint.proceed();
        } finally {
            stopWatch.stop();
            MDC.put("duration", String.valueOf(stopWatch.getTotalTimeMillis()));
            Signature signature = proceedingJoinPoint.getSignature();
            LOGGER.debug("{}", signature.getDeclaringTypeName() + "." + signature.getName() + ": " + String.valueOf(stopWatch.getTotalTimeMillis()));
            MDC.remove("duration");
        }
    }

}