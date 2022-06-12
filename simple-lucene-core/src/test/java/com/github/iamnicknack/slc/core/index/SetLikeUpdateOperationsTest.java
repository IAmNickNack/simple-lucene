package com.github.iamnicknack.slc.core.index;

import com.github.iamnicknack.slc.api.backend.LuceneBackend;
import com.github.iamnicknack.slc.api.index.DomainOperations;
import com.github.iamnicknack.slc.api.lease.Lease;
import com.github.iamnicknack.slc.api.query.Result;
import com.github.iamnicknack.slc.core.backend.LuceneBackends;
import com.github.iamnicknack.slc.core.query.DefaultQueryExecutor;
import com.github.iamnicknack.slc.core.test.TestData;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.TermQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SetLikeUpdateOperationsTest {

    private final LuceneBackend backend;
    private final DomainOperations<Map<String, Object>> domainOperations;
    private final SetLikeUpdateOperations<Map<String, Object>> operations;

    public SetLikeUpdateOperationsTest() throws IOException {
        this.backend = LuceneBackends.memory();
        this.domainOperations = new MapDomainOperations(TestData.documentDescriptor(backend));
        this.operations = new SetLikeUpdateOperations<>(domainOperations, backend);
    }

    @BeforeEach
    @SuppressWarnings("resource")
    void beforeEach() {
        try(var lease = backend.updateLeaseFactory().lease()) {
            lease.execute(leasedValue -> leasedValue.indexWriter().deleteAll());
        }
        assertEquals(0, countDocuments());
    }

    @Test
    void addsDocument() {
        var testData = TestData.createValue("TEST");
        try(var lease = backend.updateLeaseFactory().lease()) {
            lease.execute(operations.add(testData));
        }
        assertEquals(1, countDocuments());
    }

    @Test
    void cannotAddDuplicates() {
        var testData = TestData.createValue("TEST");
        try(var lease = backend.updateLeaseFactory().lease()) {
            lease.execute(operations.add(testData));
        }
        assertEquals(1, countDocuments());

        try(var lease = backend.updateLeaseFactory().lease()) {
            var exception = assertThrows(Lease.LeaseException.class, () -> lease.execute(operations.add(testData)));
            assertEquals(IllegalArgumentException.class, exception.getCause().getClass());
        }
        assertEquals(1, countDocuments());
    }

    @Test
    void updatesDocument() {
        var testData = new HashMap<>(TestData.createValue("TEST"));
        testData.put("others", List.of("first", "second"));
        try(var lease = backend.updateLeaseFactory().lease()) {
            lease.execute(operations.add(testData));
        }
        assertEquals(1, countDocuments());

        var updateData = new HashMap<>(TestData.createValue("TEST"));
        updateData.put("others", List.of("first", "second"));
        try(var lease = backend.updateLeaseFactory().lease()) {
            lease.execute(operations.update(updateData));
        }
        assertEquals(1, countDocuments());

        var executor = new DefaultQueryExecutor<>(TestData.valueKeywordQueryFactory(), backend.searcherLeaseFactory())
                .withIterator(Result.IteratorFactory.mapping(domainOperations::readDocument));
//                .withIterator(BeanIterator.factory(domainOperations::readDocument));
        try(var result = executor.execute(updateData)) {
            assertEquals(1, result.totalHits());
            var value = result.iterator().next().value();
            assertEquals(updateData, value);
        }
    }

    @Test
    void deletesDocument() {
        var testData = TestData.createValue("TEST");
        try(var lease = backend.updateLeaseFactory().lease()) {
            lease.execute(operations.add(testData));
        }
        assertEquals(1, countDocuments());

        try(var lease = backend.updateLeaseFactory().lease()) {
            lease.execute(operations.delete(testData));
        }
        assertEquals(0, countDocuments());
    }

    @Test
    void deletesAllMatchingDocuments() {
        var data = List.of(
                TestData.createValue("TEST 1"),
                TestData.createValue("TEST 2"),
                TestData.createValue("TEST 3")
        );

        try(var lease = backend.updateLeaseFactory().lease()) {
            lease.execute(operations.addAll(data));
        }
        assertEquals(3, countDocuments());

        var dataToDelete = data.stream()
                .limit(2)
                .toList();

        try(var lease = backend.updateLeaseFactory().lease()) {
            lease.execute(operations.deleteAll(dataToDelete));
        }
        assertEquals(1, countDocuments());

        try(var lease = backend.searcherLeaseFactory().lease()) {
            var result = lease.execute(components -> components.indexSearcher().search(new TermQuery(new Term("value.keyword", "TEST 3")), 1));
            assertEquals(1, result.totalHits.value);
        }
    }

    int countDocuments() {
        try(var lease = backend.searcherLeaseFactory().lease()) {
            return lease.execute(components -> components.indexSearcher().count(new MatchAllDocsQuery()));
        }
    }
}