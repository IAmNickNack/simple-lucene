package io.github.iamnicknack.slc.aspect;

import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.iamnicknack.slc.api.lease.Lease;

@Aspect
public class LeaseAspect {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Pointcut("call(* io.github.iamnicknack.slc.api.lease.LeaseFactory.lease())")
    void onLease() {}

    @AfterReturning(value = "onLease()", returning = "lease")
    public void afterLease(Lease<?> lease) {
        logger.debug("Provided lease: {}", System.identityHashCode(lease));
    }

    @Pointcut("call(* io.github.iamnicknack.slc.api.lease.Lease.close())")
    void onLeaseClose() {}

    @After(value = "onLeaseClose() && target(lease)", argNames = "lease")
    public void afterLeaseClose(Lease<?> lease) {
        logger.debug("Closed lease: {}", System.identityHashCode(lease));
    }
}
