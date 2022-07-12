package io.github.iamnicknack.slc.api.document;

import org.apache.lucene.index.IndexableField;

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
}
