package org.sfa.volunteer.service;

import org.sfa.volunteer.dto.request.VolunteerRequest;
import org.sfa.volunteer.dto.response.VolunteerResponse;
import org.sfa.volunteer.dto.response.PaginationResponse;
import org.sfa.volunteer.dto.response.IdentityDocumentMetadata;
import java.time.LocalDate;
import org.sfa.volunteer.dto.request.OnboardingAvailabilitySaveRequest;
import org.sfa.volunteer.dto.response.OnboardingAvailabilityResponse;

//import org.sfa.volunteer.dto.request.UserVolunteerSkillsRequest;
//import org.sfa.volunteer.dto.response.UserVolunteerSkillsResponse;
//import org.sfa.volunteer.model.UserVolunteerSkills;


public interface VolunteerService {
    record AvailabilityView(Integer stage, com.fasterxml.jackson.databind.JsonNode days,
            com.fasterxml.jackson.databind.JsonNode times, boolean migrationRequired) {}

    java.util.Map<String, AvailabilityView> getAvailabilityViews(java.util.List<org.sfa.volunteer.model.Volunteer> volunteers);

    OnboardingAvailabilityResponse updateVolunteerStep4(OnboardingAvailabilitySaveRequest request);

    OnboardingAvailabilityResponse getOnboardingAvailability(String userId);

    void requireOnboardingAvailabilityReady();

    PaginationResponse<VolunteerResponse> findAllVolunteersWithPagination(Integer pageNumber, Integer pageSize);
    

    String getGovtIdPath(String userId, int documentSlot) throws Exception;

    VolunteerResponse createVolunteer(VolunteerRequest volunteerRequest) throws Exception;

    VolunteerResponse updateVolunteer(VolunteerRequest volunteerRequest) throws Exception;

    VolunteerResponse updateVolunteerStep1(VolunteerRequest volunteerRequest) throws Exception;

    VolunteerResponse updateVolunteerStep2(VolunteerRequest volunteerRequest) throws Exception;

    VolunteerResponse updateVolunteerStep3(VolunteerRequest volunteerRequest) throws Exception;

    VolunteerResponse updateVolunteerStep4(VolunteerRequest volunteerRequest) throws Exception;

    VolunteerResponse updateVolunteerCompletion(VolunteerRequest volunteerRequest);

    VolunteerResponse getVolunteerByUserId(String userId) throws Exception;
    
    IdentityDocumentMetadata getIdentityDocumentMetadata(String userId, int documentSlot) throws Exception;

//    UserVolunteerSkillsResponse updateSkills(UserVolunteerSkillsRequest request) throws Exception;

//    UserVolunteerSkillsResponse findSkillsList() throws Exception;

    void updateGovtIdPath(String userId, int documentSlot, String s3Path) throws Exception;
    
   void updateGovtIdMetadata(String userId, int documentSlot, String documentName, LocalDate expiresOn) throws Exception;
}