package io.github.iamnicknack.slc.api.document;

import org.apache.lucene.index.IndexableField;

import java.util.Collections;

/**
 * Metadata describing how a field maps to a Lucene index.
 *
 * <p>This will often include more than one {@link IndexableField}, represented by {@link SubFieldDescriptor}s.
 * For example:</p>
 * <ul>
 *     <li>Numeric fields, indexed as document values but also available to read from their stored value</li>
 *     <li>Text fields, indexed as both {@link org.apache.lucene.document.TextField} and
 *     {@link org.apache.lucene.document.StringField} where it is probably simpler to use a
 *     {@link org.apache.lucene.document.StoredField} to remove ambiguity when reading data</li>
 * </ul>
 *
 * @param <T> the domain type
 */
public interface FieldDescriptor<T> extends
        FieldReader,
        Iterable<SubFieldDescriptor<T>>
{

    /**
     * Required index name for the field.
     */
    String name();

    /**
     * The field may be used for faceting
     * @return defaults to false
     */
    default boolean facetable() {
        return false;
    }

    /**
     * The field is multi-valued
     * @return defaults to false
     */
    default boolean multiValue() {
        return false;
    }

    /**
     * The field is a constituent of a unique identifier
     */
    default boolean id() {
        return false;
    }

    /**
     * Subfields used for searching and faceting.
     * @return defaults to {@link Collections#emptyIterator()}
     */
    default Iterable<SubFieldDescriptor<T>> subfields() {
        return Collections::emptyIterator;
    }

    /**
     * Create the required fields to store and index this property
     * @param value the field value
     * @return all {@link IndexableField}s
     */
    Iterable<IndexableField> fields(Object value);
}
