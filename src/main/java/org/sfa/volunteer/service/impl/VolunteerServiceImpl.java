package org.sfa.volunteer.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import jakarta.transaction.Transactional;
import org.sfa.volunteer.dto.request.VolunteerRequest;
import org.sfa.volunteer.dto.request.VolunteerUserAvailabilityRequest;
import org.sfa.volunteer.dto.response.PaginationResponse;
import org.sfa.volunteer.dto.response.VolunteerResponse;
import org.sfa.volunteer.exception.UserNotFoundException;
import org.sfa.volunteer.exception.VolunteerException;
import org.sfa.volunteer.model.User;
import org.sfa.volunteer.model.Volunteer;
import org.sfa.volunteer.repository.UserRepository;
import org.sfa.volunteer.repository.VolunteerRepository;
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

    // private final UserVolunteerSkillsRepository userVolunteerSkillsRepository;

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;

    @Autowired
    public VolunteerServiceImpl(VolunteerRepository volunteerRepository, UserRepository userRepository) {
        this.userRepository = userRepository;
        this.volunteerRepository = volunteerRepository;
        // this.userVolunteerSkillsRepository = userVolunteerSkillsRepository;
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

        return mapToVolunteerResponse(volunteer);
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
}
