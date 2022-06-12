package io.github.iamnicknack.slc.examples.data.country;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.StreamSupport;

public class CountryData {

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);


    public static List<Country> countries() throws IOException {
        return load("/ne_110m_admin_0_countries.json", Country.class);
    }

    public static List<Country> mapUnits() throws IOException {
        return load("/ne_110m_admin_0_map_units.json", Country.class);
    }

    public static List<Country> mapUnits50m() throws IOException {
        return load("/ne_50m_admin_0_map_units.json", Country.class);
    }

    public static List<Place> places() throws IOException {
        return load("/ne_110m_populated_places_simple.json", Place.class);
    }

    public static List<Place> places10m() throws IOException {
        return load("/ne_10m_populated_places_simple.json", Place.class);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Sourceable<?>> List<T> load(String resourceName, Class<T> type) throws IOException {

        try (InputStream is = CountryData.class.getResourceAsStream(resourceName)){
            ObjectNode root = objectMapper.readValue(is, ObjectNode.class);

            return StreamSupport.stream(root.path("features").spliterator(), false)
                    .map(feature -> feature.path("properties"))
                    .map(props -> objectMapper.convertValue(props, type))
                    .map(t -> (T)t.withSource(resourceName.substring(1)))
                    .toList();
        }
    }

}
