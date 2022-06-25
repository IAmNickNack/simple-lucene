package io.github.iamnicknack.slc.core.document;

import io.github.iamnicknack.slc.api.backend.LuceneBackend;
import io.github.iamnicknack.slc.api.document.DocumentDescriptor;
import io.github.iamnicknack.slc.api.document.FieldDescriptor;
import io.github.iamnicknack.slc.api.document.SubFieldDescriptor;
import org.apache.lucene.facet.FacetField;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Builder for a {@link DocumentDescriptor} based on given {@link FieldDescriptor}s
 */
public class DocumentDescriptorBuilder {

    private final List<FieldDescriptor<?>> fieldDescriptors = new ArrayList<>();

    private final LuceneBackend luceneBackend;

    /**
     *
     * @param luceneBackend the backend used to register index field names for taxonomy facets
     */
    public DocumentDescriptorBuilder(LuceneBackend luceneBackend) {
        this.luceneBackend = luceneBackend;
    }

    /**
     * Register a field descriptor
     * @param fieldDescriptor a single field definition
     * @return this
     */
    public DocumentDescriptorBuilder field(FieldDescriptor<?> fieldDescriptor) {
        fieldDescriptors.add(fieldDescriptor);
        return this;
    }

    /**
     * Render the document definition based on the provided field information
     * @return the document definition
     */
    public DocumentDescriptor build() {
        Map<String, FieldDescriptor<?>> fieldDescriptorMap = fieldDescriptors.stream()
                .collect(Collectors.toMap(FieldDescriptor::name, Function.identity()));


        record FacetPair(FieldDescriptor<?> fieldDescriptor, SubFieldDescriptor<?> subFieldDescriptor) {}
        fieldDescriptors.stream()
                .filter(FieldDescriptor::facetable)
                .flatMap(fieldDescriptor -> StreamSupport.stream(fieldDescriptor.subfields().spliterator(), false)
                        .filter(subFieldDescriptor -> subFieldDescriptor.fieldType().isAssignableFrom(FacetField.class))
                        .map(subFieldDescriptor -> new FacetPair(fieldDescriptor, subFieldDescriptor))
                )
                .forEach(facetPair -> {
                    luceneBackend.facetsConfig()
                            .setMultiValued(facetPair.subFieldDescriptor().name(), facetPair.fieldDescriptor().multiValue());
                    luceneBackend.facetsConfig()
                            .setIndexFieldName(facetPair.fieldDescriptor().name(), facetPair.subFieldDescriptor().name());
                });

        return () -> fieldDescriptorMap;
    }
}
