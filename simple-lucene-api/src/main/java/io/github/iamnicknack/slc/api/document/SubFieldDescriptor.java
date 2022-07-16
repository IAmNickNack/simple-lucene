package io.github.iamnicknack.slc.api.document;

import org.apache.lucene.index.IndexableField;

import java.util.function.Function;

/**
 * Represents a variation of the primary, named field.
 *
 * @param <T> the domain property type
 * @see FieldDescriptor
 */
public interface SubFieldDescriptor<T> extends FieldFactory<T> {

    /**
     * Value to append to the root field name.
     * Used during facet configuration
     */
    String name();

    /**
     * The type used to represent this value in the index
     */
    Class<? extends IndexableField> fieldType();

    default <V> SubFieldDescriptor<V> compose(Function<V, T> composer) {
        return new SubFieldDescriptor<>() {
            @Override
            public String name() {
                return SubFieldDescriptor.this.name();
            }

            @Override
            public Class<? extends IndexableField> fieldType() {
                return SubFieldDescriptor.this.fieldType();
            }

            @Override
            public IndexableField field(V value) {
                return SubFieldDescriptor.this.field(composer.apply(value));
            }
        };
    }
}
