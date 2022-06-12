package com.github.iamnicknack.slc.api.document;

import org.apache.lucene.index.IndexableField;

public interface FieldReader {

    Object read(IndexableField[] fields);

}
