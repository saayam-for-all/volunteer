package org.sfa.volunteer.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;

import org.junit.jupiter.api.Test;

class UserDiffblueTest {
    /**
     * Methods under test:
     *
     * <ul>
     *   <li>{@link User#User()}
     *   <li>{@link User#setAddressLine1(String)}
     *   <li>{@link User#setAddressLine2(String)}
     *   <li>{@link User#setAddressLine3(String)}
     *   <li>{@link User#setCity(String)}
     *   <li>{@link User#setCntcMbr(String)}
     *   <li>{@link User#setCountryId(String)}
     *   <li>{@link User#setEmailId(String)}
     *   <li>{@link User#setEmergencyInd(String)}
     *   <li>{@link User#setFirstName(String)}
     *   <li>{@link User#setGeoCode(String)}
     *   <li>{@link User#setId(BigInteger)}
     *   <li>{@link User#setIdnty_typ_id(BigInteger)}
     *   <li>{@link User#setLastName(String)}
     *   <li>{@link User#setMiddleName(String)}
     *   <li>{@link User#setPhoneNumber(BigInteger)}
     *   <li>{@link User#setRegisteredDate(Date)}
     *   <li>{@link User#setStateId(BigInteger)}
     *   <li>{@link User#setUserSTSDate(Date)}
     *   <li>{@link User#setUserSTSId(BigInteger)}
     *   <li>{@link User#setZipCode(BigInteger)}
     *   <li>{@link User#getAddressLine1()}
     *   <li>{@link User#getAddressLine2()}
     *   <li>{@link User#getAddressLine3()}
     *   <li>{@link User#getCity()}
     *   <li>{@link User#getCntcMbr()}
     *   <li>{@link User#getCountryId()}
     *   <li>{@link User#getEmailId()}
     *   <li>{@link User#getEmergencyInd()}
     *   <li>{@link User#getFirstName()}
     *   <li>{@link User#getGeoCode()}
     *   <li>{@link User#getId()}
     *   <li>{@link User#getIdnty_typ_id()}
     *   <li>{@link User#getLastName()}
     *   <li>{@link User#getMiddleName()}
     *   <li>{@link User#getPhoneNumber()}
     *   <li>{@link User#getRegisteredDate()}
     *   <li>{@link User#getStateId()}
     *   <li>{@link User#getUserSTSDate()}
     *   <li>{@link User#getUserSTSId()}
     *   <li>{@link User#getZipCode()}
     * </ul>
     */
    @Test
    void testGettersAndSetters() {
        // Arrange and Act
        User actualUser = new User();
        actualUser.setAddressLine1("42 Main St");
        actualUser.setAddressLine2("42 Main St");
        actualUser.setAddressLine3("42 Main St");
        actualUser.setCity("Oxford");
        actualUser.setCntcMbr("Cntc Mbr");
        actualUser.setCountryId("GB");
        actualUser.setEmailId("42");
        actualUser.setEmergencyInd("Emergency Ind");
        actualUser.setFirstName("Jane");
        actualUser.setGeoCode("Geo Code");
        actualUser.setId(BigInteger.valueOf(1L));
        actualUser.setIdnty_typ_id(BigInteger.valueOf(1L));
        actualUser.setLastName("Doe");
        actualUser.setMiddleName("Middle Name");
        actualUser.setPhoneNumber(BigInteger.valueOf(1L));
        Date registeredDate = Date.from(LocalDate.of(1970, 1, 1).atStartOfDay().atZone(ZoneOffset.UTC).toInstant());
        actualUser.setRegisteredDate(registeredDate);
        actualUser.setStateId(BigInteger.valueOf(1L));
        Date userSTSDate = Date.from(LocalDate.of(1970, 1, 1).atStartOfDay().atZone(ZoneOffset.UTC).toInstant());
        actualUser.setUserSTSDate(userSTSDate);
        actualUser.setUserSTSId(BigInteger.valueOf(1L));
        actualUser.setZipCode(BigInteger.valueOf(1L));
        String actualAddressLine1 = actualUser.getAddressLine1();
        String actualAddressLine2 = actualUser.getAddressLine2();
        String actualAddressLine3 = actualUser.getAddressLine3();
        String actualCity = actualUser.getCity();
        String actualCntcMbr = actualUser.getCntcMbr();
        String actualCountryId = actualUser.getCountryId();
        String actualEmailId = actualUser.getEmailId();
        String actualEmergencyInd = actualUser.getEmergencyInd();
        String actualFirstName = actualUser.getFirstName();
        String actualGeoCode = actualUser.getGeoCode();
        BigInteger actualId = actualUser.getId();
        BigInteger actualIdnty_typ_id = actualUser.getIdnty_typ_id();
        String actualLastName = actualUser.getLastName();
        String actualMiddleName = actualUser.getMiddleName();
        BigInteger actualPhoneNumber = actualUser.getPhoneNumber();
        Date actualRegisteredDate = actualUser.getRegisteredDate();
        BigInteger actualStateId = actualUser.getStateId();
        Date actualUserSTSDate = actualUser.getUserSTSDate();
        BigInteger actualUserSTSId = actualUser.getUserSTSId();
        BigInteger actualZipCode = actualUser.getZipCode();

        // Assert that nothing has changed
        assertEquals("42 Main St", actualAddressLine1);
        assertEquals("42 Main St", actualAddressLine2);
        assertEquals("42 Main St", actualAddressLine3);
        assertEquals("42", actualEmailId);
        assertEquals("Cntc Mbr", actualCntcMbr);
        assertEquals("Doe", actualLastName);
        assertEquals("Emergency Ind", actualEmergencyInd);
        assertEquals("GB", actualCountryId);
        assertEquals("Geo Code", actualGeoCode);
        assertEquals("Jane", actualFirstName);
        assertEquals("Middle Name", actualMiddleName);
        assertEquals("Oxford", actualCity);
        assertSame(actualId, actualIdnty_typ_id);
        assertSame(actualId, actualPhoneNumber);
        assertSame(actualId, actualStateId);
        assertSame(actualId, actualUserSTSId);
        assertSame(actualId, actualZipCode);
        assertSame(registeredDate, actualRegisteredDate);
        assertSame(userSTSDate, actualUserSTSDate);
        assertSame(actualZipCode.ONE, actualId);
    }

