package com.github.iamnicknack.slc.core.document;

import org.apache.lucene.index.IndexableField;
import com.github.iamnicknack.slc.api.document.FieldParser;
import com.github.iamnicknack.slc.api.document.FieldReader;

import java.util.Arrays;
import java.util.List;

public class MultiValueFieldReader implements FieldReader {

    private final FieldParser<?> parser;

    public MultiValueFieldReader(FieldParser<?> parser) {
        this.parser = parser;
    }

    @Override
    public List<?> read(IndexableField[] fields) {
        return Arrays.stream(fields)
                .map(parser::parse)
                .toList();
    }
}
