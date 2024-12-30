package slc.domain;

import io.github.iamnicknack.slc.api.backend.LuceneBackend;
import io.github.iamnicknack.slc.api.index.UpdateOperations;
import io.github.iamnicknack.slc.api.lease.Lease.LeaseFunction;
import io.github.iamnicknack.slc.api.query.Hit;
import io.github.iamnicknack.slc.core.index.BucketUpdateOperations;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class UpdateOperationsExampleTest {

    private final TestComponent components = DaggerTestComponent.create();

    @BeforeEach
    void beforeEach() {
        components.luceneBackend().update(components -> components.indexWriter().deleteAll());
    }

    @AfterAll
    void afterAll() throws IOException {
        components.shutdownHook().shutDown();
    }

    /**
     * Example which uses a {@link LeaseFunction} constructed by {@link UpdateOperations} to specify update
     * operations to be executed.
     *
     * <p>A {@link LeaseFunction} is then passed to {@link LuceneBackend} to be executed</p>
     */
    @Test
    void insert() {

        /*
        Bucket-like operations which allow data to be inserted without duplicate checks
         */
        var updateOperations = new BucketUpdateOperations<>(components.domainOperations());
        /*
        Function which, when executed inserts the entire collection of `countries`
         */
        var insertAllOperation = updateOperations.addAll(components.countries());
        /*
        Execute the operation via a `Lease` of `UpdateComponents`
         */
        var insertAllResult = components.luceneBackend().update(insertAllOperation);

        /*
        Unit test validation to check that the index document count matches that of the inserted data
         */
        assertThat(insertAllResult).isEqualTo(components.countries().size());
        /*
        Use the backend to perform a count operation with the Lucene API
         */
        var count = components.luceneBackend().search(components -> components.indexSearcher().count(new MatchAllDocsQuery()));
        assertThat(count).isEqualTo(components.countries().size());
    }

    /**
     * Example of updating data using the {@link LuceneBackend} to execute operations
     * created using {@link UpdateOperations}.
     */
    @Test
    void update() {

        components.luceneCollection().addAll(components.countries());

        /*
        Read any values containing an ISO code of `-99`. Update these values to `INVALID` and create
        a list to be used to update the index
         */
        var updatedCountries = components.isoTemplate().execute("-99").stream()
                // Not interested in the `Hit` container returned by the query. Just take the value
                .map(Hit::value)
                .map(country -> country.withIso("INVALID"))
                .toList();
        assertThat(updatedCountries).hasSize(5);

        /*
        Create the operation which will perform the update
         */
        var updateOperation = components.updateOperations().updateAll(updatedCountries);
        /*
        Execute the operation and keep the update count
         */
        var updateCount = components.luceneBackend().update(updateOperation);
        assertThat(updateCount).isEqualTo(5);
    }


    /**
     * Example of delete data using the {@link LuceneBackend} to execute operations
     * created using {@link UpdateOperations}.
     */
    @Test
    void delete() {

        components.luceneCollection().addAll(components.countries());

        /*
        Create a collection of any values containing an ISO code of `-99`.
         */
        var countriesToDelete = components.isoTemplate().execute("-99").stream()
                // Not interested in the `Hit` container returned by the query. Just take the value
                .map(Hit::value)
                .toList();
        assertThat(countriesToDelete).hasSize(5);

        /*
        Create the operation which will perform deletions in a transaction-like manner
         */
        var updateOperation = components.updateOperations().deleteAll(countriesToDelete);
        /*
        Execute the operation and keep the result count
         */
        var updateCount = components.luceneBackend().update(updateOperation);
        assertThat(updateCount).isEqualTo(5);

        /*
        Perform a count to assert that deletion was successful
         */
        var countOperation = (LeaseFunction<LuceneBackend.SearchComponents, Integer>) searchComponents ->
                searchComponents.indexSearcher().count(components.findByIsoQueryFactory().query("-99"));
        /*
        Execute the count
         */
        var remainingCount = components.luceneBackend().search(countOperation);
        assertThat(remainingCount).isEqualTo(0);
    }
}
