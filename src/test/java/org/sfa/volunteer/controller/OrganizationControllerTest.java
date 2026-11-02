package org.sfa.volunteer.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sfa.volunteer.dto.common.SaayamResponse;
import org.sfa.volunteer.dto.common.SaayamStatusCode;
import org.sfa.volunteer.dto.request.CreateOrganizationRequest;
import org.sfa.volunteer.dto.request.UpdateOrganizationRequest;
import org.sfa.volunteer.dto.response.OrganizationDetailsResponse;
import org.sfa.volunteer.dto.response.OrganizationResponse;
import org.sfa.volunteer.service.OrganizationService;
import org.sfa.volunteer.util.ResponseBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationControllerTest {

    @Mock
    private OrganizationService organizationService;

    @Mock
    private ResponseBuilder responseBuilder;

    @InjectMocks
    private OrganizationController organizationController;

    @Test
    void searchOrganizations_returnsOk() {
        List<OrganizationResponse> results = List.of(
                OrganizationResponse.builder()
                        .orgId("ORG-000-000-000-0001").orgName("Red Cross").build());
        SaayamResponse<List<OrganizationResponse>> wrapped =
                SaayamResponse.success(SaayamStatusCode.SUCCESS, "ok", results);

        when(organizationService.searchByName("red")).thenReturn(results);
        when(responseBuilder.buildSuccessResponse(
                eq(SaayamStatusCode.SUCCESS), any(Object[].class), eq(results)))
                .thenReturn(wrapped);

        ResponseEntity<SaayamResponse<List<OrganizationResponse>>> response =
                organizationController.searchOrganizations("red");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(organizationService).searchByName("red");
    }

    @Test
    void createOrganization_returnsCreated() {
        CreateOrganizationRequest request = CreateOrganizationRequest.builder()
                .orgName("Hope Shelter").zipCode("21228").build();
        OrganizationResponse created = OrganizationResponse.builder()
                .orgId("ORG-000-000-000-0002").orgName("Hope Shelter").build();
        SaayamResponse<OrganizationResponse> wrapped =
                SaayamResponse.success(SaayamStatusCode.ORGANIZATION_CREATED, "ok", created);

        when(organizationService.createOrganization(request)).thenReturn(created);
        when(responseBuilder.buildSuccessResponse(
                eq(SaayamStatusCode.ORGANIZATION_CREATED), any(Object[].class), eq(created)))
                .thenReturn(wrapped);

        ResponseEntity<SaayamResponse<OrganizationResponse>> response =
                organizationController.createOrganization(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void updateOrganization_returnsOk() {
        UpdateOrganizationRequest request = UpdateOrganizationRequest.builder()
                .orgName("New Name").zipCode("21228").build();
        OrganizationDetailsResponse updated = OrganizationDetailsResponse.builder()
                .orgId("ORG-000-000-000-0001").orgName("New Name").build();
        SaayamResponse<OrganizationDetailsResponse> wrapped =
                SaayamResponse.success(SaayamStatusCode.ORGANIZATION_UPDATED, "ok", updated);

        when(organizationService.updateOrganization("ORG-000-000-000-0001", request))
                .thenReturn(updated);
        when(responseBuilder.buildSuccessResponse(
                eq(SaayamStatusCode.ORGANIZATION_UPDATED), any(Object[].class), eq(updated)))
                .thenReturn(wrapped);

        ResponseEntity<SaayamResponse<OrganizationDetailsResponse>> response =
                organizationController.updateOrganization("ORG-000-000-000-0001", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void linkOrganization_delegatesToService() {
        organizationController.linkOrganization("ORG-000-000-000-0001", "SID-00-000-000-001");

        verify(organizationService)
                .linkOrganization("SID-00-000-000-001", "ORG-000-000-000-0001");
    }

    @Test
    void getUserOrganizations_returnsOk() {
        List<OrganizationDetailsResponse> orgs = List.of(
                OrganizationDetailsResponse.builder()
                        .orgId("ORG-000-000-000-0001").orgName("Red Cross").build());
        SaayamResponse<List<OrganizationDetailsResponse>> wrapped =
                SaayamResponse.success(SaayamStatusCode.SUCCESS, "ok", orgs);

        when(organizationService.getOrganizationsByUserId("SID-00-000-000-001"))
                .thenReturn(orgs);
        when(responseBuilder.buildSuccessResponse(
                eq(SaayamStatusCode.SUCCESS), any(Object[].class), eq(orgs)))
                .thenReturn(wrapped);

        ResponseEntity<SaayamResponse<List<OrganizationDetailsResponse>>> response =
                organizationController.getUserOrganizations("SID-00-000-000-001");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}