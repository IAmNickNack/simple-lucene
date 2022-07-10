package io.github.iamnicknack.slc.api.document;

import org.apache.lucene.document.Document;

/**
 * The {@code FieldReader} is used to provide individual domain property values from a {@link Document}.
 *
 * <p>These could either be simple Java types that are stored as single field, or complex types which require
 * more than one document field to store their representation.</p>
 */
public interface FieldReader {

    /**
     * Read a value from the specified {@link Document}
     * @param document the Lucene document
     * @return the reconstructed domain value
     */
    Object read(Document document);

}
