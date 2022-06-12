package io.github.iamnicknack.slc.core.query;

import io.github.iamnicknack.slc.api.backend.LuceneBackend.SearchComponents;
import io.github.iamnicknack.slc.api.lease.Lease;
import io.github.iamnicknack.slc.api.lease.LeaseFactory;
import io.github.iamnicknack.slc.api.query.QueryExecutor;
import io.github.iamnicknack.slc.api.query.QueryFactory;
import io.github.iamnicknack.slc.api.query.QueryOptions;
import io.github.iamnicknack.slc.api.query.Result;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultQueryExecutor<K> implements QueryExecutor<K, Document> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final QueryFactory<K> queryFactory;
    private final LeaseFactory<SearchComponents> searcherLeaseFactory;

    public DefaultQueryExecutor(QueryFactory<K> queryFactory,
                                LeaseFactory<SearchComponents> searcherLeaseFactory) {
        this.queryFactory = queryFactory;
        this.searcherLeaseFactory = searcherLeaseFactory;
    }

    @Override
    public Result<Document> execute(K query, QueryOptions options) {
        Lease<SearchComponents> lease = searcherLeaseFactory.lease();
        Query luceneQuery = queryFactory.query(query);
        TopDocs docs = lease.execute(components ->
                components.indexSearcher().search(luceneQuery, options.maxHits())
        );

        return new DefaultResult(docs, lease);
    }
}
