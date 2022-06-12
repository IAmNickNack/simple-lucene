package io.github.iamnicknack.slc.examples.data.nobel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.StreamSupport;

public class NobelData {

    private final List<Nobel> nobels;

    public NobelData() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        ArrayNode arrayNode;
        try (InputStream is = getClass().getResourceAsStream("/nobel.json")){
            arrayNode = (ArrayNode) objectMapper.readValue(is, ObjectNode.class).path("laureates");
        }

        this.nobels = StreamSupport.stream(arrayNode.spliterator(), false)
                .flatMap(person -> StreamSupport.stream(person.path("prizes").spliterator(), false)
                        .map(prize -> new Nobel(
                                person.path("firstname").asText(),
                                person.path("surname").asText(),
                                person.path("bornCountry").asText(),
                                prize.path("motivation").asText()
                        ))
                )
                .toList();
    }

    public List<Nobel> getNobels() {
        return nobels;
    }

    public Document createDocument(Nobel value) {
        Document doc = new Document();
        doc.add(new StringField("firstname", value.firstname(), Field.Store.YES));
        doc.add(new StringField("surname", value.surname(), Field.Store.YES));
        doc.add(new StringField("bornCountry", value.bornCountry(), Field.Store.YES));
        doc.add(new TextField("motivation", value.motivation(), Field.Store.YES));
        return doc;
    }

    public Nobel readValue(Document document) {
        return new Nobel(
                document.get("firstname"),
                document.get("surname"),
                document.get("bornCountry"),
                document.get("motivation")
        );
    }

}
