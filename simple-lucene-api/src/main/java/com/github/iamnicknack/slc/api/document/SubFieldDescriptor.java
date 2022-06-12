package com.github.iamnicknack.slc.api.document;

import org.apache.lucene.index.IndexableField;

/**
 * Represents a variation of the primary, named field.
 *
 * @param <T> the domain property type
 * @see FieldDescriptor
 */
public interface SubFieldDescriptor<T> extends FieldFactory<T> {

    /**
     * Value to append to the root field name
     */
    String name();

    Class<? extends IndexableField> fieldType();
}
