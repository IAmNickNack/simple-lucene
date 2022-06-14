package io.github.iamnicknack.slc.api.document;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertFalse;

class FieldDescriptorTest {

    @Test
    void defaultIdIsFalse() {
        var field = Mockito.mock(FieldDescriptor.class);
        Mockito.when(field.id()).thenCallRealMethod();
        assertFalse(field.id());
    }

    @Test
    void defaultSubfieldsAreEmpty() {
        var field = Mockito.mock(FieldDescriptor.class);
        Mockito.when(field.subfields()).thenCallRealMethod();
        assertFalse(field.subfields().iterator().hasNext());
    }

    @Test
    void defaultMultiValueIsFalse() {
        var field = Mockito.mock(FieldDescriptor.class);
        Mockito.when(field.multiValue()).thenCallRealMethod();
        assertFalse(field.multiValue());
    }

    @Test
    void defaultFacetableIsFalse() {
        var field = Mockito.mock(FieldDescriptor.class);
        Mockito.when(field.facetable()).thenCallRealMethod();
        assertFalse(field.facetable());
    }
}