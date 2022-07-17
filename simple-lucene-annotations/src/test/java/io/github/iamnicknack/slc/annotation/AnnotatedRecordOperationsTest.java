package io.github.iamnicknack.slc.annotation;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.iamnicknack.slc.api.backend.LuceneBackend;
import io.github.iamnicknack.slc.core.backend.LuceneBackends;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AnnotatedRecordOperationsTest {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Nested
    class ReadWriteTests {
        private final LuceneBackend backend;
        private final AnnotatedRecordOperations<TestData> configuration;

        ReadWriteTests() throws IOException {
            this.backend = LuceneBackends.memory();
            this.configuration = AnnotatedRecordOperations.create(TestData.class, backend);
        }

        @Test
        void canConstruct() {
            assertNotNull(configuration);
        }

        @Test
        void createsDocument() {
            var doc = configuration.createDocument(new TestData("string value", 1));
            doc.getFields().forEach(field -> logger.info("Document field: {}", field));

            assertEquals(6, doc.getFields().size());
            var dimConfig = backend.facetsConfig().getDimConfig("string-field");
            assertEquals("string-field.value", dimConfig.indexFieldName);
        }

        @Test
        void readsDocument() {
            var doc = configuration.createDocument(new TestData("string value", 1));
            logger.info("Created document: {}", doc.getFields());

            var testData = configuration.readDocument(doc);
            logger.info("Created value: {}", testData);
        }

        public record TestData(@IndexProperty(value = "string-field", keyword = true, facet = true) String stringField,
                               @IndexProperty(value = "int-field") int intField) {}

    }


    @Nested
    class FieldTypeTests {
        private LuceneBackend backend;

        @BeforeEach
        void beforeEach() throws IOException {
            backend = LuceneBackends.memory();
        }

        @AfterEach
        void afterEach() throws IOException {
            backend.close();
        }

        @Test
        void stringField() {
            var config = AnnotatedRecordOperations.create(StringRecord.class, backend);
            assertNotNull(config);

            var testRecord = new StringRecord("test value");
            var testDocument = config.createDocument(testRecord);
            assertNotNull(testDocument);

            var readRecord = config.readDocument(testDocument);
            assertEquals(testRecord, readRecord);
        }


        public record StringRecord(@IndexProperty("string-field") String stringValue) {
        }

        @Test
        void intField() {
            var config = AnnotatedRecordOperations.create(IntegerRecord.class, backend);
            assertNotNull(config);

            var testRecord = new IntegerRecord(10);
            var testDocument = config.createDocument(testRecord);
            assertNotNull(testDocument);

            var readRecord = config.readDocument(testDocument);
            assertEquals(testRecord, readRecord);
        }


        public record IntegerRecord(@IndexProperty("int-field") int intValue) {
        }

        @Test
        void longField() {
            var config = AnnotatedRecordOperations.create(LongRecord.class, backend);
            assertNotNull(config);

            var testRecord = new LongRecord(10);
            var testDocument = config.createDocument(testRecord);
            assertNotNull(testDocument);

            var readRecord = config.readDocument(testDocument);
            assertEquals(testRecord, readRecord);
        }

        public record LongRecord(@IndexProperty("long-field") long longValue) {
        }

        @Test
        void zonedDateTimeField() {
            var config = AnnotatedRecordOperations.create(ZonedDateTimeRecord.class, backend);
            assertNotNull(config);

            var testRecord = new ZonedDateTimeRecord(ZonedDateTime.now());
            var testDocument = config.createDocument(testRecord);
            assertNotNull(testDocument);

            var readRecord = config.readDocument(testDocument);
            assertEquals(testRecord.timestamp.toInstant().toEpochMilli(), readRecord.timestamp.toInstant().toEpochMilli());
        }

        public record ZonedDateTimeRecord(@IndexProperty("timestamp-field") ZonedDateTime timestamp) {
        }

        @Test
        void compositeRecord() {
            var config = AnnotatedRecordOperations.create(CompositeRecord.class, backend);
            assertNotNull(config);

            var testRecord = new CompositeRecord(10, "test value");
            var testDocument = config.createDocument(testRecord);
            assertNotNull(testDocument);

            var readRecord = config.readDocument(testDocument);
            assertEquals(testRecord, readRecord);
        }

        @Test
        void idField() {
            var config = AnnotatedRecordOperations.create(CompositeRecord.class, backend);
            assertNotNull(config);

            var testRecord = new CompositeRecord(10, "test value");
            var id = config.id(testRecord);
            assertEquals(testRecord.stringValue(), id);
        }


        public record CompositeRecord(@IndexProperty("int-field") int intValue,
                                      @IndexProperty(value = "string-field", text = false, keyword = true, facet = true, id = true) String stringValue) {
        }


        @Test
        void multiValueStringField() {
            var config = AnnotatedRecordOperations.create(MultiValueStringRecord.class, backend);
            assertNotNull(config);

            var testRecord = new MultiValueStringRecord(List.of("first", "second"));
            var testDocument = config.createDocument(testRecord);
            assertNotNull(testDocument);

            var readRecord = config.readDocument(testDocument);
            assertEquals(testRecord, readRecord);
        }


        public record MultiValueStringRecord(@IndexProperty(value = "string-field", parameterizedType = String.class) List<String> stringValue) {
        }


//
//        @Test
//        void instantField() throws NoSuchMethodException {
//            var config = AnnotationBeanConfiguration.create(InstantRecord.class, backend);
//            assertNotNull(config);
//
//            var testRecord = new InstantRecord(Instant.now());
//            var testDocument = config.createDocument(testRecord);
//            assertNotNull(testDocument);
//
//            var readRecord = config.readDocument(testDocument);
//            assertEquals(testRecord.instant.toEpochMilli(), readRecord.instant.toEpochMilli());
//        }
//
//        record InstantRecord(@IndexProperty("timestamp") Instant instant) {}
    }

}