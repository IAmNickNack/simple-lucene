package com.github.iamnicknack.slc.api.query;

public record HitRecord<T>(float score,
                           T value) implements Hit<T> {
}
