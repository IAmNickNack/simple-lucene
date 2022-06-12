package com.github.iamnicknack.slc.api.index;

import org.apache.lucene.document.Document;

import java.util.Objects;

/**
 * Domain specific operations required for interactions with the index
 * @param <T> the domain type
 */
public interface DomainOperations<T> {

    /**
     * Create a document representation of the domain type
     * @param value an instance of the domain value
     * @return a {@link Document}
     */
    Document createDocument(T value);

    /**
     * Convert a Lucene {@link Document} to a domain specific type
     * @param document the document to conver
     * @return a domain value
     */
    T readDocument(Document document);


    /**
     * Generate a unique identifier for a value.
     *
     * <p>The default implementation returns the string representation of {@link Object#hashCode()}</p>
     * @param value the domain value
     * @return a string unique to the value
     */
    default String id(T value) {
        Objects.requireNonNull(value, "value cannot be null");
        return "" + value.hashCode();
    }

}
