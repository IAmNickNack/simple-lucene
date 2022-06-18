package io.github.iamnicknack.slc.examples.country;

import io.github.iamnicknack.slc.annotation.AnnotatedRecordOperations;
import io.github.iamnicknack.slc.api.query.Result;
import io.github.iamnicknack.slc.core.backend.LuceneBackends;
import io.github.iamnicknack.slc.core.collection.LuceneCollection;
import io.github.iamnicknack.slc.core.query.DefaultQueryExecutor;
import io.github.iamnicknack.slc.core.query.QueryFactories;
import io.github.iamnicknack.slc.examples.data.country.Country;
import io.github.iamnicknack.slc.examples.data.country.CountryData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class Simple {

    static Logger logger = LoggerFactory.getLogger("country-search");

    public static void main(String[] args) throws IOException {

        var backend = LuceneBackends.memory();

        /*
         ***************************************************************************************************************
         * Indexing
         */

        var countryOperations = AnnotatedRecordOperations.create(Country.class, backend);
        var countryCollection = new LuceneCollection<>(countryOperations, backend);
        countryCollection.addAll(CountryData.countries());

        /*
         ***************************************************************************************************************
         * Querying
         */

        // Query factory to create Lucene queries from user input
        // This could generate a more complex query templating filters, boost parameters, etc
        var queryFactory = QueryFactories.text("_all");

        // Executor to provide `Result`
        var queryExecutor = new DefaultQueryExecutor<>(queryFactory, backend.searcherLeaseFactory())
                .withIterator(Result.IteratorFactory.mapping(countryOperations::readDocument));


        /*
         ***************************************************************************************************************
         * Execution
         */

        var queryText = (args.length > 0)
                ? args[0]
                : "France";

        // Execute via the executor and iterate results
        try(var result = queryExecutor.execute(queryText)) {
            var count = result.totalHits();

            result.forEach(countryHit ->
                    logger.info("Score: {}, Hit: {}", countryHit.score(), countryHit.value())
            );
        }

        backend.close();
    }
}
