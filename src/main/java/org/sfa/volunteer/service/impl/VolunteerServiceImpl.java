package org.sfa.volunteer.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import jakarta.transaction.Transactional;
import org.sfa.volunteer.dto.request.VolunteerRequest;
import org.sfa.volunteer.dto.request.VolunteerUserAvailabilityRequest;
import org.sfa.volunteer.dto.response.PaginationResponse;
import org.sfa.volunteer.dto.response.VolunteerBasedOnSkillsResponse;
import org.sfa.volunteer.dto.response.VolunteerResponse;
import org.sfa.volunteer.dto.response.VolunteerUserAvailabilityResponse;
import org.sfa.volunteer.exception.UserNotFoundException;
import org.sfa.volunteer.exception.VolunteerException;
import org.sfa.volunteer.model.User;
import org.sfa.volunteer.model.Volunteer;
import org.sfa.volunteer.model.VolunteerUserAvailability;
import org.sfa.volunteer.repository.UserRepository;
import org.sfa.volunteer.repository.UserSkillRepository;
import org.sfa.volunteer.repository.UserVolunteerSkillRepository;
import org.sfa.volunteer.repository.VolunteerRepository;
import org.sfa.volunteer.repository.VolunteerUserAvailabilityRepository;
import org.sfa.volunteer.service.VolunteerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class VolunteerServiceImpl implements VolunteerService {
    private final VolunteerRepository volunteerRepository;
    private final UserRepository userRepository;
    private final VolunteerUserAvailabilityRepository userAvailabilityRepository;
    private final UserVolunteerSkillRepository userVolunteerSkillRepository;
    private final UserSkillRepository userSkillRepository;

    // private final UserVolunteerSkillsRepository userVolunteerSkillsRepository;

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;

    @Autowired
    public VolunteerServiceImpl(VolunteerRepository volunteerRepository, UserRepository userRepository,
                                VolunteerUserAvailabilityRepository volunteerUserAvailabilityRepository,
                                VolunteerUserAvailabilityRepository userAvailabilityRepository,
                                UserVolunteerSkillRepository userVolunteerSkillRepository,
                                UserSkillRepository userSkillRepository) {
        this.userRepository = userRepository;
        this.volunteerRepository = volunteerRepository;
        this.userAvailabilityRepository = userAvailabilityRepository;
        this.userVolunteerSkillRepository = userVolunteerSkillRepository;
        this.userSkillRepository=userSkillRepository;
    }

    private void updateUser(User user, Integer step) {
        user.setVolunteerStage(step);
        user.setVolunteerUpdateDate(ZonedDateTime.now(ZoneId.of("UTC")));
        userRepository.save(user);
    }

    @Override
    public VolunteerResponse createVolunteer(VolunteerRequest request) throws Exception {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new UserNotFoundException(request.userId()));

        Volunteer existing = volunteerRepository.findById(request.userId()).orElse(null);
        if (existing != null) throw VolunteerException.volunteerExists(request.userId());

        if (request.step() != 1) throw VolunteerException.volunteerInvalidStep(request.userId());

        Volunteer volunteer = Volunteer.builder()
                .user(user)
                .termsAndConditions(request.termsAndConditions())
                .termsAcceptedAt(LocalDateTime.now(ZoneId.of("UTC")))
                .build();

        volunteer = volunteerRepository.save(volunteer);
        updateUser(user, request.step());

        List<String> skills = Optional.ofNullable(request.skills()).orElse(Collections.emptyList());
        if (!skills.isEmpty()) {
            saveUserSkills(user, skills);
    }

        return mapToVolunteerResponse(volunteer);
    }

    @Override
    public VolunteerResponse updateVolunteer(VolunteerRequest request) throws Exception {
        if (request.step() == 1) return updateVolunteerStep1(request);
        if (request.step() == 2) return updateVolunteerStep2(request);
        if (request.step() == 3) return updateVolunteerStep3(request);
        if (request.step() == 4) return updateVolunteerStep4(request);
        throw VolunteerException.volunteerInvalidStep(request.userId());
    }

    @Override
    public VolunteerResponse updateVolunteerStep1(VolunteerRequest request) throws Exception {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new UserNotFoundException(request.userId()));

        Volunteer volunteer = volunteerRepository.findById(request.userId())
                .orElseThrow(() -> {
                    try {
                        return VolunteerException.volunteerNotFound(request.userId());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

        if (request.step() != 1) throw VolunteerException.volunteerInvalidStep(request.userId());

        volunteer.setUser(user);
        volunteer.setTermsAndConditions(request.termsAndConditions());
        volunteer.setTermsAcceptedAt(LocalDateTime.now(ZoneId.of("UTC")));

        volunteer = volunteerRepository.save(volunteer);
        updateUser(user, request.step());

        List<String> skills = Optional.ofNullable(request.skills()).orElse(Collections.emptyList());
        if (!skills.isEmpty()) {
            saveUserSkills(user, skills);
    }

        return mapToVolunteerResponse(volunteer);
    }

    @Override
    public VolunteerResponse updateVolunteerStep2(VolunteerRequest request) throws Exception {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new UserNotFoundException(request.userId()));

        Volunteer volunteer = volunteerRepository.findById(request.userId())
                .orElseThrow(() -> {
                    try {
                        return VolunteerException.volunteerNotFound(request.userId());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

        if (request.step() != 2) throw VolunteerException.volunteerInvalidStep(request.userId());

        volunteer.setUser(user);

        // Persist new DB columns
        if (request.govtIdPath1() != null) {
            volunteer.setGovtIdPath1(request.govtIdPath1());
            volunteer.setPath1UpdatedAt(LocalDateTime.now(ZoneId.of("UTC")));
        }
        if (request.govtIdPath2() != null) {
            volunteer.setGovtIdPath2(request.govtIdPath2());
            volunteer.setPath2UpdatedAt(LocalDateTime.now(ZoneId.of("UTC")));
        }

        volunteer = volunteerRepository.save(volunteer);
        updateUser(user, request.step());
        List<String> skills = Optional.ofNullable(request.skills()).orElse(Collections.emptyList());
        if (!skills.isEmpty()) {
            saveUserSkills(user, skills);
    }

        return mapToVolunteerResponse(volunteer);
    }

    @Override
    public VolunteerResponse updateVolunteerStep3(VolunteerRequest request) throws Exception {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new UserNotFoundException(request.userId()));

        Volunteer volunteer = volunteerRepository.findById(request.userId())
                .orElseThrow(() -> {
                    try {
                        return VolunteerException.volunteerNotFound(request.userId());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

        if (request.step() != 3) throw VolunteerException.volunteerInvalidStep(request.userId());

        volunteer.setUser(user);
        volunteer = volunteerRepository.save(volunteer);
        updateUser(user, request.step());
        List<String> skills = Optional.ofNullable(request.skills()).orElse(Collections.emptyList());
        if (!skills.isEmpty()) {
            saveUserSkills(user, skills);
    }

        return mapToVolunteerResponse(volunteer);
    }

    @Override
    public VolunteerResponse updateVolunteerStep4(VolunteerRequest request) throws Exception {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new UserNotFoundException(request.userId()));

        Volunteer volunteer = volunteerRepository.findById(request.userId())
                .orElseThrow(() -> {
                    try {
                        return VolunteerException.volunteerNotFound(request.userId());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

        if (request.step() != 4) throw VolunteerException.volunteerInvalidStep(request.userId());

        volunteer.setUser(user);

        // Convert request.availability -> availability_days + availability_times JSONB
        List<VolunteerUserAvailabilityRequest> avail = Optional.ofNullable(request.availability())
                .orElse(Collections.emptyList());

        ArrayNode days = MAPPER.createArrayNode();
        Set<String> uniqueDays = new LinkedHashSet<>();
        for (var a : avail) {
            if (a != null && a.dayOfWeek() != null) uniqueDays.add(a.dayOfWeek());
        }
        uniqueDays.forEach(days::add);

        // Store full availability items as JSON array
        JsonNode times = MAPPER.valueToTree(avail);

        volunteer.setAvailabilityDays(days);
        volunteer.setAvailabilityTimes(times);

        volunteer = volunteerRepository.save(volunteer);
        updateUser(user, request.step());

        List<String> skills = Optional.ofNullable(request.skills()).orElse(Collections.emptyList());
        if (!skills.isEmpty()) {
            saveUserSkills(user, skills);
    }

        return mapToVolunteerResponse(volunteer);
    }

    @Override
    public List<VolunteerUserAvailabilityResponse> updateVolunteerUserAvailability(String userId,
                                                                                   List<VolunteerUserAvailabilityRequest> request) throws Exception {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        Volunteer volunteer = volunteerRepository.findVolunteerByUserId(userId);
        if (Objects.isNull(volunteer)) {
            throw VolunteerException.volunteerNotFound(userId);
        }

        List<VolunteerUserAvailability> userAvailabilityList = request.stream()
                .map(req -> mapToVolunteerUserAvailability(req, user)).collect(Collectors.toList());

        List<VolunteerUserAvailability> savedAvailabilityList = userAvailabilityRepository
                .saveAll(userAvailabilityList);
        return savedAvailabilityList.stream().map(this::mapToVolunteerUserAvailabilityResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<VolunteerUserAvailabilityResponse> getVolunteerUserAvailability(String userId) {
        Volunteer volunteer = volunteerRepository.findById(userId)
                .orElseThrow(() -> {
                    try {
                        return VolunteerException.volunteerNotFound(userId);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

        return toAvailabilityResponses((JsonNode) volunteer.getAvailabilityTimes(), userId);
    }

    private List<VolunteerUserAvailabilityResponse> toAvailabilityResponses(JsonNode availabilityTimes, String userId) {
        if (availabilityTimes == null || !availabilityTimes.isArray()) return List.of();

        List<VolunteerUserAvailabilityRequest> reqs = new ArrayList<>();
        for (JsonNode node : availabilityTimes) {
            reqs.add(MAPPER.convertValue(node, VolunteerUserAvailabilityRequest.class));
        }

        // Map request -> response
        return reqs.stream()
                .map(r -> VolunteerUserAvailabilityResponse.builder()
                        .id(r.id())
                        .userId(userId)
                        .dayOfWeek(r.dayOfWeek())
                        .startTime(r.startTime() == null ? null : r.startTime().atZone(ZoneId.of("UTC")))
                        .endTime(r.endTime() == null ? null : r.endTime().atZone(ZoneId.of("UTC")))
                        .lastUpdateDate(ZonedDateTime.now(ZoneId.of("UTC")))
                        .build()
                )
                .collect(Collectors.toList());
    }

    // @Override
    // public UserVolunteerSkillsResponse findSkillsList() throws Exception {
    // return null;
    // }

    @Override
    public VolunteerResponse updateVolunteerCompletion(VolunteerRequest request) throws Exception {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new UserNotFoundException(request.userId()));

        Volunteer volunteer = volunteerRepository.findById(request.userId())
                .orElseThrow(() -> {
                    try {
                        return VolunteerException.volunteerNotFound(request.userId());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

        Integer step = request.step() != null ? request.step() : 4;
        updateUser(user, step);

        return mapToVolunteerResponse(volunteer);
    }

    @Override
    public VolunteerResponse getVolunteerByUserId(String userId) throws Exception {
        Volunteer volunteer = volunteerRepository.findById(userId)
                .orElseThrow(() -> {
                    try {
                        return VolunteerException.volunteerNotFound(userId);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
        return mapToVolunteerResponse(volunteer);
    }

    @Override
    public PaginationResponse<VolunteerResponse> findAllVolunteersWithPagination(Integer pageNumber, Integer pageSize) {
        int pageNum = (pageNumber == null) ? DEFAULT_PAGE : pageNumber;
        int pageSizeNum = (pageSize == null) ? DEFAULT_SIZE : pageSize;

        Pageable pageable = PageRequest.of(pageNum, pageSizeNum);
        Page<Volunteer> volunteerPage = volunteerRepository.findAll(pageable);

        List<VolunteerResponse> volunteers = volunteerPage.stream()
                .map(this::mapToVolunteerResponse)
                .collect(Collectors.toList());

        return PaginationResponse.<VolunteerResponse>builder()
                .currentPage(volunteerPage.getNumber())
                .pageSize(volunteerPage.getSize())
                .totalPages(volunteerPage.getTotalPages())
                .totalItems(volunteerPage.getTotalElements())
                .items(volunteers)
                .hasNextPage(volunteerPage.hasNext())
                .hasPreviousPage(volunteerPage.hasPrevious())
                .build();
    }

    private VolunteerResponse mapToVolunteerResponse(Volunteer volunteer) {
        String uid = volunteer.getUser() != null ? volunteer.getUser().getId() : volunteer.getId();

        return VolunteerResponse.builder()
                .id(uid)
                .userId(uid)
                .termsAndConditions(volunteer.getTermsAndConditions())
                .termsAcceptedAt(volunteer.getTermsAcceptedAt())
                .govtIdPath1(volunteer.getGovtIdPath1())
                .govtIdPath2(volunteer.getGovtIdPath2())
                .path1UpdatedAt(volunteer.getPath1UpdatedAt())
                .path2UpdatedAt(volunteer.getPath2UpdatedAt())
                .availabilityDays((JsonNode) volunteer.getAvailabilityDays())
                .availabilityTimes((JsonNode) volunteer.getAvailabilityTimes())
                .createdAt(volunteer.getCreatedAt())
                .lastUpdatedAt(volunteer.getLastUpdatedAt())
                .build();
    }

    private VolunteerResponse mapToVolunteerResponse(Volunteer volunteer,
                                                     List<VolunteerUserAvailability> availabilityList) {
        String uid = volunteer.getUser() != null ? volunteer.getUser().getId() : volunteer.getId();

        return VolunteerResponse.builder()
                .id(uid)
                .userId(uid)
                .termsAndConditions(volunteer.getTermsAndConditions())
                .termsAcceptedAt(volunteer.getTermsAcceptedAt())
                .govtIdPath1(volunteer.getGovtIdPath1())
                .govtIdPath2(volunteer.getGovtIdPath2())
                .path1UpdatedAt(volunteer.getPath1UpdatedAt())
                .path2UpdatedAt(volunteer.getPath2UpdatedAt())
                .availabilityDays((JsonNode) volunteer.getAvailabilityDays())
                .availabilityTimes((JsonNode) volunteer.getAvailabilityTimes())
                .createdAt(volunteer.getCreatedAt())
                .lastUpdatedAt(volunteer.getLastUpdatedAt())
                .build();
    }

    private VolunteerUserAvailabilityResponse mapToVolunteerUserAvailabilityResponse(
            VolunteerUserAvailability availability) {
        return VolunteerUserAvailabilityResponse.builder()
                .id(availability.getId())
                .userId(availability.getUser().getId())
                .dayOfWeek(availability.getDayOfWeek())
                .startTime(availability.getStartTime())
                .endTime(availability.getEndTime())
                .lastUpdateDate(availability.getLastUpdateDate())
                .build();
    }

    private VolunteerUserAvailability mapToVolunteerUserAvailability(VolunteerUserAvailabilityRequest request,
                                                                     User user) {
        return VolunteerUserAvailability.builder()
                .user(user)
                .dayOfWeek(request.dayOfWeek())
                .startTime(ZonedDateTime.from(request.startTime()))
                .endTime(ZonedDateTime.from(request.endTime()))
                //.lastUpdateDate(request.lastUpdateDate())
                .build();
    }

    private void saveUserSkills(User user, List<String> skills) {
        List<org.sfa.volunteer.model.UserVolunteerSkill> entities = skills.stream()
                .filter(Objects::nonNull)
                .map(skill -> {
                    var id = new org.sfa.volunteer.model.UserVolunteerSkillId(user.getId(), skill);
                    return org.sfa.volunteer.model.UserVolunteerSkill.builder()
                            .id(id)
                            .user(user)
                            .createdAt(ZonedDateTime.now(ZoneId.of("UTC")))
                            .lastUpdatedAt(ZonedDateTime.now(ZoneId.of("UTC")))
                            .build();
                })
                .collect(Collectors.toList());

        if (!entities.isEmpty()) {
            userVolunteerSkillRepository.saveAll(entities);
        }
    }

    private List<String> generateSkillFallbacks(String skill) {
        List<String> fallbacks = new ArrayList<>();
        String current = skill;

        while (current.contains(".")) {
            fallbacks.add(current);
            current = current.substring(0, current.lastIndexOf('.'));
        }

        fallbacks.add(current);
        System.out.println("Generated skill fallbacks: " + current);
        return fallbacks;
    }

  
    public List<VolunteerBasedOnSkillsResponse> getVolunteersBasedOnSkills(String skills) {

        for (String skill : generateSkillFallbacks(skills)) {

            List<String> userIds = userSkillRepository.findByIdCatId(skill).stream()
                    .map(us -> us.getId().getUserId())
                    .distinct()
                    .limit(10)
                    .toList();

            if (!userIds.isEmpty()) {
                return userRepository.findByIdIn(userIds).stream()
                        .map(u -> new VolunteerBasedOnSkillsResponse(
                                u.getId(),
                                u.getFullName(),
                                u.getPrimaryEmailAddress()))
                        .toList();
            }
        }

        return List.of(); 
    }
    }



