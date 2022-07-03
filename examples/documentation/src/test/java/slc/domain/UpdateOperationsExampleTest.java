package slc.domain;

import io.github.iamnicknack.slc.api.backend.LuceneBackend;
import io.github.iamnicknack.slc.api.index.DomainOperations;
import io.github.iamnicknack.slc.api.index.UpdateOperations;
import io.github.iamnicknack.slc.api.lease.Lease.LeaseFunction;
import io.github.iamnicknack.slc.api.query.Hit;
import io.github.iamnicknack.slc.api.query.QueryExecutor;
import io.github.iamnicknack.slc.api.query.QueryFactory;
import io.github.iamnicknack.slc.core.collection.LuceneCollection;
import io.github.iamnicknack.slc.core.index.BucketUpdateOperations;
import name.falgout.jeffrey.testing.junit.guice.GuiceExtension;
import name.falgout.jeffrey.testing.junit.guice.IncludeModule;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import slc.domain.modules.BackendModule;
import slc.domain.modules.DataModule;
import slc.domain.modules.QueriesModule;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(GuiceExtension.class)
@IncludeModule({BackendModule.class, DataModule.class, QueriesModule.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class UpdateOperationsExampleTest {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @BeforeEach
    void beforeEach(LuceneBackend backend) {
        backend.update(components -> components.indexWriter().deleteAll());
    }

    @AfterAll
    void afterAll(BackendModule.ShutdownHook shutdownHook) throws IOException {
        shutdownHook.shutDown();
    }

    /**
     * Example which uses a {@link LeaseFunction} constructed by {@link UpdateOperations} to specify update
     * operations to be executed.
     *
     * <p>A {@link LeaseFunction} is then passed to {@link LuceneBackend} to be executed</p>
     *
     * @param backend the Lucene backend
     * @param operations marshalling operations
     * @param countries example data
     */
    @Test
    void insert(LuceneBackend backend,
                DomainOperations<ShortCountry> operations,
                List<ShortCountry> countries) {

        /*
        Bucket-like operations which allow data to be inserted without duplicate checks
         */
        var updateOperations = new BucketUpdateOperations<>(operations);
        /*
        Function which, when executed inserts the entire collection of `countries`
         */
        var insertAllOperation = updateOperations.addAll(countries);
        /*
        Execute the operation via a `Lease` of `UpdateComponents`
         */
        var insertAllResult = backend.update(insertAllOperation);

        /*
        Unit test validation to check that the index document count matches that of the inserted data
         */
        assertThat(insertAllResult).isEqualTo(countries.size());
        /*
        Use the backend to perform a count operation with the Lucene API
         */
        var count = backend.search(components -> components.indexSearcher().count(new MatchAllDocsQuery()));
        assertThat(count).isEqualTo(countries.size());
    }

    /**
     * Example of updating data using the {@link LuceneBackend} to execute operations
     * created using {@link UpdateOperations}.
     *
     * @param updateOperations operation factories actually used to perform the update
     * @param backend the lucene backend
     * @param isoTemplate the query executor to provide search results for ISO code searches
     * @param luceneCollection the collection used to create the test dataset
     * @param countries the source test dataset
     */
    @Test
    void update(UpdateOperations<ShortCountry> updateOperations,
                LuceneBackend backend,
                @QueriesModule.ISO QueryExecutor<String, ShortCountry> isoTemplate,
                LuceneCollection<ShortCountry> luceneCollection,
                List<ShortCountry> countries) {

        luceneCollection.addAll(countries);

        /*
        Read any values containing an ISO code of `-99`. Update these values to `INVALID` and create
        a list to be used to update the index
         */
        var updatedCountries = isoTemplate.execute("-99").stream()
                // Not interested in the `Hit` container returned by the query. Just take the value
                .map(Hit::value)
                .map(country -> country.withIso("INVALID"))
                .toList();
        assertThat(updatedCountries).hasSize(5);

        /*
        Create the operation which will perform the update
         */
        var updateOperation = updateOperations.updateAll(updatedCountries);
        /*
        Execute the operation and keep the update count
         */
        var updateCount = backend.update(updateOperation);
        assertThat(updateCount).isEqualTo(5);
    }


    /**
     * Example of delete data using the {@link LuceneBackend} to execute operations
     * created using {@link UpdateOperations}.
     *
     * @param updateOperations operation factories actually used to perform the update
     * @param backend the lucene backend
     * @param isoTemplate the query executor to provide search results for ISO code searches
     * @param findByIsoQueryFactory query factory used by isoTemplate
     * @param luceneCollection the collection used to create the test dataset
     * @param countries the source test dataset
     */
    @Test
    void delete(UpdateOperations<ShortCountry> updateOperations,
                LuceneBackend backend,
                @QueriesModule.ISO QueryExecutor<String, ShortCountry> isoTemplate,
                @QueriesModule.ISO QueryFactory<String> findByIsoQueryFactory,
                LuceneCollection<ShortCountry> luceneCollection,
                List<ShortCountry> countries) {

        luceneCollection.addAll(countries);

        /*
        Create a collection of any values containing an ISO code of `-99`.
         */
        var countriesToDelete = isoTemplate.execute("-99").stream()
                // Not interested in the `Hit` container returned by the query. Just take the value
                .map(Hit::value)
                .toList();
        assertThat(countriesToDelete).hasSize(5);

        /*
        Create the operation which will perform deletions in a transaction-like manner
         */
        var updateOperation = updateOperations.deleteAll(countriesToDelete);
        /*
        Execute the operation and keep the result count
         */
        var updateCount = backend.update(updateOperation);
        assertThat(updateCount).isEqualTo(5);

        /*
        Perform a count to assert that deletion was successful
         */
        var countOperation = (LeaseFunction<LuceneBackend.SearchComponents, Integer>) components ->
                components.indexSearcher().count(findByIsoQueryFactory.query("-99"));
        /*
        Execute the count
         */
        var remainingCount = backend.search(countOperation);
        assertThat(remainingCount).isEqualTo(0);
    }
}
