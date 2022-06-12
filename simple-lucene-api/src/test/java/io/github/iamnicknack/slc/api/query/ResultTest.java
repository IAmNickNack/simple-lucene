package io.github.iamnicknack.slc.api.query;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class ResultTest {

    @Test
    @SuppressWarnings("unchecked")
    void withIteratorDefersClose() {
        Result<String> result = mock(Result.class);
        when(result.withIterator(any(Result.IteratorFactory.class))).thenCallRealMethod();

        result.withIterator(iterator -> iterator).close();

        verify(result).close();
    }


    @Test
    @SuppressWarnings("unchecked")
    void withIteratorDefersTotalHits() {
        Result<String> result = mock(Result.class);
        when(result.totalHits()).thenCallRealMethod();
        when(result.withIterator(any(Result.IteratorFactory.class))).thenCallRealMethod();

        result.withIterator(ignored -> ignored).totalHits();

        verify(result).totalHits();
    }
}