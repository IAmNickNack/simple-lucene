package io.github.iamnicknack.slc.core.query;

import io.github.iamnicknack.slc.api.backend.LuceneBackend;
import io.github.iamnicknack.slc.api.index.DomainOperations;
import io.github.iamnicknack.slc.api.query.Hit;
import io.github.iamnicknack.slc.api.query.QueryExecutor;
import io.github.iamnicknack.slc.api.query.Result;
import io.github.iamnicknack.slc.core.backend.LuceneBackends;
import io.github.iamnicknack.slc.core.index.BucketUpdateOperations;
import io.github.iamnicknack.slc.core.test.BuilderDomainOperations;
import io.github.iamnicknack.slc.core.test.TestData;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DefaultQueryExecutorTest {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final LuceneBackend backend;
    private final BucketUpdateOperations<Map<String, Object>> updateOperations;
    private final DomainOperations<Map<String, Object>> domainOperations;

    private final QueryExecutor<Map<String, Object>, Document> queryExecutor;


    DefaultQueryExecutorTest() throws IOException {
        this.backend = LuceneBackends.memory();
        this.domainOperations = BuilderDomainOperations.create(backend);
        this.updateOperations = new BucketUpdateOperations<>(domainOperations);
        this.queryExecutor = new DefaultQueryExecutor<>(
                TestData.valueKeywordQueryFactory(),
                backend.searcherLeaseFactory()
        );
    }

    @BeforeEach
    void beforeEach() {
        try(var lease = backend.updateLeaseFactory().lease()) {
            lease.execute(leasedValue -> leasedValue.indexWriter().deleteAll());
            lease.execute(updateOperations.addAll(List.of(
                    TestData.createValue("TEST"),
                    TestData.createValue("IGNORED")
            )));
        }

        // assert that the data has been indexed
        try(var lease = backend.searcherLeaseFactory().lease()) {
            var value = lease.execute(leasedValue -> leasedValue.indexSearcher().count(new MatchAllDocsQuery()));
            assertEquals(2, value);
        }

        // assert that the document is searchable using the default document query executor
        try(var result = queryExecutor.execute(TestData.createValue("TEST"))) {
            assertEquals(1, result.totalHits());
            var hit = result.iterator().next();
            assertEquals("TEST", hit.value().get("value"));

            logger.info("Document hit: {}", hit);
        }
    }


    @Test
    void wrappedResult() {
        var iteratorFactory = Result.IteratorFactory.mapping(domainOperations::readDocument);
        var executor = queryExecutor.withIterator(iteratorFactory);

        try(var result = executor.execute(TestData.createValue("TEST"))) {
            assertEquals(1, result.totalHits());
            var hit = result.iterator().next();
            assertTrue(TestData.argumentMatcher(Map.of("value", "TEST")).matches(hit.value()));
            logger.info("Bean hit: {}", hit);
        }
    }

    @Test
    @SuppressWarnings("resource")
    void streamReleasesSearcher() {
        var executionResult = queryExecutor.execute(TestData.createValue("TEST"));
        var invocationCheck = mock(Runnable.class);
        var result = new Result<Document>() {
            @Override
            public void close() {
                invocationCheck.run();
                Result.super.close();
            }

            @Override
            public Iterator<Hit<Document>> iterator() {
                return executionResult.iterator();
            }
        };

        var list = result.list();

        assertEquals(1, list.size());
        verify(invocationCheck).run();
    }

    @Test
    @SuppressWarnings("resource")
    void createsCollectionWithoutTry()  {
        var collection = queryExecutor.execute(TestData.createValue("TEST")).list();
        assertEquals(1, collection.size());
    }

}