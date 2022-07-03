# Configuring Lucene

Elements of the API require access to various Lucene components, such as `IndexWriter`, `TaxonomyWriter`, 
`SearcherTaxonomyManager`, `FacetsConfig`, etc. These are configured as usual for the application and encapsulated
in a `LuceneBackend` instance.

Official documentation on the relationship and interactions between `IndexSearcher` and `IndexWriter` instances 
appears to be very difficult to source. Advice resulting from various internet searches appears to be subjective
and also very possibly out of date.

What becomes clear is that there is no "one size fits all solution" to the problem of making data searchable  
and a solution which may be appropriate for one application may not be a good fit for another.

The lowest common denominator for this problem is that:
* An application needs to manage the resources used to read and write data.
* Read and write operations require some level of coordination in order to achieve expected behaviour

The API abstracts these concerns behind the `LuceneBackend` in order that application logic might be agnostic to the
implementation.

## Resource management

A `LuceneBackend` provides an application with access to these resources via `AutoCloseable` `Lease` objects, which can 
be used in a `try-with-resources` construct to automatically close a resource or release it back to a managed pool
such as `SearcherTaxonomyManager`.

## Commit and refresh phases

In addition to resource management, `AutoCloseable#close()` provides a convenient hook into the workflow for 
performing commit or refresh operations. 

The default implementation provided by the core module implements this approach. It makes the assumption that
when an `IndexWriter` is done with and `AutoCloseable#close()` is called, the application will most likely want 
to start reading data from the index. In order for this to be possible, Lucene requires:

* `IndexWriter` changes are `commit`ed
* `TaxonomyWriter` changes are `commit`ed
* `SearcherTaxonomyManager` is refreshed

This functionality may or may not be considered too presumptive of the API, but it is required by Lucene in order to 
search the updated index. Utilising `AutoCloseable#close()` negates the need for applications to implement such 
boilerplate.

## Leased components

Two types of leases are provided by the `LuceneBackend`:

* `SearchComponents` are components relevant to read operations.
* `UpdateComponents` are components required to update the index.

The default implementation makes use of Lucene [ReferenceManager](https://lucene.apache.org/core/9_2_0/core/org/apache/lucene/search/ReferenceManager.html)
types. The API wraps interactions with the reference manager's `acquire()`, `release()` and `maybeRefresh()` in 
[AutoCloseable](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/AutoCloseable.html) leases.

