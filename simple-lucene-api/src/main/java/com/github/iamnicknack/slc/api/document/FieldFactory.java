package com.github.iamnicknack.slc.api.document;

import org.apache.lucene.index.IndexableField;

/**
 * Create an {@link IndexableField} from a value
 * @param <T> the domain property type
 */
public interface FieldFactory<T> {

    /**
     * Create an {@link IndexableField} derived from the provided value
     * @param value the domain value
     * @return an {@link IndexableField}
     */
    IndexableField field(T value);
}
