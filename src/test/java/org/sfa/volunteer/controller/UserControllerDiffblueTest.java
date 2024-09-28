package org.sfa.volunteer.controller;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.sfa.volunteer.entities.User;
import org.sfa.volunteer.service.impl.UserServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ContextConfiguration(classes = {UserController.class})
@ExtendWith(SpringExtension.class)
class UserControllerDiffblueTest {
    @Autowired
    private UserController userController;

    @MockBean
    private UserServiceImpl userServiceImpl;

    /**
     * Method under test: {@link UserController#saveUser(User)}
     */
    @Test
    void testSaveUser() throws Exception {
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
        when(userServiceImpl.saveUser(Mockito.<User>any())).thenReturn(user);

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
        String content = (new ObjectMapper()).writeValueAsString(user2);
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.post("/volunteer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(content);

        // Act and Assert
        MockMvcBuilders.standaloneSetup(userController)
                .build()
                .perform(requestBuilder)
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType("application/json"))
                .andExpect(MockMvcResultMatchers.content()
                        .string(
                                "{\"id\":1,\"idnty_typ_id\":1,\"firstName\":\"Jane\",\"middleName\":\"Middle Name\",\"lastName\":\"Doe\",\"emailId\":\"42"
                                        + "\",\"phoneNumber\":1,\"addressLine1\":\"42 Main St\",\"addressLine2\":\"42 Main St\",\"addressLine3\":\"42 Main"
                                        + " St\",\"city\":\"Oxford\",\"countryId\":\"GB\",\"stateId\":1,\"zipCode\":1,\"geoCode\":\"Geo Code\",\"registeredDate\":0"
                                        + ",\"userSTSId\":1,\"userSTSDate\":0,\"emergencyInd\":\"Emergency Ind\",\"cntcMbr\":\"Cntc Mbr\"}"));
    }

    /**
     * Method under test: {@link UserController#updateUser(User)}
     */
    @Test
    void testUpdateUser() throws Exception {
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
        when(userServiceImpl.updateUser(Mockito.<User>any())).thenReturn(user);

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
        String content = (new ObjectMapper()).writeValueAsString(user2);
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.put("/volunteer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(content);

        // Act and Assert
        MockMvcBuilders.standaloneSetup(userController)
                .build()
                .perform(requestBuilder)
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType("application/json"))
                .andExpect(MockMvcResultMatchers.content()
                        .string(
                                "{\"id\":1,\"idnty_typ_id\":1,\"firstName\":\"Jane\",\"middleName\":\"Middle Name\",\"lastName\":\"Doe\",\"emailId\":\"42"
                                        + "\",\"phoneNumber\":1,\"addressLine1\":\"42 Main St\",\"addressLine2\":\"42 Main St\",\"addressLine3\":\"42 Main"
                                        + " St\",\"city\":\"Oxford\",\"countryId\":\"GB\",\"stateId\":1,\"zipCode\":1,\"geoCode\":\"Geo Code\",\"registeredDate\":0"
                                        + ",\"userSTSId\":1,\"userSTSDate\":0,\"emergencyInd\":\"Emergency Ind\",\"cntcMbr\":\"Cntc Mbr\"}"));
    }

    /**
     * Method under test: {@link UserController#deleteUser(BigInteger)}
     */
    @Test
    void testDeleteUser() throws Exception {
        // Arrange
        doNothing().when(userServiceImpl).deleteUser(Mockito.<BigInteger>any());
        MockHttpServletRequestBuilder deleteResult = MockMvcRequestBuilders.delete("/volunteer");
        MockHttpServletRequestBuilder requestBuilder = deleteResult.param("id", String.valueOf(BigInteger.valueOf(1L)));

        // Act and Assert
        MockMvcBuilders.standaloneSetup(userController)
                .build()
                .perform(requestBuilder)
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    /**
     * Method under test: {@link UserController#getAllUsers()}
     */
    @Test
    void testGetAllUsers() throws Exception {
        // Arrange
        when(userServiceImpl.findAllUsers()).thenReturn(new ArrayList<>());
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.get("/volunteer");

        // Act and Assert
        MockMvcBuilders.standaloneSetup(userController)
                .build()
                .perform(requestBuilder)
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType("application/json"))
                .andExpect(MockMvcResultMatchers.content().string("[]"));
    }

    /**
     * Method under test: {@link UserController#getUser(BigInteger)}
     */
    @Test
    void testGetUser() throws Exception {
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
        when(userServiceImpl.findById(Mockito.<BigInteger>any())).thenReturn(ofResult);
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.get("/volunteer/{id}",
                BigInteger.valueOf(1L));

        // Act and Assert
        MockMvcBuilders.standaloneSetup(userController)
                .build()
                .perform(requestBuilder)
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType("application/json"))
                .andExpect(MockMvcResultMatchers.content()
                        .string(
                                "{\"id\":1,\"idnty_typ_id\":1,\"firstName\":\"Jane\",\"middleName\":\"Middle Name\",\"lastName\":\"Doe\",\"emailId\":\"42"
                                        + "\",\"phoneNumber\":1,\"addressLine1\":\"42 Main St\",\"addressLine2\":\"42 Main St\",\"addressLine3\":\"42 Main"
                                        + " St\",\"city\":\"Oxford\",\"countryId\":\"GB\",\"stateId\":1,\"zipCode\":1,\"geoCode\":\"Geo Code\",\"registeredDate\":0"
                                        + ",\"userSTSId\":1,\"userSTSDate\":0,\"emergencyInd\":\"Emergency Ind\",\"cntcMbr\":\"Cntc Mbr\"}"));
    }
}
