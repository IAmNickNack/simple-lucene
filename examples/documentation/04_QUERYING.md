# Query Execution

The API implements query functionality using the same `Lease` mechanism as used for [update](03_UPDATING.md) 
operations.

This design decision is based on the assumption that Lucene components such as `SearcherManager` and 
`SearcherTaxonomyManager` provide the functionality to ensure that searches we perform are in sync with  
Lucene generation increments as a result of update operations. 

Further testing is required to confirm the above assumption. Theoretically however, ensuring that the same `Lease`
is used to both execute the query and retrieve a document should mean that the `TopDocs` returned by a search
match the `Document` values that are subsequently retrieved. I.e. they are of the same document generation.


## Querying 

Three interfaces participate in a general abstraction to provide results of `<R>` for a query of `<T>`:  

### `QueryFactory`

The factory is used to encapsulate the building of a Lucene query which represents any domain-specific type.
It is essentially a mapping function from `T` to `Query`:

```java
@FunctionalInterface
public interface QueryFactory<T> {
    Query query(T value);
}
```

### `QueryExecutor`

An executor is provides results for a query based on a domain-specific type. The default executors provided
by the API delegate query generation to a `QueryFactory`. 

```java
@FunctionalInterface
public interface QueryExecutor<T, R> {
    Result<R> execute(T query);
}
```

### `Result`

Query execution provides access to an iteration of domain-specific values. 

```java
public interface Result<T> extends Iterable<Hit<T>>, AutoCloseable {
    long totalHits();

    /**
     * Provides this result as a closeable stream, adding a call to {@link #close()}
     * on {@link Stream#onClose(Runnable)}. 
     * Should 
     * @return a stream of {@link Hit}s
     */
    default Stream<Hit<T>> stream() {
        return StreamSupport.stream(spliterator(), false)
                .onClose(this::close);
    }

    /**
     * Provides this result as a collection and releases resources
     * @return a collection of {@link Hit}s
     */
    default List<Hit<T>> toList() {
        try(var stream = this.stream()) {
            return stream.toList();
        }
    }
}
```

At time of writing, a `Result` is an `AutoCloseable` resource which assumes lazy retrieval of document data. 

As stated in the introduction to this section, we ideally want the same Lucene searcher to both a) perform the 
search and b) retrieve document data. Being `AutoCloseable` allows us to utilise try-with-resources achieve this 
while also ensuring the searcher is released once we are done.

It is relatively simple to for an application to wrap this functionality in another function should eager retrieval 
be required. `Result` provides `stream` and `toList` which can be used to facilitate this.

## Examples

### Basic query workflow

* ① Create a `QueryFactory` to act as a template for querying the `_all` field with `String` values
* ② Use the factory from a `QueryExecutor` to perform searches  
* ③ Use the executor to perform a search for documents matching an instance of `T`
* ④ Reuse the executor to perform subsequent searches

```java
    @Test
    void textSearch(LuceneBackend backend) {
        // ①
        var parser = new QueryParser("_all", new StandardAnalyzer());
        QueryFactory<String> queryByName = value -> {
            try {
                return parser.parse(value);
            } catch (ParseException e) {
                throw new QueryException("Failed to parse query", e);
            }
        };
        // ②
        var queryExecutor = new DefaultQueryExecutor<>(queryByName, backend.searcherLeaseFactory());

        // ③
        logger.info("Searching for \"Spain\"");
        try(var result = queryExecutor.execute("Spain")) {
            result.forEach(hit -> logger.info("[{}] Hit: {}", hit.score(), hit.value()));
            assertThat(result.totalHits()).isEqualTo(2);
        }

        // ④
        logger.info("Searching for \"spain +madrid\"");
        try(var result = queryExecutor.execute("spain +madrid")) {
            result.forEach(hit -> logger.info("[{}] Hit: {}", hit.score(), hit.value()));
            assertThat(result.totalHits()).isEqualTo(1);
        }
    }
```

### Mapping to domain types during iteration

Query execution can map results to domain types.

* ① Create a `QueryFactory` to act as a template for querying the `iso.keyword` field
* ② Create a `QueryExecutor` whose result iterator is mapped via `domainOperations::readDocument`
* ③ Use the executor to perform searches that provide instances of `ShortCountry`

```java
    @Test
    void keywordSearch(LuceneBackend backend,
                       DomainOperations<ShortCountry> domainOperations) {
        // ①
        QueryFactory<String> queryByIsoCode = value -> new TermQuery(new Term("iso.keyword", value));

        // ②
        var queryExecutor = new DefaultQueryExecutor<>(queryByIsoCode, backend.searcherLeaseFactory())
                .withIterator(Result.IteratorFactory.mapping(domainOperations::readDocument));

        // ③
        try(var result = queryExecutor.execute("ESP")) {
            result.forEach(hit -> logger.info("[{}] Country: {}", hit.score(), hit.value()));
            assertThat(result.totalHits()).isEqualTo(1);
        }
    }
```

### Unbounded / paged / streamed results

`PagedQueryExecutor` allows an unbounded number of hits to be returned. The simplest way to handle these results
is via the `Result#stream()` method.

* ① Create a `PagedQueryExecutor`
* ② Read all results made available via `Result#stream()`
* ③ Use any `Stream` operators to provide an alternative view of the results

```java
    @Test
    void pagingResults(LuceneBackend backend) {
        var queryByRegion = QueryFactories.text("region.text");
        var queryOptions = QueryOptions.DEFAULT;

        // ①
        var pagedQueryExecutor = new DefaultPagedQueryExecutor<>(queryByRegion, backend.searcherLeaseFactory());
        // ②
        try(var stream = pagedQueryExecutor.execute("americas").stream()) {
            var pagedCount = stream.count();
            logger.info("Paged executor found {} hits", pagedCount);
            assertThat(pagedCount).isGreaterThan(queryOptions.maxHits());
        }

        // ③
        try(var stream = pagedQueryExecutor.execute("americas").stream()) {
            var limitedCount = stream.limit(20).count();
            logger.info("Limited result contain {} hits", limitedCount);
            assertThat(limitedCount).isEqualTo(20);
        }
    }
```

## Example application abstraction

Depending on the application requirements we might want to abstract away from the try-with-resources syntax. 
For example, when only a single result or only a known maximum number of results is required.

In such cases we could define a domain-specific operation, such as:

```java
@FunctionalInterface
public interface CountryLookup {
    Optional<ShortCountry> lookup(String term);
}
```

where it is trusted that either:
* `term` will uniquely match a single country, or
* scoring rules will determine the best match out of many 

An implementation of this function might then be specified as an injectable dependency, similar to:

```java
    @Singleton
    @Provides
    public CountryLookup countryByISOCodeLookup(@ISO QueryExecutor<String, ShortCountry> findByIsoCode) {
        return iso -> {
            try(var result = findByIsoCode.execute(iso)) {
                var iterator = result.iterator();
                return iterator.hasNext()
                        ? Optional.of(iterator.next().value())
                        : Optional.empty();
            }
        };
    }
```

and injected and used at the application level:

```java
    @Test
    void lookupFunction(CountryLookup countryLookup) {
        var countryOptional = countryLookup.lookup("CAN");
        countryOptional.ifPresent(country -> logger.info("Found country: {}", country));

        assertThat(countryOptional).isPresent();
        assertThat(countryOptional.map(ShortCountry::name)).isEqualTo(Optional.of("Canada"));
    }
```