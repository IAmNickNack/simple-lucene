package slc.domain.modules;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.github.iamnicknack.slc.annotation.AnnotatedRecordOperations;
import io.github.iamnicknack.slc.api.backend.LuceneBackend;
import io.github.iamnicknack.slc.api.index.DomainOperations;
import io.github.iamnicknack.slc.api.index.UpdateOperations;
import io.github.iamnicknack.slc.core.backend.LuceneBackends;
import io.github.iamnicknack.slc.core.collection.LuceneCollection;
import io.github.iamnicknack.slc.core.index.CollectionLikeUpdateOperations;
import slc.domain.ShortCountry;

import java.io.IOException;

public class BackendModule extends AbstractModule {

    @Override
    protected void configure() {
        try {
            bind(LuceneBackend.class).toInstance(LuceneBackends.memory());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Singleton
    @Provides
    public DomainOperations<ShortCountry> domainOperations(LuceneBackend backend) {
        return AnnotatedRecordOperations.create(ShortCountry.class, backend);
    }

    @Singleton
    @Provides
    public UpdateOperations<ShortCountry> updateOperations(DomainOperations<ShortCountry> domainOperations,
                                                           LuceneBackend backend) {
        return new CollectionLikeUpdateOperations<>(domainOperations, backend);
    }

    @Singleton
    @Provides
    public LuceneCollection<ShortCountry> luceneCollection(LuceneBackend backend,
                                                           DomainOperations<ShortCountry> domainOperations) {
        return new LuceneCollection<>(domainOperations, backend);
    }

    @Singleton
    @Provides
    public ShutdownHook shutdownHook(LuceneBackend backend) {
        return backend::close;
    }

    public interface ShutdownHook {
        void shutDown() throws IOException;
    }
}
