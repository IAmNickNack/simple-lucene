package io.github.iamnicknack.slc.examples.data.country;

public interface Sourceable<T> {
    T withSource(String source);

}
