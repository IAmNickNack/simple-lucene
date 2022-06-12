package com.github.iamnicknack.slc.core.index;

import com.github.iamnicknack.slc.api.index.DomainOperations;
import com.github.iamnicknack.slc.api.index.UpdateOperations;
import com.github.iamnicknack.slc.api.backend.LuceneBackend;
import com.github.iamnicknack.slc.api.lease.Lease;

/**
 * Minimal implementation of {@link UpdateOperations} which can be used to add documents to an index with no duplicate
 * check or will afterward be considered read-only.
 *
 * <p>In this case, update and delete operations are not required and as such there is no requirement for
 * the {@link UpdateOperations} instance to maintain consistency through the use of document identifiers</p>
 *
 * @param <T> the type on which operations are performed
 */
public class BucketUpdateOperations<T> implements UpdateOperations<T> {

    private final DomainOperations<T> domainOperations;

    public BucketUpdateOperations(DomainOperations<T> domainOperations) {
        this.domainOperations = domainOperations;
    }

    /**
     * Adds a document without creating additional id-type fields which would be required for later 
     * modification operations such as {@link #update(Object)} and {@link #delete(Object)}
     * @param value the value to index
     * @return the parameter value
     */
    @Override
    @SuppressWarnings("resource")
    public Lease.LeaseFunction<LuceneBackend.UpdateComponents, Void> add(T value) {
        return components -> {
            var document = domainOperations.createDocument(value);
            components.indexWriter().addDocument(components.build(document));
            return null;
        };
    }
}
