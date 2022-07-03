# Domain Operations

`DomainOperations` are a fundamental part of the API and provide the ability to marshall domain/application data 
to and from and Lucene documents. They have explicit responsibility for:

* Creating `Document` instances from domain-level data
* Creating domain-level representations from Lucene `Document`s
* Providing unique identifiers for domain data (defaulting to `Object#hashCode()`

The generic implementations provided by the APi also have implicit responsibility of maintaining the Lucene taxonomy
via `FacetsConfig`.

This example shows how the APIs generic implementations compare with the manual process of document mappings
created using the standard Lucene syntax.

* [AnnotatedRecordOperations](#`AnnotatedRecordOperations`)
* [MapDomainOperations](#`MapDomainOperations`)
* [Custom DomainOperations](#Custom domain operations)

## Example data

`ShortCountry` is an abbreviated form of the `Country` record provided in the example `datasets` module. It is
shortened to contain only a small number of fields for the purposes of this example.

## `AnnotatedRecordOperations`

`ShortCountry` is annotated with the `@IndexProperty` annotation from `simple-lucene-annotations`. This provides hints
to `AnnotatedRecordOperations` as to how the marshalling process for the type should be constructed.

Annotating the domain-level data in this way helps:
* To keep boilerplate code to a minimum.
* Requires minimal changes to be made when updating the data definition, providing a clear indication that index 
  changes may also be required. 
* Provides a clear indication of how the data is mapped to the underlying index.

A record can be annotated with:
```java
/**
 * An abbreviated form of {@link io.github.iamnicknack.slc.examples.data.country.Country}
 * @param name the name of the country
 * @param region the UN classified region
 * @param iso the 3-letter ISO code
 * @param place a list of place names associated with the country
 */
public record ShortCountry(@IndexProperty(value = "name", fields = "_all")
                           String name,
                           @IndexProperty(value = "region", keyword = true, facet = true, fields = "_all")
                           String region,
                           @IndexProperty(value = "iso", text = false, keyword = true, fields = "_all", id = true)
                           String iso,
                           @IndexProperty(value = "place", parameterizedType = String.class, fields = "_all")
                           List<String> place) {}
```

Operations can then be created by simply referencing this type:

```java
DomainOperations<ShortCountry> domainOperations = AnnotatedRecordOperations.create(ShortCountry.class, backend);
```


## `MapDomainOperations`

The core implementation (the `simple-lucene-core` module) provides `MapDomainOperations`. This can be used to define
the index structure for storing domain level data represented by `Map`.

The operations created in the code snippet below provide the same index structure as above. The only difference being
that runtime operations read and write `Map` instances rather than `ShortCountry`.

```java
    public static DomainOperations<Map<String, Object>> create(LuceneBackend backend) {
        var documentDescriptor = new DocumentDescriptorBuilder(backend)
                .field(new FieldDescriptorBuilder()
                        .name("name")
                        .id()
                        .stringField()
                        .text()
                        .alias("_all")
                        .build()
                )
                .field(new FieldDescriptorBuilder()
                        .name("region")
                        .stringField()
                        .text()
                        .keyword()
                        .facet()
                        .alias("_all")
                        .build()
                )
                .field(new FieldDescriptorBuilder()
                        .name("iso")
                        .stringField()
                        .keyword()
                        .alias("_all")
                        .build()
                )
                .field(new FieldDescriptorBuilder()
                        .name("place")
                        .multiValue()
                        .stringField()
                        .text()
                        .alias("_all")
                        .build()
                )
                .build();

        return new MapDomainOperations(documentDescriptor);
    }
```

## Custom domain operations

In both of the previous examples `DomainOperations` are generated according to provided metadata. It is perfectly 
possible to create a domain-specific implementation which uses the Lucene `Document` APIs directly.

Where it is thought that the alternatives win over this approach are that:

* This is arguably more verbose.
* It does not benefit from some opinionated choices made by the previous solutions in terms of field naming and 
  field type selection. Potentially leading to inconsistent approaches used between projects.
* Any change to domain data needs to be reflected at multiple points in the code, possibly leading to unnecessary 
  maintenance overhead.

> Obviously the author has a preference. Why else provide alternative solutions?

### `createDocument`

```java
    @Override
    public Document createDocument(ShortCountry value) {
        var document = new Document();

        document.add(new StoredField("name", value.name()));
        document.add(new TextField("name.text", value.name(), Field.Store.NO));
        document.add(new TextField("_all", value.name(), Field.Store.NO));

        document.add(new StoredField("region", value.region()));
        document.add(new StringField("region.keyword", value.region(), Field.Store.NO));
        document.add(new TextField("region.text", value.region(), Field.Store.NO));
        document.add(new FacetField("region.value", value.region()));
        document.add(new TextField("_all", value.region(), Field.Store.NO));

        document.add(new StoredField("iso", value.iso()));
        document.add(new StringField("iso.keyword", value.iso(), Field.Store.NO));
        document.add(new TextField("_all", value.iso(), Field.Store.NO));

        value.place().forEach(place -> {
            document.add(new StoredField("place", place));
            document.add(new TextField("place.text", place, Field.Store.NO));
            document.add(new TextField("_all", place, Field.Store.NO));
        });

        return document;
    }
```

### `readDocument`

```java
    @Override
    public ShortCountry readDocument(Document document) {
        return new ShortCountry(
                document.getField("name").stringValue(),
                document.getField("region").stringValue(),
                document.getField("iso").stringValue(),
                Arrays.stream(document.getFields("place"))
                        .map(IndexableField::stringValue)
                        .toList()
        );
    }
```

### `id`

```java
    @Override
    public String id(ShortCountry value) {
        return value.iso();
    }
```

### Registering taxonomy facets

Custom `DomainOperations` also need to prepare the taxonomy for facets via `FacetsConfig`

```java
public class LuceneDomainOperations implements DomainOperations<ShortCountry> {

    public LuceneDomainOperations(FacetsConfig facetsConfig) {
        facetsConfig.setIndexFieldName("region", "region.value");
    }

    // implementations as above
}
```