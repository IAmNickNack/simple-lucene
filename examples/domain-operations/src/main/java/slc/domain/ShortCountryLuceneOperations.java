package slc.domain;

import io.github.iamnicknack.slc.api.index.DomainOperations;
import org.apache.lucene.document.*;
import org.apache.lucene.facet.FacetField;
import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.index.IndexableField;

import java.util.Arrays;

/**
 * {@link DomainOperations} which use the Lucene directly, bypassing document specification
 * via {@link io.github.iamnicknack.slc.api.document.DocumentDescriptor}
 */
public class ShortCountryLuceneOperations implements DomainOperations<ShortCountry> {

    public ShortCountryLuceneOperations(FacetsConfig facetsConfig) {
        facetsConfig.setIndexFieldName("region", "region.value");
    }

    @Override
    public Document createDocument(ShortCountry value) {
        var document = new Document();

        document.add(new StoredField("name", value.name()));
        document.add(new TextField("name.text", value.name(), Field.Store.NO));
        document.add(new TextField("_all", value.name(), Field.Store.NO));

        document.add(new StoredField("region", value.region()));
        document.add(new StringField("region.keyword", value.region(), Field.Store.NO));
        document.add(new TextField("region.text", value.region(), Field.Store.NO));
        document.add(new FacetField("region.value", value.region()));
        document.add(new TextField("_all", value.region(), Field.Store.NO));

        document.add(new StoredField("iso", value.iso()));
        document.add(new StringField("iso.keyword", value.iso(), Field.Store.NO));
        document.add(new TextField("_all", value.iso(), Field.Store.NO));

        value.place().forEach(place -> {
            document.add(new StoredField("place", place));
            document.add(new TextField("place.text", place, Field.Store.NO));
            document.add(new TextField("_all", place, Field.Store.NO));
        });

        return document;
    }

    @Override
    public ShortCountry readDocument(Document document) {
        return new ShortCountry(
                document.getField("name").stringValue(),
                document.getField("region").stringValue(),
                document.getField("iso").stringValue(),
                Arrays.stream(document.getFields("place"))
                        .map(IndexableField::stringValue)
                        .toList()
        );
    }

    @Override
    public String id(ShortCountry value) {
        return value.name();
    }
}
