package io.github.iamnicknack.slc.api.index;

import io.github.iamnicknack.slc.api.backend.LuceneBackend.UpdateComponents;
import io.github.iamnicknack.slc.api.lease.Lease;
import io.github.iamnicknack.slc.api.lease.Lease.LeaseFunction;

import java.util.Collection;
import java.util.function.Function;

/**
 * Defines basic operations which may be required on an index to add, update and delete documents using an
 * {@link UpdateComponents} {@link Lease}
 *
 * @param <T> the type on which operations are performed
 */
public interface UpdateOperations<T> {

    /**
     * Function to add a value to the index
     * @param value the value to add
     * @return a function which will add the specified value once invoked
     */
    LeaseFunction<UpdateComponents, Void> add(T value);

    default LeaseFunction<UpdateComponents, Integer> addAll(Collection<T> values) {
        return chain(values, this::add);
    }

    /**
     * Function to update a value in the index
     * @param value the value to update
     * @return a function which will update the specified value once invoked
     */
    default LeaseFunction<UpdateComponents, Void> update(T value) {
        throw new UnsupportedOperationException("update");
    }

    default LeaseFunction<UpdateComponents, Integer> updateAll(Collection<T> values) {
        return chain(values, this::update);
    }

    /**
     * Function to remove a value from the index
     * @param value the value to remove
     * @return a function which will remove the specified value once invoked
     */
    default LeaseFunction<UpdateComponents, Void> delete(T value) {
        throw new UnsupportedOperationException("delete");
    }

    default LeaseFunction<UpdateComponents, Integer> deleteAll(Collection<T> values) {
        return chain(values, this::delete);
    }

    /**
     * Function to remove all domain-specific documents from the index
     * @return a function which will clear the index of all domain-specific values once invoked
     */
    default LeaseFunction<UpdateComponents, Void> clear() {
        throw new UnsupportedOperationException("clear");
    }

    /**
     * Function which will apply the specified operation to all values in the provided collection
     * @param values the values arguments
     * @param operation the operation to apply to the values
     * @return a function which will apply the specified operation to all the provided values once invoked
     * @param <T> the type on which operations are performed
     */
    static <T> LeaseFunction<UpdateComponents, Integer> chain(Collection<T> values, Function<T, LeaseFunction<UpdateComponents, Void>> operation) {
        return values.stream()
                .map(operation)
                .map(LeaseFunction::chainable)
                .reduce(value -> value, LeaseFunction::compose)
                .andThen(ignored -> values.size());
    }
}
