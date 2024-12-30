package slc.domain;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.iamnicknack.slc.annotation.AnnotatedRecordOperations;
import io.github.iamnicknack.slc.api.index.DomainOperations;
import org.apache.lucene.document.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import slc.domain.operations.ShortCountryLuceneOperations;
import slc.domain.operations.ShortCountryMapOperations;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ShortCountryComparisonTest {

    private final TestComponent components;

    private final TypeReference<Map<String, Object>> mapTypeReference = new TypeReference<>() {};
    private final ObjectMapper mapper = new ObjectMapper();

    private final List<ShortCountry> shortCountries;
    private final DomainOperations<ShortCountry> luceneOperations;
    private final DomainOperations<Map<String, Object>> mapOperations;
    private final DomainOperations<ShortCountry> shortCountryOperations;

    public ShortCountryComparisonTest() {
        components = DaggerTestComponent.create();
        components.load();

        this.shortCountries = components.countries();
        this.luceneOperations = new ShortCountryLuceneOperations(components.luceneBackend().facetsConfig());
        this.mapOperations = ShortCountryMapOperations.create(components.luceneBackend());
        this.shortCountryOperations = AnnotatedRecordOperations.create(ShortCountry.class, components.luceneBackend());
    }

    /**
     * Method-source for first 10 countries
     */
    Stream<ShortCountry> countryStream() {
        return shortCountries.stream()
                .limit(10);
    }

    @BeforeEach
    void beforeEach() {
        components.luceneBackend().update(components -> components.indexWriter().deleteAll());
    }

    @AfterAll
    void afterAll() throws IOException {
        components.shutdownHook().shutDown();
    }

    @ParameterizedTest
    @MethodSource("countryStream")
    void writesAreEquivalent(ShortCountry country) {
        var shortCountryFields = documentFieldsAsStringList(shortCountryOperations.createDocument(country));
        var mapFields = documentFieldsAsStringList(mapOperations.createDocument(mapper.convertValue(country, mapTypeReference)));
        var luceneFields = documentFieldsAsStringList(luceneOperations.createDocument(country));

        assertThat(mapFields).containsAll(shortCountryFields);
        assertThat(mapFields).containsAll(luceneFields);

        assertThat(luceneFields).containsAll(shortCountryFields);
        assertThat(luceneFields).containsAll(mapFields);

        assertThat(shortCountryFields).containsAll(luceneFields);
        assertThat(mapFields).containsAll(luceneFields);
    }

    @ParameterizedTest
    @MethodSource("countryStream")
    void readsAreEquivalent(ShortCountry input) {
        var mapCountry = readShortCountryFromMap(input);

        var annotationCountry = shortCountryOperations.readDocument(
                shortCountryOperations.createDocument(input)
        );

        var luceneCountry = luceneOperations.readDocument(
                luceneOperations.createDocument(input)
        );

        assertThat(mapCountry).isEqualTo(annotationCountry);
        assertThat(mapCountry).isEqualTo(luceneCountry);
    }

    /**
     * To utilise AssertJ collection comparisons we need a list of comparable types.
     * Converting the fields of a document to a list of string values achieves this
     * @param document the document with fields to be converted
     * @return string representations of all fields
     */
    private List<String> documentFieldsAsStringList(Document document) {
        return document.getFields().stream()
                .map(Object::toString)
                .sorted()
                .toList();
    }

    /**
     * Using a {@link ShortCountry} to test read operations on maps requires some gymnastics.
     * Moving that here to make the test case more readable
     * @param country the country to read from its map form
     * @return the country having been marshalled in both directions via {@link #mapOperations}
     */
    private ShortCountry readShortCountryFromMap(ShortCountry country) {
        return mapper.convertValue(
                mapOperations.readDocument(
                        mapOperations
                                .createDocument(mapper.convertValue(country, mapTypeReference))
                ),
                ShortCountry.class
        );
    }
}