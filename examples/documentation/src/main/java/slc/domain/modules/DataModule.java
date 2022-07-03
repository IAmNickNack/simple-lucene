package slc.domain.modules;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.github.iamnicknack.slc.examples.data.country.CountryData;
import io.github.iamnicknack.slc.examples.data.country.Place;
import slc.domain.ShortCountry;

import java.util.List;

public class DataModule extends AbstractModule {

    @Singleton
    @Provides
    public List<ShortCountry> shortCountries() {
        return CountryData.countries().stream()
                .map(ShortCountry::new)
                .map(shortCountry -> {
                    var places = CountryData.places().stream()
                            .filter(place -> place.iso3().equals(shortCountry.iso()))
                            .map(Place::name)
                            .toList();
                    return shortCountry.withPlaces(places);
                })
                .toList();
    }
}
