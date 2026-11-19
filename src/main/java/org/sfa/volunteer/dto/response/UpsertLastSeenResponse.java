package org.sfa.volunteer.dto.response;

import lombok.Builder;

@Builder
public record UpsertLastSeenResponse(
                Boolean created,
                Boolean updated,
                String message) {
}
