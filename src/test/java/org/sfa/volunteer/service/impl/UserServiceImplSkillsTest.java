package org.sfa.volunteer.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sfa.volunteer.dto.response.UserSkillsResponse;
import org.sfa.volunteer.exception.UserNotFoundException;
import org.sfa.volunteer.model.SkillLevel;
import org.sfa.volunteer.model.UserSkillId;
import org.sfa.volunteer.model.UserSkills;
import org.sfa.volunteer.repository.UserRepository;
import org.sfa.volunteer.repository.UserSkillRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplSkillsTest {

    private static final String USER_ID = "SID-1";

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserSkillRepository userSkillRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private static UserSkills skill(String catId, SkillLevel level) {
        return UserSkills.builder()
                .id(new UserSkillId(USER_ID, catId))
                .skillLevel(level)
                .lastUpdatedAt(LocalDateTime.of(2026, 1, 1, 0, 0))
                .build();
    }

    @Test
    void getUserSkillsReturnsIdsAndLevels() {
        when(userRepository.existsById(USER_ID)).thenReturn(true);
        when(userSkillRepository.findByIdUserId(USER_ID))
                .thenReturn(List.of(skill("4.2", SkillLevel.ADVANCED), skill("0.1", null)));

        UserSkillsResponse response = userService.getUserSkills(USER_ID);

        assertThat(response.getUserId()).isEqualTo(USER_ID);
        assertThat(response.getSkills()).containsExactly("4.2", "0.1");
        assertThat(response.getSkillLevels())
                .containsEntry("4.2", SkillLevel.ADVANCED)
                .containsEntry("0.1", null);
    }

    @Test
    void getUserSkillsReturnsEmptyListForUserWithoutSkills() {
        when(userRepository.existsById(USER_ID)).thenReturn(true);
        when(userSkillRepository.findByIdUserId(USER_ID)).thenReturn(List.of());

        UserSkillsResponse response = userService.getUserSkills(USER_ID);

        assertThat(response.getSkills()).isEmpty();
        assertThat(response.getSkillLevels()).isEmpty();
    }

    @Test
    void getUserSkillsThrowsUserNotFound() {
        when(userRepository.existsById(USER_ID)).thenReturn(false);

        assertThatThrownBy(() -> userService.getUserSkills(USER_ID))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void blankUserIdIsTreatedAsUnknownUser() {
        assertThatThrownBy(() -> userService.getUserSkills(" "))
                .isInstanceOf(UserNotFoundException.class);
        verify(userRepository, never()).existsById(any());
    }
}
