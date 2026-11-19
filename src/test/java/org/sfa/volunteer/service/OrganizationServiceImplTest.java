package org.sfa.volunteer.service;

import org.sfa.volunteer.model.OrgSkill;
import static org.mockito.ArgumentMatchers.anyList;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.sfa.volunteer.model.UserOrgMap;
import org.sfa.volunteer.dto.request.UpdateOrganizationRequest;
import org.sfa.volunteer.dto.response.OrganizationDetailsResponse;
import org.sfa.volunteer.exception.OrganizationNotFoundException;
import static org.mockito.Mockito.verify;
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
import org.sfa.volunteer.repository.OrgSkillRepository;
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

    @Mock
    private OrgSkillRepository orgSkillRepository;

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
                .orgId("ORG-000-000-000-0001")
                .orgName("Red Cross")
                .cityName("Washington")
                .stateId("DC")
                .build();

        when(organizationRepository.findByOrgNameContainingIgnoreCase("red"))
                .thenReturn(List.of(org));

        List<OrganizationResponse> results = organizationService.searchByName("red");

        assertEquals(1, results.size());
        assertEquals("Red Cross", results.get(0).orgName());
        assertEquals("ORG-000-000-000-0001", results.get(0).orgId());
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
                .orgId("ORG-000-000-000-0004")
                .orgName("Hope Shelter")
                .cityName("Boston")
                .stateId("MA")
                .build();

        when(organizationRepository.save(any(Organization.class)))
                .thenReturn(savedOrg);

        OrganizationResponse response = organizationService.createOrganization(request);

        assertEquals("ORG-000-000-000-0004", response.orgId());
        assertEquals("Hope Shelter", response.orgName());
    }
        @Test
    void getOrganizationsByUserId_returnsMappedOrganizations() {
        UserOrgMap link = UserOrgMap.builder()
                .userId("SID-00-000-000-001").orgId("ORG-000-000-000-0001")
                .userRole("VOLUNTEER").build();
        Organization org = Organization.builder()
                .orgId("ORG-000-000-000-0001").orgName("Red Cross")
                .orgType("non_profit").orgSize("large").cityName("Washington")
                .stateId("DC").build();

        when(userOrgMapRepository.findByUserId("SID-00-000-000-001"))
                .thenReturn(List.of(link));
        when(organizationRepository.findById("ORG-000-000-000-0001"))
                .thenReturn(Optional.of(org));
        when(orgSkillRepository.findByOrgId("ORG-000-000-000-0001"))
                .thenReturn(Collections.emptyList());

        List<OrganizationDetailsResponse> results =
                organizationService.getOrganizationsByUserId("SID-00-000-000-001");

        assertEquals(1, results.size());
        assertEquals("Red Cross", results.get(0).orgName());
        assertEquals("non_profit", results.get(0).orgType());
    }

    @Test
    void linkOrganization_savesMapping() {
        Organization org = Organization.builder()
                .orgId("ORG-000-000-000-0001").orgName("Red Cross").build();
        when(organizationRepository.findById("ORG-000-000-000-0001"))
                .thenReturn(Optional.of(org));

        organizationService.linkOrganization("SID-00-000-000-001", "ORG-000-000-000-0001");

        verify(userOrgMapRepository).save(any(UserOrgMap.class));
    }

    @Test
    void linkOrganization_throws_whenOrganizationNotFound() {
        when(organizationRepository.findById("NOPE")).thenReturn(Optional.empty());

        assertThrows(OrganizationNotFoundException.class,
                () -> organizationService.linkOrganization("SID-00-000-000-001", "NOPE"));
    }

    @Test
    void updateOrganization_updatesFieldsAndNormalisesEnums() {
        Organization existing = Organization.builder()
                .orgId("ORG-000-000-000-0001").orgName("Old Name").build();
        when(organizationRepository.findById("ORG-000-000-000-0001"))
                .thenReturn(Optional.of(existing));
        when(organizationRepository.save(any(Organization.class)))
                .thenAnswer(i -> i.getArgument(0));
        when(orgSkillRepository.findByOrgId("ORG-000-000-000-0001"))
                .thenReturn(Collections.emptyList());

        UpdateOrganizationRequest request = UpdateOrganizationRequest.builder()
                .orgName("New Name").orgType("NON_PROFIT").orgSize("SMALL")
                .zipCode("21228").build();

        OrganizationDetailsResponse result =
                organizationService.updateOrganization("ORG-000-000-000-0001", request);

        assertEquals("New Name", result.orgName());
        assertEquals("non_profit", result.orgType());
        assertEquals("small", result.orgSize());
    }

    @Test
    void updateOrganization_throws_whenOrganizationNotFound() {
        when(organizationRepository.findById("NOPE")).thenReturn(Optional.empty());

        assertThrows(OrganizationNotFoundException.class,
                () -> organizationService.updateOrganization("NOPE",
                        UpdateOrganizationRequest.builder().orgName("X").build()));
    }
    
    @Test
    void createOrganization_savesCategorySkills() {
        Organization saved = Organization.builder()
                .orgId("ORG-000-000-000-0005").orgName("Food Bank")
                .cityName("Baltimore").stateId("MD").build();
        when(organizationRepository.save(any(Organization.class))).thenReturn(saved);

        CreateOrganizationRequest request = CreateOrganizationRequest.builder()
                .orgName("Food Bank").cityName("Baltimore").stateId("MD")
                .zipCode("21201").categoryIds(List.of("1.1", "1.2")).build();

        organizationService.createOrganization(request);

        verify(orgSkillRepository).saveAll(anyList());
    }

    @Test
    void updateOrganization_replacesCategorySkills() {
        Organization existing = Organization.builder()
                .orgId("ORG-000-000-000-0001").orgName("Old").build();
        when(organizationRepository.findById("ORG-000-000-000-0001"))
                .thenReturn(Optional.of(existing));
        when(organizationRepository.save(any(Organization.class)))
                .thenAnswer(i -> i.getArgument(0));
        when(orgSkillRepository.findByOrgId("ORG-000-000-000-0001"))
                .thenReturn(List.of(OrgSkill.builder()
                        .orgId("ORG-000-000-000-0001").catId("1.1").build()));

        UpdateOrganizationRequest request = UpdateOrganizationRequest.builder()
                .orgName("New").zipCode("21228").categoryIds(List.of("2.1")).build();

        OrganizationDetailsResponse result =
                organizationService.updateOrganization("ORG-000-000-000-0001", request);

        verify(orgSkillRepository).deleteByOrgId("ORG-000-000-000-0001");
        verify(orgSkillRepository).saveAll(anyList());
        assertEquals(List.of("1.1"), result.categoryIds());
    }
}

