package io.github.iamnicknack.slc.core.document;

import io.github.iamnicknack.slc.api.index.DomainOperations;
import io.github.iamnicknack.slc.core.backend.LuceneBackends;
import io.github.iamnicknack.slc.core.index.MapDomainOperations;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.StoredField;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.function.Consumer;

class ComplexTypeDescriptorSpike {

    @Test
    void builderWithOperations() throws IOException {
        var backend = LuceneBackends.memory();
        var descriptor = new DocumentDescriptorBuilder(backend)
                .field(new FieldDescriptorBuilder()
                        .name("timestamp")
                        .domainOperations(new ZonedDatetimeDomainOperations("timestamp"))
                        .build()
                )
                .build();
        var operations = new MapDomainOperations(descriptor);

        Consumer<ZonedDateTime> printConsumer = zonedDateTime -> {
            var map = Map.<String, Object>of(
                    "timestamp", zonedDateTime
            );

            var document = operations.createDocument(map);
            document.forEach(System.out::println);

            map = operations.readDocument(document);
            map.forEach((name, value) -> System.out.println("%s: %s".formatted(name, value)));
        };

        printConsumer.accept(ZonedDateTime.now());
        ZoneId phx = ZoneId.of("-08:00");
        printConsumer.accept(LocalDate.of(2022, 7, 11).atStartOfDay(phx));
    }

    static class ZonedDatetimeDomainOperations implements DomainOperations<ZonedDateTime> {

        private final String name;

        public ZonedDatetimeDomainOperations(String name) {
            this.name = name;
        }

        @Override
        public Document createDocument(ZonedDateTime value) {
            var document = new Document();

            var millis = value.toInstant().toEpochMilli();

            document.add(new StoredField(name, millis));
            document.add(new LongPoint("%s.point".formatted(name), millis));
            document.add(new StoredField("%s.zone".formatted(name), value.getZone().getId()));

            return document;
        }

        @Override
        public ZonedDateTime readDocument(Document document) {
            var millis = document.getField(name).numericValue().longValue();
            var zone = document.getField("%s.zone".formatted(name)).stringValue();
            return ZonedDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.of(zone));
        }
    }
}
