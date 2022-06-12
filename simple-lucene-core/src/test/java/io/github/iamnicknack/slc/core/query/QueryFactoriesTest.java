package io.github.iamnicknack.slc.core.query;

import io.github.iamnicknack.slc.core.test.TestData;
import org.junit.jupiter.api.Test;
import io.github.iamnicknack.slc.api.backend.LuceneBackend;
import io.github.iamnicknack.slc.core.backend.LuceneBackends;
import io.github.iamnicknack.slc.core.index.BucketUpdateOperations;
import io.github.iamnicknack.slc.core.index.MapDomainOperations;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QueryFactoriesTest {

    private final LuceneBackend backend;

    QueryFactoriesTest() throws IOException {
        this.backend = LuceneBackends.memory();
        var bucketOperations = new BucketUpdateOperations<>(
                new MapDomainOperations(TestData.documentDescriptor(backend))
        );
        try(var lease = backend.updateLeaseFactory().lease()) {
            lease.execute(bucketOperations.addAll(List.of(
                    TestData.createValue("TEST", 1, "TEST"),
                    TestData.createValue("TEST2", 2, "TEST 2"),
                    TestData.createValue("BLAH", 3, "BLAH")
            )));
        }
    }

    @Test
    void createsKeyword() {
        var factory = QueryFactories.keyword("value.keyword");
        var executor = new DefaultQueryExecutor<>(
                factory,
                backend.searcherLeaseFactory()
        );
        try(var result = executor.execute("TEST")) {
            assertEquals(1, result.totalHits());
        }
    }

    @Test
    void createsText() {
        var factory = QueryFactories.text("description.text");
        var executor = new DefaultQueryExecutor<>(
                factory,
                backend.searcherLeaseFactory()
        );
        try(var result = executor.execute("test")) {
            assertEquals(2, result.totalHits());
        }
        try(var result = executor.execute("blah")) {
            assertEquals(1, result.totalHits());
        }
    }

}