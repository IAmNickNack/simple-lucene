package com.github.iamnicknack.slc.api.lease;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link Lease} implementation used by the default implementation of {@link LeaseFactory}
 */
public class DefaultLease<T> implements Lease<T> {

    final Logger logger = LoggerFactory.getLogger(LeaseFactory.class);

    private final T value;
    private final LeaseFactory.ReleaseConsumer<T> releaser;

    DefaultLease(T value,
                 LeaseFactory.ReleaseConsumer<T> releaser) {
        this.value = value;
        this.releaser = releaser;
    }

    @Override
    public <R> R execute(LeaseFunction<T, R> function) {
        try {
            return function.execute(value);
        } catch (Exception e) {
            throw new LeaseException("Failed to execute function with lease", e);
        }
    }

    @Override
    public void close() {
        try {
            releaser.release(value);
        } catch (Exception e) {
            logger.warn("Release failed: {}", e.getMessage(), e);
        }
    }
}
