package com.github.iamnicknack.slc.examples.data.ticker;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Ticker(@JsonProperty("Name") String name,
                     @JsonProperty("Sector") String sector,
                     @JsonProperty("Symbol") String symbol) {
}
