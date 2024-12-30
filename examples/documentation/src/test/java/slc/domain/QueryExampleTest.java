package slc.domain;

import io.github.iamnicknack.slc.api.query.QueryFactory;
import io.github.iamnicknack.slc.api.query.QueryOptions;
import io.github.iamnicknack.slc.api.query.Result;
import io.github.iamnicknack.slc.core.query.DefaultPagedQueryExecutor;
import io.github.iamnicknack.slc.core.query.DefaultQueryExecutor;
import io.github.iamnicknack.slc.core.query.QueryException;
import io.github.iamnicknack.slc.core.query.QueryFactories;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.TermQuery;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class QueryExampleTest {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final TestComponent components = DaggerTestComponent.create();

    @BeforeAll
    void beforeAll() {
        components.load();
    }

    @AfterAll
    void afterAll() throws IOException {
        components.shutdownHook().shutDown();
    }

    @Test
    void textSearch() {
        QueryParser parser = new QueryParser("_all", new StandardAnalyzer());
        QueryFactory<String> queryByName = value -> {
            try {
                return parser.parse(value);
            } catch (ParseException e) {
                throw new QueryException("Failed to parse query", e);
            }
        };

        var queryExecutor = new DefaultQueryExecutor<>(queryByName, components.luceneBackend().searcherLeaseFactory());

        logger.info("Searching for \"Spain\"");
        try(var result = queryExecutor.execute("Spain")) {
            result.forEach(hit -> logger.info("[{}] Hit: {}", hit.score(), hit.value()));
            assertThat(result.totalHits()).isEqualTo(2);
        }

        logger.info("Searching for \"spain +madrid\"");
        try(var result = queryExecutor.execute("spain +madrid")) {
            result.forEach(hit -> logger.info("[{}] Hit: {}", hit.score(), hit.value()));
            assertThat(result.totalHits()).isEqualTo(1);
        }
    }

    @Test
    void keywordSearch() {
        QueryFactory<String> queryByIsoCode = value -> new TermQuery(new Term("iso.keyword", value));

        var queryExecutor = new DefaultQueryExecutor<>(queryByIsoCode, components.luceneBackend().searcherLeaseFactory())
                .withIterator(Result.IteratorFactory.mapping(components.domainOperations()::readDocument));

        try(var result = queryExecutor.execute("ESP")) {
            result.forEach(hit -> logger.info("[{}] Country: {}", hit.score(), hit.value()));
            assertThat(result.totalHits()).isEqualTo(1);
        }
    }

    @Test
    void pagingResults() {
        var queryByRegion = QueryFactories.text("region.text");
        var queryOptions = QueryOptions.DEFAULT;

        /*
        We can't know beforehand how many documents might be identified. This could be 10's, 100's, 1000's, etc, etc.
        For this reason, Lucene and therefore also the default executor return only a pre-defined maximum number of documents.
         */
        var queryExecutor = new DefaultQueryExecutor<>(queryByRegion, components.luceneBackend().searcherLeaseFactory());
        try(var stream = queryExecutor.execute("americas", queryOptions).stream()) {
            var defaultCount = stream.count();
            logger.info("Default executor found {} hits", defaultCount);
            assertThat(defaultCount).isEqualTo(queryOptions.maxHits());
        }

        /*
        Firstly, paged query execution allows all the identified results to be returned
         */
        var pagedQueryExecutor = new DefaultPagedQueryExecutor<>(queryByRegion, components.luceneBackend().searcherLeaseFactory());
        try(var stream = pagedQueryExecutor.execute("americas").stream()) {
            var pagedCount = stream.count();
            logger.info("Paged executor found {} hits", pagedCount);
            assertThat(pagedCount).isGreaterThan(queryOptions.maxHits());
        }

        /*
        A secondary benefit of paged execution is that it is possible to defer the number of documents that are
        fetched to be deferred until after execution
         */
        try(var stream = pagedQueryExecutor.execute("americas").stream()) {
            var limitedCount = stream.limit(20).count();
            logger.info("Limited result contain {} hits", limitedCount);
            assertThat(limitedCount).isEqualTo(20);
        }
    }


    @Test
    void pagedExecution() {
        var queryByRegion = QueryFactories.text("region.text");
        var queryExecutor = new DefaultPagedQueryExecutor<>(queryByRegion, components.luceneBackend().searcherLeaseFactory());

        var pageIndex = new AtomicInteger();
        var totalHits = new AtomicLong();

        try(var pages = queryExecutor.execute("americas")) {
            pages.forEach(page -> {
                var count = page.stream().count();
                logger.info("Page index: {}, documents in page: {}", pageIndex.getAndIncrement(), count);

                totalHits.addAndGet(count);
            });
        }

        assertThat(pageIndex.get()).isEqualTo(4);
        assertThat(totalHits.get()).isEqualTo(31);
    }

    @Test
    void lookupFunction() {
        var countryOptional = components.countryLookup().lookup("CAN");
        countryOptional.ifPresent(country -> logger.info("Found country: {}", country));

        assertThat(countryOptional).isPresent();
        assertThat(countryOptional.map(ShortCountry::name)).isEqualTo(Optional.of("Canada"));
    }

    @Test
    void searchFunction() {
        var countries = components.countrySearch().search("europe");
        countries.forEach(country -> logger.info("Found country: {}", country));

        assertThat(countries.size()).isGreaterThan(10);
    }
}
