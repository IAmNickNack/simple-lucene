package io.github.iamnicknack.slc.core.test;

import io.github.iamnicknack.slc.api.index.DomainOperations;
import org.apache.lucene.document.*;
import org.apache.lucene.facet.FacetField;
import org.apache.lucene.index.IndexableField;

import java.util.*;

/**
 * {@link DomainOperations} which use the Lucene directly, bypassing document specification
 * via {@link io.github.iamnicknack.slc.api.document.DocumentDescriptor}
 */
@SuppressWarnings("unchecked")
public class LuceneDomainOperations implements DomainOperations<Map<String, Object>> {

    @Override
    public Document createDocument(Map<String, Object> value) {
        var document = new Document();

        Optional.ofNullable(value.get("value"))
                .map(String.class::cast)
                .ifPresent(s -> {
                    document.add(new StoredField("value", s));
                    document.add(new StringField("value.keyword", s, Field.Store.NO));
                });

        Optional.ofNullable(value.get("sequence"))
                .map(Integer.class::cast)
                .ifPresent(integer -> {
                    document.add(new StoredField("sequence", integer));
                    document.add(new IntPoint("sequence.point", integer));
                });

        Optional.ofNullable(value.get("description"))
                .map(String.class::cast)
                .ifPresent(s -> {
                    document.add(new StoredField("description", s));
                    document.add(new TextField("description.text", s, Field.Store.NO));
                });

        Optional.ofNullable(value.get("others"))
                .map(List.class::cast)
                .stream()
                .flatMap(Collection::stream)
                .forEach(s -> {
                    String othersValue = (String) s;
                    document.add(new StoredField("others", othersValue));
                    document.add(new StringField("others.keyword", othersValue, Field.Store.NO));
                    document.add(new FacetField("others.value", othersValue));
                });

        return document;
    }

    @Override
    public Map<String, Object> readDocument(Document document) {
        var value = new HashMap<String, Object>();

        Optional.ofNullable(document.getField("value"))
                .ifPresent(field -> value.put("value", field.stringValue()));

        Optional.ofNullable(document.getField("sequence"))
                .ifPresent(field -> value.put("sequence", field.numericValue().intValue()));

        Optional.ofNullable(document.getField("description"))
                .ifPresent(field -> value.put("description", field.stringValue()));

        value.put("others", Arrays.stream(document.getFields("others"))
                .map(IndexableField::stringValue)
                .toList()
        );

        return value;
    }

    @Override
    public String id(Map<String, Object> value) {
        return (String) Objects.requireNonNull(value.get("value"), "value field cannot be null");
    }
}
