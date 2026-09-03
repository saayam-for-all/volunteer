package org.sfa.volunteer.controller;

import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/{version}/organizations")
public class OrganizationController {

    private final OrganizationService organizationService;
    private final ResponseBuilder responseBuilder;

    public OrganizationController(OrganizationService organizationService,
                                  ResponseBuilder responseBuilder) {
        this.organizationService = organizationService;
        this.responseBuilder = responseBuilder;
    }

    @GetMapping("/search")
    public ResponseEntity<SaayamResponse<List<OrganizationResponse>>> searchOrganizations(
            @RequestParam String name) {
        List<OrganizationResponse> results = organizationService.searchByName(name);
        SaayamResponse<List<OrganizationResponse>> response = responseBuilder.buildSuccessResponse(
                SaayamStatusCode.SUCCESS, new Object[]{name}, results);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<SaayamResponse<OrganizationResponse>> createOrganization(
            @Valid @RequestBody CreateOrganizationRequest request) {
        OrganizationResponse created = organizationService.createOrganization(request);
        SaayamResponse<OrganizationResponse> response = responseBuilder.buildSuccessResponse(
                SaayamStatusCode.ORGANIZATION_CREATED, new Object[]{}, created);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{orgId}")
    public ResponseEntity<SaayamResponse<OrganizationDetailsResponse>> updateOrganization(
            @PathVariable String orgId,
            @Valid @RequestBody UpdateOrganizationRequest request) {
        OrganizationDetailsResponse updated = organizationService.updateOrganization(orgId, request);
        SaayamResponse<OrganizationDetailsResponse> response = responseBuilder.buildSuccessResponse(
                SaayamStatusCode.ORGANIZATION_UPDATED, new Object[]{orgId}, updated);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{orgId}/users/{userId}")
    public ResponseEntity<SaayamResponse<Void>> linkOrganization(
            @PathVariable String orgId,
            @PathVariable String userId) {
        organizationService.linkOrganization(userId, orgId);
        SaayamResponse<Void> response = responseBuilder.buildSuccessResponse(
                SaayamStatusCode.SUCCESS, new Object[]{orgId}, null);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<SaayamResponse<List<OrganizationDetailsResponse>>> getUserOrganizations(
            @PathVariable String userId) {
        List<OrganizationDetailsResponse> orgs = organizationService.getOrganizationsByUserId(userId);
        SaayamResponse<List<OrganizationDetailsResponse>> response = responseBuilder.buildSuccessResponse(
                SaayamStatusCode.SUCCESS, new Object[]{userId}, orgs);
        return ResponseEntity.ok(response);
    }
}