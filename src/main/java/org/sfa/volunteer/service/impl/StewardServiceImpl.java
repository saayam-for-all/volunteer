package org.sfa.volunteer.service.impl;

import java.util.List;

import org.sfa.volunteer.dto.response.PaginationResponse;
import org.sfa.volunteer.dto.response.StewardVolunteerResponse;
import org.sfa.volunteer.model.Volunteer;
import org.sfa.volunteer.repository.VolunteerRepository;
import org.sfa.volunteer.service.LegacyAvailabilityProjection;
import org.sfa.volunteer.service.StewardService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StewardServiceImpl implements StewardService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;

    private final VolunteerRepository volunteerRepository;
    private final LegacyAvailabilityProjection legacyAvailability;

    public StewardServiceImpl(VolunteerRepository volunteerRepository, LegacyAvailabilityProjection legacyAvailability) {
        this.volunteerRepository = volunteerRepository;
        this.legacyAvailability = legacyAvailability;
    }

    @Override
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
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
        var availability = legacyAvailability.read(volunteerPage.getContent());

        List<StewardVolunteerResponse> items = volunteerPage
                .stream()
                .map(volunteer -> mapToResponse(volunteer.getId(), availability.get(volunteer.getId())))
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

    private StewardVolunteerResponse mapToResponse(String userId, LegacyAvailabilityProjection.View availability) {
        return StewardVolunteerResponse.builder()
                .userId(userId)
                .volunteerStage(availability.stage())
                .availabilityDays(availability.days())
                .availabilityTimes(availability.times())
                .availabilityMigrationRequired(availability.migrationRequired())
                .build();
    }
}
