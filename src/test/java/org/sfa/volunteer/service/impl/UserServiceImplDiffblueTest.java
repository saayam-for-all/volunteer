package org.sfa.volunteer.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.sfa.volunteer.entities.User;
import org.sfa.volunteer.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ContextConfiguration(classes = {UserServiceImpl.class})
@ExtendWith(SpringExtension.class)
class UserServiceImplDiffblueTest {
    @MockBean
    private UserRepository userRepository;

    @Autowired
    private UserServiceImpl userServiceImpl;

    /**
     * Method under test: {@link UserServiceImpl#findAllUsers()}
     */
    @Test
    void testFindAllUsers() {
        // Arrange
        ArrayList<User> userList = new ArrayList<>();
        when(userRepository.findAll()).thenReturn(userList);

        // Act
        List<User> actualFindAllUsersResult = userServiceImpl.findAllUsers();

        // Assert
        verify(userRepository).findAll();
        assertTrue(actualFindAllUsersResult.isEmpty());
        assertSame(userList, actualFindAllUsersResult);
    }

    /**
     * Method under test: {@link UserServiceImpl#findById(BigInteger)}
     */
    @Test
    void testFindById() {
        // Arrange
        User user = new User();
        user.setAddressLine1("42 Main St");
        user.setAddressLine2("42 Main St");
        user.setAddressLine3("42 Main St");
        user.setCity("Oxford");
        user.setCntcMbr("Cntc Mbr");
        user.setCountryId("GB");
        user.setEmailId("42");
        user.setEmergencyInd("Emergency Ind");
        user.setFirstName("Jane");
        user.setGeoCode("Geo Code");
        user.setId(BigInteger.valueOf(1L));
        user.setIdnty_typ_id(BigInteger.valueOf(1L));
        user.setLastName("Doe");
        user.setMiddleName("Middle Name");
        user.setPhoneNumber(BigInteger.valueOf(1L));
        user.setRegisteredDate(Date.from(LocalDate.of(1970, 1, 1).atStartOfDay().atZone(ZoneOffset.UTC).toInstant()));
        user.setStateId(BigInteger.valueOf(1L));
        user.setUserSTSDate(Date.from(LocalDate.of(1970, 1, 1).atStartOfDay().atZone(ZoneOffset.UTC).toInstant()));
        user.setUserSTSId(BigInteger.valueOf(1L));
        user.setZipCode(BigInteger.valueOf(1L));
        Optional<User> ofResult = Optional.of(user);
        when(userRepository.findById(Mockito.<BigInteger>any())).thenReturn(ofResult);

        // Act
        Optional<User> actualFindByIdResult = userServiceImpl.findById(BigInteger.valueOf(1L));

        // Assert
        verify(userRepository).findById(isA(BigInteger.class));
        User getResult = actualFindByIdResult.get();
        BigInteger zipCode = getResult.getZipCode();
        assertEquals("1", zipCode.toString());
        assertSame(zipCode, getResult.getUserSTSId());
    }

    /**
     * Method under test: {@link UserServiceImpl#saveUser(User)}
     */
    @Test
    void testSaveUser() {
        // Arrange
        User user = new User();
        user.setAddressLine1("42 Main St");
        user.setAddressLine2("42 Main St");
        user.setAddressLine3("42 Main St");
        user.setCity("Oxford");
        user.setCntcMbr("Cntc Mbr");
        user.setCountryId("GB");
        user.setEmailId("42");
        user.setEmergencyInd("Emergency Ind");
        user.setFirstName("Jane");
        user.setGeoCode("Geo Code");
        user.setId(BigInteger.valueOf(1L));
        user.setIdnty_typ_id(BigInteger.valueOf(1L));
        user.setLastName("Doe");
        user.setMiddleName("Middle Name");
        user.setPhoneNumber(BigInteger.valueOf(1L));
        user.setRegisteredDate(Date.from(LocalDate.of(1970, 1, 1).atStartOfDay().atZone(ZoneOffset.UTC).toInstant()));
        user.setStateId(BigInteger.valueOf(1L));
        user.setUserSTSDate(Date.from(LocalDate.of(1970, 1, 1).atStartOfDay().atZone(ZoneOffset.UTC).toInstant()));
        user.setUserSTSId(BigInteger.valueOf(1L));
        user.setZipCode(BigInteger.valueOf(1L));
        when(userRepository.save(Mockito.<User>any())).thenReturn(user);

        User newUser = new User();
        newUser.setAddressLine1("42 Main St");
        newUser.setAddressLine2("42 Main St");
        newUser.setAddressLine3("42 Main St");
        newUser.setCity("Oxford");
        newUser.setCntcMbr("Cntc Mbr");
        newUser.setCountryId("GB");
        newUser.setEmailId("42");
        newUser.setEmergencyInd("Emergency Ind");
        newUser.setFirstName("Jane");
        newUser.setGeoCode("Geo Code");
        newUser.setId(BigInteger.valueOf(1L));
        newUser.setIdnty_typ_id(BigInteger.valueOf(1L));
        newUser.setLastName("Doe");
        newUser.setMiddleName("Middle Name");
        newUser.setPhoneNumber(BigInteger.valueOf(1L));
        newUser.setRegisteredDate(Date.from(LocalDate.of(1970, 1, 1).atStartOfDay().atZone(ZoneOffset.UTC).toInstant()));
        newUser.setStateId(BigInteger.valueOf(1L));
        newUser.setUserSTSDate(Date.from(LocalDate.of(1970, 1, 1).atStartOfDay().atZone(ZoneOffset.UTC).toInstant()));
        newUser.setUserSTSId(BigInteger.valueOf(1L));
        newUser.setZipCode(BigInteger.valueOf(1L));

        // Act
        User actualSaveUserResult = userServiceImpl.saveUser(newUser);

        // Assert
        verify(userRepository).save(isA(User.class));
        BigInteger zipCode = actualSaveUserResult.getZipCode();
        assertEquals("1", zipCode.toString());
        assertSame(zipCode, actualSaveUserResult.getUserSTSId());
    }

