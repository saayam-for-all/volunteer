package org.sfa.volunteer.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserCategoryNotFoundException extends RuntimeException {
    private final Integer userCategoryId;
}
