package io.github.iamnicknack.slc.annotation;

import org.apache.lucene.document.TextField;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation providing hints on how a value should be written to the index
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
public @interface IndexProperty {

    /**
     * Required index name for the field.
     * @return no default value
     */
    String value();

    /**
     * Whether to store index text using the standard analyser
     * @return defaults to true
     */
    boolean text() default true;

    /**
     * Whether to store an additional {@code <field>.keyword} {@link TextField}
     * @return defaults to false
     */
    boolean keyword() default false;

    /**
     * Whether to index numeric values as {@code <field>.point} {@link org.apache.lucene.document.IntPoint}, etc
     * @return defaults to true
     */
    boolean point() default true;

    /**
     * Whether to enable facets/aggregations on this field
     * @return defaults to {@code false}
     */
    boolean facet() default false;

    /**
     * Whether the field is mandatory and forms part of the unique identifier
     * @return defaults to {@code false}
     */
    boolean id() default false;

    /**
     * Whether to exclude the field from deserialisation. I.e. {@code org.apache.lucene.document.Field.Store#NO}
     * @return defaults to false
     */
    boolean exclude() default false;

    /**
     * Additional fields under which to alias this property
     * @return defaults to {@code {}}
     */
    String[] fields() default {};

    Class<?> parameterizedType() default Void.class;
}
