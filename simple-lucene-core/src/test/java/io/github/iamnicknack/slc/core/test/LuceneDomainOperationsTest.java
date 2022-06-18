package io.github.iamnicknack.slc.core.test;

import io.github.iamnicknack.slc.api.backend.LuceneBackend;
import io.github.iamnicknack.slc.api.index.DomainOperations;
import io.github.iamnicknack.slc.core.backend.LuceneBackends;
import io.github.iamnicknack.slc.core.collection.LuceneCollection;
import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.facet.taxonomy.FastTaxonomyFacetCounts;
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

import static org.junit.jupiter.api.Assertions.assertEquals;

class LuceneDomainOperationsTest {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final DomainOperations<Map<String, Object>> operations = new LuceneDomainOperations();

    private LuceneBackend backend;

    @BeforeEach
    void beforeEach() throws IOException {
        backend = LuceneBackends.memory();

        backend.facetsConfig().setMultiValued("others.value", true);
        backend.facetsConfig().setIndexFieldName("others", "others.value");
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