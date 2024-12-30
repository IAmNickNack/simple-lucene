package slc.domain;

import dagger.Component;
import io.github.iamnicknack.slc.api.backend.LuceneBackend;
import io.github.iamnicknack.slc.api.index.DomainOperations;
import io.github.iamnicknack.slc.api.index.UpdateOperations;
import io.github.iamnicknack.slc.api.query.QueryExecutor;
import io.github.iamnicknack.slc.api.query.QueryFactory;
import io.github.iamnicknack.slc.core.collection.LuceneCollection;
import jakarta.inject.Singleton;
import slc.domain.modules.BackendModule;
import slc.domain.modules.DataModule;
import slc.domain.modules.QueriesModule;

import java.util.List;

@Component(modules = {BackendModule.class, DataModule.class, QueriesModule.class})
@Singleton
interface TestComponent {
    LuceneBackend luceneBackend();
    BackendModule.ShutdownHook shutdownHook();
    UpdateOperations<ShortCountry> updateOperations();
    List<ShortCountry> countries();
    LuceneCollection<ShortCountry> luceneCollection();

    DomainOperations<ShortCountry> domainOperations();

    @QueriesModule.SearchAll
    CountrySearch countrySearch();
    @QueriesModule.ISO
    CountryLookup countryLookup();
    @QueriesModule.ISO
    QueryExecutor<String, ShortCountry> isoTemplate();
    @QueriesModule.ISO
    QueryFactory<String> findByIsoQueryFactory();

    default void load() {
        luceneBackend().update(updateOperations().addAll(countries()));
    }
}
