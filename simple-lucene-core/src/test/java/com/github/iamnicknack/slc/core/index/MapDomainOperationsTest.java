package com.github.iamnicknack.slc.core.index;

import org.apache.lucene.index.IndexableField;
import org.junit.jupiter.api.Test;
import com.github.iamnicknack.slc.api.backend.LuceneBackend;
import com.github.iamnicknack.slc.core.backend.LuceneBackends;
import com.github.iamnicknack.slc.core.test.TestData;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MapDomainOperationsTest {

    private final MapDomainOperations operations;

    public MapDomainOperationsTest() throws IOException {
        LuceneBackend backend = LuceneBackends.memory();
        this.operations = new MapDomainOperations(TestData.documentDescriptor(backend));
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
        assertEquals(10, document.getFields().size());
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
}