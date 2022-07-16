package io.github.iamnicknack.slc.api.document;

/**
 * Attributes forming part of the field description
 */
public interface FieldAttributes {

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

}
