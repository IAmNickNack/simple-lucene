package com.github.iamnicknack.slc.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Aspect
public class TimerAspect {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Pointcut("execution(public * com.github.iamnicknack.slc.core.collection.AbstractLuceneCollection+.*(..))")
    void onCollectionOperations() {}

    @Pointcut("execution(* com.github.iamnicknack.slc.api.query.QueryExecutor+.execute(..))")
    void onQueryExecute() {}

    @Around(value = "onCollectionOperations() || onQueryExecute()")
    public Object aroundCollectionOperations(ProceedingJoinPoint joinPoint) throws Throwable {

        var description = joinPoint.getSignature().toShortString();
                // "%s: %d".formatted(joinPoint.getSourceLocation().getFileName(), joinPoint.getSourceLocation().getLine());
        var start = System.currentTimeMillis();
        var result = joinPoint.proceed();
        var duration = System.currentTimeMillis() - start;
        logger.debug("[{}] Execution time for Result#{}: {} ms", description, System.identityHashCode(result), duration);
        return result;
    }


}
