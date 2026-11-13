package org.sfa.volunteer.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sfa.volunteer.dto.request.UpdatePersonalInfoRequest;
import org.sfa.volunteer.dto.response.PersonalInfoResponse;
import org.sfa.volunteer.exception.CountryNotFoundException;
import org.sfa.volunteer.exception.StateNotFoundException;
import org.sfa.volunteer.exception.UserNotFoundException;
import org.sfa.volunteer.model.Country;
import org.sfa.volunteer.model.State;
import org.sfa.volunteer.model.User;
import org.sfa.volunteer.model.UserAdditionalDetail;
import org.sfa.volunteer.repository.CountryRepository;
import org.sfa.volunteer.repository.StateRepository;
import org.sfa.volunteer.repository.UserRepository;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the personal-info methods of UserServiceImpl.
 *
 * @InjectMocks builds the real UserServiceImpl and hands it the @Mock repositories below
 * (any constructor argument we don't declare a mock for is passed as null, which is fine
 * because these methods never touch them). No database is involved: each repository call
 * is programmed with when(...).thenReturn(...).
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplPersonalInfoTest {

    private static final String USER_ID = "SID-1";

    @Mock
    private UserRepository userRepository;
    @Mock
    private StateRepository stateRepository;
    @Mock
    private CountryRepository countryRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(USER_ID)
                .dob(LocalDate.of(1990, 5, 12))
                .gender("Female")
                .addressLine1("1 Main St")
                .city("San Jose")
                .zipCode("95112")
                .state(State.builder().stateName("California").build())
                .country(Country.builder().countryName("United States").build())
                .build();
    }

    private static UpdatePersonalInfoRequest update() {
        UpdatePersonalInfoRequest request = new UpdatePersonalInfoRequest();
        request.setUserId(USER_ID);
        return request;
    }

    private void stubFindAndSave() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ---------- getPersonalInfoById ----------

    @Test
    void getReturnsMappedFieldsAndFormatsDateOfBirth() {
        user.setAdditionalDetail(UserAdditionalDetail.builder()
                .secondaryEmail1("alt@example.com").secondaryPhone1("555-0100").build());
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        PersonalInfoResponse info = userService.getPersonalInfoById(USER_ID);

        assertThat(info.getDateOfBirth()).isEqualTo("05/12/1990");   // MM/dd/yyyy
        assertThat(info.getGender()).isEqualTo("Female");
        assertThat(info.getAddressLine1()).isEqualTo("1 Main St");
        assertThat(info.getCity()).isEqualTo("San Jose");
        assertThat(info.getState()).isEqualTo("California");
        assertThat(info.getCountry()).isEqualTo("United States");
        assertThat(info.getZipCode()).isEqualTo("95112");
        assertThat(info.getSecondaryEmail()).isEqualTo("alt@example.com");
        assertThat(info.getSecondaryPhone()).isEqualTo("555-0100");
    }

    @Test
    void getReturnsNullsInsteadOfFailingWhenOptionalDataIsMissing() {
        User bare = User.builder().id(USER_ID).build();   // no dob, state, country, additional detail
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(bare));

        PersonalInfoResponse info = userService.getPersonalInfoById(USER_ID);

        assertThat(info.getDateOfBirth()).isNull();
        assertThat(info.getState()).isNull();
        assertThat(info.getCountry()).isNull();
        assertThat(info.getSecondaryEmail()).isNull();
        assertThat(info.getSecondaryPhone()).isNull();
    }

    @Test
    void getThrowsUserNotFoundForUnknownUser() {
        when(userRepository.findById("SID-404")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getPersonalInfoById("SID-404"))
                .isInstanceOf(UserNotFoundException.class);
    }

    // ---------- updatePersonalInfo ----------

    @Test
    void updateAppliesProvidedFieldsAndSaves() {
        stubFindAndSave();
        State texas = State.builder().stateName("Texas").build();
        Country usa = Country.builder().countryName("United States").build();
        when(stateRepository.findByStateName("Texas")).thenReturn(Optional.of(texas));
        when(countryRepository.findByCountryName("United States")).thenReturn(Optional.of(usa));

        UpdatePersonalInfoRequest request = update();
        request.setDateOfBirth("01/31/1985");
        request.setGender("Male");
        request.setAddressLine1("2 Oak Ave");
        request.setAddressLine2("Apt 4");
        request.setCity("Austin");
        request.setZipCode("73301");
        request.setState("Texas");
        request.setCountry("United States");
        request.setSecondaryEmail("new@example.com");
        request.setSecondaryPhone("555-0199");

        PersonalInfoResponse info = userService.updatePersonalInfo(request);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        User savedUser = saved.getValue();
        assertThat(savedUser.getDob()).isEqualTo(LocalDate.of(1985, 1, 31));
        assertThat(savedUser.getGender()).isEqualTo("Male");
        assertThat(savedUser.getCity()).isEqualTo("Austin");
        assertThat(savedUser.getState()).isSameAs(texas);
        assertThat(savedUser.getLastUpdateDate()).isNotNull();
        // a user with no additional-detail row gets one created
        assertThat(savedUser.getAdditionalDetail().getSecondaryEmail1()).isEqualTo("new@example.com");
        assertThat(savedUser.getAdditionalDetail().getSecondaryPhone1()).isEqualTo("555-0199");
        assertThat(savedUser.getAdditionalDetail().getUser()).isSameAs(savedUser);

        // and the response reflects what was saved
        assertThat(info.getDateOfBirth()).isEqualTo("01/31/1985");
        assertThat(info.getState()).isEqualTo("Texas");
    }

    @Test
    void updateLeavesOmittedFieldsUnchanged() {
        stubFindAndSave();

        UpdatePersonalInfoRequest request = update();
        request.setCity("Austin");   // everything else null

        PersonalInfoResponse info = userService.updatePersonalInfo(request);

        assertThat(info.getCity()).isEqualTo("Austin");
        assertThat(info.getGender()).isEqualTo("Female");
        assertThat(info.getAddressLine1()).isEqualTo("1 Main St");
        assertThat(info.getDateOfBirth()).isEqualTo("05/12/1990");
        assertThat(info.getState()).isEqualTo("California");
    }

    @Test
    void updateReusesExistingAdditionalDetailRow() {
        UserAdditionalDetail existing = UserAdditionalDetail.builder()
                .secondaryEmail1("old@example.com").secondaryPhone1("555-0000").build();
        user.setAdditionalDetail(existing);
        stubFindAndSave();

        UpdatePersonalInfoRequest request = update();
        request.setSecondaryEmail("new@example.com");

        userService.updatePersonalInfo(request);

        assertThat(user.getAdditionalDetail()).isSameAs(existing);
        assertThat(existing.getSecondaryEmail1()).isEqualTo("new@example.com");
        assertThat(existing.getSecondaryPhone1()).isEqualTo("555-0000");   // untouched
    }

    @Test
    void updateRejectsBadDateOfBirthWithIllegalArgumentAndSavesNothing() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        UpdatePersonalInfoRequest request = update();
        request.setDateOfBirth("1990-05-12");   // ISO format, but we expect MM/dd/yyyy

        assertThatThrownBy(() -> userService.updatePersonalInfo(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("dateOfBirth must be in MM/dd/yyyy format");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateRejectsImpossibleDateInsteadOfSilentlyAdjustingIt() {
        // Without a STRICT parser Java quietly turns 02/30/1990 into 02/28/1990 and saves it
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        UpdatePersonalInfoRequest request = update();
        request.setDateOfBirth("02/30/1990");

        assertThatThrownBy(() -> userService.updatePersonalInfo(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("dateOfBirth must be in MM/dd/yyyy format");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateAcceptsLeapDayInALeapYear() {
        stubFindAndSave();

        UpdatePersonalInfoRequest request = update();
        request.setDateOfBirth("02/29/2000");

        assertThat(userService.updatePersonalInfo(request).getDateOfBirth()).isEqualTo("02/29/2000");
    }

    @Test
    void updateRejectsLeapDayInANonLeapYear() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        UpdatePersonalInfoRequest request = update();
        request.setDateOfBirth("02/29/1999");

        assertThatThrownBy(() -> userService.updatePersonalInfo(request))
                .isInstanceOf(IllegalArgumentException.class);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateThrowsUserNotFoundForUnknownUser() {
        when(userRepository.findById("SID-404")).thenReturn(Optional.empty());

        UpdatePersonalInfoRequest request = new UpdatePersonalInfoRequest();
        request.setUserId("SID-404");

        assertThatThrownBy(() -> userService.updatePersonalInfo(request))
                .isInstanceOf(UserNotFoundException.class);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateThrowsStateNotFoundForUnknownState() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(stateRepository.findByStateName("Atlantis")).thenReturn(Optional.empty());

        UpdatePersonalInfoRequest request = update();
        request.setState("Atlantis");

        assertThatThrownBy(() -> userService.updatePersonalInfo(request))
                .isInstanceOf(StateNotFoundException.class);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateThrowsCountryNotFoundForUnknownCountry() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(countryRepository.findByCountryName("Narnia")).thenReturn(Optional.empty());

        UpdatePersonalInfoRequest request = update();
        request.setCountry("Narnia");

        assertThatThrownBy(() -> userService.updatePersonalInfo(request))
                .isInstanceOf(CountryNotFoundException.class);
        verify(userRepository, never()).save(any(User.class));
    }
}
