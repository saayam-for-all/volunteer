package org.sfa.volunteer.service.impl;

import org.springframework.http.HttpStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.jdbc.core.JdbcTemplate;
import org.sfa.volunteer.dto.request.OnboardingAvailabilitySaveRequest;
import org.sfa.volunteer.dto.response.OnboardingAvailabilityResponse;
import org.sfa.volunteer.dto.response.OnboardingAvailabilitySlotResponse;
import org.sfa.volunteer.exception.OnboardingAvailabilityException;
import org.sfa.volunteer.model.VolunteerUserAvailability;
import org.sfa.volunteer.repository.VolunteerUserAvailabilityRepository;
import org.sfa.volunteer.service.UserService;
import org.sfa.volunteer.util.AvailabilityTimeConverter;
import org.sfa.volunteer.util.AvailabilityTimeConverter.LocalSlot;
import java.time.Clock;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.DayOfWeek;
import java.time.LocalTime;
import org.sfa.volunteer.dto.request.VolunteerRequest;
import org.sfa.volunteer.dto.request.VolunteerUserAvailabilityRequest;
import org.sfa.volunteer.dto.response.PaginationResponse;
import org.sfa.volunteer.dto.response.VolunteerResponse;
import org.sfa.volunteer.dto.common.SaayamStatusCode;
import org.sfa.volunteer.exception.UserNotFoundException;
import org.sfa.volunteer.exception.VolunteerException;
import org.sfa.volunteer.exception.IdentityDocumentException;
import org.sfa.volunteer.model.DocumentStatus;
import org.sfa.volunteer.model.User;
import org.sfa.volunteer.model.Volunteer;
import org.sfa.volunteer.repository.UserRepository;
import org.sfa.volunteer.repository.VolunteerRepository;
import org.sfa.volunteer.dto.response.IdentityDocumentMetadata;
import org.sfa.volunteer.service.VolunteerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class)
public class VolunteerServiceImpl implements VolunteerService {
    private final VolunteerRepository volunteerRepository;
    private final UserRepository userRepository;

    @PersistenceContext private EntityManager entityManager;
    @Autowired private UserService userService;
    @Autowired private VolunteerUserAvailabilityRepository userAvailabilityRepository;
    @Autowired private JdbcTemplate availabilityJdbc;
    @Autowired private Clock availabilityClock = Clock.systemUTC();

    private static final Set<String> TEXT = Set.of("character varying", "text", "character");
    private static final Set<String> INTEGER = Set.of("integer", "smallint");
    private static final Set<String> TIMESTAMP = Set.of("timestamp without time zone");
    private static final Map<String, Set<String>> REQUIRED = Map.ofEntries(
            Map.entry("users.user_id", TEXT),
            Map.entry("users.time_zone", TEXT),
            Map.entry("users.primary_email_address", TEXT),
            Map.entry("users.promotion_wizard_stage", INTEGER),
            Map.entry("users.promotion_wizard_last_updated_at",
                    Set.of("timestamp without time zone", "timestamp with time zone")),
            Map.entry("users.is_emergency_available", Set.of("boolean")),
            Map.entry("volunteer_details.user_id", TEXT),
            Map.entry("volunteer_details.availability_days", Set.of("json", "jsonb")),
            Map.entry("volunteer_details.availability_times", Set.of("json", "jsonb")),
            Map.entry("user_availability.user_availability_id", INTEGER),
            Map.entry("user_availability.user_id", TEXT),
            Map.entry("user_availability.day_of_week", TEXT),
            Map.entry("user_availability.start_time", TIMESTAMP),
            Map.entry("user_availability.end_time", TIMESTAMP),
            Map.entry("user_availability.last_updated_at", TIMESTAMP));

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;

    @Autowired
    public VolunteerServiceImpl(VolunteerRepository volunteerRepository, UserRepository userRepository) {
        this.userRepository = userRepository;
        this.volunteerRepository = volunteerRepository;
        // this.userVolunteerSkillsRepository = userVolunteerSkillsRepository;
    }

