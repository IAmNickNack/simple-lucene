package io.github.iamnicknack.slc.api.lease;

import io.github.iamnicknack.slc.api.index.UpdateOperations;

import java.util.Collection;
import java.util.function.Function;

/**
 * Generic type to allow possibly non-closeable types to utilise try-with-resources.
 * @param <T> the type being leased
 */
public interface Lease<T> extends AutoCloseable {

    /**
     * Execute a function using the leased instance
     * @param function function to execute
     * @param <R> the return type of the provided function
     * @return the function result
     */
    <R> R execute(LeaseFunction<T, R> function) throws LeaseException;

    @Override
    void close();

    /**
     * Wrapper exception thrown when any stage of leasing fails
     */
    class LeaseException extends RuntimeException {
        public LeaseException(String message, Throwable t) {
            super(message, t);
        }
    }

    /**
     * A function that can be performed on a {@link Lease}
     * @param <T> the leased type
     * @param <R> the function result type
     */
    interface LeaseFunction<T, R> {

        /**
         * Execution on a leased object
         * @param leasedValue the leased value
         * @return a function result
         * @throws Exception in case of any exception
         */
        R execute(T leasedValue) throws Exception;

        /**
         * Execute this instance using the result of {@code other} as the parameter
         * @param other the function providing the input parameter to this instance
         * @return a composition of {@code this} and {@code other}
         */
        default <V> LeaseFunction<V, R> compose(LeaseFunction<? super V, ? extends T> other) {
            return value -> execute(other.execute(value));
        }

        /**
         * Execute the {@code other} using the result of this function
         * @param other the function accepting the output of this instance
         * @return a composition of {@code other} and {@code this}
         */
        default <V> LeaseFunction<T, V> andThen(LeaseFunction<? super R, ? extends V> other) {
            return value -> other.execute(execute(value));
        }

        /**
         * Convert an instance to an instance which returns the lease argument. This enables 
         * {@link java.util.function.Function#andThen(Function)} to be used when chaining operations.
         * @see UpdateOperations#chain(Collection, Function)
         */
        default LeaseFunction<T, T> chainable() {
            return leasedValue -> {
                execute(leasedValue);
                return leasedValue;
            };
        }
    }
}
