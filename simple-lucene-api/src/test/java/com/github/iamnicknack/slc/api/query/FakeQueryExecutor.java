package com.github.iamnicknack.slc.api.query;

import java.util.List;

record FakeQueryExecutor(List<Object> hitValues) implements QueryExecutor<Object, Object> {
    @Override
    public Result<Object> execute(Object query, QueryOptions options) {
        return () -> hitValues.stream()
                .map(value -> (Hit<Object>) new HitRecord<>(1, value))
                .iterator();
    }
}
