package io.github.iamnicknack.slc.api.query;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class QueryExecutorTest {

    @Test
    @SuppressWarnings({"resource", "unchecked"})
    void defaultInvokesExecuteWithOptions_withInvocationCheck() {
        var invocationCheck = (BiConsumer<Object, QueryOptions>) mock(BiConsumer.class);
        QueryExecutor<Object, Object> executor = (query, options) -> {
            invocationCheck.accept(query, options);
            return Collections::emptyIterator;
        };

        executor.execute("TEST");

        verify(invocationCheck).accept("TEST", QueryOptions.DEFAULT);
    }

    @Test
    @SuppressWarnings({"resource", "unchecked"})
    void defaultInvokesExecuteWithOptions_withMockQueryExecutor() {
        QueryExecutor<Object, Object> executor = mock(QueryExecutor.class);
        when(executor.execute("TEST")).thenCallRealMethod();

        executor.execute("TEST");

        verify(executor).execute("TEST", QueryOptions.DEFAULT);
    }


    @Test
    @SuppressWarnings({"resource", "unchecked"})
    void withOptionsOverridesDefaults() {
        var invocationCheck = (BiConsumer<Object, QueryOptions>) mock(BiConsumer.class);
        QueryExecutor<Object, Object> executor = (query, options) -> {
            invocationCheck.accept(query, options);
            return Collections::emptyIterator;
        };

        var options = QueryOptions.TOP_HIT;
        executor.withOptions(options).execute("TEST");

        verify(invocationCheck).accept("TEST", options);
    }


    @Test
    void withOptionsWrapsOnlyPrimaryInstance() {
        QueryExecutor<Object, Object> executor = (query, options) -> Collections::emptyIterator;

        var rootHashCode = System.identityHashCode(executor);
        var wrappedExecutor = (QueryExecutor.Wrapper<Object, Object>)executor.withOptions(QueryOptions.TOP_HIT);

        assertEquals(QueryExecutor.Wrapper.class, wrappedExecutor.getClass());
        assertEquals(rootHashCode, System.identityHashCode(wrappedExecutor.delegate()));

        wrappedExecutor = (QueryExecutor.Wrapper<Object, Object>)wrappedExecutor.withOptions(QueryOptions.DEFAULT);
        assertEquals(rootHashCode, System.identityHashCode(wrappedExecutor.delegate()));

        assertEquals(QueryOptions.DEFAULT, wrappedExecutor.options());
    }


    @Test
    @SuppressWarnings("resource")
    void withIteratorMapsValues() {
        record OutputType(String value) {}
        var iteratorFactory = Result.IteratorFactory.mapping(o -> new OutputType(o.toString()));
        var data = List.<Object>of("first", "second");
        var executor = new FakeQueryExecutor(data)
                .withIterator(iteratorFactory);

        var list = executor.execute(null).list();
        for (int i = 0; i < list.size(); i++) {
            assertEquals(data.get(i), list.get(i).value().value);
        }
    }
}
