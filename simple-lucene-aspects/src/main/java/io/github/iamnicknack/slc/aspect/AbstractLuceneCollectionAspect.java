package io.github.iamnicknack.slc.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

@Aspect
public class AbstractLuceneCollectionAspect {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Pointcut("target(io.github.iamnicknack.slc.core.collection.AbstractLuceneCollection+)")
    void onCollection() {}

    @Pointcut("execution(* *.*All(java.util.Collection)) && args(from)")
    void onCollectionFunction(Collection<?> from) {}

    @After(value = "onCollection() && onCollectionFunction(from)", argNames = "from")
    public void afterCollectionOperation(JoinPoint joinPoint, Collection<?> from) {
        var sig = joinPoint.getSignature().toShortString();
        logger.debug("[{}] {} documents", sig, from.size());
    }

}
