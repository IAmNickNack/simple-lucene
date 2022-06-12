package com.github.iamnicknack.slc.api.backend;

import com.github.iamnicknack.slc.api.lease.Lease;
import com.github.iamnicknack.slc.api.lease.LeaseFactory;
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
     * Components required query operations
     */
    interface SearchComponents {
        IndexSearcher indexSearcher();
        TaxonomyReader taxonomyReader();
        FacetsConfig facetsConfig();
    }

    /**
     * Components required for update operations
     */
    interface UpdateComponents {
        IndexWriter indexWriter();
        TaxonomyWriter taxonomyWriter();
        FacetsConfig facetsConfig();

        default Document build(Document document) throws IOException {
            return facetsConfig().build(taxonomyWriter(), document);
        }
    }
}
