package com.github.iamnicknack.slc.core.index;

import com.github.iamnicknack.slc.api.index.DomainOperations;
import com.github.iamnicknack.slc.api.index.UpdateOperations;
import com.github.iamnicknack.slc.api.query.QueryExecutor;
import com.github.iamnicknack.slc.api.query.QueryOptions;
import com.github.iamnicknack.slc.core.query.DefaultQueryExecutor;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.TermQuery;
import com.github.iamnicknack.slc.api.backend.LuceneBackend;
import com.github.iamnicknack.slc.api.lease.Lease;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Provides java {@link Collection}-like operations for adding and removing documents from the index. Specifically:
 *
 * <ul>
 *     <li>Allows adding of duplicate documents via {@link #add(Object)}</li>
 *     <li>{@link #delete(Object)} removes only the first occurrence of a document</li>
 * </ul>
 *
 * Other operations:
 *
 * <ul>
 *     <li>{@link #update(Object)} is implemented to behave in a similar manner to {@link #delete(Object)}
 *     in that only the first occurrence of the document is updated.
 *     </li>
 * </ul>
 *
 * @param <T> the type on which operations are performed
 */
public class CollectionLikeUpdateOperations<T> implements UpdateOperations<T> {

    private static final String idField = "_id";
    private static final String serialField = "_serial";

    private final QueryExecutor<T, Document> queryExecutor;

    private final DomainOperations<T> documentOperations;

    private final AtomicLong serial = new AtomicLong();

    public CollectionLikeUpdateOperations(DomainOperations<T> documentOperations,
                                          LuceneBackend backend) {
        this.documentOperations = documentOperations;
        this.queryExecutor = new DefaultQueryExecutor<T>(
                value -> new TermQuery(new Term(idField, documentOperations.id(value))),
                backend.searcherLeaseFactory()
        ).withOptions(QueryOptions.TOP_HIT);
    }

    /**
     * Add a document to the index, allowing duplicates
     * @param value the value to add
     * @return the parameter value
     */
    @Override
    @SuppressWarnings("resource")
    public Lease.LeaseFunction<LuceneBackend.UpdateComponents, Void> add(T value) {
        return components -> {
            var id = documentOperations.id(value);
            var ser = "%s.%s".formatted(id, serial.getAndIncrement());

            var document = documentOperations.createDocument(value);
            document.add(new StringField(idField, id, Field.Store.YES));
            document.add(new StringField(serialField, ser, Field.Store.YES));
            components.indexWriter().addDocument(components.build(document));
            return null;
        };
    }

    /**
     * Updates the first document found with {@link #idField} field matching that
     * of the provided document
     * @param value the value to update
     * @return the parameter value
     */
    @Override
    @SuppressWarnings("resource")
    public Lease.LeaseFunction<LuceneBackend.UpdateComponents, Void> update(T value) {
        return components -> {
            try(var result = queryExecutor.execute(value)) {
                if(result.totalHits() > 0) {
                    var current = result.iterator().next().value();
                    var term = new Term(serialField, current.get(serialField));

                    var document = documentOperations.createDocument(value);
                    document.add(current.getField(idField));
                    document.add(current.getField(serialField));

                    components.indexWriter().updateDocument(term, components.build(document));
                }
                else {
                    add(value).execute(components);
                }
            }
            return null;
        };
     }

    /**
     * Delete the first document matching the specified value
     * @param value the value to delete
     * @return the parameter value
     */
    @Override
    @SuppressWarnings("resource")
    public Lease.LeaseFunction<LuceneBackend.UpdateComponents, Void> delete(T value) {
        return components -> {
            try(var result = queryExecutor.execute(value)) {
                if (result.totalHits() > 0) {
                    Document document = result.iterator().next().value();
                    TermQuery termQuery = new TermQuery(new Term(serialField, document.get(serialField)));
                    components.indexWriter().deleteDocuments(termQuery);
                }
            }
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
                .map(documentOperations::id)
                .map(id -> new BooleanClause(new TermQuery(new Term(idField, id)), BooleanClause.Occur.SHOULD))
                .forEach(builder::add);
        var query = builder.build();

        return components -> {
            components.indexWriter().deleteDocuments(query);
            return query.clauses().size();
        };
    }
}
