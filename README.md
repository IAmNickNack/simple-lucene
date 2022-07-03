# Simple Lucene Abstractions

[//]: # (![example workflow]&#40;https://github.com/iamnicknack/simple-lucene/actions/workflows/maven/badge.svg&#41;)

## What this is

* An opinionated, low-level abstraction of Lucene which aims to simplify resource management and query execution.
* Facilitates modelling of domain types and programmatically define the structure of the Lucene index.

## What this is not

* NOT a dedicated persistence layer.
    * Lucene is not a good candidate for a persistence layer. Lucene is an index. It's primary function is to provide
      features for data retrieval. To achieve this, write operations are considered a necessary evil and are only
      possible either in *near*-real-time or with significant latency.
    * [Hibernate](https://docs.jboss.org/hibernate/search/6.1/reference/en-US/html_single/) provides a full-featured
      ORM.

## What are these abstractions for?

If you come to Lucene from any of the well known distributions such as [Elastic](https://elastic.co) or
[Solr](https://solr.apache.org), the barrier to entry can seem quite high.

* Lucene requires us to pay close attention to resource usage when using searchers, readers, etc writers so as to
  avoid resource leakage.
* We need to choose appropriate field types and maintain instances of them correctly when updating the index
  in order for Lucene to do the work we're expecting it to do at query time.
* Attention needs to be paid to the details of indexing data such as when to commit or updating taxonomy for
  a document.
* ...

This is not an exhaustive list. We may not be experts on Lucene (yet) and the list of details for the
developer to remember to account for each time Lucene is brought into a project will probably grow over time.

Re-writing features to support these types of scenarios each time an application wants to use Lucene might be a
waste of development time and resources. By accepting some opinionated choices around field names and types, we
can negate the need to duplicate code each time we build an application with Lucene.

## Documentation

Documentation is provided along with working examples in the [examples/documentation](examples/documentation/README.md)
module