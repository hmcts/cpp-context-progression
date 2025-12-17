package uk.gov.moj.cpp.progression;

import java.util.stream.Stream;

public enum Country {
    WALES("Wales"),
    ENGLAND("England");

    private String name;

    Country(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    /**
     * Get country by country name. For unknown countries returns England
     *
     * @return Country
     */
    public static uk.gov.moj.cpp.progression.Country getCountryByName(final String name) {
        return Stream.of(uk.gov.moj.cpp.progression.Country.values())
                .filter(country -> country.getName().equalsIgnoreCase(name))
                .findAny().orElse(ENGLAND);
    }
}
