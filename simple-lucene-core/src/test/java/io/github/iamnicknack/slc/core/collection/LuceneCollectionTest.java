package io.github.iamnicknack.slc.core.collection;

import org.apache.lucene.search.MatchAllDocsQuery;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import io.github.iamnicknack.slc.api.backend.LuceneBackend;
import io.github.iamnicknack.slc.api.index.DomainOperations;
import io.github.iamnicknack.slc.api.query.Hit;
import io.github.iamnicknack.slc.core.backend.LuceneBackends;
import io.github.iamnicknack.slc.core.index.MapDomainOperations;
import io.github.iamnicknack.slc.core.test.TestData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntFunction;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class LuceneCollectionTest {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    static DomainOperations<Map<String, Object>> documentOperations;
    static LuceneBackend backend;

    @BeforeAll
    static void beforeAll() throws IOException {
        backend = LuceneBackends.memory();
        documentOperations = new MapDomainOperations(TestData.documentDescriptor(backend));
    }

    @AfterAll
    static void afterAll() throws Exception {
        backend.close();
    }

    private LuceneCollection<Map<String, Object>> collection;

    private final List<Map<String, Object>> testRecords = List.of(
            TestData.createValue("value 1"),
            TestData.createValue("value 2")
    );

    @BeforeEach
    void beforeEach() {
        collection = new LuceneCollection<>(documentOperations, backend);
        collection.clear();
    }

    @Test
    void add() {
        collection.add(TestData.createValue("value 1"));
        assertEquals(1, collection.size());

        collection.add(TestData.createValue("value 2"));
        assertEquals(2, collection.size());

        collection.forEach(stringObjectMap -> logger.info("{}", stringObjectMap));
    }

    @Test
    void addAll() {
        collection.addAll(testRecords);
        assertEquals(2, collection.size());
    }

    @Test
    void remove() {
        addAll();

        collection.remove(TestData.createValue("value 1"));
        assertEquals(1, collection.size());
    }

    @Test
    void removeAllClearsCollection() {
        addAll();

        List<Map<String, Object>> values = collection.stream().toList();
        assertEquals(2, collection.size());

        assertTrue(collection.removeAll(values));
        assertEquals(0, collection.size());
        assertTrue(collection.isEmpty());
    }

    @Test
    void removeAllRetainsUnmatched() {
        addAll();

        List<Map<String, Object>> values = collection.stream().toList();
        assertEquals(2, collection.size());

        collection.add(TestData.createValue("value 3"));
        assertTrue(collection.removeAll(values));
        assertEquals(1, collection.size());
        assertFalse(collection.isEmpty());
    }

    @Test
    void removeEmptyListDoesNotModify() {
        addAll();
        assertFalse(collection.removeAll(Collections.<Map<String, Object>>emptyList()));
    }

    @Test
    void contains() {
        addAll();

        assertTrue(collection.contains(TestData.createValue("value 1")));
        assertFalse(collection.contains(TestData.createValue("value 3")));
    }

    @Test
    void containsAll() {
        addAll();

        collection.add(TestData.createValue("value 3"));

        assertTrue(collection.containsAll(List.of(
                TestData.createValue("value 1"),
                TestData.createValue("value 2")
        )));
        assertTrue(collection.containsAll(List.of(
                TestData.createValue("value 1"),
                TestData.createValue("value 3")
        )));
        assertTrue(collection.containsAll(List.of(
                TestData.createValue("value 1"),
                TestData.createValue("value 2"),
                TestData.createValue("value 3")
        )));
    }

    @Test
    void doesNotContainAll() {
        addAll();

        assertFalse(collection.containsAll(List.of(
                TestData.createValue("value 1"),
                TestData.createValue("value 3")
        )));
        assertFalse(collection.containsAll(List.of(
                TestData.createValue("value 1"),
                TestData.createValue("value 2"),
                TestData.createValue("value 3")
        )));
    }


    @Test
    void retainAll() {
        addAll();

        collection.add(TestData.createValue("value 3"));
        assertEquals(3, collection.size());

        collection.retainAll(testRecords);
        assertEquals(2, collection.size());
    }

    @Test
    void clear() {
        addAll();
        collection.clear();

        try(var lease = backend.searcherLeaseFactory().lease()) {
            var count = lease.execute(leasedValue -> leasedValue.indexSearcher().count(new MatchAllDocsQuery()));
            assertEquals(0, count);
        }
    }

    @Test
    void iterates() {
        addAll();
        AtomicInteger count = new AtomicInteger();
        collection.iterator().forEachRemaining(ignored -> count.incrementAndGet());
        assertEquals(2, count.get());
    }

    @Test
    void toArrayNoArgsIsUnsupported() {
        assertThrows(UnsupportedOperationException.class, collection::toArray);
    }

    @Test
    void toArrayIntFunctionIsUnsupported() {
        IntFunction<Map<String, Object>[]> fn = Map[]::new;
        assertThrows(UnsupportedOperationException.class, () -> collection.toArray(fn));
    }

    @Test
    @SuppressWarnings("unchecked")
    void toArrayWithArrayIsUnsupported() {
        Map<String, Object>[] target = new Map[2];
        assertThrows(UnsupportedOperationException.class, () -> collection.toArray(target));
    }

    @Test
    void streamCompletesBeforeSearchIsClosed() {
        addAll();
        Stream<Hit<Map<String, Object>>> str = collection.queryStream(new MatchAllDocsQuery());
        str.forEach(System.out::println);
    }
}