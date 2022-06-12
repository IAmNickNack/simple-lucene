package com.github.iamnicknack.slc.api.index;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.github.iamnicknack.slc.api.backend.LuceneBackend.UpdateComponents;
import com.github.iamnicknack.slc.api.lease.Lease;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
class UpdateOperationsTest {

    private final Consumer<Integer> invocationCheck = mock(Consumer.class);
    private final UpdateComponents updateComponents = mock(UpdateComponents.class);

    /*
    Fake lease operations
     */
    private final UpdateOperations<Integer> updateOperations = new UpdateOperations<>() {
        @Override
        public Lease.LeaseFunction<UpdateComponents, Void> add(Integer value) {
            return leasedValue -> {
                invocationCheck.accept(value);
                return null;
            };
        }

        @Override
        public Lease.LeaseFunction<UpdateComponents, Void> update(Integer value) {
            return leasedValue -> {
                invocationCheck.accept(value);
                return null;
            };
        }

        @Override
        public Lease.LeaseFunction<UpdateComponents, Void> delete(Integer value) {
            return leasedValue -> {
                invocationCheck.accept(value);
                return null;
            };
        }
    };


    @BeforeEach
    void beforeEach() {
        reset(invocationCheck, updateComponents);
    }

    /**
     * Test that {@link UpdateOperations#add(Object)} is called for all objects in a collection
     */
    @Test
    void addAllChainsOperationsForMultipleValues() throws Exception {
        chainTest(updateOperations::addAll);
    }

    /**
     * Test that {@link UpdateOperations#update(Object)} is called for all objects in a collection
     */
    @Test
    void updateAllChainsOperationsForMultipleValues() throws Exception {
        chainTest(updateOperations::updateAll);
    }

    /**
     * Test that {@link UpdateOperations#add(Object)} is called for all objects in a collection
     */
    @Test
    void deleteAllChainsOperationsForMultipleValues() throws Exception {
        chainTest(updateOperations::deleteAll);
    }

    private void chainTest(Function<Collection<Integer>, Lease.LeaseFunction<UpdateComponents, Integer>> function) throws Exception {
        var testValues = List.of(1, 2, 3);
        /*
        Create the `xxxAll` composition for all `testValues`
         */
        var operation = function.apply(testValues);
        /*
        Verify that single-arg version is invoked for every value
         */
        operation.execute(updateComponents);
        testValues.forEach(value -> verify(invocationCheck).accept(value));
    }

    /**
     * Relatively redundant test for default method
     */
    @Test
    void defaultUpdateThrowsException() {
        UpdateOperations<Integer> operations = value -> null;
        assertThrows(UnsupportedOperationException.class, () -> operations.updateAll(List.of(1)));
    }

    /**
     * Relatively redundant test for default method
     */
    @Test
    void defaultDeleteThrowsException() {
        UpdateOperations<Integer> operations = value -> null;
        assertThrows(UnsupportedOperationException.class, () -> operations.deleteAll(List.of(1)));
    }

    /**
     * Relatively redundant test for default method
     */
    @Test
    void defaultClearThrowsException() {
        UpdateOperations<Integer> operations = value -> null;
        assertThrows(UnsupportedOperationException.class, operations::clear);
    }
}