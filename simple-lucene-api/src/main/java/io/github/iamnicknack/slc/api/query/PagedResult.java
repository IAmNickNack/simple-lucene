package io.github.iamnicknack.slc.api.query;

import java.io.Closeable;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Pageable {@link Result}s. Allows closing or releasing of an {@link org.apache.lucene.search.IndexSearcher}
 * in order to utilise the same searcher instance across all pages.
 * @param <T> the hit-type
 */
public interface PagedResult<T> extends Iterable<Result<T>>, AutoCloseable {

    /**
     * Generate a {@link Stream} of {@link Hit}s from all {@link Result}s provided by {@link #iterator()}
     * @return a stream of {@link Hit}s
     */
    default Stream<Hit<T>> stream() {
        return StreamSupport.stream(spliterator(), false)
                .flatMap(Result::stream)
                .onClose(this::close);
    }


    /**
     * Override {@link Closeable#close()} to not throw {@link java.io.IOException}
     */
    default void close() {}

}
