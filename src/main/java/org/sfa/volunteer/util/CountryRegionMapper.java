package org.sfa.volunteer.util;

import java.util.Set;

public class CountryRegionMapper {
    private static final Set<String> EU_COUNTRIES = Set.of(
            "Austria", "Belgium", "Bulgaria", "Croatia", "Cyprus", "Czech Republic",
            "Denmark", "Estonia", "Finland", "France", "Germany", "Greece", "Hungary",
            "Ireland", "Italy", "Latvia", "Lithuania", "Luxembourg", "Malta",
            "Netherlands", "Poland", "Portugal", "Romania", "Slovakia", "Slovenia", "Spain", "Sweden"
    );

    //Quick Lookup
    public static boolean isEU(String country) {
        return EU_COUNTRIES.contains(country);
    }
}
