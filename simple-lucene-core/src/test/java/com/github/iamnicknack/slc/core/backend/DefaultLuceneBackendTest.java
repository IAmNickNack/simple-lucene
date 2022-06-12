package com.github.iamnicknack.slc.core.backend;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.facet.taxonomy.SearcherTaxonomyManager;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyWriter;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

class DefaultLuceneBackendTest {

    @Test
    void test() throws IOException {
        var config = new IndexWriterConfig(new StandardAnalyzer())
                .setOpenMode(IndexWriterConfig.OpenMode.CREATE);

        var indexWriter = new IndexWriter(new ByteBuffersDirectory(), config);
        var taxonomyWriter = new DirectoryTaxonomyWriter(new ByteBuffersDirectory());
        var searcherTaxonomyManager = new SearcherTaxonomyManager(indexWriter, null, taxonomyWriter);

        var backend = new DefaultLuceneBackend(
                indexWriter,
                taxonomyWriter,
                new FacetsConfig(),
                searcherTaxonomyManager
        );

        backend.close();

        Assertions.assertFalse(indexWriter.isOpen());

    }
}