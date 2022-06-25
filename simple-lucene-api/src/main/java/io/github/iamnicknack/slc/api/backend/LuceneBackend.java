package io.github.iamnicknack.slc.api.backend;

import io.github.iamnicknack.slc.api.lease.Lease;
import io.github.iamnicknack.slc.api.lease.LeaseFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.facet.taxonomy.SearcherTaxonomyManager;
import org.apache.lucene.facet.taxonomy.TaxonomyReader;
import org.apache.lucene.facet.taxonomy.TaxonomyWriter;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.IndexSearcher;

import java.io.Closeable;
import java.io.IOException;

/**
 * Components required to read to and write from Lucene.
 *
 * <p>Leases provided via an instance can be used to manage the underlying Lucene resource required for
 * read-write operations.</p>
 *
 * <p>Leased components include a reference to the {@link #facetsConfig()} which can be used at startup
 * to specify facet dimensions and their mappings to index field names</p>
 */
public interface LuceneBackend extends Closeable {

    /**
     * {@link LeaseFactory} for components required for query operations.
     * The supplied lease will have responsibility for obtaining and releasing underlying Lucene resources.
     */
    LeaseFactory<SearchComponents> searcherLeaseFactory();

    /**
     * {@link LeaseFactory} for components required for update operations.
     * The supplied lease will have responsibility for obtaining and releasing underlying Lucene resources.
     * <p>{@link Lease} implementations returned by this method can implicitly
     * call {@link IndexWriter#commit()}, {@link TaxonomyWriter#commit()} and
     * {@link SearcherTaxonomyManager#maybeRefresh()}</p>, allowing a single {@link Lease} instance
     * to perform in a transaction-like manner
     */
    LeaseFactory<UpdateComponents> updateLeaseFactory();

    /**
     * The Lucene facets configuration
     */
    FacetsConfig facetsConfig();

    /**
     * Execute the {@link io.github.iamnicknack.slc.api.lease.Lease.LeaseFunction} via
     * {@link #searcherLeaseFactory()}
     * @param searchFunction the function to execute
     * @return the function result
     * @param <T> the result type
     */
    default <T> T search(Lease.LeaseFunction<SearchComponents, T> searchFunction) {
        try(var lease = searcherLeaseFactory().lease()) {
            return lease.execute(searchFunction);
        }
    }

    /**
     * Execute the {@link io.github.iamnicknack.slc.api.lease.Lease.LeaseFunction} via
     * {@link #updateLeaseFactory()}
     * @param updateFunction the function to execute
     * @return the function result
     * @param <T> the result type
     */
    default <T> T update(Lease.LeaseFunction<UpdateComponents, T> updateFunction) {
        try(var lease = updateLeaseFactory().lease()) {
            return lease.execute(updateFunction);
        }
    }

    /**
     * Components required query operations
     */
    interface SearchComponents {
        /**
         * Used to perform search operations
         */
        IndexSearcher indexSearcher();

        /**
         * Used with {@link #facetsConfig()} to perform facet queries
         */
        TaxonomyReader taxonomyReader();
        /**
         * Used with {@link #taxonomyReader()} to perform facet queries
         */
        FacetsConfig facetsConfig();
    }

    /**
     * Components required for update operations
     */
    interface UpdateComponents {
        /**
         * Used to write changes to the index
         */
        IndexWriter indexWriter();

        /**
         * Used with {@link #facetsConfig()} to update taxonomy facets during update operations
         */
        TaxonomyWriter taxonomyWriter();
        /**
         * Used with {@link #taxonomyWriter()} to update taxonomy facets during update operations
         */
        FacetsConfig facetsConfig();

        /**
         * Utility to update taxonomy during document write operations
         * @param document the document being written
         */
        default Document build(Document document) throws IOException {
            return facetsConfig().build(taxonomyWriter(), document);
        }
    }
}
