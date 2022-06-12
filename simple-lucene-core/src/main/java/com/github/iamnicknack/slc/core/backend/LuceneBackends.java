package com.github.iamnicknack.slc.core.backend;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.facet.taxonomy.SearcherTaxonomyManager;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyWriter;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.NIOFSDirectory;
import com.github.iamnicknack.slc.api.backend.LuceneBackend;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Helper factories for constructing {@link LuceneBackend}s.
 */
public class LuceneBackends {

    /**
     * Configures an in-memory index using the {@link StandardAnalyzer} to process text
     * @return a backend instance
     * @throws IOException if Lucene fails to create writers
     */
    public static LuceneBackend memory() throws IOException {
        return memory(new StandardAnalyzer());
    }

    /**
     * Configures an in-memory index using the specified {@link Analyzer} to process text
     * @param analyzer the analyzer
     * @return a backend instance
     * @throws IOException if Lucene fails to create writers
     */
    public static LuceneBackend memory(Analyzer analyzer) throws IOException {

        var config = new IndexWriterConfig(analyzer)
                .setOpenMode(IndexWriterConfig.OpenMode.CREATE);

        var indexWriter = new IndexWriter(new ByteBuffersDirectory(), config);
        var taxonomyWriter = new DirectoryTaxonomyWriter(new ByteBuffersDirectory());

        return new DefaultLuceneBackend(
                indexWriter,
                taxonomyWriter,
                new FacetsConfig(),
                new SearcherTaxonomyManager(indexWriter, null, taxonomyWriter)
        );
    }

    /**
     * Configures an index on the file system using the {@link StandardAnalyzer} to process text
     * @return a backend instance
     * @throws IOException if Lucene fails to create writers
     */
    public static LuceneBackend directory(Path path) throws IOException {
        return directory(path, new StandardAnalyzer());
    }

    /**
     * Configures an index on the file system using the specified analyzer to process text
     * @param analyzer the analyzer
     * @return a backend instance
     * @throws IOException if Lucene fails to create writers
     */
    public static LuceneBackend directory(Path path, Analyzer analyzer) throws IOException {

        Path indexPath = path.resolve("index");
        Path taxonomyPath = path.resolve("taxonomy");

        Files.createDirectories(indexPath.toAbsolutePath());
        Files.createDirectories(taxonomyPath.toAbsolutePath());

        var config = new IndexWriterConfig(analyzer)
                .setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);

        var indexWriter = new IndexWriter(new NIOFSDirectory(indexPath), config);
        var taxonomyWriter = new DirectoryTaxonomyWriter(new NIOFSDirectory(taxonomyPath));

        return new DefaultLuceneBackend(
                indexWriter,
                taxonomyWriter,
                new FacetsConfig(),
                new SearcherTaxonomyManager(indexWriter, null, taxonomyWriter)
        );
    }
}