    /**
     * Method under test: {@link UserServiceImpl#updateUser(User)}
     */
    @Test
    void testUpdateUser() {
        // Arrange
        User user = new User();
        user.setAddressLine1("42 Main St");
        user.setAddressLine2("42 Main St");
        user.setAddressLine3("42 Main St");
        user.setCity("Oxford");
        user.setCntcMbr("Cntc Mbr");
        user.setCountryId("GB");
        user.setEmailId("42");
        user.setEmergencyInd("Emergency Ind");
        user.setFirstName("Jane");
        user.setGeoCode("Geo Code");
        user.setId(BigInteger.valueOf(1L));
        user.setIdnty_typ_id(BigInteger.valueOf(1L));
        user.setLastName("Doe");
        user.setMiddleName("Middle Name");
        user.setPhoneNumber(BigInteger.valueOf(1L));
        user.setRegisteredDate(Date.from(LocalDate.of(1970, 1, 1).atStartOfDay().atZone(ZoneOffset.UTC).toInstant()));
        user.setStateId(BigInteger.valueOf(1L));
        user.setUserSTSDate(Date.from(LocalDate.of(1970, 1, 1).atStartOfDay().atZone(ZoneOffset.UTC).toInstant()));
        user.setUserSTSId(BigInteger.valueOf(1L));
        user.setZipCode(BigInteger.valueOf(1L));
        when(userRepository.save(Mockito.<User>any())).thenReturn(user);

        User user2 = new User();
        user2.setAddressLine1("42 Main St");
        user2.setAddressLine2("42 Main St");
        user2.setAddressLine3("42 Main St");
        user2.setCity("Oxford");
        user2.setCntcMbr("Cntc Mbr");
        user2.setCountryId("GB");
        user2.setEmailId("42");
        user2.setEmergencyInd("Emergency Ind");
        user2.setFirstName("Jane");
        user2.setGeoCode("Geo Code");
        user2.setId(BigInteger.valueOf(1L));
        user2.setIdnty_typ_id(BigInteger.valueOf(1L));
        user2.setLastName("Doe");
        user2.setMiddleName("Middle Name");
        user2.setPhoneNumber(BigInteger.valueOf(1L));
        user2.setRegisteredDate(Date.from(LocalDate.of(1970, 1, 1).atStartOfDay().atZone(ZoneOffset.UTC).toInstant()));
        user2.setStateId(BigInteger.valueOf(1L));
        user2.setUserSTSDate(Date.from(LocalDate.of(1970, 1, 1).atStartOfDay().atZone(ZoneOffset.UTC).toInstant()));
        user2.setUserSTSId(BigInteger.valueOf(1L));
        user2.setZipCode(BigInteger.valueOf(1L));

        // Act
        User actualUpdateUserResult = userServiceImpl.updateUser(user2);

        // Assert
        verify(userRepository).save(isA(User.class));
        BigInteger zipCode = actualUpdateUserResult.getZipCode();
        assertEquals("1", zipCode.toString());
        assertSame(zipCode, actualUpdateUserResult.getUserSTSId());
    }

    /**
     * Method under test: {@link UserServiceImpl#deleteUser(BigInteger)}
     */
    @Test
    void testDeleteUser() {
        // Arrange
        doNothing().when(userRepository).deleteById(Mockito.<BigInteger>any());

        // Act
        userServiceImpl.deleteUser(BigInteger.valueOf(1L));

        // Assert that nothing has changed
        verify(userRepository).deleteById(isA(BigInteger.class));
        assertTrue(userServiceImpl.findAllUsers().isEmpty());
    }
}
