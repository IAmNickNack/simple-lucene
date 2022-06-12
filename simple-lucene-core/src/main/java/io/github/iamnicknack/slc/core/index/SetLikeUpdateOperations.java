package io.github.iamnicknack.slc.core.index;

import io.github.iamnicknack.slc.api.index.DomainOperations;
import io.github.iamnicknack.slc.api.index.UpdateOperations;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.TermQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.iamnicknack.slc.api.backend.LuceneBackend;
import io.github.iamnicknack.slc.api.lease.Lease;

import java.util.Collection;

/**
 * Provides java {@link java.util.Set}-like operations for adding and removing unique documents from the index.
 * @param <T> the type on which operations are performed.
 */
public class SetLikeUpdateOperations<T> implements UpdateOperations<T> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final String idField = "_id";

    private final DomainOperations<T> domainOperations;
    private final LuceneBackend backend;

    public SetLikeUpdateOperations(DomainOperations<T> documentOperations,
                                   LuceneBackend backend) {
        this.domainOperations = documentOperations;
        this.backend = backend;
    }

    /**
     * Add a document to the index, first checking for duplicates.
     *
     * <p>Use {@link #update(Object)}</p> to skip any duplicate check and update an existing item.
     *
     * @param value the value to add
     * @return the parameter value
     * @throws IllegalArgumentException if a value with the same id already exists
     */
    @Override
    @SuppressWarnings("resource")
    public Lease.LeaseFunction<LuceneBackend.UpdateComponents, Void> add(T value) {
        return components -> {
            var id = domainOperations.id(value);
            var term = new Term("_id", id);

            try(var lease = backend.searcherLeaseFactory().lease()) {
                int count = lease.execute(searchComponents -> searchComponents.indexSearcher().count(new TermQuery(term)));
                if(count != 0) {
                    logger.warn("Value already exists: {}, {}", id, value);
                    throw new IllegalArgumentException("Value already exists");
                }
            }

            var document = domainOperations.createDocument(value);
            document.add(new StringField(idField, id, Field.Store.YES));
            components.indexWriter().addDocument(components.build(document));

            return null;
        };
    }

    /**
     * Add or update an item regardless of whether it already exists in the index
     * @param value the value to update
     * @return the parameter value
     */
    @Override
    @SuppressWarnings("resource")
    public Lease.LeaseFunction<LuceneBackend.UpdateComponents, Void> update(T value) {
        return components -> {
            var id = domainOperations.id(value);
            var term = new Term("_id", id);

            var document = domainOperations.createDocument(value);
            document.add(new StringField(idField, id, Field.Store.YES));
            components.indexWriter().updateDocument(term, components.build(document));
            return null;
        };
    }

    /**
     * Remove an item from the index. This operation does not check that the value exists before performing the
     * underlying delete operation.
     * @param value the value to delete
     * @return the parameter value
     */
    @Override
    @SuppressWarnings("resource")
    public Lease.LeaseFunction<LuceneBackend.UpdateComponents, Void> delete(T value) {
        return components -> {
            var id = domainOperations.id(value);
            var term = new Term("_id", id);

            components.indexWriter().deleteDocuments(term);
            return null;
        };
    }

    /**
     * Delete all documents matching any element in the provided collection
     * @param values the values to match for deletion
     * @return the number of distinct terms included in the deletion query
     */
    @Override
    @SuppressWarnings("resource")
    public Lease.LeaseFunction<LuceneBackend.UpdateComponents, Integer> deleteAll(Collection<T> values) {
        var builder = new BooleanQuery.Builder();
        values.stream()
                .map(domainOperations::id)
                .map(id -> new BooleanClause(new TermQuery(new Term(idField, id)), BooleanClause.Occur.SHOULD))
                .forEach(builder::add);
        var query = builder.build();

        return components -> {
            components.indexWriter().deleteDocuments(query);
            return query.clauses().size();
        };
    }
}
