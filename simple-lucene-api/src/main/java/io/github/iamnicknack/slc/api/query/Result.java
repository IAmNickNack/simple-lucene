package io.github.iamnicknack.slc.api.query;

import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;


/**
 * An iterable set of {@link Hit} instances as returned by a {@link QueryExecutor}
 * @param <T> the hit-type
 */
public interface Result<T> extends Iterable<Hit<T>>, AutoCloseable {

    /**
     * Total hits reported by Lucene for this search result
     */
    default long totalHits() {
        return 0;
    }

    /**
     * Close any resources used by the instance
     */
    default void close() {}

    /**
     * Provides this result as a stream, adding a call to {@link #close()}
     * on {@link Stream#onClose(Runnable)}
     * @return a stream of {@link Hit}s
     */
    default Stream<Hit<T>> stream() {
        return StreamSupport.stream(spliterator(), false)
                .onClose(this::close);
    }

    /**
     * Provides this result as a collection and releases resources
     * @return a collection of {@link Hit}s
     */
    default List<Hit<T>> list() {
        try(var stream = this.stream()) {
            return stream.toList();
        }
    }

    /**
     * Provide a result which iterates using an alternative iterator.
     * @param iteratorFactory the factory to create the alternative iterator
     * @return a result of type V
     * @param <V> the desired type for each {@link Hit}
     */
    default <V> Result<V> withIterator(IteratorFactory<T, V> iteratorFactory) {
        return new Result<>() {

            @Override
            public long totalHits() {
                return Result.this.totalHits();
            }

            @Override
            public void close() {
                Result.this.close();
            }

            @Override
            public Iterator<Hit<V>> iterator() {
                return iteratorFactory.create(Result.this.iterator());
            }
        };
    }

    /**
     * Function to create an iterator of type {@link R} from one of type {@link T}
     * @param <T> the input type
     * @param <R> the output type
     */
    interface IteratorFactory<T, R> {

        /**
         * Create an instance using the provided mapping function to perform conversion between {@link T} and {@link R}
         * @param mapper the mapping function
         * @return an {@link IteratorFactory} to convert {@link T} to {@link R}
         * @param <T> the input type
         * @param <R> the output type
         */
        static <T, R> IteratorFactory<T, R> mapping(Function<T, R> mapper) {
            return iterator -> new Iterator<>() {
                @Override
                public boolean hasNext() {
                    return iterator.hasNext();
                }

                @Override
                public Hit<R> next() {
                    Hit<T> next = iterator.next();
                    return new HitRecord<>(next.score(), mapper.apply(next.value()));
                }
            };
        }

        /**
         * Convert the source iterator to one of the target type
         * @param iterator the iterator to be wrapped
         * @return an iterator of the target type
         */
        Iterator<Hit<R>> create(Iterator<Hit<T>> iterator);
    }
}
