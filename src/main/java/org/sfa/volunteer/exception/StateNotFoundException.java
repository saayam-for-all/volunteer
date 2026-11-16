package org.sfa.volunteer.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StateNotFoundException extends RuntimeException {
    private final String stateName;
}
