package com.github.iamnicknack.slc.core.document;

import com.github.iamnicknack.slc.api.document.DocumentDescriptor;
import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.facet.taxonomy.FastTaxonomyFacetCounts;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.github.iamnicknack.slc.api.backend.LuceneBackend;
import com.github.iamnicknack.slc.core.backend.LuceneBackends;
import com.github.iamnicknack.slc.core.index.MapDomainOperations;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DocumentDescriptorOperationsTest {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Test
    void canReadAndWrite() throws IOException {
        LuceneBackend backend = LuceneBackends.memory();
        DocumentDescriptor documentDescriptor = new DocumentDescriptorBuilder(backend)
                .field(new FieldDescriptorBuilder()
                        .name("stringField")
                        .stringField()
                        .keyword()
                        .build()
                )
                .field(new FieldDescriptorBuilder()
                        .name("intField")
                        .intField()
                        .build()
                )
                .build();

        var operations = new MapDomainOperations(documentDescriptor);

        var map = Map.<String, Object>of(
                "stringField", "string value",
                "intField", 99
        );
        var document = operations.createDocument(map);

        logger.info("Created: {}", document);

        assertNotNull(document.getField("stringField"));
        assertNotNull(document.getField("intField"));

        var readDocument = operations.readDocument(document);
        assertEquals(map.get("stringField"), readDocument.get("stringField"));
        assertEquals(map.get("intField"), readDocument.get("intField"));
    }


    @Test
    @SuppressWarnings("resource")
    void canFacet() throws IOException {
        var backend = LuceneBackends.memory();
        var indexDescriptor = new DocumentDescriptorBuilder(backend)
                .field(new FieldDescriptorBuilder()
                        .name("string-field")
                        .stringField()
                        .keyword()
                        .facet()
                        .build()
                )
                .build();

        var operations = new MapDomainOperations(indexDescriptor);

        var map = Map.<String, Object>of(
                "string-field", "string value"
        );
        var document = operations.createDocument(map);

        try(var lease = backend.updateLeaseFactory().lease()) {
            lease.execute(components -> components.indexWriter().addDocument(components.build(components.build(document))));
        }

        try(var lease = backend.searcherLeaseFactory().lease()) {
            var topdocs = lease.execute(components -> components.indexSearcher().search(new MatchAllDocsQuery(), 1));
            assertEquals(1, topdocs.totalHits.value);
        }

        try(var lease = backend.searcherLeaseFactory().lease()) {
            lease.execute(components -> {
                var collector = new FacetsCollector();
                var topdocs = FacetsCollector.search(components.indexSearcher(), new MatchAllDocsQuery(), 10, collector);
                assertEquals(1, topdocs.totalHits.value);

                var facets = new FastTaxonomyFacetCounts(components.taxonomyReader(), components.facetsConfig(), collector);
                assertNotNull(facets);

                var children = facets.getTopChildren(1, "string-field.value");
                assertEquals(1, children.childCount);

                Arrays.stream(children.labelValues).forEach(labelAndValue -> logger.info("Facet: {}", labelAndValue));

                return null;
            });
        }
    }
}