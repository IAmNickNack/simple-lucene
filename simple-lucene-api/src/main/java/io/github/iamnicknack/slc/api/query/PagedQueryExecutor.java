package io.github.iamnicknack.slc.api.query;

/**
 * Abstraction to provide a simplified interface to execute a query while retrieving results
 * in `pages` containing {@link QueryOptions#maxHits()} documents.
 *
 * <p>Useful when a significant number of results are expected, but the total number is not
 * known ahead of time</p>
 * @param <K> the query term
 * @param <V> the {@link Hit}-type
 */
public interface PagedQueryExecutor<K, V> {


    /**
     * Execute the query term using the specified options
     * @param query the query term
     * @param options optional query parameters
     * @return matching results
     */
    PagedResult<V> execute(K query, QueryOptions options);

    /**
     * Execute the query term using the current default query options ({@link QueryOptions#DEFAULT}
     * or {@link #withOptions(QueryOptions)})
     * @param query the query term
     * @return matching results
     */
    default PagedResult<V> execute(K query) {
        return execute(query, QueryOptions.DEFAULT);
    }

    /**
     * Override the default {@link QueryOptions}
     * @param queryOptions alternative query options
     * @return a new instance using the specified options as defaults
     */
    default PagedQueryExecutor<K, V> withOptions(QueryOptions queryOptions) {
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
    record Wrapper<K, V>(PagedQueryExecutor<K, V> delegate,
                         QueryOptions options) implements PagedQueryExecutor<K, V> {
        @Override
        public PagedResult<V> execute(K query, QueryOptions ignored) {
            return delegate.execute(query, options);
        }
    }

}
