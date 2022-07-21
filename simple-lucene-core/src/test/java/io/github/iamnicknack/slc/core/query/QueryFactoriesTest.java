package io.github.iamnicknack.slc.core.query;

import io.github.iamnicknack.slc.api.backend.LuceneBackend;
import io.github.iamnicknack.slc.core.backend.LuceneBackends;
import io.github.iamnicknack.slc.core.index.BucketUpdateOperations;
import io.github.iamnicknack.slc.core.test.BuilderDomainOperations;
import io.github.iamnicknack.slc.core.test.TestData;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QueryFactoriesTest {

    private final LuceneBackend backend;

    private final ZonedDateTime referenceDate = LocalDate.of(2020, 1, 1).atStartOfDay(ZoneOffset.UTC);

    QueryFactoriesTest() throws IOException {
        this.backend = LuceneBackends.memory();
        var bucketOperations = new BucketUpdateOperations<>(BuilderDomainOperations.create(backend));
        try(var lease = backend.updateLeaseFactory().lease()) {
            lease.execute(bucketOperations.addAll(List.of(
                    TestData.createValue("TEST", 1, "TEST", Collections.emptyList(), referenceDate.minus(Period.ofYears(1))),
                    TestData.createValue("TEST2", 2, "TEST 2", Collections.emptyList(), referenceDate),
                    TestData.createValue("BLAH", 3, "BLAH", Collections.emptyList(), referenceDate.plus(Period.ofYears(1)))
            )));
        }
    }

    @Test
    void createsKeyword() {
        var factory = QueryFactories.keyword("value.keyword");
        var executor = new DefaultQueryExecutor<>(
                factory,
                backend.searcherLeaseFactory()
        );
        try(var result = executor.execute("TEST")) {
            assertEquals(1, result.totalHits());
        }
    }

    @Test
    void createsText() {
        var factory = QueryFactories.text("description.text");
        var executor = new DefaultQueryExecutor<>(
                factory,
                backend.searcherLeaseFactory()
        );
        try(var result = executor.execute("test")) {
            assertEquals(2, result.totalHits());
        }
        try(var result = executor.execute("blah")) {
            assertEquals(1, result.totalHits());
        }
    }

    @Test
    void createsZonedDateTimeAfter() {
        var factory = QueryFactories.after("timestamp.point");
        var executor = new DefaultQueryExecutor<>(
                factory,
                backend.searcherLeaseFactory()
        );

        try(var result = executor.execute(referenceDate)) {
            assertEquals(2, result.totalHits());
        }
    }

    @Test
    void createsZonedDateTimeBefore() {
        var factory = QueryFactories.before("timestamp.point");
        var executor = new DefaultQueryExecutor<>(
                factory,
                backend.searcherLeaseFactory()
        );

        try(var result = executor.execute(referenceDate)) {
            assertEquals(1, result.totalHits());
            assertEquals("TEST", result.iterator().next()
                    .value().getField("value").stringValue()
            );
        }
    }

    @Test
    void createsZonedDateTimeBetween() {
        var factory = QueryFactories.between("timestamp.point");
        var executor = new DefaultQueryExecutor<>(
                factory,
                backend.searcherLeaseFactory()
        );

        var dates = new ZonedDateTime[] { referenceDate.minus(Period.ofDays(1)), referenceDate.plus(Period.ofDays(1)) };
        try(var result = executor.execute(dates)) {
            assertEquals(1, result.totalHits());
            assertEquals("TEST2", result.iterator().next()
                    .value().getField("value").stringValue()
            );
        }
    }

    @Test
    void failsZonedDateTimeBetween() {
        assertThrows(QueryException.class, () ->
                QueryFactories.between("any").query(new ZonedDateTime[] { referenceDate })
        );
    }
}