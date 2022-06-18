package io.github.iamnicknack.slc.core.index;

import io.github.iamnicknack.slc.api.backend.LuceneBackend;
import io.github.iamnicknack.slc.api.index.DomainOperations;
import io.github.iamnicknack.slc.core.backend.LuceneBackends;
import io.github.iamnicknack.slc.core.collection.LuceneCollection;
import io.github.iamnicknack.slc.core.test.BuilderDomainOperations;
import io.github.iamnicknack.slc.core.test.LuceneDomainOperations;
import io.github.iamnicknack.slc.core.test.TestData;
import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.facet.taxonomy.FastTaxonomyFacetCounts;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to assert that the behaviour provided by {@link MapDomainOperations} and {@link LuceneDomainOperations}
 * is equivalent
 */
class MapDomainOperationsComparisonTest {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final LuceneBackend backend;
    private static final DomainOperations<Map<String, Object>> mapDomainOperations;
    private static final LuceneDomainOperations luceneDomainOperations;

    static {
        try {
            backend = LuceneBackends.memory();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        mapDomainOperations = BuilderDomainOperations.create(backend);
        luceneDomainOperations = new LuceneDomainOperations();
    }

    static Stream<DomainOperations<Map<String, Object>>> operationsStream() {
        return Stream.of(mapDomainOperations, luceneDomainOperations);
    }

    @BeforeEach
    void beforeEach() {
        try(var lease = backend.updateLeaseFactory().lease()) {
            lease.execute(components -> components.indexWriter().deleteAll());
            logger.debug("Deleted index contents");
        }
    }

    @ParameterizedTest
    @MethodSource("operationsStream")
    void generatesId(DomainOperations<Map<String, Object>> operations) {
        var data = TestData.createValue("TEST", 1);
        assertEquals("TEST", operations.id(data));
    }

    @ParameterizedTest
    @MethodSource("operationsStream")
    void ifFailsIfPropertyNotSet(DomainOperations<Map<String, Object>> operations) {
        var data = Map.<String, Object>of("sequence", 1);
        assertThrows(RuntimeException.class, () -> operations.id(data));
    }

    @ParameterizedTest
    @MethodSource("operationsStream")
    void createsDocumentWithAvailableFields(DomainOperations<Map<String, Object>> operations) {
        var data = TestData.createValue("TEST");
        var document = operations.createDocument(data);
        assertEquals(2, document.getFields().size());
        assertNotNull(document.getField("value"));
        assertNotNull(document.getField("value.keyword"));
        assertNull(document.getField("sequence"));
        assertNull(document.getField("sequence.point"));
    }

    @ParameterizedTest
    @MethodSource("operationsStream")
    void createsDocumentWithAllFields(DomainOperations<Map<String, Object>> operations) {
        var others = List.of("first", "second");
        var data = TestData.createValue("TEST", 1, "the description", others);
        var document = operations.createDocument(data);

        document.getFields().forEach(field -> logger.debug("{}: {}", operations.getClass().getSimpleName(), field));

        assertEquals(12, document.getFields().size());
        assertNotNull(document.getField("value"));
        assertNotNull(document.getField("value.keyword"));
        assertNotNull(document.getField("sequence"));
        assertNotNull(document.getField("sequence.point"));
        assertNotNull(document.getField("description"));
        assertTrue(Arrays.stream(document.getFields("others"))
                .map(IndexableField::stringValue)
                .toList()
                .containsAll(others)
        );
        assertTrue(Arrays.stream(document.getFields("others.keyword"))
                .map(IndexableField::stringValue)
                .toList()
                .containsAll(others)
        );
    }

    @ParameterizedTest
    @MethodSource("operationsStream")
    void readsDocument(DomainOperations<Map<String, Object>> operations) {
        var data = TestData.createValue("TEST", 1);
        var document = operations.createDocument(data);
        var read = operations.readDocument(document);

        assertTrue(TestData.argumentMatcher(data).matches(read));
    }

    @ParameterizedTest
    @MethodSource("operationsStream")
    void canFacet(DomainOperations<Map<String, Object>> operations) {
        var data = List.of(
                TestData.createValue("first", 1, "first document", List.of("group-a")),
                TestData.createValue("second", 2, "second document", List.of("group-b")),
                TestData.createValue("third", 3, "third document", List.of("group-c", "group-a"))
        );

        var collection = new LuceneCollection<>(operations, backend);
        collection.addAll(data);
        assertEquals(3, collection.size());

        try(var lease = backend.searcherLeaseFactory().lease()) {
            var labelValues = lease.execute(components -> {

                var collector = new FacetsCollector();
                FacetsCollector.search(components.indexSearcher(), new MatchAllDocsQuery(), 1, collector);
                var counts = new FastTaxonomyFacetCounts(components.taxonomyReader(), components.facetsConfig(), collector);
                var facets = counts.getTopChildren(10, "others.value");

                return Arrays.stream(facets.labelValues)
                        .collect(Collectors.toMap(o -> o.label, o -> o.value));
            });

            assertEquals(3, labelValues.size());
            labelValues.forEach((s, number) -> logger.debug("{}: {}", s, number));
            assertEquals(2, labelValues.get("group-a"));
            assertEquals(1, labelValues.get("group-b"));
            assertEquals(1, labelValues.get("group-c"));
        }
    }
}