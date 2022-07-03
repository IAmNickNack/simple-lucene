package io.github.iamnicknack.slc.annotation;

import io.github.iamnicknack.slc.api.document.DocumentDescriptor;
import io.github.iamnicknack.slc.api.document.FieldDescriptor;
import org.apache.lucene.document.Document;
import io.github.iamnicknack.slc.api.backend.LuceneBackend;
import io.github.iamnicknack.slc.core.document.DocumentDescriptorBuilder;
import io.github.iamnicknack.slc.core.document.FieldDescriptorBuilder;
import io.github.iamnicknack.slc.api.index.DomainOperations;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Factory for {@link DomainOperations} derived from {@link IndexProperty} annotations
 * @param <T> the introspected type
 */
public interface AnnotatedRecordOperations<T extends Record> extends DomainOperations<T> {


    /**
     * Create {@link DomainOperations} derived from {@link IndexProperty} annotations
     * @param type the annotated type
     * @param backend the index configuration which may be updated according to any required facets
     * @return a {@link DomainOperations} instance
     * @param <T> the annotated type
     */
    static <T extends Record> AnnotatedRecordOperations<T> create(Class<T> type, LuceneBackend backend) {

        record AccessorDescriptor<T>(Method accessor, FieldDescriptor<T> fieldDescriptor) {}
        var accessors = Arrays.stream(type.getRecordComponents())
                .map(recordComponent -> {
                    var method = recordComponent.getAccessor();
                    var annotation = method.getAnnotation(IndexProperty.class);
                    var fieldDescriptor = PropertyDescriptorFactory.get(recordComponent.getType())
                            .fieldDescriptor(annotation);

                    return new AccessorDescriptor<>(method, fieldDescriptor);
                })
                .toList();

        /*
         **************************************************************************************************************
         * Create index definition, registering fields with backend
         */
        var builder = new DocumentDescriptorBuilder(backend);
        accessors.stream()
                .map(AccessorDescriptor::fieldDescriptor)
                .forEach(builder::field);
        var descriptor = builder.build();

        /*
         **************************************************************************************************************
         * Read support
         */
        var constructorTypes = accessors.stream()
                .map(accessorDescriptor -> accessorDescriptor.accessor().getReturnType())
                .toArray(Class[]::new);

        Constructor<T> constructor;
        try {
            constructor = type.getConstructor(constructorTypes);
        } catch (NoSuchMethodException e) {
            throw new AnnotationConfigurationException(e);
        }

        var constructorArgs = new Function<Document, Object[]>() {
            @Override
            public Object[] apply(Document document) {
                return accessors.stream()
                        .map(accessorDescriptor -> accessorDescriptor.fieldDescriptor()
                                .read(document.getFields(accessorDescriptor.fieldDescriptor().name()))
                        )
                        .toArray();
            }
        };

        /*
         **************************************************************************************************************
         * Document id.
         * This approach has a significant dependency on reflection. This could definitely be optimised.
         * However:
         * > ID functions are only used during CRUD operations for which Lucene is not optimised.
         * > Use cases benefiting from Lucene are generally read-heavy and as such wouldn't hit this code
         * once data is indexed.
         * > Any optimisations can be deferred until use cases exist which require more frequent update operations
         */
        var idMethods = accessors.stream()
                .filter(accessorDescriptor -> accessorDescriptor.fieldDescriptor().id())
                .map(AccessorDescriptor::accessor)
                .toList();

        Function<T, String> idFunction = idMethods.isEmpty()
                ? t -> t.hashCode() + ""
                : t -> idMethods.stream()
                .map(method -> {
                    try {
                        return method.invoke(t);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw new AnnotationConfigurationException(e);
                    }
                })
                .filter(Objects::nonNull)
                .map(Object::toString)
                .collect(Collectors.joining("."));

        return new AnnotatedRecordOperations<>() {

            @Override
            public DocumentDescriptor documentDescriptor() {
                return descriptor;
            }

            @Override
            public String id(T value) {
                return idFunction.apply(value);
            }

            @Override
            public T readDocument(Document document) {
                try {
                    return constructor.newInstance(constructorArgs.apply(document));
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    throw new AnnotationConfigurationException(e);
                }
            }

            @Override
            public Document createDocument(T value) {
                Document document = new Document();

                accessors.forEach(accessorDescriptor -> {
                    try {
                        accessorDescriptor.fieldDescriptor().fields(accessorDescriptor.accessor().invoke(value))
                                .forEach(document::add);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw new AnnotationConfigurationException(e);
                    }
                });

                return document;
            }
        };
    }

    DocumentDescriptor documentDescriptor();

    interface PropertyDescriptorFactory {
        FieldDescriptor<?> fieldDescriptor(IndexProperty annotation);

        static PropertyDescriptorFactory get(Class<?> type) {
            return Optional.ofNullable(lookup.get(type))
                    .orElseThrow(() -> new AnnotationConfigurationException("Unsupported type: " + type.getName()));
        }

        PropertyDescriptorFactory listFactory = annotation -> {
            var valueDescriptor = get(annotation.parameterizedType());
            return valueDescriptor.fieldDescriptor(annotation);
        };

        PropertyDescriptorFactory stringFactory = annotation -> {
            var builder = new FieldDescriptorBuilder()
                    .name(annotation.value());

            if(annotation.exclude()) builder.exclude();
            if(annotation.parameterizedType() != Void.class) builder.multiValue();

            var stringBuilder = annotation.id()
                    ? builder.id().stringField()
                    : builder.stringField();

            if(annotation.keyword()) stringBuilder.keyword();
            if(annotation.text()) stringBuilder.text();
            if(annotation.facet()) stringBuilder.facet();
            stringBuilder.alias(annotation.fields());

            return stringBuilder.build();
        };

        PropertyDescriptorFactory integerFactory = annotation -> {
            var builder = new FieldDescriptorBuilder()
                    .name(annotation.value());

            if(annotation.parameterizedType() != Void.class) builder.multiValue();
            if(annotation.exclude()) builder.exclude();

            var intBuilder = builder.intField();

            if(annotation.point()) intBuilder.point();
            if(annotation.facet()) intBuilder.facet();

            return intBuilder.build();
        };

        PropertyDescriptorFactory longFactory = annotation -> {
            var builder = new FieldDescriptorBuilder()
                    .name(annotation.value());

            if(annotation.parameterizedType() != Void.class) builder.multiValue();
            if(annotation.exclude()) builder.exclude();

            var longBuilder = builder.longField();

            if(annotation.point()) longBuilder.point();
            if(annotation.facet()) longBuilder.facet();

            return longBuilder.build();
        };

        Map<Class<?>, PropertyDescriptorFactory> lookup = Map.of(
                String.class, stringFactory,
                Integer.class, integerFactory,
                int.class, integerFactory,
                Long.class, longFactory,
                long.class, longFactory,
                List.class, listFactory
        );
    }
}
