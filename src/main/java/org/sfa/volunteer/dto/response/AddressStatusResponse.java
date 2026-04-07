package org.sfa.volunteer.dto.response;

public record AddressStatusResponse(
		String userId,
	    Boolean addressAvailable
) {}
