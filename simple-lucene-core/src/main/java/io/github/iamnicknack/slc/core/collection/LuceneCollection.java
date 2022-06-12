package io.github.iamnicknack.slc.core.collection;

import io.github.iamnicknack.slc.api.backend.LuceneBackend;
import io.github.iamnicknack.slc.api.index.DomainOperations;
import io.github.iamnicknack.slc.core.index.CollectionLikeUpdateOperations;

public class LuceneCollection<T> extends AbstractLuceneCollection<T> {

    public LuceneCollection(DomainOperations<T> domainOperations,
                            LuceneBackend backend) {
        super(domainOperations, backend, new CollectionLikeUpdateOperations<>(domainOperations, backend));
    }
}
