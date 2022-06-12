package com.github.iamnicknack.slc.core.query;

import com.github.iamnicknack.slc.api.query.Hit;
import com.github.iamnicknack.slc.api.query.HitRecord;
import com.github.iamnicknack.slc.api.query.Result;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import com.github.iamnicknack.slc.api.backend.LuceneBackend;
import com.github.iamnicknack.slc.api.lease.Lease;

import java.util.Iterator;

public class DefaultResult implements Result<Document> {

    private final TopDocs topDocs;
    private final Lease<LuceneBackend.SearchComponents> lease;

    public DefaultResult(TopDocs topDocs,
                         Lease<LuceneBackend.SearchComponents> lease) {
        this.topDocs = topDocs;
        this.lease = lease;
    }

    @Override
    public long totalHits() {
        return topDocs.totalHits.value;
    }

    @Override
    public void close() {
        lease.close();
    }

    @Override
    public Iterator<Hit<Document>> iterator() {
        return new Iterator<>() {
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < topDocs.scoreDocs.length;
            }

            @Override
            public Hit<Document> next() {
                ScoreDoc doc = topDocs.scoreDocs[index++];
                return new HitRecord<>(
                        doc.score,
                        lease.execute(components ->
                                components.indexSearcher().doc(doc.doc)
                        )
                );
            }
        };
    }}
