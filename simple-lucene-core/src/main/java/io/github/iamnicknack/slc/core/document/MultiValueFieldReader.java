package io.github.iamnicknack.slc.core.document;

import org.apache.lucene.document.Document;
import io.github.iamnicknack.slc.api.document.FieldParser;
import io.github.iamnicknack.slc.api.document.FieldReader;

import java.util.Arrays;
import java.util.List;

public class MultiValueFieldReader implements FieldReader {

    private final String name;
    private final FieldParser<?> parser;

    public MultiValueFieldReader(String name, FieldParser<?> parser) {
        this.name = name;
        this.parser = parser;
    }

    @Override
    public List<?> read(Document document) {
        return Arrays.stream(document.getFields(name))
                .map(parser::parse)
                .toList();
    }
}
