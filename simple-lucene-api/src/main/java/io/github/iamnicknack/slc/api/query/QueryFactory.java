package io.github.iamnicknack.slc.api.query;


import org.apache.lucene.search.Query;

/**
 * Abstraction to create a {@link Query} from a value of {@link T}
 * @param <T> the type of query term
 */
public interface QueryFactory<T> {

    /**
     * Construct a query with the specified term
     * @param value the query term
     * @return a Lucene {@link Query}
     */
    Query query(T value);
}
