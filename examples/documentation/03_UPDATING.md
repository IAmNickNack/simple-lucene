# Insert, update and delete

The API implements a functional approach to update operations, where updates are encapsulated in a `LeaseFunction`
instance prior to being applied. The function is then executed via a `Lease` on the update resources provided 
by the `LuceneBackend`. 

The default implementation provided by `simple-lucene-core` executes the operation as a single transaction, implicitly
committing changes made via `IndexWriter` and `TaxonomyWriter` once the function execution completes.

This seems like a reasonable approach. However, it may be desirable to for an application to implement explicit, 
periodic commits via a timer. In such a case it would be entirely feasible to provide an alternative implementation of 
the `LuceneBackend` which did not implicitly commit changes after the execution of every operation.

The API may provide such approaches in the future, but some investigation will be required in how best to do it. 
For example, this might require making use of Lucene's `NRTManager` alongside or in place of `SearcherTaxonomyManager`. 
For the time being then, it is the responsibility of the application developer to implement such behaviour as required.

## Inserting data

The example below uses `BucketUpdateOperations` to index the contents of the `countries` collection. 

We don't need a new instance of this each time we update data. In reality this would
probably be a shared instance, injected in place of the `DomainOperations`.

Key steps required by the application are ② and ③.

* ① Bucket-like operations which allow data to be inserted without duplicate checks.<br> 
* ② **Create the operation to insert the entire collection of `countries`**
* ③ **Execute the operation via a `Lease` of `UpdateComponents`**
* ④ Unit test validation

```java
    @Test
    void insert(LuceneBackend backend,
                DomainOperations<ShortCountry> operations,
                List<ShortCountry> countries) {

        // ①
        var updateOperations = new BucketUpdateOperations<>(operations);
        // ② 
        var insertAllOperation = updateOperations.addAll(countries);
        // ③ 
        var insertAllResult = backend.update(insertAllOperation);

        // ④
        assertThat(insertAllResult).isEqualTo(countries.size());
        var count = backend.search(components -> components.indexSearcher().count(new MatchAllDocsQuery()));
        assertThat(count).isEqualTo(countries.size());
    }
```

## Updating data

This example uses `UpdateOperations` provided as a shared dependency (marked Ⓐ). 

Steps ③ and ④ specify and perform the update operation.

* Ⓐ **Shared update operations**
* ① Index the test dataset
* ② Modify some records for the purposes of this example
* ③ **Create the operation which will perform the update**
* ④ **Execute the operation**

```java
    @Test
    void update(UpdateOperations<ShortCountry> updateOperations, // Ⓐ
                LuceneBackend backend,
                @QueriesModule.ISO QueryExecutor<String, ShortCountry> isoTemplate,
                LuceneCollection<ShortCountry> luceneCollection,
                List<ShortCountry> countries) {

        // ①
        luceneCollection.addAll(countries);

        // ②
        var updatedCountries = isoTemplate.execute("-99").stream()
                // Not interested in the `Hit` container returned by the query. Just take the value
                .map(Hit::value)
                .map(country -> country.withIso("INVALID"))
                .toList();
        assertThat(updatedCountries).hasSize(5);
        
        // ③
        var updateOperation = updateOperations.updateAll(updatedCountries);
        // ④
        var updateCount = backend.update(updateOperation);
        
        assertThat(updateCount).isEqualTo(5);
    }
```

## Deleting data

Steps ③ and ④ specify and perform the update operation.

* ① Index the test dataset
* ② Identify some records to delete for the purposes of this example
* ③ **Create the operation which will perform the deletion**
* ④ **Execute the operation**
* ⑤ Unit test validation which includes performing a count via the Lucene API for documents that should have 
  been deleted

```java
    @Test
    void delete(UpdateOperations<ShortCountry> updateOperations,
                LuceneBackend backend,
                @QueriesModule.ISO QueryExecutor<String, ShortCountry> isoTemplate,
                @QueriesModule.ISO QueryFactory<String> findByIsoQueryFactory,
                LuceneCollection<ShortCountry> luceneCollection,
                List<ShortCountry> countries) {

        // ①
        luceneCollection.addAll(countries);

        // ②
        var countriesToDelete = isoTemplate.execute("-99").stream()
                // Not interested in the `Hit` container returned by the query. Just take the value
                .map(Hit::value)
                .toList();
        assertThat(countriesToDelete).hasSize(5);

        // ③
        var updateOperation = updateOperations.deleteAll(countriesToDelete);
        // ④
        var updateCount = backend.update(updateOperation);

        // ⑤ 
        assertThat(updateCount).isEqualTo(5);
        
        var countOperation = (LeaseFunction<LuceneBackend.SearchComponents, Integer>) components ->
                components.indexSearcher().count(findByIsoQueryFactory.query("-99"));
        var remainingCount = backend.search(countOperation);
        assertThat(remainingCount).isEqualTo(0);
    }
```

<hr>

Next: [Querying](04_QUERYING.md)
