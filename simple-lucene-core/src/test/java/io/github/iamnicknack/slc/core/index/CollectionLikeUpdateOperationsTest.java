package io.github.iamnicknack.slc.core.index;

import io.github.iamnicknack.slc.api.backend.LuceneBackend;
import io.github.iamnicknack.slc.api.index.DomainOperations;
import io.github.iamnicknack.slc.core.backend.LuceneBackends;
import io.github.iamnicknack.slc.core.query.DefaultQueryExecutor;
import io.github.iamnicknack.slc.core.test.BuilderDomainOperations;
import io.github.iamnicknack.slc.core.test.TestData;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CollectionLikeUpdateOperationsTest {

    private final LuceneBackend backend;
    private final CollectionLikeUpdateOperations<Map<String, Object>> operations;
    private final DomainOperations<Map<String, Object>> domainOperations;

    CollectionLikeUpdateOperationsTest() throws IOException {
        this.backend = LuceneBackends.memory();
        this.domainOperations = BuilderDomainOperations.create(backend);
        this.operations = new CollectionLikeUpdateOperations<>(domainOperations, backend);
    }

    @BeforeEach
    void beforeEach() {
        try(var lease = backend.updateLeaseFactory().lease()) {
            lease.execute(leasedValue -> leasedValue.indexWriter().deleteAll());
        }
        try(var lease = backend.searcherLeaseFactory().lease()) {
            var count = lease.execute(components -> components.indexSearcher().count(new MatchAllDocsQuery()));
            Assertions.assertEquals(0, count);
        }
    }

    @Test
    void addsDocument() {
        var data = TestData.createValue("TEST");
        try(var lease = backend.updateLeaseFactory().lease()) {
            lease.execute(operations.add(data));
        }
        assertEquals(1, countDocuments());
    }

    @Test
    void addsDuplicateDocuments() {
        var data = TestData.createValue("TEST");
        try(var lease = backend.updateLeaseFactory().lease()) {
            lease.execute(operations.add(data));
            lease.execute(operations.add(data));
        }
        assertEquals(2, countDocuments());
    }

    @Test
    void deletesDocument() {
        var data = TestData.createValue("TEST");
        try(var lease = backend.updateLeaseFactory().lease()) {
            lease.execute(operations.add(data));
        }
        try(var lease = backend.updateLeaseFactory().lease()) {
            lease.execute(operations.delete(data));
        }
        assertEquals(0, countDocuments());
    }

    @Test
    void deletesSingleReference() {
        var data = TestData.createValue("TEST");
        try(var lease = backend.updateLeaseFactory().lease()) {
            lease.execute(operations.add(data));
            lease.execute(operations.add(data));
        }
        try(var lease = backend.updateLeaseFactory().lease()) {
            lease.execute(operations.delete(data));
        }
        assertEquals(1, countDocuments());
    }

    @Test
    void deletesSingleValue() {
        var data = List.of(
                TestData.createValue("TEST"),
                TestData.createValue("TEST")
        );
        try(var lease = backend.updateLeaseFactory().lease()) {
            lease.execute(operations.addAll(data));
        }
        try(var lease = backend.updateLeaseFactory().lease()) {
            lease.execute(operations.delete(TestData.createValue("TEST")));
        }
        assertEquals(1, countDocuments());
    }

    @Test
    void doesNotDeleteWhenNoDocumentFound() {
        try(var lease = backend.updateLeaseFactory().lease()) {
            lease.execute(operations.add(TestData.createValue("TEST")));
        }
        assertEquals(1, countDocuments());

        try(var lease = backend.updateLeaseFactory().lease()) {
            lease.execute(operations.delete(TestData.createValue("DELETE ME")));
        }
        assertEquals(1, countDocuments());
    }

    @Test
    void deletesAll() {
        var data = TestData.createValue("TEST");
        try(var lease = backend.updateLeaseFactory().lease()) {
            lease.execute(operations.add(data));
            lease.execute(operations.add(data));
        }
        try(var lease = backend.updateLeaseFactory().lease()) {
            lease.execute(operations.deleteAll(List.of(data)));
        }
        assertEquals(0, countDocuments());
    }

    @Test
    void updatesDocument() {
        var data = TestData.createValue("TEST", 0, "the description", Collections.emptyList());
        try(var lease = backend.updateLeaseFactory().lease()) {
            lease.execute(operations.add(data));
        }
        assertEquals(1, countDocuments());

        var updateData = TestData.createValue("TEST", 1);
        try(var lease = backend.updateLeaseFactory().lease()) {
            lease.execute(operations.update(updateData));
        }
        assertEquals(1, countDocuments());

        DefaultQueryExecutor<Map<String, Object>> executor = new DefaultQueryExecutor<>(
                TestData.valueKeywordQueryFactory(),
                backend.searcherLeaseFactory()
        );

        try(var result = executor.execute(data)) {
            Assertions.assertEquals(1, result.totalHits());
            var hit = result.iterator().next();
            Assertions.assertEquals(1, hit.value().getField("sequence").numericValue().intValue());

            assertTrue(TestData.argumentMatcher(domainOperations.readDocument(hit.value())).matches(updateData));
        }
    }

    @Test
    void addsDocumentWhenNoExistingDocumentFound() {
        var data = TestData.createValue("TEST", 1);
        try(var lease = backend.updateLeaseFactory().lease()) {
            lease.execute(operations.update(data));
        }
        assertEquals(1, countDocuments());

        DefaultQueryExecutor<Map<String, Object>> executor = new DefaultQueryExecutor<>(
                TestData.valueKeywordQueryFactory(),
                backend.searcherLeaseFactory()
        );

        try(var result = executor.execute(data)) {
            Assertions.assertEquals(1, result.totalHits());
            var hit = result.iterator().next();
            Assertions.assertEquals(1, hit.value().getField("sequence").numericValue().intValue());
        }
    }

    private int countDocuments() {
        try(var lease = backend.searcherLeaseFactory().lease()) {
            return lease.execute(components -> components.indexSearcher().count(new MatchAllDocsQuery()));
        }
    }
}