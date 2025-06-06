package org.sfa.volunteer.service.impl;
import jakarta.transaction.Transactional;

import lombok.extern.slf4j.Slf4j;
import org.sfa.volunteer.dto.request.CreateUserRequest;
import org.sfa.volunteer.dto.request.UpdateUserProfileRequest;
import org.sfa.volunteer.dto.response.CreateUserResponse;
import org.sfa.volunteer.dto.response.PaginationResponse;
import org.sfa.volunteer.dto.response.UserProfileResponse;
import org.sfa.volunteer.dto.response.WizardStatusResponse;
import org.sfa.volunteer.exception.UserCategoryNotFoundException;
import org.sfa.volunteer.exception.UserNotFoundException;
import org.sfa.volunteer.model.Country;
import org.sfa.volunteer.model.User;
import org.sfa.volunteer.model.UserCategory;
import org.sfa.volunteer.model.UserStatus;
import org.sfa.volunteer.repository.CountryRepository;
import org.sfa.volunteer.repository.StateRepository;
import org.sfa.volunteer.repository.UserCategoryRepository;
import org.sfa.volunteer.repository.UserRepository;
import org.sfa.volunteer.repository.UserStatusRepository;
import org.sfa.volunteer.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
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
    private S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final RestTemplate restTemplate = new RestTemplate();
    @Value("${aws.s3.bucket}")
    private String bucketName;
    @Value("${aws.s3.users.folder}")
    private String usersFolder;
    @Value("${aws.s3.region}")
    private String region;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, UserStatusRepository userStatusRepository, UserCategoryRepository userCategoryRepository,
                           CountryRepository countryRepository, StateRepository stateRepository, S3Client s3Client, S3Presigner s3Presigner) {
        this.userRepository = userRepository;
        this.userStatusRepository = userStatusRepository;
        this.userCategoryRepository = userCategoryRepository;
        this.countryRepository = countryRepository;
        this.stateRepository = stateRepository;
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
    }

    @Override
    public CreateUserResponse createUser(CreateUserRequest request) {

        UserStatus userStatus = userStatusRepository.findById(DEFAULT_USER_STATUS_ID)
                .orElseThrow(() -> new UserCategoryNotFoundException(DEFAULT_USER_STATUS_ID));

        UserCategory userCategory = userCategoryRepository.findById(DEFAULT_USER_CATEGORY_ID)
                .orElseThrow(() -> new UserCategoryNotFoundException(DEFAULT_USER_CATEGORY_ID));

        Country country = countryRepository.findByCountryName(request.country())
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
                .country(country)
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
                .countryName(user.getCountry() != null ? user.getCountry().getCountryName() : null)
                .build();
    }

    @Override
    public UserProfileResponse updateUserProfile(String userId, UpdateUserProfileRequest request, MultipartFile file) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        String GovtIdS3URI = generateProfilepicS3Url(file, userId);
        // Update all fields from the request without null checks
        user.setFirstName(request.firstName());
        user.setMiddleName(request.middleName());
        user.setLastName(request.lastName());
        user.setAddressLine1(request.addressLine1());
        user.setAddressLine2(request.addressLine2());
        user.setAddressLine3(request.addressLine3());
        user.setCity(request.cityName());
        user.setZipCode(request.zipCode());
        user.setProfilePicturePath(GovtIdS3URI);
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
    public UserProfileResponse getUserProfileById(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        return mapToUserProfileResponse(user);
    }

