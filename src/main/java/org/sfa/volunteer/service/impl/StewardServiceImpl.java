package org.sfa.volunteer.service.impl;

import java.util.List;

import org.sfa.volunteer.dto.response.PaginationResponse;
import org.sfa.volunteer.dto.response.StewardVolunteerResponse;
import org.sfa.volunteer.model.User;
import org.sfa.volunteer.model.Volunteer;
import org.sfa.volunteer.repository.VolunteerRepository;
import org.sfa.volunteer.service.StewardService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class StewardServiceImpl implements StewardService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;

    private final VolunteerRepository volunteerRepository;

    public StewardServiceImpl(VolunteerRepository volunteerRepository) {
        this.volunteerRepository = volunteerRepository;
    }

    @Override
    public PaginationResponse<StewardVolunteerResponse> getVolunteers(
            Integer page,
            Integer size
    ) {
        int pageNumber = page == null || page < 0
                ? DEFAULT_PAGE
                : page;

        int pageSize = size == null || size <= 0
                ? DEFAULT_SIZE
                : size;

        Pageable pageable = PageRequest.of(pageNumber, pageSize);

        Page<Volunteer> volunteerPage =
                volunteerRepository.findAll(pageable);

        List<StewardVolunteerResponse> items = volunteerPage
                .stream()
                .map(this::mapToResponse)
                .toList();

        return PaginationResponse.<StewardVolunteerResponse>builder()
                .currentPage(volunteerPage.getNumber())
                .pageSize(volunteerPage.getSize())
                .totalPages(volunteerPage.getTotalPages())
                .totalItems(volunteerPage.getTotalElements())
                .items(items)
                .hasNextPage(volunteerPage.hasNext())
                .hasPreviousPage(volunteerPage.hasPrevious())
                .build();
    }

    private StewardVolunteerResponse mapToResponse(
            Volunteer volunteer
    ) {
        User user = volunteer.getUser();

        String userId = user != null
                ? user.getId()
                : volunteer.getId();

        Integer volunteerStage = user != null
                ? user.getVolunteerStage()
                : null;

        return StewardVolunteerResponse.builder()
                .userId(userId)
                .volunteerStage(volunteerStage)
                .availabilityDays(volunteer.getAvailabilityDays())
                .availabilityTimes(volunteer.getAvailabilityTimes())
                .build();
    }
}