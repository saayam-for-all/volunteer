package org.sfa.volunteer.service.impl;

import jakarta.transaction.Transactional;
import org.sfa.volunteer.dto.request.AvailabilitySlotRequest;
import org.sfa.volunteer.dto.request.CreateUserRequest;
import org.sfa.volunteer.dto.request.UpdateAvailabilityRequest;
import org.sfa.volunteer.dto.request.UpdateUserProfileRequest;
import org.sfa.volunteer.dto.request.UserPreferenceRequest;
import org.sfa.volunteer.dto.response.*;
import org.sfa.volunteer.exception.CountryNotFoundException;
import org.sfa.volunteer.exception.UserCategoryNotFoundException;
import org.sfa.volunteer.exception.UserNotFoundException;
import org.sfa.volunteer.model.AvailabilityFrequency;
import org.sfa.volunteer.model.Country;
import org.sfa.volunteer.model.State;
import org.sfa.volunteer.model.User;
import org.sfa.volunteer.model.UserAdditionalDetail;
import org.sfa.volunteer.model.UserSignOffReason;
import org.sfa.volunteer.model.SkillLevel;
import org.sfa.volunteer.model.UserStatus;
import org.sfa.volunteer.model.VolunteerUserAvailability;
import org.sfa.volunteer.repository.CountryRepository;
import org.sfa.volunteer.repository.StateRepository;
import org.sfa.volunteer.repository.UserCategoryRepository;
import org.sfa.volunteer.repository.UserRepository;
import org.sfa.volunteer.repository.UserSignOffReasonRepository;
import org.sfa.volunteer.repository.UserSkillRepository;
import org.sfa.volunteer.repository.UserStatusRepository;
import org.sfa.volunteer.repository.UserAdditionalDetailRepository;
import org.sfa.volunteer.repository.VolunteerUserAvailabilityRepository;
import org.sfa.volunteer.service.ProfileImageStorageService;
import org.sfa.volunteer.service.UserService;
import org.sfa.volunteer.util.AvailabilityTimeConverter;
import org.sfa.volunteer.util.AvailabilityTimeConverter.LocalSlot;
import org.sfa.volunteer.util.AvailabilityTimeConverter.UtcSlot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
    public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserStatusRepository userStatusRepository;
    private final UserSignOffReasonRepository userSignOffReasonRepository;
    private final UserCategoryRepository userCategoryRepository;
    private final CountryRepository countryRepository;
    private final StateRepository stateRepository;
    private final UserAdditionalDetailRepository userAdditionalDetailRepository;
    private final UserSkillRepository userSkillRepository;
    private final VolunteerUserAvailabilityRepository userAvailabilityRepository;

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;

    // Default IDs for user status and category
    private static final Integer DEFAULT_USER_STATUS_ID = 1; // Active user
    private static final Integer DEFAULT_USER_CATEGORY_ID = 1; // User Category: common user
    private static final Integer VOLUNTEER_CATEGORY_ID = 2; // User Category: volunteer
    private static final String DEFAULT_TIMEZONE = "UTC";
    private static final String DEFAULT_LOCALE = "en_US";
    private static final int MAX_AVAILABILITY_SLOTS = 20;
    // STRICT so "24:00" is rejected instead of resolving to midnight
    private static final DateTimeFormatter AVAILABILITY_TIME =
            DateTimeFormatter.ofPattern("HH:mm").withResolverStyle(ResolverStyle.STRICT);

    @Autowired
    public UserServiceImpl(
            UserRepository userRepository,
            UserStatusRepository userStatusRepository,
            UserCategoryRepository userCategoryRepository,
            CountryRepository countryRepository,
            StateRepository stateRepository,
            UserSignOffReasonRepository userSignOffReasonRepository,
            UserAdditionalDetailRepository userAdditionalDetailRepository,
            UserSkillRepository userSkillRepository,
            VolunteerUserAvailabilityRepository userAvailabilityRepository) {

        this.userRepository = userRepository;
        this.userStatusRepository = userStatusRepository;
        this.userCategoryRepository = userCategoryRepository;
        this.countryRepository = countryRepository;
        this.stateRepository = stateRepository;
        this.userSignOffReasonRepository = userSignOffReasonRepository;
        this.userAdditionalDetailRepository = userAdditionalDetailRepository;
        this.userSkillRepository = userSkillRepository;
        this.userAvailabilityRepository = userAvailabilityRepository;
    }

    @Override
    public CreateUserResponse createUser(CreateUserRequest request) {

        UserStatus userStatus = userStatusRepository.findById(DEFAULT_USER_STATUS_ID)
                .orElseThrow(() -> new UserCategoryNotFoundException(DEFAULT_USER_STATUS_ID));

        Country country = countryRepository.findByCountryName(request.country())
                .orElseThrow(() -> new CountryNotFoundException(request.country()));


        String timeZone =
                (request.timeZone() != null && !request.timeZone().isBlank())
                        ? request.timeZone()
                        : DEFAULT_TIMEZONE;

        String locale =
                (request.locale() != null && !request.locale().isBlank())
                        ? request.locale()
                        : DEFAULT_LOCALE;

        // Create a new User entity from the request data
        User user = User.builder()
                .fullName(request.name())
                .primaryEmailAddress(request.email())
                .primaryPhoneNumber(request.phoneNumber())
                .timeZone(timeZone)
                .lastUpdateDate(ZonedDateTime.now(ZoneId.of("UTC")))
                .userStatus(userStatus)
                .country(country)
                .build();

        // Save the User entity to the database
        user = userRepository.save(user);

        // Create a response object from the saved User entity
        return CreateUserResponse.builder()
                .name(user.getFullName())
                .email(user.getPrimaryEmailAddress())
                .phoneNumber(user.getPrimaryPhoneNumber())
                .timeZone(user.getTimeZone())
                .userId(user.getId())
                .countryName(user.getCountry() != null ? user.getCountry().getCountryName() : null)
                .build();
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
    public PaginationResponse<UserProfileResponse> searchUsers(String query, Integer pageNumber, Integer pageSize) {
        if (query == null || query.isBlank()) {
            return PaginationResponse.<UserProfileResponse>builder()
                    .currentPage(0)
                    .pageSize(0)
                    .totalPages(0)
                    .totalItems(0)
                    .items(List.of())
                    .hasNextPage(false)
                    .hasPreviousPage(false)
                    .build();
        }

        int pageNum = (pageNumber == null) ? DEFAULT_PAGE : pageNumber;
        int pageSizeNum = (pageSize == null) ? DEFAULT_SIZE : pageSize;
        Pageable pageable = PageRequest.of(pageNum, pageSizeNum);

        String q = "%" + query.trim().toLowerCase() + "%";
        Page<User> userPage = userRepository.searchUsers(q, pageable);

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
    public boolean isAdminUser(String userId) {
        if (userId == null || userId.isBlank()) return false;

        // userCategory is no longer mapped on User (see issue #165 follow-up on
        // the user_category_id schema removal), so admin status can't be
        // determined here yet. Fail closed (deny admin) rather than throw.
        return false;
    }

    @Override
    public UserProfileResponse getUserProfileById(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        return mapToUserProfileResponse(user);
    }

    @Override
    public WizardStatusResponse getWizardStatus(String userId) {
        UserProfileResponse userProfile = getUserProfileById(userId);

        return new WizardStatusResponse(
            userId,
            userProfile.promotionWizardStage()
        );
    }

    private boolean isUserAddressAvailable(UserProfileResponse userProfile) {
        return StringUtils.hasText(userProfile.countryName())
                && StringUtils.hasText(userProfile.addressLine1())
                && StringUtils.hasText(userProfile.stateName())
                && StringUtils.hasText(userProfile.city())
                && StringUtils.hasText(userProfile.zipCode());
    }
    
    @Override
    public AddressStatusResponse getAddressStatus(String userId) {
        UserProfileResponse userProfile = getUserProfileById(userId);

        return new AddressStatusResponse(
            userId,
            isUserAddressAvailable(userProfile)
        );
    }




    @Override
    public UserProfileResponse getUserProfileByEmail(String email) {
        List<User> user = userRepository.findByPrimaryEmailAddress(email.trim());

        if (user==null || user.isEmpty()) {
            throw new UserNotFoundException(email);
        }

        return mapToUserProfileResponse(user.get(0));
    }

    @Override
    public UserIdResponse getUserIdByEmail(String email){
        List<User> user = userRepository.findFirstByPrimaryEmailAddressIgnoreCase(email.trim());

        if(user==null || user.isEmpty()){
            throw new UserNotFoundException(email);
        }

        return mapToUserIdResponse(user.get(0));
    }

    private UserIdResponse mapToUserIdResponse(User user){
        return UserIdResponse.builder().user_id(user.getId()).build();
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
                .stateName(user.getState() != null ? user.getState().getStateName() : null)
                .countryName(user.getCountry() != null ? user.getCountry().getCountryName() : null)
                .userStatus(user.getUserStatus() != null ? user.getUserStatus().getUserStatus() : null)
                // userCategory no longer mapped on User; see issue #165 follow-up
                .userCategory(null)
                .gender(user.getGender())
                .lastLocation(user.getLastLocation())
                .language1(user.getLanguage1())
                .language2(user.getLanguage2())
                .language3(user.getLanguage3())
                .promotionWizardStage(user.getVolunteerStage())
                .promotionWizardLastUpdateDate(user.getVolunteerUpdateDate())
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
        if (StringUtils.hasText(request.profilePicturePath())) {
            user.setProfilePicturePath(request.profilePicturePath());
        }
        user.setVolunteerStage(request.volunteerStage());
        user.setVolunteerUpdateDate(request.volunteerUpdateDate());
        user.setLastUpdateDate(ZonedDateTime.now(ZoneId.of("UTC")));

        User updatedUser = userRepository.save(user);

        return mapToUserProfileResponse(updatedUser);
    }

    // Profile Pic Upload
    // S3 URI <-> DB //
    @Override
    @Transactional
    public void setProfilePicturePath(String userId, String s3Uri) {
        int updated = userRepository.updateProfilePicturePath(userId, s3Uri, ZonedDateTime.now(ZoneId.of("UTC")));
        if (updated == 0) {
            throw new UserNotFoundException(userId);
        }
    }
    @Override
    public Optional<String> getProfilePicturePath(String userId) {
        return userRepository.findProfilePicturePathById(userId)
                .filter(Objects::nonNull)
                .filter(s -> !s.isBlank());
    }

    @Override
    public boolean userExists(String userId) {
        return userRepository.existsById(userId);
    }
    @Override
    public boolean isEmailOwnedByUser(String userId, String email) {
        if (userId == null || userId.isBlank() || email == null || email.isBlank()) {
            return false;
        }
        return userRepository.existsByIdAndPrimaryEmailAddressIgnoreCase(userId, email.trim());
    }

    @Override
    public UserSkillsResponse getUserSkills(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId is required");
        }
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }

        // LinkedHashMap keeps the query's cat_id order and allows null levels
        Map<String, SkillLevel> skillLevels = new LinkedHashMap<>();
        userSkillRepository.findByIdUserIdOrderByIdCatIdAsc(userId)
                .forEach(us -> skillLevels.put(us.getId().getCatId(), us.getSkillLevel()));

        return new UserSkillsResponse(userId, List.copyOf(skillLevels.keySet()), skillLevels);
    }

    @Override
    public AvailabilityResponse getAvailability(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId is required");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        return toAvailabilityResponse(user, userAvailabilityRepository.findByUser_IdOrderByIdAsc(userId));
    }

    @Override
    public AvailabilityResponse updateAvailability(UpdateAvailabilityRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        String userId = request.getUserId();
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId is required");
        }
        ZoneId zone = parseTimezone(request.getTimezone());
        List<LocalSlot> localSlots = parseSlots(request.getSlots());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        user.setTimeZone(zone.getId());
        userRepository.save(user);

        // Vacation fields are accepted but not stored: user_availability has no columns for them yet
        LocalDate today = LocalDate.now(zone);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        List<VolunteerUserAvailability> rows = new ArrayList<>();
        for (LocalSlot slot : localSlots) {
            UtcSlot utc = AvailabilityTimeConverter.toUtc(slot.day(), slot.start(), slot.end(), zone, today);
            rows.add(VolunteerUserAvailability.builder()
                    .user(user)
                    .dayOfWeek(dayName(utc.day()))
                    .startTime(utc.start())
                    .endTime(utc.end())
                    .lastUpdatedAt(now)
                    .build());
        }

        // Replace, not merge: flush the deletes so they run before the inserts
        userAvailabilityRepository.deleteByUser_Id(userId);
        userAvailabilityRepository.flush();
        List<VolunteerUserAvailability> saved = userAvailabilityRepository.saveAll(rows);

        return toAvailabilityResponse(user, saved);
    }

    private static ZoneId parseTimezone(String timezone) {
        if (timezone == null || !ZoneId.getAvailableZoneIds().contains(timezone)) {
            throw new IllegalArgumentException("timezone must be a valid IANA zone: " + timezone);
        }
        return ZoneId.of(timezone);
    }

    // One local slot per (day, start, end); exact duplicates are dropped
    private static List<LocalSlot> parseSlots(List<AvailabilitySlotRequest> slots) {
        if (slots == null) {
            throw new IllegalArgumentException("slots is required");
        }
        if (slots.size() > MAX_AVAILABILITY_SLOTS) {
            throw new IllegalArgumentException("slots cannot have more than " + MAX_AVAILABILITY_SLOTS + " entries");
        }

        LinkedHashSet<LocalSlot> localSlots = new LinkedHashSet<>();
        for (int i = 0; i < slots.size(); i++) {
            AvailabilitySlotRequest slot = slots.get(i);
            String field = "slots[" + i + "]";
            if (slot == null) {
                throw new IllegalArgumentException(field + " is required");
            }
            AvailabilityFrequency frequency;
            try {
                frequency = AvailabilityFrequency.fromLabel(slot.getFrequency());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(field + ".frequency: " + e.getMessage());
            }
            LocalTime start = parseTime(slot.getStartTime(), field + ".startTime");
            LocalTime end = parseTime(slot.getEndTime(), field + ".endTime");
            if (start.equals(end)) {
                throw new IllegalArgumentException(field + ": startTime and endTime must differ");
            }
            frequency.getDays().forEach(day -> localSlots.add(new LocalSlot(day, start, end)));
        }
        return List.copyOf(localSlots);
    }

    private static LocalTime parseTime(String value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        try {
            return LocalTime.parse(value, AVAILABILITY_TIME);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(field + " must be HH:mm: " + value);
        }
    }

    private record TimeRange(LocalTime start, LocalTime end) {}

    private record CollapsedSlot(Integer id, AvailabilityFrequency frequency, TimeRange range) {}

    // Rows are UTC, one per day; convert to the user's zone and group back into Everyday / Weekdays / Weekends
    private static AvailabilityResponse toAvailabilityResponse(User user, List<VolunteerUserAvailability> rows) {
        ZoneId zone = zoneOrUtc(user.getTimeZone());

        // time range -> local day -> smallest row id
        Map<TimeRange, Map<DayOfWeek, Integer>> byRange = new LinkedHashMap<>();
        LocalDateTime lastUpdatedAt = null;
        for (VolunteerUserAvailability row : rows) {
            LocalSlot local = AvailabilityTimeConverter.toLocal(row.getStartTime(), row.getEndTime(), zone);
            byRange.computeIfAbsent(new TimeRange(local.start(), local.end()), r -> new EnumMap<>(DayOfWeek.class))
                    .merge(local.day(), row.getId(), Math::min);
            if (row.getLastUpdatedAt() != null
                    && (lastUpdatedAt == null || row.getLastUpdatedAt().isAfter(lastUpdatedAt))) {
                lastUpdatedAt = row.getLastUpdatedAt();
            }
        }

        List<CollapsedSlot> collapsed = new ArrayList<>();
        byRange.forEach((range, dayIds) -> {
            for (AvailabilityFrequency frequency : AvailabilityTimeConverter.collapse(dayIds.keySet())) {
                Integer id = frequency.getDays().stream()
                        .map(dayIds::get)
                        .filter(Objects::nonNull)
                        .min(Integer::compare)
                        .orElse(null);
                collapsed.add(new CollapsedSlot(id, frequency, range));
            }
        });
        collapsed.sort(Comparator.comparing(CollapsedSlot::frequency)
                .thenComparing(c -> c.range().start())
                .thenComparing(c -> c.range().end()));

        List<AvailabilitySlotResponse> slots = collapsed.stream()
                .map(c -> new AvailabilitySlotResponse(c.id(), c.frequency().getLabel(),
                        c.range().start().format(AVAILABILITY_TIME), c.range().end().format(AVAILABILITY_TIME)))
                .toList();
        return new AvailabilityResponse(user.getId(), zone.getId(), slots, lastUpdatedAt);
    }

    private static ZoneId zoneOrUtc(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            return ZoneId.of(DEFAULT_TIMEZONE);
        }
        try {
            return ZoneId.of(timezone);
        } catch (DateTimeException e) {
            return ZoneId.of(DEFAULT_TIMEZONE);
        }
    }

    // "MONDAY" -> "Monday", as the day_of_week CHECK constraint expects
    private static String dayName(DayOfWeek day) {
        String name = day.name();
        return name.charAt(0) + name.substring(1).toLowerCase();
    }

    @Transactional
    @Override
    public SignOffResponse signOffUser(String userId, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        // Save sign-off reason if provided
        if (reason != null && !reason.isBlank()) {
            userSignOffReasonRepository.save(new UserSignOffReason(reason));
        }
        // Delete user
        userRepository.delete(user);
        //  Return response
        return new SignOffResponse(
                userId
        );
    }

    @Override
        public UserPreferenceResponse updateUserPreferences(String userId, UserPreferenceRequest request) throws Exception {
        // fetch User by userId
         User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));


            // update language prefs
            user.setLanguage1(request.language1());
            user.setLanguage2(request.language2());
            user.setLanguage3(request.language3());
            userRepository.save(user);

            // fetch UserAdditionalDetail
            UserAdditionalDetail detail = userAdditionalDetailRepository.findByUserId(user.getId());
            if(detail == null) {
                detail = new UserAdditionalDetail();
                detail.setUser(user);
            }
            detail.setSecondaryEmail1(request.secondaryEmail1());
            detail.setSecondaryEmail2(request.secondaryEmail2());
            detail.setSecondaryPhone1(request.secondaryPhone1());
            detail.setSecondaryPhone2(request.secondaryPhone2());
            userAdditionalDetailRepository.save(detail);

            return UserPreferenceResponse.builder()
                    .userId(user.getId())
                    .language1(user.getLanguage1())
                    .language2(user.getLanguage2())
                    .language3(user.getLanguage3())
                    .secondaryEmail1(detail.getSecondaryEmail1())
                    .secondaryEmail2(detail.getSecondaryEmail2())
                    .secondaryPhone1(detail.getSecondaryPhone1())
                    .secondaryPhone2(detail.getSecondaryPhone2())
                    .build();
    }
}
