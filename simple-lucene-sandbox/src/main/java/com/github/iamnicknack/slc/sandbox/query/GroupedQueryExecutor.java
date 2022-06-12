package com.github.iamnicknack.slc.sandbox.query;

import com.github.iamnicknack.slc.api.backend.LuceneBackend;
import com.github.iamnicknack.slc.api.lease.LeaseFactory;
import com.github.iamnicknack.slc.api.query.*;
import com.github.iamnicknack.slc.core.query.DefaultQueryExecutor;
import org.apache.lucene.document.Document;
import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.facet.LabelAndValue;
import org.apache.lucene.facet.taxonomy.FastTaxonomyFacetCounts;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.TermQuery;

import java.util.Iterator;
import java.util.function.Function;

public class GroupedQueryExecutor<K, V> implements QueryExecutor<K, GroupedQueryExecutor.GroupedResult<V>> {

    private final String fieldName;
    private final QueryFactory<K> queryFactory;
    private final Function<Document, V> beanReader;
    private final LeaseFactory<LuceneBackend.SearchComponents> leaseFactory;

    public GroupedQueryExecutor(String fieldName,
                                QueryFactory<K> queryFactory,
                                Function<Document, V> beanReader,
                                LeaseFactory<LuceneBackend.SearchComponents> leaseFactory) {
        this.fieldName = fieldName;
        this.queryFactory = queryFactory;
        this.beanReader = beanReader;
        this.leaseFactory = leaseFactory;
    }

    @Override
    public Result<GroupedResult<V>> execute(K query, QueryOptions options) {
        var indexSearcherLease = leaseFactory.lease();

        var luceneQuery = queryFactory.query(query);
        var childQueryFactory = (QueryFactory<LabelAndValue>) value -> new BooleanQuery.Builder()
                .add(luceneQuery, BooleanClause.Occur.MUST)
                .add(new TermQuery(new Term(fieldName + ".keyword", value.label)), BooleanClause.Occur.MUST)
                .build();
        var childQueryExecutor = new DefaultQueryExecutor<>(
                childQueryFactory,
                leaseFactory
        ).withIterator(Result.IteratorFactory.mapping(beanReader));

        return indexSearcherLease.execute(components -> {
            var collector = new FacetsCollector();
            /*
            Executing search but not using results from this query. Only facets
             */
            FacetsCollector.search(components.indexSearcher(), luceneQuery, 1, collector);
            var counts = new FastTaxonomyFacetCounts(components.taxonomyReader(), components.facetsConfig(), collector);
            var facets = counts.getTopChildren(options.maxHits(), fieldName + ".value");

            return new Result<GroupedResult<V>>() {
                @Override
                public long totalHits() {
                    return facets.childCount;
                }

                @Override
                public void close() {
                    indexSearcherLease.close();
                }

                @Override
                public Iterator<Hit<GroupedResult<V>>> iterator() {
                    return new Iterator<>() {
                        private int index = 0;

                        @Override
                        public boolean hasNext() {
                            return index < facets.childCount;
                        }

                        @Override
                        public Hit<GroupedResult<V>> next() {
                            var current = facets.labelValues[index++];
                            return new HitRecord<>(
                                    current.value.floatValue(),
                                    new GroupedResult<>(current.label, childQueryExecutor.execute(current))
                            );
                        }
                    };
                }
            };
        });
    }

    public record GroupedResult<V>(String key,
                                   Result<V> result) {}
}
