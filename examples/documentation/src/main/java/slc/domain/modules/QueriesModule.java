package slc.domain.modules;

import dagger.Module;
import dagger.Provides;
import io.github.iamnicknack.slc.api.backend.LuceneBackend;
import io.github.iamnicknack.slc.api.index.DomainOperations;
import io.github.iamnicknack.slc.api.query.QueryExecutor;
import io.github.iamnicknack.slc.api.query.QueryFactory;
import io.github.iamnicknack.slc.api.query.Result;
import io.github.iamnicknack.slc.core.query.DefaultPagedQueryExecutor;
import io.github.iamnicknack.slc.core.query.DefaultQueryExecutor;
import io.github.iamnicknack.slc.core.query.QueryFactories;
import jakarta.inject.Singleton;
import slc.domain.CountryLookup;
import slc.domain.CountrySearch;
import slc.domain.ShortCountry;

import javax.inject.Qualifier;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Optional;

@Module
public class QueriesModule {

    @Singleton
    @Provides
    @ISO
    public QueryFactory<String> findByIsoQueryFactory() {
        return QueryFactories.keyword("iso.keyword");
    }

    @Singleton
    @Provides
    @ISO
    public QueryExecutor<String, ShortCountry> findByIsoCode(LuceneBackend backend,
                                                             DomainOperations<ShortCountry> domainOperations,
                                                             @ISO QueryFactory<String> isoQuery) {
        return new DefaultQueryExecutor<>(isoQuery, backend.searcherLeaseFactory())
                .withIterator(Result.IteratorFactory.mapping(domainOperations::readDocument));
    }

    @Singleton
    @Provides
    @ISO
    public CountryLookup countryByISOCodeLookup(@ISO QueryExecutor<String, ShortCountry> findByIsoCode) {
        return iso -> {
            try(var result = findByIsoCode.execute(iso)) {
                var iterator = result.iterator();
                return iterator.hasNext()
                        ? Optional.of(iterator.next().value())
                        : Optional.empty();
            }
        };
    }

    @Singleton
    @Provides
    @SearchAll
    public CountrySearch countryFinder(LuceneBackend backend,
                                       DomainOperations<ShortCountry> domainOperations) {
        var factory = QueryFactories.text("_all");
        var executor = new DefaultPagedQueryExecutor<>(factory, backend.searcherLeaseFactory());

        return phrase -> {
            try(var result = executor.execute(phrase)) {
                return result.stream()
                        .map(hit -> domainOperations.readDocument(hit.value()))
                        .toList();
            }
        };
    }

    @Qualifier
    @Retention(RetentionPolicy.RUNTIME)
    public @interface ISO {}

    @Qualifier
    @Retention(RetentionPolicy.RUNTIME)
    public @interface SearchAll {}
}