    @Override
    public OnboardingAvailabilityResponse updateVolunteerStep4(OnboardingAvailabilitySaveRequest request) {
        if (request == null) throw availabilityBadRequest("Request body is required");
        requireAvailabilityUserId(request.userId());
        if (!Integer.valueOf(4).equals(request.step())) throw availabilityBadRequest("step must be 4");
        if (request.isEmergencyAvailable() == null) {
            throw availabilityBadRequest("isEmergencyAvailable must be an explicit boolean");
        }
        if (request.availability() == null || request.availability().isEmpty()) {
            throw availabilityBadRequest("At least one availability interval is required");
        }
        if (request.availability().size() > 20) {
            throw availabilityBadRequest("availability must contain at most 20 input intervals");
        }
        if (request.availability().stream().anyMatch(Objects::isNull)) {
            throw availabilityBadRequest("Each availability interval is required");
        }
        List<LocalSlot> slots = normalizeOnboardingIntervals(request);
        requireOnboardingAvailabilityReady();
        var account = userRepository.lockAvailabilityAccountById(request.userId())
                .orElseThrow(() -> new OnboardingAvailabilityException(404, "USER_NOT_FOUND", "Account not found"));
        if (!volunteerRepository.existsById(request.userId())) {
            throw new OnboardingAvailabilityException(404, "VOLUNTEER_NOT_FOUND", "Volunteer not found");
        }
        ZoneId zone = storedAvailabilityZone(account.getTimeZone());
        LocalDate anchorDate = LocalDate.now(availabilityClock.withZone(zone));
        requireRepresentableOnboardingTimes(slots, zone, anchorDate);
        var rows = userService.replaceAvailabilityRows(request.userId(), zone, slots, anchorDate);
        userRepository.saveOnboardingAvailabilityState(request.userId(), request.isEmergencyAvailable(),
                LocalDateTime.ofInstant(availabilityClock.instant(), java.time.ZoneOffset.UTC));
        return onboardingResponse(request.userId(), request.isEmergencyAvailable(),
                Math.max(account.getPromotionWizardStage() == null ? 0 : account.getPromotionWizardStage(), 4), zone, rows);
    }

    @Override
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public OnboardingAvailabilityResponse getOnboardingAvailability(String userId) {
        requireOnboardingAvailabilityReady();
        var account = userRepository.findAvailabilityAccountById(userId)
                .orElseThrow(() -> new OnboardingAvailabilityException(404, "USER_NOT_FOUND", "Account not found"));
        if (!volunteerRepository.existsById(userId)) {
            throw new OnboardingAvailabilityException(404, "VOLUNTEER_NOT_FOUND", "Volunteer not found");
        }
        return onboardingResponse(userId, userRepository.isEmergencyAvailable(userId),
                account.getPromotionWizardStage(), storedAvailabilityZone(account.getTimeZone()),
                userService.readAvailabilityRows(userId));
    }

    @Override
    public void requireOnboardingAvailabilityReady() {
        Map<String, String> types = new HashMap<>();
        Map<String, String> nullable = new HashMap<>();
        availabilityJdbc.query("""
                SELECT table_name, column_name, data_type, is_nullable FROM information_schema.columns
                WHERE table_schema = current_schema()
                  AND table_name IN ('users', 'user_availability', 'volunteer_details')
                """, (org.springframework.jdbc.core.RowCallbackHandler) row -> {
            String key = row.getString("table_name") + "." + row.getString("column_name");
            types.put(key, row.getString("data_type"));
            nullable.put(key, row.getString("is_nullable"));
        });
        boolean ready = REQUIRED.entrySet().stream()
                .allMatch(field -> field.getValue().contains(types.getOrDefault(field.getKey(), "")))
                && "NO".equals(nullable.get("users.is_emergency_available"));
        if (!ready) {
            throw new OnboardingAvailabilityException(503, "FEATURE_UNAVAILABLE",
                    "Onboarding availability is not ready in this environment");
        }
    }

