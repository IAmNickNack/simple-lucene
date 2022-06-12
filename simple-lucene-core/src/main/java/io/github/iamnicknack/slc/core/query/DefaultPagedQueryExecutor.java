package io.github.iamnicknack.slc.core.query;

import io.github.iamnicknack.slc.api.backend.LuceneBackend;
import io.github.iamnicknack.slc.api.lease.Lease;
import io.github.iamnicknack.slc.api.lease.LeaseFactory;
import io.github.iamnicknack.slc.api.query.*;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

import java.util.Iterator;

public class DefaultPagedQueryExecutor<K> implements PagedQueryExecutor<K, Document> {

    private final QueryFactory<K> queryFactory;
    private final LeaseFactory<LuceneBackend.SearchComponents> searcherLeaseFactory;

    public DefaultPagedQueryExecutor(QueryFactory<K> queryFactory,
                                     LeaseFactory<LuceneBackend.SearchComponents> searcherLeaseFactory) {
        this.queryFactory = queryFactory;
        this.searcherLeaseFactory = searcherLeaseFactory;
    }

    @Override
    public PagedResult<Document> execute(K query, QueryOptions options) {
        var lease = searcherLeaseFactory.lease();
        Query luceneQuery = queryFactory.query(query);

        PageFactory pageFactory = last -> lease.execute(components ->
                components.indexSearcher().searchAfter(last, luceneQuery, options.maxHits())
        );

        return new PagedResultImpl(pageFactory, lease);
    }

    static class PagedResultImpl implements PagedResult<Document> {

        private final PageFactory pageFactory;
        private final Lease<LuceneBackend.SearchComponents> lease;

        public PagedResultImpl(PageFactory pageFactory,
                               Lease<LuceneBackend.SearchComponents> lease) {
            this.pageFactory = pageFactory;
            this.lease = lease;
        }

        @Override
        public void close() {
            lease.close();
        }

        @Override
        public Iterator<Result<Document>> iterator() {
            return new Iterator<>() {

                private TopDocs lastDocs = null;
                private int currentIndex = 0;

                @Override
                public boolean hasNext() {
                    return lastDocs == null
                            // this assumes that `totalHits.value` is never less than the reported value.
                            || currentIndex < lastDocs.totalHits.value;
                }

                @Override
                public Result<Document> next() {
                    lastDocs = pageFactory.nextPage(lastDoc(lastDocs));
                    currentIndex += lastDocs.scoreDocs.length;
                    return new DefaultResult(lastDocs, lease) {
                        @Override
                        public void close() {
                            // no op
                        }
                    };
                }

                private ScoreDoc lastDoc(TopDocs docs) {
                    return docs != null
                            ? docs.scoreDocs[docs.scoreDocs.length - 1]
                            : null;
                }
            };
        }
    }

    interface PageFactory {
        TopDocs nextPage(ScoreDoc last);
    }
}
