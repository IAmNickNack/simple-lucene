package com.github.iamnicknack.slc.aspect;

import com.github.iamnicknack.slc.api.query.Result;
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Aspect
public class ResultAspect {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Pointcut("execution(com.github.iamnicknack.slc.api.query.Result com.github.iamnicknack.slc.api.query.QueryExecutor.execute(..))")
    void executeQuery() {}

    @AfterReturning(value = "executeQuery()", returning = "result")
    public void afterExecuteQuery(Result<?> result) {
        logger.debug("Created result: {}", System.identityHashCode(result));
    }

    @Pointcut("execution(void com.github.iamnicknack.slc.api.query.Result+.close())")
    void close() {}

    @After("close() && this(result)")
    public void afterClose(Result<?> result) {
        logger.debug("Closed result: {}", System.identityHashCode(result));
    }
}
