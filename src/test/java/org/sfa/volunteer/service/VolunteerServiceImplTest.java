package org.sfa.volunteer.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sfa.volunteer.dto.response.IdentityDocumentMetadata;
import org.sfa.volunteer.model.Volunteer;
import org.sfa.volunteer.repository.VolunteerRepository;
import org.sfa.volunteer.service.impl.VolunteerServiceImpl;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VolunteerServiceImplTest {

    private static final String USER_ID = "SID-001";

    @Mock
    private VolunteerRepository volunteerRepository;

    @InjectMocks
    private VolunteerServiceImpl service;

    private Volunteer volunteerWith(String path, String name, LocalDate expiry) {
        Volunteer v = new Volunteer();
        v.setGovtIdPath1(path);
        v.setGovtIdName1(name);
        v.setGovtIdExpiry1(expiry);
        return v;
    }

    @Test
    void returnsMetadataWhenDocumentStored() throws Exception {
        LocalDate expiry = LocalDate.now().plusDays(90);
        when(volunteerRepository.findById(USER_ID))
                .thenReturn(Optional.of(volunteerWith("s3://bucket/key", "passport.pdf", expiry)));

        IdentityDocumentMetadata metadata = service.getIdentityDocumentMetadata(USER_ID, 1);

        assertNotNull(metadata);
    }

    /**
     * A document uploaded during ba #31 onboarding has a stored path but no
     * expiry date. It must still report as present (FR-21, AC-20).
     */
    @Test
    void returnsMetadataWhenDocumentStoredWithoutExpiry() throws Exception {
        when(volunteerRepository.findById(USER_ID))
                .thenReturn(Optional.of(volunteerWith("s3://bucket/key", "passport.pdf", null)));

        IdentityDocumentMetadata metadata = service.getIdentityDocumentMetadata(USER_ID, 1);

        assertNotNull(metadata);
    }

    @Test
    void returnsNullWhenNoPathStored() throws Exception {
        when(volunteerRepository.findById(USER_ID))
                .thenReturn(Optional.of(volunteerWith(null, null, null)));

        assertNull(service.getIdentityDocumentMetadata(USER_ID, 1));
    }

    @Test
    void returnsNullWhenPathIsBlank() throws Exception {
        when(volunteerRepository.findById(USER_ID))
                .thenReturn(Optional.of(volunteerWith("   ", null, null)));

        assertNull(service.getIdentityDocumentMetadata(USER_ID, 1));
    }

    @Test
    void rejectsSlotBelowRange() {
        assertThrows(ResponseStatusException.class,
                () -> service.getIdentityDocumentMetadata(USER_ID, 0));
    }

    @Test
    void rejectsSlotAboveRange() {
        assertThrows(ResponseStatusException.class,
                () -> service.getIdentityDocumentMetadata(USER_ID, 3));
    }

    @Test
    void throwsWhenVolunteerNotFound() {
        when(volunteerRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThrows(Exception.class,
                () -> service.getIdentityDocumentMetadata(USER_ID, 1));
    }
}