//    @Override
//    public WizardStatusResponse getWizardStatus(String userId) {
//    UserProfileResponse userProfile = getUserProfileById(userId);
//
//    String addressAvailable = (userProfile.addressLine1() != null && !userProfile.addressLine1().trim().isEmpty())
//            ? "Y" : "N";
//
//    return new WizardStatusResponse(
//        userId,
//        userProfile.promotionWizardStage(),
//        addressAvailable
//    );
//}

    @Override
    public UserProfileResponse getUserProfileByEmail(String email) {
        User user = userRepository.findByPrimaryEmailAddress(email);

        if (user == null) {
            throw new UserNotFoundException(email);
        }

        String base64Image = getBase64ImageFromS3(user.getProfilePicturePath());

        return mapToUserProfileResponse(user).toBuilder()
                .base64ProfileImage(base64Image)
                .build();
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

    private String generateProfilepicS3Url(MultipartFile file, String folderName) {
        if (validateImage(file)) {
            String key = usersFolder + "/" + folderName.toLowerCase() + "/" + file.getOriginalFilename();
            uploadImageToS3(file, key);
            log.info("uploading image to S3");
            return s3Client.utilities().getUrl(builder -> builder.bucket(bucketName).key(key)).toString();
        } else {
            return "Invalid image.";
        }
    }

    public boolean validateImage(MultipartFile file) {
        final long MAX_SIZE = 5 * 1024 * 1024; // 5MB
        List<String> allowedTypes = List.of("image/png", "image/jpeg", "image/jpg", "image/gif");
        boolean result = false;
        String contentType = file.getContentType();
        if (file.getSize() > MAX_SIZE) {
            log.info("Image is larger than 5MB");
            throw new IllegalArgumentException("Image size exceeds the maximum allowed size of 5MB.");
        } else if (contentType == null || !allowedTypes.contains(contentType)) {
            log.info("image files (PNG, JPEG, JPG, GIF) are not provided");
            throw new IllegalArgumentException("Only image files (PNG, JPEG, JPG, GIF) are allowed.");
        } else {
            result = true;
        }
        return result;
    }


    // Upload the file to S3
    private void uploadImageToS3(MultipartFile file, String key) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName) // Specify the bucket name
                .key(key)//The key (name/path) for the file in S3
                .contentType(file.getContentType())//set content type(for images like PNG,JPG..)
                .build();
        // Get the InputStream from the MultipartFile and upload the file
        try (InputStream inputStream = file.getInputStream()) {

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(inputStream, file.getSize()));
            log.info("Successfully uploaded image '{}' to bucket '{}'", key, bucketName);

        } catch (Exception e) {
            log.info("Failed to upload image '{}' to S3: {}", key, e.getMessage());

        }
    }

//    public String generatePresignedUrl(String bucketName, String objectKeyOrUrl) {
//        try {
//            String expectedHost = bucketName + ".s3.amazonaws.com";
//            String objectKey;
//
//            if (objectKeyOrUrl.startsWith("http")) {
//                // Parse URL properly
//                URI uri = new URI(objectKeyOrUrl);
//                String host = uri.getHost();
//
//                if (host == null || !host.equals(expectedHost)) {
//                    throw new IllegalArgumentException("Invalid S3 host. Expected: " + expectedHost + ", but got: " + host);
//                }
//
//                // Trim leading slash from path to get the object key
//                objectKey = uri.getPath().startsWith("/") ? uri.getPath().substring(1) : uri.getPath();
//            } else {
//                objectKey = objectKeyOrUrl; // already a clean key
//            }
//
//            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
//                    .bucket(bucketName)
//                    .key(objectKey)
//                    .build();
//
//            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
//                    .signatureDuration(Duration.ofMinutes(15))
//                    .getObjectRequest(getObjectRequest)
//                    .build();
//
//            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
//
//            return presignedRequest.url().toString();
//        } catch (URISyntaxException e) {
//            throw new IllegalArgumentException("Malformed S3 URL: " + objectKeyOrUrl, e);
//        }
//    }

    private String getBase64ImageFromS3(String profilePicturePath) {
        if (profilePicturePath == null || profilePicturePath.isEmpty()) return null;

        try {
            // Extract just the key from the full S3 URL (profilePicturePath)
            URI uri = new URI(profilePicturePath);
            String key = uri.getPath().startsWith("/") ? uri.getPath().substring(1) : uri.getPath();

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            try (InputStream inputStream = s3Client.getObject(getObjectRequest)) {
                byte[] imageBytes = inputStream.readAllBytes();
                return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(imageBytes);
            }
        } catch (Exception e) {
            log.error("Failed to fetch or encode image from S3 for key: {}", profilePicturePath, e);
            return null;
        }
    }

}
