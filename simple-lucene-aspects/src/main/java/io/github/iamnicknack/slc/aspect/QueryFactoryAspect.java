package io.github.iamnicknack.slc.aspect;

import org.apache.lucene.search.Query;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Aspect
public class QueryFactoryAspect {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Pointcut("call(* io.github.iamnicknack.slc.api.query.QueryFactory.query(..))")
    void callQueryFactory() {}

    @AfterReturning(value = "callQueryFactory() && args(domain, ..)", argNames = "domain", returning = "query")
    public void logQuery(Query query, Object domain) {
        logger.debug("Generated lucene query: `{}`, from: `{}`", query, domain);
    }

}
