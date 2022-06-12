package com.github.iamnicknack.slc.examples.country;

import com.github.iamnicknack.slc.annotation.AnnotatedRecordOperations;
import com.github.iamnicknack.slc.api.query.Result;
import com.github.iamnicknack.slc.core.backend.LuceneBackends;
import com.github.iamnicknack.slc.core.collection.LuceneCollection;
import com.github.iamnicknack.slc.core.query.DefaultQueryExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.github.iamnicknack.slc.core.query.QueryFactories;
import com.github.iamnicknack.slc.examples.data.country.Country;
import com.github.iamnicknack.slc.examples.data.country.CountryData;

import java.io.IOException;
import java.nio.file.Path;

public class Simple {

    static Logger logger = LoggerFactory.getLogger("country-search");

    public static void main(String[] args) throws IOException {

        var backend = LuceneBackends.directory(Path.of("./test-indices/country/simple"));
//        var backend = LuceneBackends.memory();

        /*
         ***************************************************************************************************************
         * Indexing
         */

        var countryOperations = AnnotatedRecordOperations.create(Country.class, backend);
        var countryCollection = new LuceneCollection<>(countryOperations, backend);
        if(countryCollection.isEmpty()) {
            // Use collections to add data to the index
            countryCollection.addAll(CountryData.countries());
        }

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
