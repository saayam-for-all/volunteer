package org.sfa.volunteer.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserOrganizationNotFoundException extends RuntimeException {
  private final String userId;
}
