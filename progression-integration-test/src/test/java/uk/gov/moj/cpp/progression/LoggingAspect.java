package uk.gov.moj.cpp.progression;

import java.util.concurrent.TimeUnit;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Pointcut;

public class LoggingAspect {

    @Pointcut("execution(* uk.gov.moj.cpp.progression.*(..))")
    public void allMethods() {}

    @Around("allMethods()")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        System.out.println("------------Started method: " + joinPoint.getSignature());
        Object proceed = joinPoint.proceed();
        long executionTime = System.currentTimeMillis() - start;
        System.out.println("------------Finished method: " + joinPoint.getSignature() + " in " + TimeUnit.MILLISECONDS.toSeconds(executionTime) + "s");
        return proceed;
    }

}
