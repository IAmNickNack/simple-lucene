package slc.domain.operations;

import io.github.iamnicknack.slc.api.backend.LuceneBackend;
import io.github.iamnicknack.slc.api.index.DomainOperations;
import io.github.iamnicknack.slc.core.document.DocumentDescriptorBuilder;
import io.github.iamnicknack.slc.core.document.FieldDescriptorBuilder;
import io.github.iamnicknack.slc.core.index.MapDomainOperations;

import java.util.Map;

/**
 * Factory for {@link DomainOperations} used in tests
 */
public class ShortCountryMapOperations {

    public static DomainOperations<Map<String, Object>> create(LuceneBackend backend) {
        var documentDescriptor = new DocumentDescriptorBuilder(backend)
                .field(new FieldDescriptorBuilder()
                        .name("name")
                        .id()
                        .stringField()
                        .text()
                        .alias("_all")
                        .build()
                )
                .field(new FieldDescriptorBuilder()
                        .name("region")
                        .stringField()
                        .text()
                        .keyword()
                        .facet()
                        .alias("_all")
                        .build()
                )
                .field(new FieldDescriptorBuilder()
                        .name("iso")
                        .stringField()
                        .keyword()
                        .alias("_all")
                        .build()
                )
                .field(new FieldDescriptorBuilder()
                        .name("place")
                        .multiValue()
                        .stringField()
                        .text()
                        .alias("_all")
                        .build()
                )
                .build();

        return new MapDomainOperations(documentDescriptor);
    }

}
