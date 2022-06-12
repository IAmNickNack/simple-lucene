package io.github.iamnicknack.slc.core.document;

import org.apache.lucene.index.IndexableField;
import io.github.iamnicknack.slc.api.document.FieldParser;
import io.github.iamnicknack.slc.api.document.FieldReader;

import java.util.Arrays;

public class SingleValueFieldReader implements FieldReader {

    private final FieldParser<?> parser;

    public SingleValueFieldReader(FieldParser<?> parser) {
        this.parser = parser;
    }

    @Override
    public Object read(IndexableField[] fields) {
        return Arrays.stream(fields)
                .findFirst()
                .map(parser::parse)
                .orElse(null);
    }
}
