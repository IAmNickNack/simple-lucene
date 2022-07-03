package slc.domain;

import java.util.Optional;

/**
 * Find the best matching {@link ShortCountry} for a given term, trusting that either:
 * <ul>
 *     <li>{@code term} uniquely identifies a single country</li>
 *     <li>Scoring rules can be relied on to return the <em>best</em> match</li>
 * </ul>
 */
@FunctionalInterface
public interface CountryLookup {
    Optional<ShortCountry> lookup(String term);
}
