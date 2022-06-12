package io.github.iamnicknack.slc.examples.country;

import io.github.iamnicknack.slc.annotation.AnnotatedRecordOperations;
import io.github.iamnicknack.slc.api.query.QueryExecutor;
import io.github.iamnicknack.slc.api.query.QueryFactory;
import io.github.iamnicknack.slc.api.query.Result;
import io.github.iamnicknack.slc.core.backend.LuceneBackends;
import io.github.iamnicknack.slc.core.collection.LuceneCollection;
import io.github.iamnicknack.slc.core.query.DefaultQueryExecutor;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.iamnicknack.slc.api.backend.LuceneBackend;
import io.github.iamnicknack.slc.core.query.QueryFactories;
import io.github.iamnicknack.slc.examples.data.country.Country;
import io.github.iamnicknack.slc.examples.data.country.CountryData;
import io.github.iamnicknack.slc.examples.data.country.Place;
import io.github.iamnicknack.slc.examples.data.country.Sourceable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

public class Main {

    static Logger logger = LoggerFactory.getLogger("country-search");

    public static void main(String[] args) throws IOException {

        var backend = LuceneBackends.memory();

        // Countries
        var countryOperations = AnnotatedRecordOperations.create(Country.class, backend);
        // using a Collection to insert data into the index
        var countryCollection = new LuceneCollection<>(countryOperations, backend);
        // Add countries to the index
        countryCollection.addAll(CountryData.countries());
        countryCollection.addAll(CountryData.mapUnits50m());

        // Places
        var placeOperations = AnnotatedRecordOperations.create(Place.class, backend);
        var placeCollection = new LuceneCollection<>(placeOperations, backend);
        placeCollection.addAll(CountryData.places());

        var queryFactory = new QueryFactory<String>() {
            final QueryFactory<String> text = QueryFactories.text("_all");

            @Override
            public Query query(String value) {
                var builder = new BooleanQuery.Builder()
                        .add(text.query(value), BooleanClause.Occur.MUST)
                        .add(new BoostQuery(new TermQuery(new Term("source.keyword", "ne_50m_admin_0_map_units.json")), 2f), BooleanClause.Occur.SHOULD)
                        .add(new BoostQuery(new TermQuery(new Term("source.keyword", "ne_110m_populated_places_simple.json")), 1.5f), BooleanClause.Occur.SHOULD)
                        .setMinimumNumberShouldMatch(0);
                return builder.build();
            }
        };

        var queryExecutor = new DefaultQueryExecutor<>(queryFactory, backend.searcherLeaseFactory())
                .withIterator(Result.IteratorFactory.mapping(document -> document.get("source").contains("place")
                        ? placeOperations.readDocument(document)
                        : (Sourceable<?>)countryOperations.readDocument(document)));

        if(args.length == 0) {
            System.out.print("> ");
            String input;
            while ((input = new BufferedReader(new InputStreamReader(System.in)).readLine()) != null) {
                if(input.trim().length() == 0) {
                    break;
                }
                doSearch(input, queryExecutor);
                System.out.print("> ");
            }
        }
        else {
            doSearch(args[0], queryExecutor);
        }

        dumpIndexSummary(backend);

        backend.close();
    }

    static void doSearch(String input, QueryExecutor<String, Sourceable<?>> queryExecutor) {
        logger.info("Searching for: {}", input);
        long count;
        try(var result = queryExecutor.execute(input)) {
            count = result.totalHits();
            result.forEach(countryHit -> logger.info("Score: {}, Hit: {}", countryHit.score(), countryHit.value()));
        }
        logger.info("Found {} documents", count);
    }

    static void dumpIndexSummary(LuceneBackend backend) {
        try(var lease = backend.updateLeaseFactory().lease()) {
            lease.execute(leasedValue -> {
                Arrays.stream(leasedValue.indexWriter().getDirectory().listAll())
                        .forEach(filename -> {
                            try {
                                long length = leasedValue.indexWriter().getDirectory().fileLength(filename);
                                logger.info("Lucene index file: {}: {} bytes", filename, length);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });

                long total = Arrays.stream(leasedValue.indexWriter().getDirectory().listAll())
                        .map(filename -> {
                            try {
                                return leasedValue.indexWriter().getDirectory().fileLength(filename);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        })
                        .reduce(0L, Long::sum);

                logger.info("Total index size: {} bytes ({} kB)", total, total/1024D);
                return null;
            });
        }
    }
}