    /**
     * Methods under test:
     *
     * <ul>
     *   <li>
     * {@link User#User(BigInteger, BigInteger, String, String, String, String, BigInteger, String, String, String, String, String, BigInteger, BigInteger, String, Date, BigInteger, Date, String, String)}
     *   <li>{@link User#setAddressLine1(String)}
     *   <li>{@link User#setAddressLine2(String)}
     *   <li>{@link User#setAddressLine3(String)}
     *   <li>{@link User#setCity(String)}
     *   <li>{@link User#setCntcMbr(String)}
     *   <li>{@link User#setCountryId(String)}
     *   <li>{@link User#setEmailId(String)}
     *   <li>{@link User#setEmergencyInd(String)}
     *   <li>{@link User#setFirstName(String)}
     *   <li>{@link User#setGeoCode(String)}
     *   <li>{@link User#setId(BigInteger)}
     *   <li>{@link User#setIdnty_typ_id(BigInteger)}
     *   <li>{@link User#setLastName(String)}
     *   <li>{@link User#setMiddleName(String)}
     *   <li>{@link User#setPhoneNumber(BigInteger)}
     *   <li>{@link User#setRegisteredDate(Date)}
     *   <li>{@link User#setStateId(BigInteger)}
     *   <li>{@link User#setUserSTSDate(Date)}
     *   <li>{@link User#setUserSTSId(BigInteger)}
     *   <li>{@link User#setZipCode(BigInteger)}
     *   <li>{@link User#getAddressLine1()}
     *   <li>{@link User#getAddressLine2()}
     *   <li>{@link User#getAddressLine3()}
     *   <li>{@link User#getCity()}
     *   <li>{@link User#getCntcMbr()}
     *   <li>{@link User#getCountryId()}
     *   <li>{@link User#getEmailId()}
     *   <li>{@link User#getEmergencyInd()}
     *   <li>{@link User#getFirstName()}
     *   <li>{@link User#getGeoCode()}
     *   <li>{@link User#getId()}
     *   <li>{@link User#getIdnty_typ_id()}
     *   <li>{@link User#getLastName()}
     *   <li>{@link User#getMiddleName()}
     *   <li>{@link User#getPhoneNumber()}
     *   <li>{@link User#getRegisteredDate()}
     *   <li>{@link User#getStateId()}
     *   <li>{@link User#getUserSTSDate()}
     *   <li>{@link User#getUserSTSId()}
     *   <li>{@link User#getZipCode()}
     * </ul>
     */
    @Test
    void testGettersAndSetters2() {
        // Arrange
        BigInteger id = BigInteger.valueOf(1L);
        BigInteger idnty_typ_id = BigInteger.valueOf(1L);
        BigInteger phoneNumber = BigInteger.valueOf(1L);
        BigInteger stateId = BigInteger.valueOf(1L);
        BigInteger zipCode = BigInteger.valueOf(1L);
        Date registeredDate = Date.from(LocalDate.of(1970, 1, 1).atStartOfDay().atZone(ZoneOffset.UTC).toInstant());
        BigInteger userSTSId = BigInteger.valueOf(1L);

        // Act
        User actualUser = new User(id, idnty_typ_id, "Jane", "Middle Name", "Doe", "42", phoneNumber, "42 Main St",
                "42 Main St", "42 Main St", "Oxford", "GB", stateId, zipCode, "Geo Code", registeredDate, userSTSId,
                Date.from(LocalDate.of(1970, 1, 1).atStartOfDay().atZone(ZoneOffset.UTC).toInstant()), "Emergency Ind",
                "Cntc Mbr");
        actualUser.setAddressLine1("42 Main St");
        actualUser.setAddressLine2("42 Main St");
        actualUser.setAddressLine3("42 Main St");
        actualUser.setCity("Oxford");
        actualUser.setCntcMbr("Cntc Mbr");
        actualUser.setCountryId("GB");
        actualUser.setEmailId("42");
        actualUser.setEmergencyInd("Emergency Ind");
        actualUser.setFirstName("Jane");
        actualUser.setGeoCode("Geo Code");
        actualUser.setId(BigInteger.valueOf(1L));
        actualUser.setIdnty_typ_id(BigInteger.valueOf(1L));
        actualUser.setLastName("Doe");
        actualUser.setMiddleName("Middle Name");
        actualUser.setPhoneNumber(BigInteger.valueOf(1L));
        Date registeredDate2 = Date.from(LocalDate.of(1970, 1, 1).atStartOfDay().atZone(ZoneOffset.UTC).toInstant());
        actualUser.setRegisteredDate(registeredDate2);
        actualUser.setStateId(BigInteger.valueOf(1L));
        Date userSTSDate = Date.from(LocalDate.of(1970, 1, 1).atStartOfDay().atZone(ZoneOffset.UTC).toInstant());
        actualUser.setUserSTSDate(userSTSDate);
        actualUser.setUserSTSId(BigInteger.valueOf(1L));
        actualUser.setZipCode(BigInteger.valueOf(1L));
        String actualAddressLine1 = actualUser.getAddressLine1();
        String actualAddressLine2 = actualUser.getAddressLine2();
        String actualAddressLine3 = actualUser.getAddressLine3();
        String actualCity = actualUser.getCity();
        String actualCntcMbr = actualUser.getCntcMbr();
        String actualCountryId = actualUser.getCountryId();
        String actualEmailId = actualUser.getEmailId();
        String actualEmergencyInd = actualUser.getEmergencyInd();
        String actualFirstName = actualUser.getFirstName();
        String actualGeoCode = actualUser.getGeoCode();
        BigInteger actualId = actualUser.getId();
        BigInteger actualIdnty_typ_id = actualUser.getIdnty_typ_id();
        String actualLastName = actualUser.getLastName();
        String actualMiddleName = actualUser.getMiddleName();
        BigInteger actualPhoneNumber = actualUser.getPhoneNumber();
        Date actualRegisteredDate = actualUser.getRegisteredDate();
        BigInteger actualStateId = actualUser.getStateId();
        Date actualUserSTSDate = actualUser.getUserSTSDate();
        BigInteger actualUserSTSId = actualUser.getUserSTSId();
        BigInteger actualZipCode = actualUser.getZipCode();

        // Assert that nothing has changed
        assertEquals("42 Main St", actualAddressLine1);
        assertEquals("42 Main St", actualAddressLine2);
        assertEquals("42 Main St", actualAddressLine3);
        assertEquals("42", actualEmailId);
        assertEquals("Cntc Mbr", actualCntcMbr);
        assertEquals("Doe", actualLastName);
        assertEquals("Emergency Ind", actualEmergencyInd);
        assertEquals("GB", actualCountryId);
        assertEquals("Geo Code", actualGeoCode);
        assertEquals("Jane", actualFirstName);
        assertEquals("Middle Name", actualMiddleName);
        assertEquals("Oxford", actualCity);
        assertSame(actualId, actualIdnty_typ_id);
        assertSame(actualId, actualPhoneNumber);
        assertSame(actualId, actualStateId);
        assertSame(actualId, actualUserSTSId);
        assertSame(actualId, actualZipCode);
        assertSame(registeredDate2, actualRegisteredDate);
        assertSame(userSTSDate, actualUserSTSDate);
        assertSame(actualZipCode.ONE, actualId);
    }
}
