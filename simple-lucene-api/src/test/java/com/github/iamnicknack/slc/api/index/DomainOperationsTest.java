package com.github.iamnicknack.slc.api.index;

import org.apache.lucene.document.Document;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DomainOperationsTest {

    private final DomainOperations<Object> domainOperations = new DomainOperations<>() {
        @Override
        public Document createDocument(Object value) {
            throw new UnsupportedOperationException("createDocument");
        }

        @Override
        public Object readDocument(Document document) {
            throw new UnsupportedOperationException("readDocument");
        }
    };

    @Test
    void defaultIdProvidesHashCode() {
        var value = new Object();
        assertEquals(value.hashCode(), Integer.parseInt(domainOperations.id(value)));
    }

    @Test
    void valueCannotBeNull() {
        NullPointerException npe = assertThrows(NullPointerException.class, () -> domainOperations.id(null));
        assertEquals("value cannot be null", npe.getMessage());
    }

}