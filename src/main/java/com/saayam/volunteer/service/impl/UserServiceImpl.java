package com.saayam.volunteer.service.impl;

import com.saayam.volunteer.domain.CreateUserRequest;
import com.saayam.volunteer.domain.CreateUserResponse;
import com.saayam.volunteer.domain.PaginationResponse;
import com.saayam.volunteer.domain.UpdateUserProfileRequest;
import com.saayam.volunteer.domain.UserProfileResponse;
import com.saayam.volunteer.model.User;
import com.saayam.volunteer.model.UserCategory;
import com.saayam.volunteer.model.UserStatus;
import com.saayam.volunteer.repository.CountryRepository;
import com.saayam.volunteer.repository.StateRepository;
import com.saayam.volunteer.repository.UserCategoryRepository;
import com.saayam.volunteer.repository.UserRepository;
import com.saayam.volunteer.repository.UserStatusRepository;
import com.saayam.volunteer.service.UserService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserStatusRepository userStatusRepository;

    private final UserCategoryRepository userCategoryRepository;
    private final CountryRepository countryRepository;
    private final StateRepository stateRepository;

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;

    // Default IDs for user status and category
    private static final Integer DEFAULT_USER_STATUS_ID = 1; // Active user
    private static final Integer DEFAULT_USER_CATEGORY_ID = 1; // User Category: common user
    private static final Integer VOLUNTEER_CATEGORY_ID = 2; // User Category: volunteer

    @Autowired
    public UserServiceImpl(UserRepository userRepository, UserStatusRepository userStatusRepository, UserCategoryRepository userCategoryRepository,
                           CountryRepository countryRepository, StateRepository stateRepository) {
        this.userRepository = userRepository;
        this.userStatusRepository = userStatusRepository;
        this.userCategoryRepository = userCategoryRepository;
        this.countryRepository = countryRepository;
        this.stateRepository = stateRepository;
    }

    @Override
    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public CreateUserResponse createUser(CreateUserRequest request) {
        // Retrieve UserStatus from the database
        UserStatus userStatus = userStatusRepository.findById(DEFAULT_USER_STATUS_ID)
                .orElseThrow(() -> new IllegalArgumentException("UserStatus not found"));

        // Retrieve UserCategory from the database
        UserCategory userCategory = userCategoryRepository.findById(DEFAULT_USER_CATEGORY_ID)
                .orElseThrow(() -> new IllegalArgumentException("UserCategory not found"));

        // Create a new User entity from the request data
        User user = User.builder()
                .fullName(request.name())
                .emailAddress(request.email())
                .phoneNumber(request.phoneNumber())
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
                .email(user.getEmailAddress())
                .phoneNumber(user.getPhoneNumber())
                .timeZone(user.getTimeZone())
                .userId(user.getId())
                .build();
    }

    @Override
    public UserProfileResponse updateUserProfile(String userId, UpdateUserProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

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
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
        return mapToUserProfileResponse(user);
    }

    private UserProfileResponse mapToUserProfileResponse(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .middleName(user.getMiddleName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .emailAddress(user.getEmailAddress())
                .phoneNumber(user.getPhoneNumber())
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
                .build();
    }
}
