package org.sfa.volunteer.service.impl;

import org.springframework.http.HttpStatus;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.sfa.volunteer.dto.request.VolunteerRequest;
import org.sfa.volunteer.dto.response.PaginationResponse;
import org.sfa.volunteer.dto.response.VolunteerResponse;
import org.sfa.volunteer.dto.common.SaayamStatusCode;
import org.sfa.volunteer.exception.UserNotFoundException;
import org.sfa.volunteer.exception.VolunteerException;
import org.sfa.volunteer.exception.IdentityDocumentException;
import org.sfa.volunteer.exception.OnboardingAvailabilityException;
import org.sfa.volunteer.model.DocumentStatus;
import org.sfa.volunteer.model.User;
import org.sfa.volunteer.model.Volunteer;
import org.sfa.volunteer.repository.AvailabilityAccountRepository;
import org.sfa.volunteer.repository.OnboardingAvailabilityRepository;
import org.sfa.volunteer.repository.VolunteerRepository;
import org.sfa.volunteer.dto.response.IdentityDocumentMetadata;
import org.sfa.volunteer.service.LegacyAvailabilityProjection;
import org.sfa.volunteer.service.OnboardingAvailabilityService;
import org.sfa.volunteer.service.VolunteerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class VolunteerServiceImpl implements VolunteerService {
    private final VolunteerRepository volunteerRepository;
    private final AvailabilityAccountRepository accounts;
    private final OnboardingAvailabilityRepository wizardProgress;
    private final EntityManager entityManager;
    private final Clock clock;
    private final LegacyAvailabilityProjection legacyAvailability;
    private final OnboardingAvailabilityService onboardingAvailability;

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;

    @Autowired
    public VolunteerServiceImpl(VolunteerRepository volunteerRepository, AvailabilityAccountRepository accounts,
            OnboardingAvailabilityRepository wizardProgress, EntityManager entityManager, Clock clock,
            LegacyAvailabilityProjection legacyAvailability, OnboardingAvailabilityService onboardingAvailability) {
        this.volunteerRepository = volunteerRepository;
        this.accounts = accounts;
        this.wizardProgress = wizardProgress;
        this.entityManager = entityManager;
        this.clock = clock;
        this.legacyAvailability = legacyAvailability;
        this.onboardingAvailability = onboardingAvailability;
    }

    private void lockAccount(String userId) {
        accounts.lockById(userId).orElseThrow(() -> new UserNotFoundException(userId));
    }

    private Volunteer lockedVolunteer(String userId) {
        lockAccount(userId);
        Volunteer volunteer = volunteerRepository.findById(userId)
                .orElseThrow(() -> VolunteerException.volunteerNotFound(userId));
        entityManager.refresh(volunteer);
        return volunteer;
    }

    private void advanceProgress(VolunteerRequest request) {
        wizardProgress.advanceProgress(request.userId(), request.step(),
                LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC));
    }

    @Override
    public VolunteerResponse createVolunteer(VolunteerRequest request) throws Exception {
        lockAccount(request.userId());

        Volunteer existing = volunteerRepository.findById(request.userId()).orElse(null);
        if (existing != null) throw VolunteerException.volunteerExists(request.userId());

        if (request.step() != 1) throw VolunteerException.volunteerInvalidStep(request.userId());

        Volunteer volunteer = Volunteer.builder()
                .user(entityManager.getReference(User.class, request.userId()))
                .termsAndConditions(request.termsAndConditions())
                .termsAcceptedAt(LocalDateTime.now(ZoneId.of("UTC")))
                .build();

        volunteer = volunteerRepository.save(volunteer);
        advanceProgress(request);

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
        Volunteer volunteer = lockedVolunteer(request.userId());

        if (request.step() != 1) throw VolunteerException.volunteerInvalidStep(request.userId());

        volunteer.setTermsAndConditions(request.termsAndConditions());
        volunteer.setTermsAcceptedAt(LocalDateTime.now(ZoneId.of("UTC")));

        volunteer = volunteerRepository.saveAndFlush(volunteer);
        advanceProgress(request);

        return mapToVolunteerResponse(volunteer);
    }

    @Override
    public VolunteerResponse updateVolunteerStep2(VolunteerRequest request) throws Exception {
        Volunteer volunteer = lockedVolunteer(request.userId());

        if (request.step() != 2) throw VolunteerException.volunteerInvalidStep(request.userId());

        // Persist new DB columns
        if (request.govtIdPath1() != null) {
            volunteer.setGovtIdPath1(request.govtIdPath1());
            volunteer.setPath1UpdatedAt(LocalDateTime.now(ZoneId.of("UTC")));
        }
        if (request.govtIdPath2() != null) {
            volunteer.setGovtIdPath2(request.govtIdPath2());
            volunteer.setPath2UpdatedAt(LocalDateTime.now(ZoneId.of("UTC")));
        }

        volunteer = volunteerRepository.saveAndFlush(volunteer);
        advanceProgress(request);

        return mapToVolunteerResponse(volunteer);
    }
    
    @Override
    public IdentityDocumentMetadata getIdentityDocumentMetadata(String userId, int documentSlot) throws Exception {
        validateSlot(documentSlot);

        Volunteer volunteer = volunteerRepository.findById(userId)
            .orElseThrow(() -> VolunteerException.volunteerNotFound(userId));
       
        
        String path = (documentSlot == 1) ? volunteer.getGovtIdPath1() : volunteer.getGovtIdPath2();
        if (path == null || path.isBlank()) {
           return null;   // no document stored in this slot
        }

       String name = (documentSlot == 1) ? volunteer.getGovtIdName1() : volunteer.getGovtIdName2();
        LocalDate expiry = (documentSlot == 1) ? volunteer.getGovtIdExpiry1() : volunteer.getGovtIdExpiry2();
        DocumentStatus status = (expiry == null) ? null : DocumentStatus.from(expiry);

       return new IdentityDocumentMetadata(name, expiry, status);
    }

    @Override
    public VolunteerResponse updateVolunteerStep3(VolunteerRequest request) throws Exception {
        Volunteer volunteer = lockedVolunteer(request.userId());

        if (request.step() != 3) throw VolunteerException.volunteerInvalidStep(request.userId());

        advanceProgress(request);

        return mapToVolunteerResponse(volunteer);
    }

    @Override
    public VolunteerResponse updateVolunteerStep4(VolunteerRequest request) {
        throw new OnboardingAvailabilityException(400, "BAD_REQUEST",
                "Step 4 requires local HH:mm availability and an explicit emergency preference");
    }


    // @Override
    // public UserVolunteerSkillsResponse findSkillsList() throws Exception {
    // return null;
    // }

    @Override
    public VolunteerResponse updateVolunteerCompletion(VolunteerRequest request) {
        onboardingAvailability.complete(request.userId(), request.step());
        Volunteer volunteer = volunteerRepository.findById(request.userId())
                .orElseThrow(() -> VolunteerException.volunteerNotFound(request.userId()));
        return mapToVolunteerResponse(volunteer);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true,
            isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public VolunteerResponse getVolunteerByUserId(String userId) throws Exception {
        Volunteer volunteer = volunteerRepository.findById(userId)
                .orElseThrow(() -> VolunteerException.volunteerNotFound(userId));
        return mapToVolunteerResponse(volunteer, legacyAvailability.read(List.of(volunteer)).get(userId));
    }
    
   @Override
    public void updateGovtIdPath(String userId, int documentSlot, String s3Path) throws Exception {
        validateSlot(documentSlot);

        Volunteer volunteer = volunteerRepository.findById(userId)
            .orElseThrow(() -> VolunteerException.volunteerNotFound(userId));

        LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC"));

        if (documentSlot == 1) {
            volunteer.setGovtIdPath1(s3Path);
            volunteer.setPath1UpdatedAt(now);
        } else {
            volunteer.setGovtIdPath2(s3Path);
            volunteer.setPath2UpdatedAt(now);
        }

        volunteerRepository.save(volunteer);
    }

    /**
     * Returns the stored s3:// path for a slot, or null if nothing has been
     * uploaded there yet. Callers rely on the null - it distinguishes an empty
     * slot from a missing volunteer, which throws instead.
     */
    @Override
    public String getGovtIdPath(String userId, int documentSlot) throws Exception {
        validateSlot(documentSlot);

        Volunteer volunteer = volunteerRepository.findById(userId)
                .orElseThrow(() -> VolunteerException.volunteerNotFound(userId));

        return documentSlot == 1 ? volunteer.getGovtIdPath1() : volunteer.getGovtIdPath2();
    }

    @Override
    public void updateGovtIdMetadata(String userId, int documentSlot, String documentName, LocalDate expiresOn) throws Exception {
        validateSlot(documentSlot);

       Volunteer volunteer = volunteerRepository.findById(userId)
                .orElseThrow(() -> VolunteerException.volunteerNotFound(userId));

       if (documentSlot == 1) {
           volunteer.setGovtIdName1(documentName);
           volunteer.setGovtIdExpiry1(expiresOn);
       } else {
           volunteer.setGovtIdName2(documentName);
           volunteer.setGovtIdExpiry2(expiresOn);
       }

       volunteerRepository.save(volunteer);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true,
            isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public PaginationResponse<VolunteerResponse> findAllVolunteersWithPagination(Integer pageNumber, Integer pageSize) {
        int pageNum = (pageNumber == null) ? DEFAULT_PAGE : pageNumber;
        int pageSizeNum = (pageSize == null) ? DEFAULT_SIZE : pageSize;

        Pageable pageable = PageRequest.of(pageNum, pageSizeNum);
        Page<Volunteer> volunteerPage = volunteerRepository.findAll(pageable);
        var availability = legacyAvailability.read(volunteerPage.getContent());

        List<VolunteerResponse> volunteers = volunteerPage.stream()
                .map(volunteer -> mapToVolunteerResponse(volunteer, availability.get(volunteer.getId())))
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
        return mapToVolunteerResponse(volunteer, legacyAvailability.read(List.of(volunteer)).get(volunteer.getId()));
    }

    private VolunteerResponse mapToVolunteerResponse(Volunteer volunteer, LegacyAvailabilityProjection.View availability) {
        String uid = volunteer.getId();

        return VolunteerResponse.builder()
                .id(uid)
                .userId(uid)
                .termsAndConditions(volunteer.getTermsAndConditions())
                .termsAcceptedAt(volunteer.getTermsAcceptedAt())
                .govtIdPath1(volunteer.getGovtIdPath1())
                .govtIdPath2(volunteer.getGovtIdPath2())
                .path1UpdatedAt(volunteer.getPath1UpdatedAt())
                .path2UpdatedAt(volunteer.getPath2UpdatedAt())
                .availabilityDays(availability.days())
                .availabilityTimes(availability.times())
                .availabilityMigrationRequired(availability.migrationRequired())
                .createdAt(volunteer.getCreatedAt())
                .lastUpdatedAt(volunteer.getLastUpdatedAt())
                .build();
    }

    private void validateSlot(int documentSlot) {
        if (documentSlot != 1 && documentSlot != 2) {
            throw new IdentityDocumentException(SaayamStatusCode.INVALID_DOCUMENT_SLOT,
                    HttpStatus.BAD_REQUEST, "Document slot must be 1 or 2");
        }
    }
}

