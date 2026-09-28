package org.sfa.volunteer.service;

import java.util.List;

import org.sfa.volunteer.dto.request.CreateOrganizationRequest;
import org.sfa.volunteer.dto.request.UpdateOrganizationRequest;
import org.sfa.volunteer.dto.response.OrganizationDetailsResponse;
import org.sfa.volunteer.dto.response.OrganizationResponse;

public interface OrganizationService {

    List<OrganizationResponse> searchByName(String name);

    OrganizationResponse createOrganization(
            CreateOrganizationRequest request
    );

    List<OrganizationDetailsResponse> getOrganizationsByUserId(
            String userId
    );

    void linkOrganization(String userId, String orgId);

    OrganizationDetailsResponse updateOrganization(String orgId, UpdateOrganizationRequest request);
}