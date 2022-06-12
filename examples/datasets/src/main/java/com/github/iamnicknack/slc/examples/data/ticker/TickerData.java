package com.github.iamnicknack.slc.examples.data.ticker;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class TickerData {

    private final List<Ticker> tickers;

    public TickerData() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        try(InputStream is = getClass().getResourceAsStream("/tickers.json")) {
            tickers = objectMapper.readValue(is, new TypeReference<>() {
            });
        }
    }

    public List<Ticker> getTickers() {
        return tickers;
    }

    public Document createDocument(Ticker ticker) {
        Document doc = new Document();
        doc.add(new TextField("name", ticker.name(), Field.Store.YES));
        doc.add(new StringField("name.keyword", ticker.name(), Field.Store.YES));
        doc.add(new TextField("sector", ticker.sector(), Field.Store.YES));
        doc.add(new StringField("sector.keyword", ticker.sector(), Field.Store.YES));
        doc.add(new TextField("symbol", ticker.symbol(), Field.Store.YES));
        doc.add(new StringField("symbol.keyword", ticker.symbol(), Field.Store.YES));
        doc.add(new TextField("_all", ticker.name(), Field.Store.NO));
        doc.add(new TextField("_all", ticker.sector(), Field.Store.NO));
        doc.add(new TextField("_all", ticker.symbol(), Field.Store.NO));

//        doc.add(new FacetField("sector.facet", ticker.sector()));

        return doc;
    }

    public Ticker readValue(Document document) {
        return new Ticker(
                document.get("name"),
                document.get("sector"),
                document.get("symbol")
        );
    }
}
