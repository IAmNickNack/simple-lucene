package io.github.iamnicknack.slc.api.document;

import org.apache.lucene.index.IndexableField;

/**
 * Reads a domain-specific property from an {@link IndexableField}
 * @param <T> the domain property type
 */
public interface FieldParser<T> {

    T parse(IndexableField field);

}
