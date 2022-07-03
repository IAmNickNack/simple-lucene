package slc.domain;

import io.github.iamnicknack.slc.annotation.IndexProperty;
import io.github.iamnicknack.slc.examples.data.country.Country;

import java.util.Collections;
import java.util.List;

/**
 * An abbreviated form of {@link Country}
 * @param name the name of the country
 * @param region the UN classified region
 * @param iso the 3-letter ISO code
 * @param place a list of place names associated with the country
 */
public record ShortCountry(@IndexProperty(value = "name", fields = "_all", id = true)
                           String name,
                           @IndexProperty(value = "region", keyword = true, facet = true, fields = "_all")
                           String region,
                           @IndexProperty(value = "iso", text = false, keyword = true, fields = "_all")
                           String iso,
                           @IndexProperty(value = "place", parameterizedType = String.class, fields = "_all")
                           List<String> place) {

    /**
     * Derive this instance from the example datasets
     * @param country full country instance
     */
    public ShortCountry(Country country) {
        this(country.name(), country.regionUN(), country.iso3(), Collections.emptyList());
    }

    /**
     * Add `places` (cities)
     * @param places string list of places
     * @return a new instance
     */
    public ShortCountry withPlaces(List<String> places) {
        return new ShortCountry(name, region, iso, places);
    }

    /**
     * Update the ISO property
     * @param iso the new value
     * @return a new instance
     */
    public ShortCountry withIso(String iso) {
        return new ShortCountry(name, region, iso, place);
    }

}
