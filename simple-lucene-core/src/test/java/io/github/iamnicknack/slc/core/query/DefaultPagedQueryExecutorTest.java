package io.github.iamnicknack.slc.core.query;

import io.github.iamnicknack.slc.core.backend.LuceneBackends;
import io.github.iamnicknack.slc.core.index.BucketUpdateOperations;
import io.github.iamnicknack.slc.core.index.MapDomainOperations;
import io.github.iamnicknack.slc.core.test.TestData;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DefaultPagedQueryExecutorTest {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Test
    void test() throws IOException {
        countPagesWithSize(1, 3, 10);
        countPagesWithSize(2, 3, 2);
    }

    @Test
    void returnsCorrectPageCount() {
         IntStream.rangeClosed(1, 10)
                 .forEach(numDocs -> IntStream.of(2, 5, 10)
                         .forEach(pageSize -> {
                             int expected = pageSize > numDocs
                                     ? 1
                                     : (int)Math.ceil((double)numDocs / (double)pageSize);
                             try {
                                 countPagesWithSize(expected, numDocs, pageSize);
                             } catch (IOException e) {
                                 Assertions.fail(e.getMessage());
                             }
                         })
                 );
    }

    private void countPagesWithSize(int expectedPageCount, int numDocs, int pageSize) throws IOException {
        var backend = LuceneBackends.memory();
        var domainOperations = new MapDomainOperations(TestData.documentDescriptor(backend));
        var updateOperations = new BucketUpdateOperations<>(domainOperations);

        var list = IntStream.range(0, numDocs)
                .mapToObj(i -> TestData.createValue(Integer.toString(i), i))
                .toList();

        try(var lease = backend.updateLeaseFactory().lease()) {
            lease.execute(updateOperations.addAll(list));
        }

        try(var lease = backend.searcherLeaseFactory().lease()) {
            var count = lease.execute(leasedValue -> leasedValue.indexSearcher().count(new MatchAllDocsQuery()));
            assertEquals(numDocs, count);
        }

        var pagedQuery = new DefaultPagedQueryExecutor<>(ignored -> new MatchAllDocsQuery(), backend.searcherLeaseFactory())
                .withOptions(() -> pageSize);

        try(var result = pagedQuery.execute(null)) {
            var counter = new AtomicInteger(0);
            result.forEach(ignored -> counter.incrementAndGet());

            var hits = result.stream().toList();
            assertEquals(numDocs, hits.size());
            assertEquals(expectedPageCount, counter.get());
        }
    }


}