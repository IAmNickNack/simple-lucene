package io.github.iamnicknack.slc.api.query;


public interface Hit<T> {

    float score();

    T value();

}
