package slc.domain;

import java.util.List;

public interface CountrySearch {
    List<ShortCountry> search(String term);
}
