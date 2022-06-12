package fuzzyjoin;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.iamnicknack.slc.annotation.AnnotatedRecordOperations;
import io.github.iamnicknack.slc.api.document.DocumentDescriptor;
import io.github.iamnicknack.slc.api.query.QueryFactory;
import io.github.iamnicknack.slc.core.backend.LuceneBackends;
import io.github.iamnicknack.slc.core.collection.LuceneCollection;
import io.github.iamnicknack.slc.core.document.FieldDescriptorBuilder;
import io.github.iamnicknack.slc.core.index.CollectionLikeUpdateOperations;
import io.github.iamnicknack.slc.core.index.MapDomainOperations;
import io.github.iamnicknack.slc.core.query.QueryFactories;
import io.github.iamnicknack.slc.examples.data.country.Country;
import io.github.iamnicknack.slc.examples.data.country.CountryData;
import io.github.iamnicknack.slc.examples.data.country.Place;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger("fuzzy-join");

    public static void main(String[] args) throws Exception {
        var placesBacked = LuceneBackends.memory();
        var placeOperations = AnnotatedRecordOperations.create(Place.class, placesBacked);
        var placeCollection = new LuceneCollection<>(placeOperations, placesBacked);
        placeCollection.addAll(CountryData.places10m());
        logger.info("Loaded {} places", placeCollection.size());

        var countriesBacked = LuceneBackends.memory();
        var countriesOperations = AnnotatedRecordOperations.create(Country.class, countriesBacked);
        var countriesCollection = new LuceneCollection<>(countriesOperations, countriesBacked);
        countriesCollection.addAll(CountryData.mapUnits());
        logger.info("Loaded {} countries", countriesCollection.size());



        /*

         */
        var countryFieldMap = new HashMap<>(countriesOperations.documentDescriptor().fieldMap());
        countryFieldMap.put("place", new FieldDescriptorBuilder()
                .name("place")
                .exclude()
                .multiValue()
                .stringField()
                .build()
        );
        var countryMapDescriptor = (DocumentDescriptor) () -> countryFieldMap;
        var countryMapOperations = new MapDomainOperations(countryMapDescriptor) {
            @Override
            public String id(Map<String, Object> value) {
                return countriesOperations.id(Country.id((String)value.get("name"), (String)value.get("sovereign"), (String)value.get("source")));
            }
        };
        var countryMapCollection = new LuceneCollection<>(countryMapOperations, countriesBacked);
        assert countryMapCollection.size() == countriesCollection.size();

        var countryUpdateOperations = new CollectionLikeUpdateOperations<>(countryMapOperations, countriesBacked);
        var placeIsoQuery = QueryFactories.keyword("iso2.keyword");
        var updateCountries = countryMapCollection.stream()
                .peek(countryAsMap -> {
                    var places = placeCollection.query(placeIsoQuery.query((String)countryAsMap.get("iso2")))
                            .stream()
                            .map(placeHit -> placeHit.value().name())
                            .toList();
                    countryAsMap.put("place", places);
                    countryAsMap.put("place_count", places.size());
                })
                .toList();

        try(var lease = countriesBacked.updateLeaseFactory().lease()) {
            lease.execute(countryUpdateOperations.updateAll(updateCountries));
        }

        record CountryPlaceQuery(String country, String place) {}
        QueryFactory<CountryPlaceQuery> countryPlaceQueryFactory = new QueryFactory<>() {
            private final QueryFactory<String> all = QueryFactories.text("_all");
            private final QueryFactory<String> place = QueryFactories.text("place");

            @Override
            public Query query(CountryPlaceQuery value) {
                return new BooleanQuery.Builder()
                        .add(all.query(value.country), BooleanClause.Occur.SHOULD)
                        .add(new BoostQuery(place.query(value.place), 1.5f), BooleanClause.Occur.SHOULD)
                        .build();
            }
        };

        var testRecords = List.of(
                new CountryPlaceQuery("Hungary (now Slovakia)", "Pressburg (now Bratislava)"),
                new CountryPlaceQuery("Austria-Hungary (now Poland)", "Rymanow"),
                new CountryPlaceQuery("Russian Empire (now Azerbaijan)", "Baku"),
                new CountryPlaceQuery("Germany (now Poland)", "Kattowitz (now Katowice)"),
                new CountryPlaceQuery("Germany (now France)", "Strasbourg"),
                new CountryPlaceQuery("Germany (now France)", "Kaysersberg"),
                new CountryPlaceQuery("Germany (now France)", "Guebwiller"),
                new CountryPlaceQuery("Austria-Hungary (now Slovenia)", "Laibach (now Ljubljana)"),
                new CountryPlaceQuery("Germany (now Poland)", "Goldschmieden, near Breslau"),
                new CountryPlaceQuery("Austria-Hungary (now Croatia)", "Vukovar"),
                new CountryPlaceQuery("Austria-Hungary (now Croatia)", "Budapest"),
                new CountryPlaceQuery("Austria-Hungary (now Czech Republic)", "Prague")
        );

        testRecords.forEach(countryPlace -> countryMapCollection.queryFirst(countryPlaceQueryFactory.query(countryPlace))
                .ifPresent(mapHit -> logger.info("[{}] Found {} from {}", mapHit.score(), mapHit.value().get("name"), countryPlace)));

    }

}
