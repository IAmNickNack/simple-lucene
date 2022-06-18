package io.github.iamnicknack.slc.core.test;

import io.github.iamnicknack.slc.api.backend.LuceneBackend;
import io.github.iamnicknack.slc.api.index.DomainOperations;
import io.github.iamnicknack.slc.core.document.DocumentDescriptorBuilder;
import io.github.iamnicknack.slc.core.document.FieldDescriptorBuilder;
import io.github.iamnicknack.slc.core.index.MapDomainOperations;

import java.util.Map;

/**
 * Factory for {@link DomainOperations} used in tests
 */
public class BuilderDomainOperations {

    public static DomainOperations<Map<String, Object>> create(LuceneBackend backend) {
        var documentDescriptor = new DocumentDescriptorBuilder(backend)
                .field(new FieldDescriptorBuilder()
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
                        .facet()
                        .build()
                )
                .build();

        return new MapDomainOperations(documentDescriptor);
    }

}
