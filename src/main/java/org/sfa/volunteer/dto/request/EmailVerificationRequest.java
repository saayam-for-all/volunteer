package org.sfa.volunteer.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmailVerificationRequest {
    private String email;
    private Long userId;
    private boolean useMagicLink;
}
