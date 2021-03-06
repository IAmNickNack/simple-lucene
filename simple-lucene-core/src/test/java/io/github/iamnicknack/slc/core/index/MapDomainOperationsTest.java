package io.github.iamnicknack.slc.core.index;

import io.github.iamnicknack.slc.api.backend.LuceneBackend;
import io.github.iamnicknack.slc.api.index.DomainOperations;
import io.github.iamnicknack.slc.core.backend.LuceneBackends;
import io.github.iamnicknack.slc.core.collection.LuceneCollection;
import io.github.iamnicknack.slc.core.test.BuilderDomainOperations;
import io.github.iamnicknack.slc.core.test.TestData;
import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.facet.taxonomy.FastTaxonomyFacetCounts;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class MapDomainOperationsTest {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final LuceneBackend backend;
    private final DomainOperations<Map<String, Object>> operations;

    public MapDomainOperationsTest() throws IOException {
        this.backend = LuceneBackends.memory();
        this.operations = BuilderDomainOperations.create(backend);
    }

    @BeforeEach
    void beforeEach() {
        try(var lease = backend.updateLeaseFactory().lease()) {
            lease.execute(components -> components.indexWriter().deleteAll());
        }
    }

    @Test
    void generatesId() {
        var data = TestData.createValue("TEST", 1);
        assertEquals("TEST", operations.id(data));
    }

    @Test
    void ifFailsIfPropertyNotSet() {
        var data = Map.<String, Object>of("sequence", 1);
        assertThrows(RuntimeException.class, () -> operations.id(data));
    }

    @Test
    void createsDocumentWithAvailableFields() {
        var data = TestData.createValue("TEST");
        var document = operations.createDocument(data);
        assertEquals(2, document.getFields().size());
        assertNotNull(document.getField("value"));
        assertNotNull(document.getField("value.keyword"));
        assertNull(document.getField("sequence"));
        assertNull(document.getField("sequence.point"));
    }

    @Test
    void createsDocumentWithAllFields() {
        var others = List.of("first", "second");
        var data = TestData.createValue("TEST", 1, "the description", others);
        var document = operations.createDocument(data);
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

    @Test
    void readsDocument() {
        var data = TestData.createValue("TEST", 1);
        var document = operations.createDocument(data);
        var read = operations.readDocument(document);

        assertTrue(TestData.argumentMatcher(data).matches(read));
    }

    @Test
    void canFacet() {
        var data = List.of(
                TestData.createValue("first", 1, "first document", List.of("group-a")),
                TestData.createValue("second", 2, "second document", List.of("group-b")),
                TestData.createValue("third", 3, "second document", List.of("group-c", "group-a"))
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
            labelValues.forEach((s, number) -> logger.info("{}: {}", s, number));
            assertEquals(2, labelValues.get("group-a"));
            assertEquals(1, labelValues.get("group-b"));
            assertEquals(1, labelValues.get("group-c"));
        }
    }
}