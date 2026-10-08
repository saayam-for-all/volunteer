package org.sfa.volunteer.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrgSizeTest {

    @Test
    void fromString_returnsNull_whenValueIsNull() {
        assertNull(OrgSize.fromString(null));
    }

    @Test
    void fromString_matchesDbValue() {
        assertEquals(OrgSize.SMALL, OrgSize.fromString("small"));
    }

    @Test
    void fromString_matchesEnumNameCaseInsensitively() {
        assertEquals(OrgSize.LARGE, OrgSize.fromString("LARGE"));
    }

    @Test
    void fromString_matchesMixedCase() {
        assertEquals(OrgSize.MEDIUM, OrgSize.fromString("MeDiUm"));
    }

    @Test
    void fromString_throws_whenValueInvalid() {
        assertThrows(IllegalArgumentException.class,
                () -> OrgSize.fromString("enormous"));
    }
}