// package org.sfa.volunteer.service.impl;

// public class StewardServiceImplTest {
    
// }
package org.sfa.volunteer.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sfa.volunteer.dto.response.PaginationResponse;
import org.sfa.volunteer.dto.response.StewardVolunteerResponse;
import org.sfa.volunteer.model.User;
import org.sfa.volunteer.model.Volunteer;
import org.sfa.volunteer.repository.VolunteerRepository;
import org.sfa.volunteer.service.LegacyAvailabilityProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class StewardServiceImplTest {

    @Mock
    private VolunteerRepository volunteerRepository;
    @Mock
    private LegacyAvailabilityProjection legacyAvailability;

    private StewardServiceImpl stewardService;

    @BeforeEach
    void setUp() {
        stewardService = new StewardServiceImpl(volunteerRepository, legacyAvailability);
    }

    @Test
    void shouldReturnPaginatedVolunteers() {
        User user = new User();
        user.setId("USR-001");
        user.setVolunteerStage(4);

        Volunteer volunteer = Volunteer.builder()
                .id("USR-001")
                .user(user)
                .build();

        Page<Volunteer> page = new PageImpl<>(List.of(volunteer));

        when(volunteerRepository.findAll(any(Pageable.class)))
                .thenReturn(page);
        when(legacyAvailability.read(List.of(volunteer))).thenReturn(Map.of("USR-001",
                new LegacyAvailabilityProjection.View(4, null, null, false)));

        PaginationResponse<StewardVolunteerResponse> response =
                stewardService.getVolunteers(0, 10);

        assertEquals(1, response.items().size());
        assertEquals(
                "USR-001",
                response.items().get(0).userId()
        );
        assertEquals(
                4,
                response.items().get(0).volunteerStage()
        );

        verify(volunteerRepository).findAll(any(Pageable.class));
    }
}
