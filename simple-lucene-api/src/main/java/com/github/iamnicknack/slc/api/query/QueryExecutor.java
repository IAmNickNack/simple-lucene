package com.github.iamnicknack.slc.api.query;

/**
 * Abstraction to provide a simplified interface to execute a query or lookup using a single value
 * @param <K> the query value type
 * @param <V> the result type
 */
public interface QueryExecutor<K, V> {

    /**
     * Execute the query term using the specified options
     * @param query the query term
     * @param options optional query parameters
     * @return matching results
     */
    Result<V> execute(K query, QueryOptions options);

    /**
     * Execute the query term using the current default query options ({@link QueryOptions#DEFAULT}
     * or {@link #withOptions(QueryOptions)})
     * @param query the query term
     * @return matching results
     */
    default Result<V> execute(K query) {
        return execute(query, QueryOptions.DEFAULT);
    }

    /**
     * Provides an instance returning hits of type {@link T}. 
     * <p>Essentially a convenience method wrapping {@link Result#withIterator(Result.IteratorFactory)}</p>
     * @param iteratorFactory factory to create an iterator which transforms to type {@link T}
     * @return a query executor wrapping this instance
     * @param <T> the type represented by each document
     * @see Result#withIterator(Result.IteratorFactory)
     */
    @SuppressWarnings("resource")
    default <T> QueryExecutor<K, T> withIterator(Result.IteratorFactory<V, T> iteratorFactory) {
        return (query, options) -> this.execute(query, options)
                .withIterator(iteratorFactory);
    }

    /**
     * Override the default {@link QueryOptions}
     * @param queryOptions alternative query options
     * @return a new instance using the specified options as defaults
     */
    default QueryExecutor<K, V> withOptions(QueryOptions queryOptions) {
        return (this instanceof Wrapper<K,V> wrapper)
                ? new Wrapper<>(wrapper.delegate, queryOptions)
                : new Wrapper<>(this, queryOptions);
    }

    /**
     * Wrapper record used to limit the size of the stack that would otherwise
     * result from multiple calls to {@link #withOptions(QueryOptions)}
     * @param delegate root executor instance
     * @param options alternative query options
     * @param <K> the query type
     * @param <V> the result type
     */
    record Wrapper<K, V>(QueryExecutor<K, V> delegate,
                         QueryOptions options) implements QueryExecutor<K, V> {
        @Override
        public Result<V> execute(K query, QueryOptions ignored) {
            return delegate.execute(query, options);
        }
    }

}
