package com.github.iamnicknack.slc.core.test;

import com.github.iamnicknack.slc.api.document.DocumentDescriptor;
import com.github.iamnicknack.slc.api.query.QueryFactory;
import com.github.iamnicknack.slc.core.document.DocumentDescriptorBuilder;
import com.github.iamnicknack.slc.core.document.FieldDescriptorBuilder;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;
import org.mockito.ArgumentMatcher;
import com.github.iamnicknack.slc.api.backend.LuceneBackend;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class TestData {

    private TestData() {}

    /**
     * Document descriptor for test domain
     */
    public static DocumentDescriptor documentDescriptor(LuceneBackend backend) {
        DocumentDescriptorBuilder builder = new DocumentDescriptorBuilder(backend);
        return builder.field(new FieldDescriptorBuilder()
                        .name("value")
                        .id()
                        .stringField()
                        .keyword()
                        .build()
                )
                .field(new FieldDescriptorBuilder()
                        .name("sequence")
                        .intField()
                        .point()
                        .build()
                )
                .field(new FieldDescriptorBuilder()
                        .name("description")
                        .stringField()
                        .text()
                        .build()
                )
                .field(new FieldDescriptorBuilder()
                        .name("others")
                        .multiValue()
                        .stringField()
                        .keyword()
                        .build()
                )
                .build();
    }

    public static Map<String, Object> createValue(String value, int sequence, String description, List<String> others) {
        return Map.of("value", value, "sequence", sequence, "description", description, "others", others);
    }

    public static Map<String, Object> createValue(String value, int sequence, String description) {
        return Map.of("value", value, "sequence", sequence, "description", description);
    }

    public static Map<String, Object> createValue(String value, int sequence) {
        return Map.of("value", value, "sequence", sequence);
    }

    public static Map<String, Object> createValue(String value) {
        return Map.of("value", value);
    }

    public static QueryFactory<Map<String, Object>> valueKeywordQueryFactory() {
        return value -> new TermQuery(new Term("value.keyword", (String)value.get("value")));
    }

    public static ArgumentMatcher<Map<String, Object>> argumentMatcher(Map<String, Object> value) {
        return stringObjectMap -> value.entrySet().stream()
                // TODO: Make sure collections are matched
                .filter(e -> !Collection.class.isAssignableFrom(e.getValue().getClass()))
                .allMatch(e -> stringObjectMap.entrySet().stream()
                        .filter(t -> t.getKey().equals(e.getKey()))
                        .anyMatch(t -> t.getValue().equals(e.getValue()))
                );
    }
}
