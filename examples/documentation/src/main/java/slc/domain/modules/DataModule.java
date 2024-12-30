package slc.domain.modules;

import dagger.Module;
import dagger.Provides;
import io.github.iamnicknack.slc.examples.data.country.CountryData;
import io.github.iamnicknack.slc.examples.data.country.Place;
import jakarta.inject.Singleton;
import slc.domain.ShortCountry;

import java.util.List;

@Module
public class DataModule {

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
