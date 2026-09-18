package org.sfa.volunteer.service;

import org.sfa.volunteer.dto.request.VolunteerRequest;
import org.sfa.volunteer.dto.request.VolunteerUserAvailabilityRequest;
import org.sfa.volunteer.dto.response.VolunteerResponse;
import org.sfa.volunteer.dto.response.PaginationResponse;
import org.sfa.volunteer.dto.response.VolunteerUserAvailabilityResponse;
import org.sfa.volunteer.dto.response.IdentityDocumentMetadata;
import java.time.LocalDate;

//import org.sfa.volunteer.dto.request.UserVolunteerSkillsRequest;
//import org.sfa.volunteer.dto.response.UserVolunteerSkillsResponse;
//import org.sfa.volunteer.model.UserVolunteerSkills;

import java.util.List;

public interface VolunteerService {

    PaginationResponse<VolunteerResponse> findAllVolunteersWithPagination(Integer pageNumber, Integer pageSize);
    

    String getGovtIdPath(String userId, int documentSlot) throws Exception;

    VolunteerResponse createVolunteer(VolunteerRequest volunteerRequest) throws Exception;

    VolunteerResponse updateVolunteer(VolunteerRequest volunteerRequest) throws Exception;

    VolunteerResponse updateVolunteerStep1(VolunteerRequest volunteerRequest) throws Exception;

    VolunteerResponse updateVolunteerStep2(VolunteerRequest volunteerRequest) throws Exception;

    VolunteerResponse updateVolunteerStep3(VolunteerRequest volunteerRequest) throws Exception;

    VolunteerResponse updateVolunteerStep4(VolunteerRequest volunteerRequest) throws Exception;

    VolunteerResponse updateVolunteerCompletion(VolunteerRequest volunteerRequest) throws Exception;

    VolunteerResponse getVolunteerByUserId(String userId) throws Exception;
    
    IdentityDocumentMetadata getIdentityDocumentMetadata(String userId, int documentSlot) throws Exception;

    List<VolunteerUserAvailabilityResponse> updateVolunteerUserAvailability(String userId, List<VolunteerUserAvailabilityRequest> request) throws Exception;

    List<VolunteerUserAvailabilityResponse> getVolunteerUserAvailability(String userId) throws Exception;

//    UserVolunteerSkillsResponse updateSkills(UserVolunteerSkillsRequest request) throws Exception;

//    UserVolunteerSkillsResponse findSkillsList() throws Exception;

    void updateGovtIdPath(String userId, int documentSlot, String s3Path) throws Exception;
    
   void updateGovtIdMetadata(String userId, int documentSlot, String documentName, LocalDate expiresOn) throws Exception;
}