    private static List<LocalSlot> normalizeOnboardingIntervals(OnboardingAvailabilitySaveRequest request) {
        List<LocalSlot> slots = new ArrayList<>();
        for (var slot : request.availability()) {
            LocalTime start = parseOnboardingTime(slot.startTime());
            LocalTime end = parseOnboardingTime(slot.endTime());
            if (!end.isAfter(start)) throw availabilityBadRequest("endTime must be later than startTime on the same day");
            for (DayOfWeek day : onboardingDays(slot.dayOfWeek())) {
                slots.add(new LocalSlot(day, start, end));
            }
        }
        if (hasAvailabilityOverlap(slots)) {
            throw availabilityBadRequest("Availability intervals must not overlap on the same day");
        }
        return List.copyOf(slots);
    }

    private static boolean hasAvailabilityOverlap(List<LocalSlot> slots) {
        for (int first = 0; first < slots.size(); first++) {
            for (int second = first + 1; second < slots.size(); second++) {
                if (AvailabilityTimeConverter.overlaps(slots.get(first), slots.get(second))) return true;
            }
        }
        return false;
    }

    private static void requireRepresentableOnboardingTimes(List<LocalSlot> slots, ZoneId zone, LocalDate anchorDate) {
        for (LocalSlot slot : slots) {
            try {
                var utc = AvailabilityTimeConverter.toUtc(slot.day(), slot.start(), slot.end(), zone, anchorDate);
                if (!slot.equals(AvailabilityTimeConverter.toLocal(utc.start(), utc.end(), zone))
                        || !utc.end().isAfter(utc.start())) throw invalidAvailabilityOccurrence();
            } catch (IllegalArgumentException invalidTime) {
                throw invalidAvailabilityOccurrence();
            }
        }
    }

    private static OnboardingAvailabilityException invalidAvailabilityOccurrence() {
        return availabilityBadRequest("Availability times must exist in the account timezone on their next occurrence");
    }

