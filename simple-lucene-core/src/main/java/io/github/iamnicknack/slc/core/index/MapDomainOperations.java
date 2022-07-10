package io.github.iamnicknack.slc.core.index;

import io.github.iamnicknack.slc.api.document.DocumentDescriptor;
import io.github.iamnicknack.slc.api.document.FieldDescriptor;
import io.github.iamnicknack.slc.api.index.DomainOperations;
import org.apache.lucene.document.Document;

import java.util.AbstractMap.SimpleEntry;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.StreamSupport.stream;

/**
 * Generic {@link DomainOperations} implementation for use with {@link Map} instances.
 *
 * <p>Requires the document structure to have been previously specified via {@link DocumentDescriptor}.
 * Map entries with no corresponding {@link FieldDescriptor} are not indexed</p>
 */
public class MapDomainOperations implements DomainOperations<Map<String, Object>> {

    private final DocumentDescriptor documentDescriptor;

    public MapDomainOperations(DocumentDescriptor documentDescriptor) {
        this.documentDescriptor = documentDescriptor;
    }

    @Override
    public String id(Map<String, Object> value) {
        return Optional.of(documentDescriptor.fieldMap()
                .entrySet().stream()
                .filter(e -> e.getValue().id() && value.containsKey(e.getKey()))
                .sorted(Map.Entry.comparingByKey())
                .map(e -> value.get(e.getKey()).toString())
                .collect(Collectors.joining("."))
        )
                .filter(s -> s.length() > 0)
                .orElseThrow(() -> new RuntimeException("No fields available to create id"));

    }

    @Override
    public Document createDocument(Map<String, Object> map) {
        var document = new Document();

        documentDescriptor.fieldMap().entrySet().stream()
                .filter(descriptorEntry -> map.containsKey(descriptorEntry.getKey()))
                .flatMap(descriptorEntry -> Stream.of(map.get(descriptorEntry.getKey()))
                        .flatMap(value -> stream(descriptorEntry.getValue().fields(value).spliterator(), false))
                )
                .forEach(document::add);

        return document;
    }

    @Override
    public Map<String, Object> readDocument(Document document) {

        return documentDescriptor.fieldMap().entrySet().stream()
                .map(descriptorEntry -> new SimpleEntry<>(descriptorEntry.getKey(), descriptorEntry.getValue().read(document)))
                .filter(e -> e.getValue() != null)
                .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue));
    }
}
