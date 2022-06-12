package com.github.iamnicknack.slc.core.document;

import com.github.iamnicknack.slc.api.document.SubFieldDescriptor;
import org.apache.lucene.document.*;
import org.apache.lucene.facet.FacetField;
import org.apache.lucene.index.IndexableField;

import java.util.function.Function;

public class SubFieldDescriptors {

    static SubFieldDescriptor<String> storedString(String field) {
        return new SubFieldRecord<>(field, StoredField.class, s -> new StoredField(field, s));
    }

    static SubFieldDescriptor<Integer> storedInt(String field) {
        return new SubFieldRecord<>(field, StoredField.class, i -> new StoredField(field, i));
    }

    static SubFieldDescriptor<Long> storedLong(String field) {
        return new SubFieldRecord<>(field, StoredField.class, l -> new StoredField(field, l));
    }

    static SubFieldDescriptor<String> text(String field) {
        String name = "%s.%s".formatted(field, "text");
        return new SubFieldRecord<>(name, TextField.class, s -> new TextField(name, s, Field.Store.NO));
    }

    static SubFieldDescriptor<String> keyword(String field) {
        String name = "%s.%s".formatted(field, "keyword");
        return new SubFieldRecord<>(name, StringField.class, s -> new StringField(name, s, Field.Store.NO));
    }

    static SubFieldDescriptor<String> stringFacet(String field) {
        String name = "%s.%s".formatted(field, "value");
        return new SubFieldRecord<>(name, FacetField.class, s -> new FacetField(name, s));
    }

    static SubFieldDescriptor<String> alias(String ignored, String alias) {
        return new SubFieldRecord<>(alias, TextField.class, s -> new TextField(alias, s, Field.Store.NO));
    }

    static <T extends Number> SubFieldDescriptor<T> numericFacet(String field) {
        String name = "%s.%s".formatted(field, "value");
        return new SubFieldRecord<>(name, NumericDocValuesField.class, n -> new NumericDocValuesField(name, n.longValue()));
    }

    static SubFieldDescriptor<Integer> integerPoint(String field) {
        String name = "%s.%s".formatted(field, "point");
        return new SubFieldRecord<>(name, IntPoint.class, i -> new IntPoint(name, i));
    }

    static SubFieldDescriptor<Long> longPoint(String field) {
        String name = "%s.%s".formatted(field, "point");
        return new SubFieldRecord<>(name, LongPoint.class, l -> new LongPoint(name, l));
    }

    private record SubFieldRecord<T>(String name,
                                     Class<? extends IndexableField> fieldType,
                                     Function<T, ? extends IndexableField> function) implements SubFieldDescriptor<T> {
        @Override
        public IndexableField field(T value) {
            return function().apply(value);
        }
    }
}
