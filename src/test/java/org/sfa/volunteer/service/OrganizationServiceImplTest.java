package org.sfa.volunteer.service;

import org.sfa.volunteer.dto.request.CreateOrganizationRequest;
import static org.mockito.ArgumentMatchers.any;
import org.sfa.volunteer.model.Organization;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sfa.volunteer.dto.response.OrganizationResponse;
import org.sfa.volunteer.repository.OrganizationRepository;
import org.sfa.volunteer.repository.UserOrgMapRepository;
import org.sfa.volunteer.service.impl.OrganizationServiceImpl;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationServiceImplTest {

    @Mock
    private OrganizationRepository organizationRepository;
 
    @Mock
    private UserOrgMapRepository userOrgMapRepository;

    @InjectMocks
    private OrganizationServiceImpl organizationService;

    @Test
    void searchByName_returnsEmptyList_whenNoMatches() {
        when(organizationRepository.findByOrgNameContainingIgnoreCase("xyz"))
                .thenReturn(Collections.emptyList());

        List<OrganizationResponse> results = organizationService.searchByName("xyz");

        assertTrue(results.isEmpty());
    }
    @Test
    void searchByName_returnsMatchingOrganizations() {
        Organization org = Organization.builder()
                .orgId("ORG-00-000-000-001")
                .orgName("Red Cross")
                .cityName("Washington")
                .stateId("DC")
                .build();

        when(organizationRepository.findByOrgNameContainingIgnoreCase("red"))
                .thenReturn(List.of(org));

        List<OrganizationResponse> results = organizationService.searchByName("red");

        assertEquals(1, results.size());
        assertEquals("Red Cross", results.get(0).orgName());
        assertEquals("ORG-00-000-000-001", results.get(0).orgId());
    }
    @Test
    void createOrganization_returnsGeneratedOrgId() {
        CreateOrganizationRequest request = CreateOrganizationRequest.builder()
                .orgName("Hope Shelter")
                .cityName("Boston")
                .stateId("MA")
                .zipCode("02101")
                .build();

        Organization savedOrg = Organization.builder()
                .orgId("ORG-00-000-000-004")
                .orgName("Hope Shelter")
                .cityName("Boston")
                .stateId("MA")
                .build();

        when(organizationRepository.save(any(Organization.class)))
                .thenReturn(savedOrg);

        OrganizationResponse response = organizationService.createOrganization(request);

        assertEquals("ORG-00-000-000-004", response.orgId());
        assertEquals("Hope Shelter", response.orgName());
    }
}

