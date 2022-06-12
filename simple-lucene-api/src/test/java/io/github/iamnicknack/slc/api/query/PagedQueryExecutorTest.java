package io.github.iamnicknack.slc.api.query;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class PagedQueryExecutorTest {

    @Test
    @SuppressWarnings({"resource", "unchecked"})
    void defaultInvokesExecuteWithOptions_withInvocationCheck() {
        var invocationCheck = (BiConsumer<Object, QueryOptions>) mock(BiConsumer.class);
        PagedQueryExecutor<Object, Object> executor = (query, options) -> {
            invocationCheck.accept(query, options);
            return Collections::emptyIterator;
        };

        executor.execute("TEST");

        verify(invocationCheck).accept("TEST", QueryOptions.DEFAULT);
    }

    @Test
    @SuppressWarnings({"resource", "unchecked"})
    void defaultInvokesExecuteWithOptions_withMockQueryExecutor() {
        PagedQueryExecutor<Object, Object> executor = mock(PagedQueryExecutor.class);
        when(executor.execute("TEST")).thenCallRealMethod();

        executor.execute("TEST");

        verify(executor).execute("TEST", QueryOptions.DEFAULT);
    }


    @Test
    @SuppressWarnings({"resource", "unchecked"})
    void withOptionsOverridesDefaults() {
        var invocationCheck = (BiConsumer<Object, QueryOptions>) mock(BiConsumer.class);
        PagedQueryExecutor<Object, Object> executor = (query, options) -> {
            invocationCheck.accept(query, options);
            return Collections::emptyIterator;
        };

        var options = QueryOptions.TOP_HIT;
        executor.withOptions(options).execute("TEST");

        verify(invocationCheck).accept("TEST", options);
    }


    @Test
    void withOptionsWrapsOnlyPrimaryInstance() {
        PagedQueryExecutor<Object, Object> executor = (query, options) -> Collections::emptyIterator;

        var rootHashCode = System.identityHashCode(executor);
        var wrappedExecutor = (PagedQueryExecutor.Wrapper<Object, Object>)executor.withOptions(QueryOptions.TOP_HIT);

        assertEquals(PagedQueryExecutor.Wrapper.class, wrappedExecutor.getClass());
        assertEquals(rootHashCode, System.identityHashCode(wrappedExecutor.delegate()));

        wrappedExecutor = (PagedQueryExecutor.Wrapper<Object, Object>)wrappedExecutor.withOptions(QueryOptions.DEFAULT);
        assertEquals(rootHashCode, System.identityHashCode(wrappedExecutor.delegate()));

        assertEquals(QueryOptions.DEFAULT, wrappedExecutor.options());
    }
}
