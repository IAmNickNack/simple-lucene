package io.github.iamnicknack.slc.core.document;

import org.apache.lucene.document.Document;
import io.github.iamnicknack.slc.api.document.FieldParser;
import io.github.iamnicknack.slc.api.document.FieldReader;

import java.util.Arrays;

public class SingleValueFieldReader implements FieldReader {

    private final String name;
    private final FieldParser<?> parser;

    public SingleValueFieldReader(String name, FieldParser<?> parser) {
        this.name = name;
        this.parser = parser;
    }

    @Override
    public Object read(Document document) {
        return Arrays.stream(document.getFields(name))
                .findFirst()
                .map(parser::parse)
                .orElse(null);
    }
}
