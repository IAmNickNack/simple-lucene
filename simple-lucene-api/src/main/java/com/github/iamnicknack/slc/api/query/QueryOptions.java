package com.github.iamnicknack.slc.api.query;

// TODO: Should support sorting

/**
 * Options that can be passed to Lucene to perform query execution
 */
public interface QueryOptions {

    /**
     * Default values
     */
    QueryOptions DEFAULT = () -> 10;

    /**
     * Return only the highest ranked document
     */
    QueryOptions TOP_HIT = () -> 1;

    /**
     * The maximum number of hits to return per result
     */
    int maxHits();

}
