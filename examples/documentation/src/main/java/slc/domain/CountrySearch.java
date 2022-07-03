package slc.domain;

import java.util.List;

/**
 * Find the best matching {@link ShortCountry} records for a given term:
 */
@FunctionalInterface
public interface CountrySearch {
    List<ShortCountry> search(String term);
}

