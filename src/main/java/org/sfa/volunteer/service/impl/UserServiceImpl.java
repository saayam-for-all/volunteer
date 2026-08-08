package org.sfa.volunteer.service.impl;

import jakarta.transaction.Transactional;
import org.sfa.volunteer.dto.request.CreateUserRequest;
import org.sfa.volunteer.dto.request.UpdateOrganizationRequest;
import org.sfa.volunteer.dto.request.UpdateUserProfileRequest;
import org.sfa.volunteer.dto.request.UserPreferenceRequest;
import org.sfa.volunteer.dto.response.*;
import org.sfa.volunteer.exception.CountryNotFoundException;
import org.sfa.volunteer.exception.UserCategoryNotFoundException;
import org.sfa.volunteer.exception.UserNotFoundException;
import org.sfa.volunteer.exception.UserOrganizationNotFoundException;
import org.sfa.volunteer.model.Country;
import org.sfa.volunteer.model.Organization;
import org.sfa.volunteer.model.State;
import org.sfa.volunteer.model.User;
import org.sfa.volunteer.model.UserAdditionalDetail;
import org.sfa.volunteer.model.UserCategory;
import org.sfa.volunteer.model.UserSignOffReason;
import org.sfa.volunteer.model.UserStatus;
import org.sfa.volunteer.repository.CountryRepository;
import org.sfa.volunteer.repository.OrganizationRepository;
import org.sfa.volunteer.repository.StateRepository;
import org.sfa.volunteer.repository.UserCategoryRepository;
import org.sfa.volunteer.repository.UserRepository;
import org.sfa.volunteer.repository.UserSignOffReasonRepository;
import org.sfa.volunteer.repository.UserStatusRepository;
import org.sfa.volunteer.repository.UserAdditionalDetailRepository;
import org.sfa.volunteer.service.ProfileImageStorageService;
import org.sfa.volunteer.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
    public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserStatusRepository userStatusRepository;
    private final OrganizationRepository organizationRepository;
    private final UserSignOffReasonRepository userSignOffReasonRepository;
    private final UserCategoryRepository userCategoryRepository;
    private final CountryRepository countryRepository;
    private final StateRepository stateRepository;
    private final UserAdditionalDetailRepository userAdditionalDetailRepository;

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;

    // Default IDs for user status and category
    private static final Integer DEFAULT_USER_STATUS_ID = 1; // Active user
    private static final Integer DEFAULT_USER_CATEGORY_ID = 1; // User Category: common user
    private static final Integer VOLUNTEER_CATEGORY_ID = 2; // User Category: volunteer
    private static final String DEFAULT_TIMEZONE = "UTC";
    private static final String DEFAULT_LOCALE = "en_US";

    @Autowired
    public UserServiceImpl(
            UserRepository userRepository,
            UserStatusRepository userStatusRepository,
            OrganizationRepository organizationRepository,
            UserCategoryRepository userCategoryRepository,
            CountryRepository countryRepository,
            StateRepository stateRepository,
            UserSignOffReasonRepository userSignOffReasonRepository,
            UserAdditionalDetailRepository userAdditionalDetailRepository) {

        this.userRepository = userRepository;
        this.userStatusRepository = userStatusRepository;
        this.organizationRepository = organizationRepository;
        this.userCategoryRepository = userCategoryRepository;
        this.countryRepository = countryRepository;
        this.stateRepository = stateRepository;
        this.userSignOffReasonRepository = userSignOffReasonRepository;
        this.userAdditionalDetailRepository = userAdditionalDetailRepository;
    }

    @Override
    public CreateUserResponse createUser(CreateUserRequest request) {

        UserStatus userStatus = userStatusRepository.findById(DEFAULT_USER_STATUS_ID)
                .orElseThrow(() -> new UserCategoryNotFoundException(DEFAULT_USER_STATUS_ID));

        UserCategory userCategory = userCategoryRepository.findById(DEFAULT_USER_CATEGORY_ID)
                .orElseThrow(() -> new UserCategoryNotFoundException(DEFAULT_USER_CATEGORY_ID));

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
                .userCategory(userCategory)
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
        user.setProfilePicturePath(request.profilePicturePath());
        user.setVolunteerStage(request.volunteerStage());
        user.setVolunteerUpdateDate(request.volunteerUpdateDate());
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

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return false;

        String category = (user.getUserCategory() == null) ? null : user.getUserCategory().getUserCategory();

        if (category != null && category.toLowerCase().contains("admin")) return true;
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
                .userCategory(user.getUserCategory() != null ? user.getUserCategory().getUserCategory() : null)
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
    public OrganizationResponse updateUserOrganization(String userId, UpdateOrganizationRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        Organization organization = organizationRepository.findByUser(user).orElse(null);

        if (organization == null) {
            organization = new Organization();
            organization.setUser(user);
        }

        if (request.organizationName() != null) organization.setOrganizationName(request.organizationName());
        if (request.organizationType() != null) organization.setOrganizationType(request.organizationType());
        if (request.phoneNumber() != null) organization.setPhoneNumber(request.phoneNumber());
        if (request.email() != null) organization.setEmail(request.email());
        if (request.url() != null) organization.setUrl(request.url());
        if (request.streetAddress1() != null) organization.setStreetAddress1(request.streetAddress1());
        if (request.streetAddress2() != null) organization.setStreetAddress2(request.streetAddress2());
        if (request.city() != null) organization.setCity(request.city());
        if (request.state() != null) organization.setState(request.state());
        if (request.zipCode() != null) organization.setZipCode(request.zipCode());

        organization.setLastUpdateDate(ZonedDateTime.now(ZoneId.of("UTC")));
        Organization updatedOrganization = organizationRepository.save(organization);

        return OrganizationResponse.builder()
                .id(updatedOrganization.getId())
                .organizationName(updatedOrganization.getOrganizationName())
                .organizationType(updatedOrganization.getOrganizationType())
                .phoneNumber(updatedOrganization.getPhoneNumber())
                .email(updatedOrganization.getEmail())
                .url(updatedOrganization.getUrl())
                .streetAddress1(updatedOrganization.getStreetAddress1())
                .streetAddress2(updatedOrganization.getStreetAddress2())
                .city(updatedOrganization.getCity())
                .state(updatedOrganization.getState())
                .zipCode(updatedOrganization.getZipCode())
                .build();
    }

    @Override
    public OrganizationResponse getOrganizationByUserId(String userId) {
        Organization organization = organizationRepository.findByUserId(userId)
                .orElseThrow(() -> new UserOrganizationNotFoundException(userId));

        return OrganizationResponse.builder()
                .id(organization.getId())
                .organizationName(organization.getOrganizationName())
                .organizationType(organization.getOrganizationType())
                .phoneNumber(organization.getPhoneNumber())
                .email(organization.getEmail())
                .url(organization.getUrl())
                .streetAddress1(organization.getStreetAddress1())
                .streetAddress2(organization.getStreetAddress2())
                .city(organization.getCity())
                .state(organization.getState())
                .zipCode(organization.getZipCode())
                .build();
    }
    // Profile Pic Upload
    // S3 URI <-> DB //
    @Override
    public void setProfilePicturePath(String userId, String s3Uri) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        user.setProfilePicturePath(s3Uri);  // store S3 URI here
        user.setLastUpdateDate(ZonedDateTime.now(ZoneId.of("UTC")));
        userRepository.save(user);
    }
    @Override
    public Optional<String> getProfilePicturePath(String userId) {
        return userRepository.findById(userId)
                .map(User::getProfilePicturePath)
                .filter(Objects::nonNull)
                .filter(s -> !s.isBlank());
    }

    @Override
    public boolean userExists(String userId) {
        return userRepository.existsById(userId);
    }
    @Override
    public String getUserIdByEmailForAuth(String email) {
        if (email == null || email.isBlank()) {
            throw new UserNotFoundException("email is blank");
        }
        var userOpt = userRepository.findFirstByPrimaryEmailAddressOrderByLastUpdateDateDesc(email);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findFirstByPrimaryEmailAddressOrderByIdDesc(email);
        }

        User user = userOpt.orElseThrow(() -> new UserNotFoundException(email));
        return user.getId();
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
