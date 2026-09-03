package org.sfa.volunteer.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrgTypeTest {

    @Test
    void fromString_returnsNull_whenValueIsNull() {
        assertNull(OrgType.fromString(null));
    }

    @Test
    void fromString_matchesDbValue() {
        assertEquals(OrgType.NON_PROFIT, OrgType.fromString("non_profit"));
    }

    @Test
    void fromString_matchesEnumNameCaseInsensitively() {
        assertEquals(OrgType.FOR_PROFIT, OrgType.fromString("FOR_PROFIT"));
    }

    @Test
    void fromString_throws_whenValueInvalid() {
        assertThrows(IllegalArgumentException.class,
                () -> OrgType.fromString("banana"));
    }

    @Test
    void getDbValue_returnsLowercaseValue() {
        assertEquals("non_profit", OrgType.NON_PROFIT.getDbValue());
    }
}