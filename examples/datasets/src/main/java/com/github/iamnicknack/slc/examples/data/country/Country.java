package com.github.iamnicknack.slc.examples.data.country;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.iamnicknack.slc.annotation.IndexProperty;

public record Country(@IndexProperty(value = "name", keyword = true, fields = "_all", id = true) @JsonProperty("NAME") String name,
                      @IndexProperty(value = "sovereign", fields = "_all", id = true) @JsonProperty("SOVEREIGNT") String sovereign,
                      @IndexProperty(value = "economy", fields = "_all") @JsonProperty("ECONOMY") String economy,
                      @IndexProperty(value = "region_un", fields = "_all") @JsonProperty("REGION_UN") String regionUN,
                      @IndexProperty(value = "region_wb", fields = "_all") @JsonProperty("REGION_WB") String regionWB,
                      @IndexProperty(value = "iso3", keyword = true, fields = "_all") @JsonProperty("ISO_A3") String iso3,
                      @IndexProperty(value = "iso2", keyword = true, fields = "_all") @JsonProperty("ISO_A2") String iso2,
                      @IndexProperty(value = "abbrev", fields = "_all") @JsonProperty("ABBREV") String abbrev,
                      @IndexProperty(value = "scale") @JsonProperty("scalerank") int scaleRank,
                      @IndexProperty(value = "source", keyword = true, id = true) String source,
                      @IndexProperty(value = "place_count") int placeCount
) implements Sourceable<Country> {

    public static Country id(String name, String sovereign, String source) {
        return new Country(name, sovereign, null, null, null, null, null, null, 0, source, 0);
    }

    @Override
    public Country withSource(String source) {
        return new Country(name, sovereign, economy, regionUN, regionWB, iso3, iso2, abbrev, scaleRank, source, placeCount);
    }

    public Country withPlaceCount(int placeCount) {
        return new Country(name, sovereign, economy, regionUN, regionWB, iso3, iso2, abbrev, scaleRank, source, placeCount);
    }

}
