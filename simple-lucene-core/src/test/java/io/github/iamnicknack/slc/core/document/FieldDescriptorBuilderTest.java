package io.github.iamnicknack.slc.core.document;

import io.github.iamnicknack.slc.api.document.SubFieldDescriptor;
import org.apache.lucene.document.*;
import org.apache.lucene.facet.FacetField;
import org.apache.lucene.index.IndexableField;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.*;

class FieldDescriptorBuilderTest {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Test
    void doesNotCreateFieldsForNullValues() {
        var field = new FieldDescriptorBuilder()
                .name("foo")
                .stringField()
                .build();

        var fields = field.fields("bar");
        assertTrue(fields.iterator().hasNext());

        fields = field.fields(null);
        assertFalse(fields.iterator().hasNext());
    }

    @Test
    void buildStringField() {
        var field = new FieldDescriptorBuilder()
                .name("foo")
                .id()
                .stringField()
                .alias("_alias1", "_alias2")
                .keyword()
                .text()
                .facet()
                .build();

        Assertions.assertEquals(6, StreamSupport.stream(field.spliterator(), false).count());
        assertFieldDescriptors(List.of("foo.text", "foo.keyword", "foo.value"), field.subfields());

        var result = StreamSupport.stream(field.spliterator(), false)
                .map(factory -> factory.field("bar"))
                .toList();

        assertIndexableFields(List.of(StringField.class, TextField.class, StoredField.class, FacetField.class), result);

        field.forEach(factory -> logger.info("buildStringField: {}", factory.field("bar")));
    }

    @Test
    void buildSimpleStringField() {
        var field = new FieldDescriptorBuilder()
                .name("foo")
                .stringField()
                .text()
                .build();

        Assertions.assertEquals(2, StreamSupport.stream(field.spliterator(), false).count());
        assertFieldDescriptors(List.of("foo.text"), field.subfields());

        var result = StreamSupport.stream(field.spliterator(), false)
                .map(factory -> factory.field("bar"))
                .toList();

        assertIndexableFields(List.of(TextField.class, StoredField.class), result);

        field.forEach(factory -> logger.info("buildSimpleStringField: {}", factory.field("bar")));
    }


    @Test
    void buildIgnoredField() {
        var field = new FieldDescriptorBuilder()
                .name("foo")
                .exclude()
                .stringField()
                .text() // <- ignored as handled by exclude
                .build();

        Assertions.assertEquals(1, StreamSupport.stream(field.spliterator(), false).count());
        assertFieldDescriptors(List.of("foo"), field.subfields());

        var result = StreamSupport.stream(field.spliterator(), false)
                .map(factory -> factory.field("bar"))
                .toList();

        assertIndexableFields(List.of(TextField.class), result);

        field.forEach(factory -> logger.info("buildIgnoredField: {}", factory.field("bar")));
    }


    @Test
    void buildIntField() {
        var field = new FieldDescriptorBuilder()
                .id()
                .intField()
                .facet()
                .point()
                .name("foo")
                .build();

        Assertions.assertEquals(3, StreamSupport.stream(field.spliterator(), false).count());
        assertFieldDescriptors(List.of("foo.point", "foo.value"), field.subfields());

        var result = StreamSupport.stream(field.spliterator(), false)
                .map(factory -> factory.field(999))
                .toList();

        assertIndexableFields(List.of(IntPoint.class, StoredField.class), result);

        field.forEach(factory -> logger.info("buildIntField: {}", factory.field(999)));
    }

    @Test
    void buildLongField() {
        var field = new FieldDescriptorBuilder()
                .id()
                .longField()
                .facet()
                .point()
                .name("foo")
                .build();

        Assertions.assertEquals(3, StreamSupport.stream(field.spliterator(), false).count());
        assertFieldDescriptors(List.of("foo.point", "foo.value"), field.subfields());

        var result = StreamSupport.stream(field.spliterator(), false)
                .map(factory -> factory.field(999L))
                .toList();

        assertIndexableFields(List.of(LongPoint.class, NumericDocValuesField.class, StoredField.class), result);

        field.forEach(factory -> logger.info("buildLongField: {}", factory.field(999L)));
    }

    @Test
    void buildZonedDateTimeField() {
        var field = new FieldDescriptorBuilder()
                .zonedDateTime()
                .point()
                .facet()
                .name("foo")
                .build();


        Assertions.assertEquals(3, StreamSupport.stream(field.spliterator(), false).count());
        assertFieldDescriptors(List.of("foo.point", "foo.value", "foo"), field.subfields());

        var now = ZonedDateTime.now();
        var result = StreamSupport.stream(field.spliterator(), false)
                .map(factory -> factory.field(now))
                .toList();

        assertIndexableFields(List.of(LongPoint.class, NumericDocValuesField.class, StoredField.class), result);

        Document document = new Document();
        field.forEach(factory -> document.add(factory.field(now)));

        var value = field.read(document);
        logger.info("Read value: {}", value);
        org.assertj.core.api.Assertions.assertThat(((ZonedDateTime)value).toInstant().toEpochMilli())
                .isEqualTo(now.toInstant().toEpochMilli());

        field.forEach(factory -> logger.info("buildZonedDateTimeField: {}", factory.field(now)));
    }


    private <T> void assertFieldDescriptors(List<String> expected, Iterable<SubFieldDescriptor<T>> subFieldDescriptors) {
        assertTrue(() -> expected.stream()
                .allMatch(fieldName -> StreamSupport.stream(subFieldDescriptors.spliterator(), true)
                        .map(SubFieldDescriptor::name)
                        .anyMatch(subfieldName -> subfieldName.equals(fieldName))
                )
        );
    }

    private <T> void assertIndexableFields(List<Class<? extends IndexableField>> expected, List<IndexableField> fields) {
        assertTrue(() -> expected.stream()
                .allMatch(expectedType -> fields.stream()
                        .map(Object::getClass)
                        .anyMatch(actualType -> actualType.isAssignableFrom(expectedType))
                )
        );
    }

}