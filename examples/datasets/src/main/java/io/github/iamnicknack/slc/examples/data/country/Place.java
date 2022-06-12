package io.github.iamnicknack.slc.examples.data.country;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.iamnicknack.slc.annotation.IndexProperty;

public record Place(@IndexProperty(value = "name", keyword = true, fields = "_all", id = true) @JsonProperty("name") String name,
                    @IndexProperty(value = "sovereign", fields = "_all", id = true) @JsonProperty("sov0name") String sovereign,
                    @IndexProperty(value = "iso3", keyword = true, fields = "_all") @JsonProperty("sov_a3") String iso3,
                    @IndexProperty(value = "iso2", keyword = true, fields = "_all") @JsonProperty("iso_a2") String iso2,
                    @IndexProperty(value = "source", keyword = true) String source
) implements Sourceable<Place> {
    @Override
    public Place withSource(String source) {
        return new Place(name, sovereign, iso3, iso2, source);
    }
}
