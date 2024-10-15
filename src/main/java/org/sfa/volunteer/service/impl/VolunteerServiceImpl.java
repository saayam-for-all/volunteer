package org.sfa.volunteer.service.impl;
import jakarta.transaction.Transactional;
import org.sfa.volunteer.dto.request.VolunteerRequest;
import org.sfa.volunteer.dto.response.VolunteerResponse;
import org.sfa.volunteer.dto.response.PaginationResponse;
import org.sfa.volunteer.exception.UserNotFoundException;
import org.sfa.volunteer.exception.VolunteerException;
import org.sfa.volunteer.model.Volunteer;
import org.sfa.volunteer.model.User;
import org.sfa.volunteer.repository.UserRepository;
import org.sfa.volunteer.repository.VolunteerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.sfa.volunteer.service.VolunteerService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.sfa.volunteer.exception.VolunteerException.volunteerNotFound;

@Service
@Transactional
public class VolunteerServiceImpl implements VolunteerService {
    private final VolunteerRepository volunteerRepository;
    private final UserRepository userRepository;

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;

    @Autowired
    public VolunteerServiceImpl(VolunteerRepository volunteerRepository, UserRepository userRepository) {
        this.userRepository = userRepository;
        this.volunteerRepository = volunteerRepository;
    }

    private void updateUser(User user, Integer step) {
        user.setVolunteerStage(step);
        user.setVolunteerUpdateDate(ZonedDateTime.now(ZoneId.of("UTC")));
        user = userRepository.save(user);
    }

    @Override
    public VolunteerResponse createVolunteer(VolunteerRequest request) throws Exception {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new UserNotFoundException(request.userId()));

        Volunteer volunteer = volunteerRepository.findVolunteerByUserId(request.userId());
        if (Objects.nonNull(volunteer)) {
            throw VolunteerException.volunteerExists(request.userId());
        }
        if (request.step() != 1)
            throw VolunteerException.volunteerInvalidStep(request.userId());
        else {
            volunteer = Volunteer.builder()
                    .user(user)
                    .termsAndConditions(request.termsAndConditions())
                    .tcUpdateDate(ZonedDateTime.now(ZoneId.of("UTC")))
                    .build();
            volunteer = volunteerRepository.save(volunteer);
            updateUser(user, request.step());
            return mapToVolunteerResponse(volunteer);
        }
    }

    @Override
    public VolunteerResponse updateVolunteer(VolunteerRequest request) throws Exception {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new UserNotFoundException(request.userId()));

        VolunteerResponse response = null;
        if (request.step() ==1)
            response = updateVolunteerStep1(request);

        return response;
    }

    @Override
    public VolunteerResponse updateVolunteerStep1(VolunteerRequest request) throws Exception {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new UserNotFoundException(request.userId()));

        Volunteer volunteer = volunteerRepository.findVolunteerByUserId(request.userId());
        if (Objects.isNull(volunteer)) {
            throw VolunteerException.volunteerNotFound(request.userId());
        }
        if (request.step() != 1)
            throw VolunteerException.volunteerInvalidStep(request.userId());

        volunteer.setTermsAndConditions(request.termsAndConditions());
        volunteer.setTcUpdateDate(ZonedDateTime.now(ZoneId.of("UTC")));
        volunteer = volunteerRepository.save(volunteer);

        updateUser(user, request.step());

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

    @Override
    public VolunteerResponse getVolunteerByUserId(String userId) throws Exception {
        Volunteer volunteer = volunteerRepository.findVolunteerByUserId(userId);
        if (volunteer == null || volunteer.getId() == 0) {
            throw VolunteerException.volunteerNotFound(userId);
        }
        return mapToVolunteerResponse(volunteer);
    }

    private VolunteerResponse mapToVolunteerResponse(Volunteer volunteer) {
        return VolunteerResponse.builder()
                .id(volunteer.getId())
                .userId(volunteer.getUser().getId())
                .termsAndConditions(volunteer.getTermsAndConditions())
                .tcUpdateDate(volunteer.getTcUpdateDate())
                .govtIdFilename(volunteer.getGovtIdFilename())
                .govtUpdateDate(volunteer.getGovtUpdateDate())
                .pii(volunteer.getPii())
                .notification(volunteer.getNotification())
                .isCompleted(volunteer.getIsCompleted())
                .completedDate(volunteer.getCompletedDate())
                .build();
    }
}
