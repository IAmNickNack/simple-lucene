package io.github.iamnicknack.slc.api.lease;

/**
 * Acts as a supplier for {@link Lease} instances of {@link T}
 * @param <T> the type being leased
 */
public interface LeaseFactory<T> {

    /**
     * Create a {@link LeaseFactory} instance using the specified operations
     * @param supplier supply a value of {@link T}
     * @param releaser function used to release a value of type {@link T}
     * @param <T> the type being leased
     * @return a new factory for lease instances
     */
    static <T> LeaseFactory<T> create(LeaseSupplier<T> supplier,
                                      ReleaseConsumer<T> releaser) {


        return () -> {
            try {
                return new DefaultLease<>(supplier.lease(), releaser);
            }
            catch (Exception e) {
                throw new Lease.LeaseException("Failed to acquire lease", e);
            }
        };
    }

    /**
     * Acquire a new lease of {@link T}
     */
    Lease<T> lease();

    /**
     * Helper function for when only a single function needs to execute.
     * Provides the {@code try-with-resources} block required to release resources.
     * @param function the function to execute
     * @return the function result
     * @param <R> the function return type
     * @throws Lease.LeaseException on execution failure
     */
    default <R> R execute(Lease.LeaseFunction<T, R> function) throws Lease.LeaseException {
        try(var lease = this.lease()) {
            return lease.execute(function);
        }
        catch (Exception e) {
            throw new Lease.LeaseException("Failed to execute lease", e);
        }
    }

    /**
     * Function returning the component being leased
     * @param <T> the leased type
     */
    interface LeaseSupplier<T> {
        T lease() throws Exception;
    }

    /**
     * Consumer able to free resources used by the leased type
     * @param <T> the leased type
     */
    interface ReleaseConsumer<T> {
        void release(T value) throws Exception;
    }
}
