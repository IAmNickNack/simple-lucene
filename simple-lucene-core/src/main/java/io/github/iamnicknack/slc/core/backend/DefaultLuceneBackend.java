package io.github.iamnicknack.slc.core.backend;

import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.facet.taxonomy.SearcherTaxonomyManager;
import org.apache.lucene.facet.taxonomy.TaxonomyReader;
import org.apache.lucene.facet.taxonomy.TaxonomyWriter;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.IndexSearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.iamnicknack.slc.api.backend.LuceneBackend;
import io.github.iamnicknack.slc.api.lease.LeaseFactory;

import java.io.IOException;

/**
 * Default implementation of {@link LuceneBackend} that constructs {@link LeaseFactory} instances
 * using provided Lucene components.
 */
public class DefaultLuceneBackend implements LuceneBackend {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final LeaseFactory<SearchComponents> searcherLeaseFactory;
    private final LeaseFactory<UpdateComponents> updateLeaseFactory;

    private final IndexWriter indexWriter;
    private final TaxonomyWriter taxonomyWriter;
    private final FacetsConfig facetsConfig;
    private final SearcherTaxonomyManager searcherTaxonomyManager;

    @SuppressWarnings("resource")
    public DefaultLuceneBackend(IndexWriter indexWriter,
                                TaxonomyWriter taxonomyWriter,
                                FacetsConfig facetsConfig,
                                SearcherTaxonomyManager searcherTaxonomyManager) {

        this.indexWriter = indexWriter;
        this.taxonomyWriter = taxonomyWriter;
        this.facetsConfig = facetsConfig;
        this.searcherTaxonomyManager = searcherTaxonomyManager;

        this.searcherLeaseFactory = LeaseFactory.create(
                () -> new SearchComponentsRecord(searcherTaxonomyManager.acquire(), facetsConfig),
                value -> searcherTaxonomyManager.release(((SearchComponentsRecord)value).searcherAndTaxonomy())
        );

        this.updateLeaseFactory = LeaseFactory.create(
                () -> new UpdateComponentsRecord(
                        indexWriter,
                        taxonomyWriter,
                        facetsConfig
                ),
                components -> {
                    components.indexWriter().commit();
                    components.taxonomyWriter().commit();
                    searcherTaxonomyManager.maybeRefresh();
                }
        );
    }

    @Override
    public FacetsConfig facetsConfig() {
        return facetsConfig;
    }

    @Override
    public LeaseFactory<SearchComponents> searcherLeaseFactory() {
        return searcherLeaseFactory;
    }

    @Override
    public LeaseFactory<UpdateComponents> updateLeaseFactory() {
        return updateLeaseFactory;
    }

    @Override
    public void close() throws IOException {
        indexWriter.flush();
        indexWriter.forceMerge(1, true);

        searcherTaxonomyManager.close();
        try {
            indexWriter.close();
        } catch (IOException e) {
            logger.warn("Failed to close index writer: {}", e.getMessage(), e);
        }

        try {
            taxonomyWriter.close();
        } catch (IOException e) {
            logger.warn("Failed to close taxonomy writer: {}", e.getMessage(), e);
        }
    }

    private record SearchComponentsRecord(SearcherTaxonomyManager.SearcherAndTaxonomy searcherAndTaxonomy,
                                          FacetsConfig facetsConfig) implements SearchComponents {

        @Override
        public IndexSearcher indexSearcher() {
            return searcherAndTaxonomy.searcher;
        }

        @Override
        public TaxonomyReader taxonomyReader() {
            return searcherAndTaxonomy.taxonomyReader;
        }
    }

    private record UpdateComponentsRecord(IndexWriter indexWriter,
                                          TaxonomyWriter taxonomyWriter,
                                          FacetsConfig facetsConfig) implements UpdateComponents {
    }
}
