package com.github.iamnicknack.slc.core.collection;

import com.github.iamnicknack.slc.api.backend.LuceneBackend;
import com.github.iamnicknack.slc.api.index.DomainOperations;
import com.github.iamnicknack.slc.core.index.CollectionLikeUpdateOperations;

public class LuceneCollection<T> extends AbstractLuceneCollection<T> {

    public LuceneCollection(DomainOperations<T> domainOperations,
                            LuceneBackend backend) {
        super(domainOperations, backend, new CollectionLikeUpdateOperations<>(domainOperations, backend));
    }
}
