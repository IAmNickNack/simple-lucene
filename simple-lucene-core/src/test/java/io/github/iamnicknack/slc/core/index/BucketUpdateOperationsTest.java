package io.github.iamnicknack.slc.core.index;

import io.github.iamnicknack.slc.api.query.QueryExecutor;
import io.github.iamnicknack.slc.core.test.TestData;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.TermQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import io.github.iamnicknack.slc.api.backend.LuceneBackend;
import io.github.iamnicknack.slc.core.backend.LuceneBackends;
import io.github.iamnicknack.slc.core.query.DefaultQueryExecutor;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BucketUpdateOperationsTest {
    private final LuceneBackend backend;
    private final BucketUpdateOperations<Map<String, Object>> operations;

    private final QueryExecutor<Map<String, Object>, Document> queryExecutor;

    public BucketUpdateOperationsTest() throws IOException {
        this.backend = LuceneBackends.memory();
        var domainOperations = new MapDomainOperations(TestData.documentDescriptor(backend));
        this.operations = new BucketUpdateOperations<>(domainOperations);

        this.queryExecutor = new DefaultQueryExecutor<>(
                value -> new BooleanQuery.Builder()
                        .add(new TermQuery(new Term("value.keyword", (String)value.get("value"))), BooleanClause.Occur.MUST)
                        .add(IntPoint.newExactQuery("sequence.point", (int)value.get("sequence")), BooleanClause.Occur.MUST)
                        .build(),
                backend.searcherLeaseFactory()
        );
    }

    @BeforeEach
    @SuppressWarnings("resource")
    void beforeEach() {
        try(var lease = backend.updateLeaseFactory().lease()) {
            lease.execute(leasedValue -> leasedValue.indexWriter().deleteAll());
        }
        try(var lease = backend.searcherLeaseFactory().lease()) {
            var count = lease.execute(components -> components.indexSearcher().count(new MatchAllDocsQuery()));
            assertEquals(0, count);
        }
    }

    @Test
    void addsDocument() {
        try(var lease = backend.updateLeaseFactory().lease()) {
            lease.execute(operations.add(TestData.createValue("TEST", 1)));
        }

        try(var result = queryExecutor.execute(TestData.createValue("TEST", 1))) {
            assertEquals(1, result.totalHits());
        }
        try(var result = queryExecutor.execute(TestData.createValue("TEST", 0))) {
            assertEquals(0, result.totalHits());
        }
        try(var result = queryExecutor.execute(TestData.createValue("TEST", 2))) {
            assertEquals(0, result.totalHits());
        }
    }
}