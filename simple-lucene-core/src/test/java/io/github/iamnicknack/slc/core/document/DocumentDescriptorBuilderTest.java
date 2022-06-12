package io.github.iamnicknack.slc.core.document;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import io.github.iamnicknack.slc.api.backend.LuceneBackend;
import io.github.iamnicknack.slc.core.backend.LuceneBackends;

import java.io.IOException;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentDescriptorBuilderTest {

    private LuceneBackend backend;
    private DocumentDescriptorBuilder builder;

    @BeforeEach
    void beforeEach() throws IOException {
        backend = LuceneBackends.memory();
        builder = new DocumentDescriptorBuilder(backend);
    }

    @Test
    void registersFacet() {
        builder.field(new FieldDescriptorBuilder()
                .name("should-facet")
                .stringField()
                .facet()
                .build()
        );

        var document = builder.build();
        assertTrue(document.fieldMap().containsKey("should-facet"));
        assertTrue(backend.facetsConfig().isDimConfigured("should-facet"));
    }

    @Test
    void doesNotRegisterFacet() {
        builder.field(new FieldDescriptorBuilder()
                .name("should-not-facet")
                .stringField()
                .build()
        );

        var document = builder.build();
        assertTrue(document.fieldMap().containsKey("should-not-facet"));
        assertFalse(backend.facetsConfig().isDimConfigured("should-not-facet"));
    }

    @Test
    void registersFields() {
        builder.field(new FieldDescriptorBuilder()
                .name("field-1")
                .stringField()
                .build()
        );
        builder.field(new FieldDescriptorBuilder()
                .name("field-2")
                .stringField()
                .build()
        );

        var document = builder.build();
        assertTrue(Stream.of("field-1", "field-2").allMatch(document.fieldMap()::containsKey));
    }
}