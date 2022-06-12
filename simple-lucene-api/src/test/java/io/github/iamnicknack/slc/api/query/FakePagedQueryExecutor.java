package io.github.iamnicknack.slc.api.query;

import java.util.Iterator;
import java.util.List;

record FakePagedQueryExecutor(List<Object> hitValues,
                              Runnable invocationCheck) implements PagedQueryExecutor<Object, Object> {
    @Override
    public PagedResult<Object> execute(Object query, QueryOptions options) {

        return new PagedResult<>() {
            @Override
            public Iterator<Result<Object>> iterator() {
                return hitValues.stream()
                        .map(value -> (Result<Object>) () -> {
                            var hit = (Hit<Object>) new HitRecord<>(1, value);
                            return List.of(hit).iterator();
                        })
                        .iterator();
            }

            @Override
            public void close() {
                invocationCheck.run();
                PagedResult.super.close();
            }
        };
    }
}