    private static Set<DayOfWeek> onboardingDays(String value) {
        if ("Everyday".equalsIgnoreCase(value)) return EnumSet.allOf(DayOfWeek.class);
        if (value != null) {
            try {
                return EnumSet.of(DayOfWeek.valueOf(value.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {
                // Group labels other than Everyday are not onboarding choices.
            }
        }
        throw availabilityBadRequest("dayOfWeek must be Monday through Sunday or Everyday");
    }

    private static LocalTime parseOnboardingTime(String value) {
        if (value == null || !value.matches("(?:[01][0-9]|2[0-3]):[0-5][0-9]")) {
            throw availabilityBadRequest("startTime and endTime must use local HH:mm without dates, seconds or offsets");
        }
        return LocalTime.parse(value);
    }

    private static ZoneId storedAvailabilityZone(String timezone) {
        if (timezone == null || !ZoneId.getAvailableZoneIds().contains(timezone)) {
            throw new OnboardingAvailabilityException(409, "ACCOUNT_TIMEZONE_REQUIRED",
                    "Set a valid account timezone before using onboarding availability");
        }
        return ZoneId.of(timezone);
    }

    private static void requireAvailabilityUserId(String userId) {
        if (userId == null || userId.isBlank()) throw availabilityBadRequest("userId is required");
    }

    private static OnboardingAvailabilityException availabilityBadRequest(String message) {
        return new OnboardingAvailabilityException(400, "BAD_REQUEST", message);
    }

    private static OnboardingAvailabilityResponse onboardingResponse(String userId, boolean emergency, Integer stage,
            ZoneId zone, List<VolunteerUserAvailability> rows) {
        var slots = rows.stream().map(row -> {
            var local = AvailabilityTimeConverter.toLocal(row.getStartTime(), row.getEndTime(), zone);
            String day = local.day().name().charAt(0) + local.day().name().substring(1).toLowerCase(Locale.ROOT);
            return new OnboardingAvailabilitySlotResponse(row.getId(), day,
                    local.start().toString(), local.end().toString());
        }).sorted(Comparator.comparing((OnboardingAvailabilitySlotResponse slot) ->
                DayOfWeek.valueOf(slot.dayOfWeek().toUpperCase(Locale.ROOT)))
                .thenComparing(OnboardingAvailabilitySlotResponse::startTime)
                .thenComparing(OnboardingAvailabilitySlotResponse::endTime)).toList();
        return new OnboardingAvailabilityResponse(userId, emergency, stage, slots);
    }

    public Map<String, AvailabilityView> getAvailabilityViews(List<Volunteer> volunteers) {
        if (volunteers.isEmpty()) return Map.of();
        List<String> ids = volunteers.stream().map(Volunteer::getId).toList();
        var accountsById = userRepository.findAvailabilityAccountsByIds(ids).stream()
                .collect(Collectors.toMap(UserRepository.AvailabilityAccount::getUserId, account -> account));
        var rowsById = userAvailabilityRepository.findByUser_IdInOrderByIdAsc(ids).stream()
                .collect(Collectors.groupingBy(row -> row.getUser().getId()));
        Map<String, AvailabilityView> result = new LinkedHashMap<>();
        for (Volunteer volunteer : volunteers) {
            String id = volunteer.getId();
            var account = accountsById.get(id);
            Integer stage = account == null ? null : account.getPromotionWizardStage();
            List<VolunteerUserAvailability> rows = rowsById.getOrDefault(id, List.of());
            if (rows.isEmpty()) {
                JsonNode days = volunteer.getAvailabilityDays();
                JsonNode times = volunteer.getAvailabilityTimes();
                result.put(id, new AvailabilityView(stage, days, times,
                        hasLegacyAvailabilityContent(days) || hasLegacyAvailabilityContent(times)));
            } else {
                ZoneId zone = UserServiceImpl.zoneOrUtc(account == null ? null : account.getTimeZone());
                result.put(id, relational(stage, rows, zone));
            }
        }
        return result;
    }

    private static AvailabilityView relational(Integer stage, List<VolunteerUserAvailability> rows, ZoneId zone) {
        var days = MAPPER.createArrayNode();
        var times = MAPPER.createArrayNode();
        var localDays = EnumSet.noneOf(DayOfWeek.class);
        for (var row : rows) {
            var local = AvailabilityTimeConverter.toLocal(row.getStartTime(), row.getEndTime(), zone);
            localDays.add(local.day());
            times.addObject()
                    .put("id", row.getId())
                    .put("dayOfWeek", availabilityDayName(local.day()))
                    .put("startTime", row.getStartTime().toInstant(ZoneOffset.UTC).toString())
                    .put("endTime", row.getEndTime().toInstant(ZoneOffset.UTC).toString());
        }
        localDays.forEach(day -> days.add(availabilityDayName(day)));
        return new AvailabilityView(stage, days, times, false);
    }

    private static String availabilityDayName(DayOfWeek day) {
        return day.getDisplayName(java.time.format.TextStyle.FULL, Locale.ENGLISH);
    }

    private static boolean hasLegacyAvailabilityContent(JsonNode value) {
        if (value == null || value.isNull()) return false;
        if (value.isContainerNode()) return !value.isEmpty();
        return !value.isTextual() || !value.textValue().isBlank();
    }

    private void completeAvailability(String userId, Integer requestedStep) {
        requireAvailabilityUserId(userId);
        int step = requestedStep == null ? 4 : requestedStep;
        if (step != 4 && step != 5) {
            throw new OnboardingAvailabilityException(400, "BAD_REQUEST", "Completion step must be 4 or 5");
        }
        requireOnboardingAvailabilityReady();
        var account = userRepository.lockAvailabilityAccountById(userId).orElseThrow(() -> missingAvailabilityAccount());
        requireAvailabilityVolunteer(userId);
        if (account.getPromotionWizardStage() == null || account.getPromotionWizardStage() < 4) throw stepFourRequired();
        requirePersistedIntervals(userService.readAvailabilityRows(userId), storedAvailabilityZone(account.getTimeZone()));
        if (step == 5 && account.getPromotionWizardStage() < step) {
            userRepository.advanceWizardProgress(userId, step, LocalDateTime.ofInstant(availabilityClock.instant(), ZoneOffset.UTC));
        }
    }

    private static void requirePersistedIntervals(List<VolunteerUserAvailability> rows, ZoneId zone) {
        if (rows.isEmpty()) throw stepFourRequired();
        List<LocalSlot> local = new ArrayList<>(rows.size());
        for (var row : rows) {
            var start = row.getStartTime().atOffset(ZoneOffset.UTC).atZoneSameInstant(zone);
            var end = row.getEndTime().atOffset(ZoneOffset.UTC).atZoneSameInstant(zone);
            if (!start.toLocalDate().equals(end.toLocalDate())
                    || !end.toLocalTime().isAfter(start.toLocalTime())
                    || !end.toInstant().isAfter(start.toInstant())
                    || start.getSecond() != 0 || start.getNano() != 0 || end.getSecond() != 0 || end.getNano() != 0) {
                throw stepFourRequired();
            }
            local.add(new LocalSlot(start.getDayOfWeek(), start.toLocalTime(), end.toLocalTime()));
        }
        if (hasAvailabilityOverlap(local)) throw stepFourRequired();
    }

    private static OnboardingAvailabilityException stepFourRequired() {
        return new OnboardingAvailabilityException(409, "STEP_FOUR_REQUIRED",
                "Save valid onboarding availability before completing this step");
    }

    private static OnboardingAvailabilityException missingAvailabilityAccount() {
        return new OnboardingAvailabilityException(404, "USER_NOT_FOUND", "Account not found");
    }

    private void requireAvailabilityVolunteer(String userId) {
        if (!volunteerRepository.existsById(userId)) {
            throw new OnboardingAvailabilityException(404, "VOLUNTEER_NOT_FOUND", "Volunteer not found");
        }
    }

    private void lockAccount(String userId) {
        userRepository.lockAvailabilityAccountById(userId).orElseThrow(() -> new UserNotFoundException(userId));
    }

    private Volunteer lockedVolunteer(String userId) {
        lockAccount(userId);
        Volunteer volunteer = volunteerRepository.findById(userId)
                .orElseThrow(() -> VolunteerException.volunteerNotFound(userId));
        entityManager.refresh(volunteer);
        return volunteer;
    }

    private void advanceProgress(VolunteerRequest request) {
        userRepository.advanceWizardProgress(request.userId(), request.step(),
                LocalDateTime.ofInstant(availabilityClock.instant(), ZoneOffset.UTC));
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
        throw availabilityBadRequest("Step 4 requires local HH:mm availability and an explicit emergency preference");
    }

    // @Override
    // public UserVolunteerSkillsResponse findSkillsList() throws Exception {
    // return null;
    // }

    @Override
    public VolunteerResponse updateVolunteerCompletion(VolunteerRequest request) {
        completeAvailability(request.userId(), request.step());
        Volunteer volunteer = volunteerRepository.findById(request.userId())
                .orElseThrow(() -> VolunteerException.volunteerNotFound(request.userId()));
        return mapToVolunteerResponse(volunteer);
    }

    @Override
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public VolunteerResponse getVolunteerByUserId(String userId) throws Exception {
        Volunteer volunteer = volunteerRepository.findById(userId)
                .orElseThrow(() -> VolunteerException.volunteerNotFound(userId));
        return mapToVolunteerResponse(volunteer);
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
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public PaginationResponse<VolunteerResponse> findAllVolunteersWithPagination(Integer pageNumber, Integer pageSize) {
        int pageNum = (pageNumber == null) ? DEFAULT_PAGE : pageNumber;
        int pageSizeNum = (pageSize == null) ? DEFAULT_SIZE : pageSize;

        Pageable pageable = PageRequest.of(pageNum, pageSizeNum);
        Page<Volunteer> volunteerPage = volunteerRepository.findAll(pageable);

        var availability = getAvailabilityViews(volunteerPage.getContent());
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
        return mapToVolunteerResponse(volunteer, getAvailabilityViews(List.of(volunteer)).get(volunteer.getId()));
    }

    private VolunteerResponse mapToVolunteerResponse(Volunteer volunteer, AvailabilityView availability) {
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

