package io.github.iamnicknack.slc.core.collection;

import io.github.iamnicknack.slc.api.backend.LuceneBackend;
import io.github.iamnicknack.slc.api.index.DomainOperations;
import io.github.iamnicknack.slc.core.index.SetLikeUpdateOperations;

import java.util.Set;

public class LuceneSet<T> extends AbstractLuceneCollection<T> implements Set<T> {

    public LuceneSet(DomainOperations<T> domainOperations,
                            LuceneBackend backend) {
        super(domainOperations, backend, new SetLikeUpdateOperations<>(domainOperations, backend));
    }
}
