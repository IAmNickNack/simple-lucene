package io.github.iamnicknack.slc.api.backend;

import io.github.iamnicknack.slc.api.backend.LuceneBackend.SearchComponents;
import io.github.iamnicknack.slc.api.backend.LuceneBackend.UpdateComponents;
import io.github.iamnicknack.slc.api.lease.LeaseFactory;
import io.github.iamnicknack.slc.api.lease.LeaseFactory.LeaseSupplier;
import io.github.iamnicknack.slc.api.lease.LeaseFactory.ReleaseConsumer;
import org.apache.lucene.facet.FacetsConfig;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class LuceneBackendTest {


    @Test
    @SuppressWarnings({"unchecked", "resource"})
    void searchClosesLease() throws Exception {
        SearchComponents searchComponents = mock(SearchComponents.class);
        LeaseSupplier<SearchComponents> searchSupplier = () -> searchComponents;
        ReleaseConsumer<SearchComponents> searchConsumer = mock(ReleaseConsumer.class);
        LeaseFactory<SearchComponents> searchComponentsLeaseFactory = LeaseFactory.create(searchSupplier, searchConsumer);

        LuceneBackend backend = new LuceneBackend() {
            @Override
            public LeaseFactory<SearchComponents> searcherLeaseFactory() {
                return searchComponentsLeaseFactory;
            }

            @Override
            public LeaseFactory<UpdateComponents> updateLeaseFactory() {
                return null;
            }

            @Override
            public FacetsConfig facetsConfig() {
                return null;
            }

            @Override
            public void close() {
            }
        };

        backend.search(leasedValue -> "ignored");

        verify(searchConsumer).release(searchComponents);
    }

    @Test
    @SuppressWarnings({"unchecked", "resource"})
    void updateClosesLease() throws Exception {
        UpdateComponents updateComponents = mock(UpdateComponents.class);
        LeaseSupplier<UpdateComponents> updateSupplier = () -> updateComponents;
        ReleaseConsumer<UpdateComponents> updateConsumer = mock(ReleaseConsumer.class);
        LeaseFactory<UpdateComponents> updateComponentsLeaseFactory = LeaseFactory.create(updateSupplier, updateConsumer);

        LuceneBackend backend = new LuceneBackend() {
            @Override
            public LeaseFactory<SearchComponents> searcherLeaseFactory() {
                return null;
            }

            @Override
            public LeaseFactory<UpdateComponents> updateLeaseFactory() {
                return updateComponentsLeaseFactory;
            }

            @Override
            public FacetsConfig facetsConfig() {
                return null;
            }

            @Override
            public void close() {
            }
        };

        backend.update(leasedValue -> "ignored");
        verify(updateConsumer).release(updateComponents);
    }

}