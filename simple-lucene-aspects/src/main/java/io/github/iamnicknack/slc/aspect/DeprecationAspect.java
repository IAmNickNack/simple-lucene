package io.github.iamnicknack.slc.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Aspect
public class DeprecationAspect {

    private final Logger logger = LoggerFactory.getLogger(getClass());
//
    @Pointcut("@annotation(java.lang.Deprecated) && execution(* io.github.iamnicknack..*.*(..))")
    private void deprecated() {}


    @Before(value = "deprecated()")
    public void beforeDeprecatedExecution(JoinPoint joinPoint) {
        var calledFrom = Thread.currentThread().getStackTrace()[3];
        var sourceLocation = "%s (%s#%d)".formatted(calledFrom.getClassName(),
                calledFrom.getFileName(),
                calledFrom.getLineNumber()
        );
        logger.warn("Call to deprecated method: {}, from {}",
                joinPoint.getSignature().toShortString(),
                sourceLocation
        );
    }
}
