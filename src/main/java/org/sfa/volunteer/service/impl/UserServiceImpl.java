package org.sfa.volunteer.service.impl;

import jakarta.transaction.Transactional;
import org.sfa.volunteer.dto.request.CreateUserRequest;
import org.sfa.volunteer.dto.request.UpdateUserProfileRequest;
import org.sfa.volunteer.dto.response.CreateUserResponse;
import org.sfa.volunteer.dto.response.PaginationResponse;
import org.sfa.volunteer.dto.response.UserProfileResponse;
import org.sfa.volunteer.dto.response.VolunteerProfileResponse;
import org.sfa.volunteer.exception.UserCategoryNotFoundException;
import org.sfa.volunteer.exception.UserNotFoundException;
import org.sfa.volunteer.model.entity.*;
import org.sfa.volunteer.model.enums.RequestTypeEnum;
import org.sfa.volunteer.model.enums.SkillLevelEnum;
import org.sfa.volunteer.model.enums.UserStatusEnum;
import org.sfa.volunteer.repository.*;
import org.sfa.volunteer.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserStatusRepository userStatusRepository;

    private final UserCategoryRepository userCategoryRepository;
    private final CountryRepository countryRepository;
    private final StateRepository stateRepository;

    private final UserSkillsRepository userSkillsRepository;
    private final SkillListRepository skillListRepository;

    private final UserAvailabilityRepository userAvailabilityRepository;

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;

    // Default IDs for user status and category
    private static final Integer DEFAULT_USER_STATUS_ID = 1; // Active user
    private static final Integer DEFAULT_USER_CATEGORY_ID = 1; // User Category: common user
    private static final Integer VOLUNTEER_CATEGORY_ID = 2; // User Category: volunteer

    private static final ZonedDateTime EARLIEST_DATE = ZonedDateTime.ofInstant(Instant.EPOCH, ZoneId.systemDefault());


    @Autowired
    public UserServiceImpl(UserRepository userRepository, UserStatusRepository userStatusRepository, UserCategoryRepository userCategoryRepository,
                           CountryRepository countryRepository, StateRepository stateRepository, SkillListRepository skillListRepository, UserSkillsRepository userSkillsRepository, UserAvailabilityRepository userAvailabilityRepository) {
        this.userRepository = userRepository;
        this.userStatusRepository = userStatusRepository;
        this.userCategoryRepository = userCategoryRepository;
        this.countryRepository = countryRepository;
        this.stateRepository = stateRepository;
        this.skillListRepository = skillListRepository;
        this.userSkillsRepository = userSkillsRepository;
        this.userAvailabilityRepository = userAvailabilityRepository;
    }

    @Override
    public CreateUserResponse createUser(CreateUserRequest request) {

        UserStatus userStatus = userStatusRepository.findById(DEFAULT_USER_STATUS_ID)
                .orElseThrow(() -> new UserCategoryNotFoundException(DEFAULT_USER_STATUS_ID));

        UserCategory userCategory = userCategoryRepository.findById(DEFAULT_USER_CATEGORY_ID)
                .orElseThrow(() -> new UserCategoryNotFoundException(DEFAULT_USER_CATEGORY_ID));

        // Create a new User entity from the request data
        User user = User.builder()
                .fullName(request.name())
                .primaryEmailAddress(request.email())
                .primaryPhoneNumber(request.phoneNumber())
                .timeZone(request.timeZone())
                .lastUpdateDate(ZonedDateTime.now(ZoneId.of("UTC")))
                .userCategory(userCategory)
                .userStatus(userStatus)
                .build();

        // Save the User entity to the database
        user = userRepository.save(user);

        // Create a response object from the saved User entity
        return CreateUserResponse.builder()
                .name(user.getFullName())
                .email(user.getPrimaryEmailAddress())
                .phoneNumber(user.getPrimaryEmailAddress())
                .timeZone(user.getTimeZone())
                .userId(user.getId())
                .build();
    }

    @Override
    public UserProfileResponse updateUserProfile(String userId, UpdateUserProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // Update all fields from the request without null checks
        user.setFirstName(request.firstName());
        user.setMiddleName(request.middleName());
        user.setLastName(request.lastName());
        user.setAddressLine1(request.addressLine1());
        user.setAddressLine2(request.addressLine2());
        user.setAddressLine3(request.addressLine3());
        user.setCity(request.cityName());
        user.setZipCode(request.zipCode());

        user.setLastUpdateDate(ZonedDateTime.now(ZoneId.of("UTC")));

        User updatedUser = userRepository.save(user);

        return mapToUserProfileResponse(updatedUser);
    }

    @Override
    public PaginationResponse<UserProfileResponse> findAllUsersWithPagination(Integer pageNumber, Integer pageSize) {
        int pageNum = (pageNumber == null) ? DEFAULT_PAGE : pageNumber;
        int pageSizeNum = (pageSize == null) ? DEFAULT_SIZE : pageSize;
        Pageable pageable = PageRequest.of(pageNum, pageSizeNum);
        Page<User> userPage = userRepository.findAll(pageable);

        List<UserProfileResponse> userProfiles = userPage.stream()
                .map(this::mapToUserProfileResponse)
                .collect(Collectors.toList());

        return PaginationResponse.<UserProfileResponse>builder()
                .currentPage(userPage.getNumber())
                .pageSize(userPage.getSize())
                .totalPages(userPage.getTotalPages())
                .totalItems(userPage.getTotalElements())
                .items(userProfiles)
                .hasNextPage(userPage.hasNext())
                .hasPreviousPage(userPage.hasPrevious())
                .build();
    }

    @Override
    public UserProfileResponse getUserProfileById(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        return mapToUserProfileResponse(user);
    }

    private UserProfileResponse mapToUserProfileResponse(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .middleName(user.getMiddleName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .emailAddress(user.getPrimaryEmailAddress())
                .phoneNumber(user.getPrimaryPhoneNumber())
                .timeZone(user.getTimeZone())
                .profilePicturePath(user.getProfilePicturePath())
                .addressLine1(user.getAddressLine1())
                .addressLine2(user.getAddressLine2())
                .addressLine3(user.getAddressLine3())
                .city(user.getCity())
                .zipCode(user.getZipCode())
                .lastUpdateDate(user.getLastUpdateDate())
                .gender(user.getGender())
                .language1(user.getLanguage1())
                .language2(user.getLanguage2())
                .language3(user.getLanguage3())
                .stateName(user.getState() != null ? user.getState().getStateName() : null)
                .countryName(user.getCountry() != null ? user.getCountry().getCountryName() : null)
                .userStatus(user.getUserStatus() != null ? user.getUserStatus().getUserStatus().name() : null)
                .userCategory(user.getUserCategory() != null ? user.getUserCategory().getUserCategory().name() : null)
                .build();
    }

    private VolunteerProfileResponse mapToVolunteerProfileResponse(User user, int priorityNum) {
        return VolunteerProfileResponse.builder()
                .priorityNum(priorityNum)
                .id(user.getId())
                .fullName(user.getFullName())
                .emailAddress(user.getPrimaryEmailAddress())
                .phoneNumber(user.getPrimaryPhoneNumber())
                .build();
    }

    public PaginationResponse<VolunteerProfileResponse> getTopVolunteersForRequestCategory(Integer pageNumber, Integer pageSize, Request request) {
        int pageNum = (pageNumber == null) ? DEFAULT_PAGE : pageNumber;
        int pageSizeNum = (pageSize == null) ? DEFAULT_SIZE : pageSize;
        Pageable pageable = PageRequest.of(pageNum, pageSizeNum);

        List<SkillList> skills = skillListRepository.findByRequestCategoryId(request.getRequestCategory().getRequestCategoryId());

        if (skills.size() == 1) {
            SkillList skill = skills.get(0);
            Integer skillId = skill.getSkillListId();
            List<UserSkills> userSkills = userSkillsRepository.findBySkillId(skillId);
            Map<String, List<UserSkills>> userSkillsMap = userSkills.stream()
                    .collect(Collectors.groupingBy(UserSkills::getUserId));

            List<User> users = userRepository.findAllById(userSkillsMap.keySet());

            List<User> activeUsers = users.stream()
                    .filter(user -> UserStatusEnum.ACTIVE.equals(user.getUserStatus().getUserStatus()))
                    .collect(Collectors.toList());

            if (activeUsers.isEmpty()) {
                return PaginationResponse.<VolunteerProfileResponse>builder()
                        .message("No active volunteers!!")
                        .build();
            }

            String requesterLanguage1 = userRepository.getLanguagePreference1(request.getRequesterId());
            String requesterLanguage2 = userRepository.getLanguagePreference2(request.getRequesterId());
            String requesterLanguage3 = userRepository.getLanguagePreference3(request.getRequesterId());

            RequestTypeEnum requestTypeEnum = RequestTypeEnum.fromId(request.getRequestType().getRequestTypeId());
            ZonedDateTime requestSubmissionTime = request.getSubmittedAt();

            List<VolunteerProfileResponse> volunteerProfileResponses = activeUsers.stream()
                    .map(user -> {
                        int cityPriority = 0;
                        int zipCodePriority = 0;
                        int languagePriority = calculateLanguagePriority(user, requesterLanguage1, requesterLanguage2, requesterLanguage3);
                        int skillPriority = calculateSkillPriority(userSkillsMap.get(user.getId()));
                        int lastUsedDatePriority = calculateLastUsedDatePriority(userSkillsMap.get(user.getId()));

                        if (RequestTypeEnum.IN_PERSON.equals(requestTypeEnum)) {
                            if (!isVolunteerAvailable(user.getId(), requestSubmissionTime)) {
                                return null;
                            }
                            cityPriority = calculateCityPriority(user.getCity(), request.getCity());
                            zipCodePriority = calculateZipCodePriority(user.getZipCode(), request.getZipCode());
                        }

                        int priorityNumber = cityPriority * 10000 + zipCodePriority * 1000 + languagePriority * 100 + skillPriority * 10 + lastUsedDatePriority;
                        return mapToVolunteerProfileResponse(user, priorityNumber);
                    })
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparingInt(VolunteerProfileResponse::priorityNum).reversed())
                    .collect(Collectors.toList());

            int start = Math.min((int) pageable.getOffset(), volunteerProfileResponses.size());
            int end = Math.min((start + pageable.getPageSize()), volunteerProfileResponses.size());

            List<VolunteerProfileResponse> paginatedList = volunteerProfileResponses.subList(start, end);

            Page<VolunteerProfileResponse> paginatedVolunteerProfiles = new PageImpl<>(paginatedList, pageable, volunteerProfileResponses.size());

            return PaginationResponse.<VolunteerProfileResponse>builder()
                    .currentPage(paginatedVolunteerProfiles.getNumber())
                    .pageSize(paginatedVolunteerProfiles.getSize())
                    .totalPages(paginatedVolunteerProfiles.getTotalPages())
                    .totalItems(paginatedVolunteerProfiles.getTotalElements())
                    .items(paginatedVolunteerProfiles.getContent())
                    .hasNextPage(paginatedVolunteerProfiles.hasNext())
                    .hasPreviousPage(paginatedVolunteerProfiles.hasPrevious())
                    .build();
        } else {
            return new PaginationResponse<>(0, 0, pageNum, pageSizeNum, List.of(), false, false, "Yet to be implemented!");
        }
    }

    private boolean isVolunteerAvailable(String userId, ZonedDateTime requestSubmissionTime) {
        List<UserAvailability> availabilities = userAvailabilityRepository.findByUserId(userId);
        return availabilities.stream().anyMatch(availability -> {
            ZonedDateTime startTime = availability.getStartTime();
            ZonedDateTime endTime = availability.getEndTime();
            return !requestSubmissionTime.isBefore(startTime) && !requestSubmissionTime.isAfter(endTime);
        });
    }


    private int calculateCityPriority(String userCity, String requestCity) {
        return userCity.equalsIgnoreCase(requestCity) ? 1 : 0;
    }

    private int calculateZipCodePriority(String userZipCode, String requestZipCode) {
        return userZipCode.equalsIgnoreCase(requestZipCode) ? 1 : 0;
    }

    private int calculateLanguagePriority(User user, String requesterLanguage1, String requesterLanguage2, String requesterLanguage3) {
        int score = 0;

        if (user.getLanguage1().equalsIgnoreCase(requesterLanguage1) ||
                user.getLanguage1().equalsIgnoreCase(requesterLanguage2) ||
                user.getLanguage1().equalsIgnoreCase(requesterLanguage3)) {
            score++;
        }

        if (user.getLanguage2().equalsIgnoreCase(requesterLanguage1) ||
                user.getLanguage2().equalsIgnoreCase(requesterLanguage2) ||
                user.getLanguage2().equalsIgnoreCase(requesterLanguage3)) {
            score++;
        }

        if (user.getLanguage3().equalsIgnoreCase(requesterLanguage1) ||
                user.getLanguage3().equalsIgnoreCase(requesterLanguage2) ||
                user.getLanguage3().equalsIgnoreCase(requesterLanguage3)) {
            score++;
        }

        return score;
    }

    private int calculateSkillPriority(List<UserSkills> userSkills) {
        return userSkills.stream()
                .mapToInt(us -> SkillLevelEnum.valueOf(us.getSkillLevel().name()).getId())
                .max()
                .orElse(0);
    }

    private int calculateLastUsedDatePriority(List<UserSkills> userSkills) {
        return userSkills.stream()
                .map(UserSkills::getLastUsedDate)
                .max(ZonedDateTime::compareTo)
                .orElse(EARLIEST_DATE)
                .getDayOfYear();
    }
}
