package io.github.iamnicknack.slc.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Aspect
public class LeaseFunctionAspect {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Pointcut("call(public * io.github.iamnicknack.slc.api.lease.Lease.LeaseFunction.execute(..))")
    void onLeaseFunctionExecute() {}

    @AfterReturning(value = "onLeaseFunctionExecute()", returning = "result")
    public void afterLeaseFunction(JoinPoint joinPoint, Object result) {
        logger.info("Lease value: {}, returning: {}",
                joinPoint.getArgs()[0].getClass().getSimpleName(),
                (result != null) ? result.getClass().getSimpleName() : Void.class.getSimpleName()
        );
    }

}
