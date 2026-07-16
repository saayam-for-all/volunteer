package org.sfa.volunteer.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CountryNotFoundException extends RuntimeException {
    private final String countryName;
}