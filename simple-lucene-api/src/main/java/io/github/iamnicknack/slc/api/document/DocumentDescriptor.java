package io.github.iamnicknack.slc.api.document;

import java.util.Map;

/**
 * Describes the known structure of an index to assist with marshalling
 */
public interface DocumentDescriptor {

    /**
     * Field descriptors by name
     */
    Map<String, FieldDescriptor<?>> fieldMap();

}
