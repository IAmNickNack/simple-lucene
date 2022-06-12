package com.github.iamnicknack.slc.core.collection;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import com.github.iamnicknack.slc.api.backend.LuceneBackend;
import com.github.iamnicknack.slc.api.index.DomainOperations;
import com.github.iamnicknack.slc.api.index.UpdateOperations;
import com.github.iamnicknack.slc.api.query.Hit;
import com.github.iamnicknack.slc.api.query.HitRecord;
import com.github.iamnicknack.slc.core.query.DefaultPagedQueryExecutor;
import com.github.iamnicknack.slc.core.query.DefaultQueryExecutor;
import com.github.iamnicknack.slc.core.query.QueryFactories;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.stream.Stream;

/**
 * Utility implementation of {@link Collection} which provides a simple API for creating a Lucene index
 * @param <T> the object type to be stored in the index
 */
public abstract class AbstractLuceneCollection<T> extends AbstractCollection<T> {

    private final DomainOperations<T> domainOperations;
    private final LuceneBackend backend;
    private final UpdateOperations<T> updateOperations;

    protected AbstractLuceneCollection(DomainOperations<T> domainOperations,
                                       LuceneBackend backend,
                                       UpdateOperations<T> updateOperations) {
        this.domainOperations = domainOperations;
        this.backend = backend;
        this.updateOperations = updateOperations;
    }

    @Override
    public boolean add(T t) {
        var operation = updateOperations.add(t);
        backend.updateLeaseFactory().execute(operation);
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        var values = c.stream().map(v -> (T)v).toList();
        var operation = updateOperations.addAll(values);
        backend.updateLeaseFactory().execute(operation);

        return true;
    }


    @Override
    @SuppressWarnings("unchecked")
    public boolean remove(Object value) {
        var operation = updateOperations.delete((T)value);
        backend.updateLeaseFactory().execute(operation);

        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean removeAll(Collection<?> c) {
        List<T> values = c.stream()
                .map(o -> (T)o)
                .toList();

        var operation = updateOperations.deleteAll(values);
        backend.updateLeaseFactory().execute(operation);

        return values.size() > 0;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean contains(Object o) {
        Term term = idTerm((T)o);
        return backend.searcherLeaseFactory()
                .execute(components -> components.indexSearcher().count(new TermQuery(term))) > 0;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        BooleanQuery query = queryForCollection(c);

        return backend.searcherLeaseFactory()
                .execute(components -> components.indexSearcher().count(query) == query.clauses().size());
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        BooleanQuery query = new BooleanQuery.Builder()
                .add(new MatchAllDocsQuery(), BooleanClause.Occur.MUST)
                .add(queryForCollection(c), BooleanClause.Occur.MUST_NOT)
                .build();

        backend.updateLeaseFactory().execute(components -> components.indexWriter().deleteDocuments(query));

        return true;
    }

    @Override
    public void clear() {
        backend.updateLeaseFactory()
                .execute(components -> components.indexWriter().deleteDocuments(new MatchAllDocsQuery()));
    }

    @Override
    public int size() {
        return backend.searcherLeaseFactory()
                .execute(components -> components.indexSearcher().count(new MatchAllDocsQuery()));
    }

    @Override
    public boolean isEmpty() {
        return this.size() == 0;
    }

    @Override
    public Iterator<T> iterator() {
        return stream().iterator();
    }

    @Override
    public Stream<T> stream() {
        var executor = new DefaultPagedQueryExecutor<>(QueryFactories.lucene(), backend.searcherLeaseFactory());

        try(var result = executor.execute(new MatchAllDocsQuery())) {
            return result.stream()
                    .map(hit -> domainOperations.readDocument(hit.value()));
        }
    }

    @Override
    public void forEach(Consumer<? super T> action) {
        stream().forEach(action);
    }


    @Override
    public Object[] toArray() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T1> T1[] toArray(IntFunction<T1[]> generator) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T1> T1[] toArray(T1[] a) {
        throw new UnsupportedOperationException();
    }


    /**
     * Create a {@link Term} to match documents by id
     * @param value the value from which to derive the term
     * @return the ID term
     */
    private Term idTerm(T value) {
        return new Term("_id", domainOperations.id(value));
    }

    /**
     * Build a {@code terms} query to match on any item in the collection
     * @param c items to match
     * @return {@link BooleanQuery} using {@link BooleanClause.Occur#SHOULD}
     */
    @SuppressWarnings("unchecked")
    private BooleanQuery queryForCollection(Collection<?> c) {
        List<Term> terms = c.stream()
                .map(o -> domainOperations.id((T)o))
                .distinct()
                .map(id -> new Term("_id", id))
                .toList();

        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        for(Term t : terms) {
            builder.add(new TermQuery(t), BooleanClause.Occur.SHOULD);
        }
        return builder.setMinimumNumberShouldMatch(1).build();
    }

    public Optional<Hit<T>> queryFirst(Query query) {
        var executor = new DefaultQueryExecutor<>(QueryFactories.lucene(), backend.searcherLeaseFactory())
                .withOptions(() -> 1);

        try(var result = executor.execute(query)) {
            return result.stream()
                    .map(hit -> (Hit<T>)new HitRecord<>(hit.score(), domainOperations.readDocument(hit.value())))
                    .findFirst();
        }
    }

    public List<Hit<T>> query(Query query) {
        var executor = new DefaultPagedQueryExecutor<>(QueryFactories.lucene(), backend.searcherLeaseFactory());

        try(var result = executor.execute(query)) {
            return result.stream()
                    .map(hit -> (Hit<T>)new HitRecord<>(hit.score(), domainOperations.readDocument(hit.value())))
                    .toList();
        }
    }

    public Stream<Hit<T>> queryStream(Query query) {
        var executor = new DefaultPagedQueryExecutor<>(QueryFactories.lucene(), backend.searcherLeaseFactory());

        try(var result = executor.execute(query)) {
            return result.stream()
                    .map(hit -> new HitRecord<>(hit.score(), domainOperations.readDocument(hit.value())));
        }
    }
}
