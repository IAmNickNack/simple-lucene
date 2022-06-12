package io.github.iamnicknack.slc.api.query;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PagedResultTest {

    @Test
    @SuppressWarnings("resource")
    void streamProvidesAllValuesAndCloses() {
        var values = List.<Object>of(1, 2, 3);
        var invocationValidation = Mockito.mock(Runnable.class);
        var executor = new FakePagedQueryExecutor(values, invocationValidation);

        List<Hit<Object>> list;
        try(var stream = executor.execute(null).stream()) {
            list = stream.toList();
        }

        var validation = values.stream()
                .allMatch(value -> list.stream().anyMatch(hit -> hit.value().equals(value)));

        assertTrue(validation);
        Mockito.verify(invocationValidation).run();
    }

}