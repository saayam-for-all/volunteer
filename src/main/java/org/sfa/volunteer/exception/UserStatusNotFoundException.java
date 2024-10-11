package org.sfa.volunteer.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserStatusNotFoundException extends RuntimeException {
    private final Integer userStatusId;
}
