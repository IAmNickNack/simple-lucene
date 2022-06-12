package com.github.iamnicknack.slc.sandbox.query;

import org.apache.lucene.search.MatchAllDocsQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.github.iamnicknack.slc.annotation.AnnotatedRecordOperations;
import com.github.iamnicknack.slc.annotation.IndexProperty;
import com.github.iamnicknack.slc.api.backend.LuceneBackend;
import com.github.iamnicknack.slc.api.index.DomainOperations;
import com.github.iamnicknack.slc.core.backend.LuceneBackends;
import com.github.iamnicknack.slc.core.collection.LuceneCollection;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class GroupedQueryExecutorTest {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private LuceneBackend backend;
    private DomainOperations<KeyValueRecord> documentOperations;
    private LuceneCollection<KeyValueRecord> luceneCollection;

    @BeforeEach
    void beforeEach() throws IOException {
        backend = LuceneBackends.memory();
        documentOperations = AnnotatedRecordOperations.create(KeyValueRecord.class, backend);
        luceneCollection = new LuceneCollection<>(documentOperations, backend);
    }

    @AfterEach
    void afterEach() throws IOException {
        this.backend.close();
    }

    @Test
    void groupByString() {
        luceneCollection.addAll(List.of(
                new KeyValueRecord("key 1", "value 1.1"),
                new KeyValueRecord("key 1", "value 1.2"),
                new KeyValueRecord("key 2", "value 2")
        ));

        /*
            This represents the set of valid results.
            When a record is returned as a facet, remove it from this list.
            Potential success indicator if this list is empty at the end of this test.
         */
        Set<KeyValueRecord> set = new HashSet<>(luceneCollection);
        assertEquals(3, set.size());

        var executor = new GroupedQueryExecutor<String, KeyValueRecord>(
                "key",
                ignored -> new MatchAllDocsQuery(),
                documentOperations::readDocument,
                backend.searcherLeaseFactory()
        );

        try(var result = executor.execute(null)) {
            // Results should contain only two hits `key 1` and `key 2`
            assertEquals(2, result.totalHits());
            result.forEach(group -> {
                // all values should contain a result
                assertTrue(group.value().result().iterator().hasNext());
                // assert that each child value is a member of the test group
                group.value().result().forEach(child -> {
                    logger.info("[{}] {}", child.value().key, child);
                    assertTrue(set.contains(child.value()));
                    // Once we have tested a value, remove it from the list of valid results
                    assertTrue(set.remove(child.value()));
                });
            });
        }

        // assert that valid results have all been reported
        assertTrue(set.isEmpty());
    }

    public record KeyValueRecord(@IndexProperty(value = "key", text = false, keyword = true, facet = true) String key,
                                 @IndexProperty("value") String value) {}


}