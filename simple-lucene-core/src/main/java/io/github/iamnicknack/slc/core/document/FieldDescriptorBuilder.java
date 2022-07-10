package io.github.iamnicknack.slc.core.document;

import io.github.iamnicknack.slc.api.document.FieldDescriptor;
import io.github.iamnicknack.slc.api.document.SubFieldDescriptor;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import io.github.iamnicknack.slc.api.document.FieldParser;
import io.github.iamnicknack.slc.api.document.FieldReader;

import java.util.*;
import java.util.stream.Stream;

/**
 * Builder utility to construct {@link FieldDescriptor} instances
 */
public class FieldDescriptorBuilder {

    private String name;

    private boolean id;

    private boolean multiValue;

    private boolean exclude;

    /**
     * The root name of the field
     * @param name the name to assign
     */
    public FieldDescriptorBuilder name(String name) {
        this.name = name;
        return this;
    }

    /**
     * Field forms a part of the unique identifier for the document
     */
    public FieldDescriptorBuilder id() {
        this.id = true;
        return this;
    }

    /**
     * Field is of type {@link List}
     */
    public FieldDescriptorBuilder multiValue() {
        this.multiValue = true;
        return this;
    }

    /**
     * Excluded fields have {@link org.apache.lucene.document.Field.Store#NO}
     */
    public FieldDescriptorBuilder exclude() {
        this.exclude = true;
        return this;
    }

    public StringFieldDescriptorBuilder stringField() {
        return new StringFieldDescriptorBuilder();
    }

    public IntFieldDescriptorBuilder intField() {
        return new IntFieldDescriptorBuilder();
    }

    public LongFieldDescriptorBuilder longField() {
        return new LongFieldDescriptorBuilder();
    }

    /**
     * String-specific features
     */
    public class StringFieldDescriptorBuilder extends TypedFieldDescriptorBuilder<String> {

        StringFieldDescriptorBuilder() {
            super(String.class);
            if(!exclude) subFieldFactories.add(SubFieldDescriptors::storedString);
            else subFieldFactories.add(n -> SubFieldDescriptors.alias(n, name));
        }

        /**
         * Field is stored as text and queried using a {@link org.apache.lucene.queryparser.classic.QueryParser}
         */
        public StringFieldDescriptorBuilder text() {
            if(!exclude) subFieldFactories.add(SubFieldDescriptors::text);
            return this;
        }

        /**
         * Field is stored without tokenization and can be queried with {@link org.apache.lucene.search.TermQuery}
         */
        public StringFieldDescriptorBuilder keyword() {
            subFieldFactories.add(SubFieldDescriptors::keyword);
            return this;
        }

        /**
         * Faceting is required on the field
         */
        public StringFieldDescriptorBuilder facet() {
            subFieldFactories.add(SubFieldDescriptors::stringFacet);
            return this;
        }

        /**
         * Additional field names under which to store the value
         * @param fields required alias fields
         */
        public StringFieldDescriptorBuilder alias(String... fields) {
            Arrays.stream(fields).forEach(field -> subFieldFactories.add(
                    ignored -> SubFieldDescriptors.alias(ignored, field)
            ));
            return this;
        }

        @Override
        public FieldParser<String> fieldParser() {
            return IndexableField::stringValue;
        }
    }

    /**
     * Integer-specific features
     */
    public class IntFieldDescriptorBuilder extends TypedFieldDescriptorBuilder<Integer> {
        IntFieldDescriptorBuilder() {
            super(Integer.class);
            if(!exclude) subFieldFactories.add(SubFieldDescriptors::storedInt);
        }

        public IntFieldDescriptorBuilder facet() {
            this.subFieldFactories.add(SubFieldDescriptors::numericFacet);
            return this;
        }

        public IntFieldDescriptorBuilder point() {
            this.subFieldFactories.add(SubFieldDescriptors::integerPoint);
            return this;
        }

        @Override
        public FieldParser<Integer> fieldParser() {
            return field -> field.numericValue().intValue();
        }
    }

    /**
     * Long-specific features
     */
    public class LongFieldDescriptorBuilder extends TypedFieldDescriptorBuilder<Long> {
        LongFieldDescriptorBuilder() {
            super(Long.class);
            if(!exclude) subFieldFactories.add(SubFieldDescriptors::storedLong);
        }

        public LongFieldDescriptorBuilder facet() {
            this.subFieldFactories.add(SubFieldDescriptors::numericFacet);
            return this;
        }

        public LongFieldDescriptorBuilder point() {
            this.subFieldFactories.add(SubFieldDescriptors::longPoint);
            return this;
        }

        @Override
        public FieldParser<Long> fieldParser() {
            return field -> field.numericValue().longValue();
        }
    }

    /**
     * Base class for type-specific builders
     */
    public abstract class TypedFieldDescriptorBuilder<T> {

        private final Class<T> type;

        protected final List<LazySubField<T>> subFieldFactories = new ArrayList<>();

        protected TypedFieldDescriptorBuilder(Class<T> type) {
            this.type = type;
        }

        public TypedFieldDescriptorBuilder<T> name(String name) {
            FieldDescriptorBuilder.this.name = name;
            return this;
        }

        public FieldDescriptor<T> build() {
            Objects.requireNonNull(name);

            var subFields = subFieldFactories.stream()
                    .map(factory -> factory.createField(name))
                    .toList();

            FieldReader fieldReader = multiValue
                    ? new MultiValueFieldReader(name, fieldParser())
                    : new SingleValueFieldReader(name, fieldParser());

            return new FieldDescriptorRecord<>(name,
                    id,
                    type.isAssignableFrom(String.class),
                    multiValue,
                    fieldReader,
                    subFields
            );
        }

        /**
         * Function via which the domain value can be derived from an {@link IndexableField}
         */
        abstract FieldParser<T> fieldParser();
    }

    private interface LazySubField<T> {
        SubFieldDescriptor<T> createField(String name);
    }

    private record FieldDescriptorRecord<T>(String name,
                                            boolean id,
                                            boolean facetable,
                                            boolean multiValue,
                                            FieldReader fieldReader,
                                            List<SubFieldDescriptor<T>> subfields) implements FieldDescriptor<T> {

        @Override
        @SuppressWarnings("unchecked")
        public Iterable<IndexableField> fields(Object value) {
            // do not store null values
            if(value == null) {
                return Collections::emptyIterator;
            }

            var stream = value instanceof Collection<?> collection
                    ? collection.stream()
                    : Stream.of(value);

            return () -> stream
                    .flatMap(v -> subfields.stream()
                            .map(factory -> factory.field((T)v))
                    )
                    .iterator();
        }

        @Override
        public Iterator<SubFieldDescriptor<T>> iterator() {
            return subfields.iterator();
        }

        @Override
        @SuppressWarnings("unchecked")
        public T read(Document document) {
            return (T)fieldReader.read(document);
        }
    }
}
