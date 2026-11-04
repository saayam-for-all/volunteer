package org.sfa.volunteer.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sfa.volunteer.dto.common.SaayamStatusCode;
import org.sfa.volunteer.dto.request.SaveUserSkillsRequest;
import org.sfa.volunteer.dto.response.SaveUserSkillsResponse;
import org.sfa.volunteer.exception.SkillsException;
import org.sfa.volunteer.exception.UserNotFoundException;
import org.sfa.volunteer.model.HelpCategory;
import org.sfa.volunteer.model.User;
import org.sfa.volunteer.model.UserSkill;
import org.sfa.volunteer.repository.HelpCategoryRepository;
import org.sfa.volunteer.repository.UserRepository;
import org.sfa.volunteer.repository.UserSkillRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SkillsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private HelpCategoryRepository helpCategoryRepository;

    @Mock
    private UserSkillRepository userSkillRepository;

    @InjectMocks
    private SkillsServiceImpl service;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
    }

    @Test
    void saveUserSkills_success() {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(helpCategoryRepository.findAllById(List.of("1.1", "1.2")))
                .thenReturn(List.of(
                        new HelpCategory("1.1", "FOOD_ASSISTANCE", "desc"),
                        new HelpCategory("1.2", "CLOTHING_ASSISTANCE", "desc")));
        when(userSkillRepository.saveAll(anyList())).thenReturn(List.of());

        SaveUserSkillsRequest request = SaveUserSkillsRequest.builder()
                .userId("user-1")
                .catIds(List.of("1.1", "1.2"))
                .build();

        SaveUserSkillsResponse response = service.saveUserSkills(request);

        assertThat(response.userId()).isEqualTo("user-1");
        assertThat(response.savedCatIds()).containsExactly("1.1", "1.2");
        assertThat(response.wizardStage()).isEqualTo(3);
        verify(userSkillRepository).deleteByUserId("user-1");
        verify(userSkillRepository).saveAll(anyList());
        verify(userRepository).save(user);
    }

    @Test
    void saveUserSkills_throwsBadRequest_whenUserIdBlank() {
        SaveUserSkillsRequest request = SaveUserSkillsRequest.builder()
                .userId("  ")
                .catIds(List.of("1.1"))
                .build();

        SkillsException ex = assertThrows(SkillsException.class,
                () -> service.saveUserSkills(request));

        assertThat(ex.getMessage()).contains(SaayamStatusCode.BAD_REQUEST.toString());
        verifyNoInteractions(userRepository, helpCategoryRepository, userSkillRepository);
    }

    @Test
    void saveUserSkills_throwsBadRequest_whenCatIdsEmpty() {
        SaveUserSkillsRequest request = SaveUserSkillsRequest.builder()
                .userId("user-1")
                .catIds(List.of())
                .build();

        SkillsException ex = assertThrows(SkillsException.class,
                () -> service.saveUserSkills(request));

        assertThat(ex.getMessage()).contains(SaayamStatusCode.BAD_REQUEST.toString());
        verifyNoInteractions(userRepository, helpCategoryRepository, userSkillRepository);
    }

    @Test
    void saveUserSkills_throwsBadRequest_whenCatIdsNull() {
        SaveUserSkillsRequest request = SaveUserSkillsRequest.builder()
                .userId("user-1")
                .catIds(null)
                .build();

        SkillsException ex = assertThrows(SkillsException.class,
                () -> service.saveUserSkills(request));

        assertThat(ex.getMessage()).contains(SaayamStatusCode.BAD_REQUEST.toString());
        verifyNoInteractions(userRepository, helpCategoryRepository, userSkillRepository);
    }

    @Test
    void saveUserSkills_throwsUserNotFound_whenUserDoesNotExist() {
        when(userRepository.findById("unknown")).thenReturn(Optional.empty());

        SaveUserSkillsRequest request = SaveUserSkillsRequest.builder()
                .userId("unknown")
                .catIds(List.of("1.1"))
                .build();

        assertThrows(UserNotFoundException.class, () -> service.saveUserSkills(request));
        verifyNoInteractions(helpCategoryRepository, userSkillRepository);
    }

    @Test
    void saveUserSkills_throwsInvalidCategory_whenCatIdDoesNotExist() {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(helpCategoryRepository.findAllById(List.of("9.9")))
                .thenReturn(List.of());

        SaveUserSkillsRequest request = SaveUserSkillsRequest.builder()
                .userId("user-1")
                .catIds(List.of("9.9"))
                .build();

        SkillsException ex = assertThrows(SkillsException.class,
                () -> service.saveUserSkills(request));

        assertThat(ex.getMessage()).contains(SaayamStatusCode.INVALID_SKILL_CATEGORY.toString());
        verifyNoInteractions(userSkillRepository);
    }

    @Test
    void saveUserSkills_replacesExistingSkills() {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(helpCategoryRepository.findAllById(List.of("2.1")))
                .thenReturn(List.of(new HelpCategory("2.1", "TUTORING", "desc")));
        when(userSkillRepository.saveAll(anyList())).thenReturn(List.of());

        SaveUserSkillsRequest request = SaveUserSkillsRequest.builder()
                .userId("user-1")
                .catIds(List.of("2.1"))
                .build();

        service.saveUserSkills(request);

        verify(userSkillRepository).deleteByUserId("user-1");
        verify(userSkillRepository).saveAll(List.of(
                UserSkill.builder().userId("user-1").catId("2.1").build()));
    }
}
