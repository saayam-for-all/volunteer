package org.sfa.volunteer.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sfa.volunteer.dto.response.PaginationResponse;
import org.sfa.volunteer.dto.response.UserProfileResponse;
import org.sfa.volunteer.model.User;
import org.sfa.volunteer.repository.CountryRepository;
import org.sfa.volunteer.repository.OrganizationRepository;
import org.sfa.volunteer.repository.StateRepository;
import org.sfa.volunteer.repository.UserAdditionalDetailRepository;
import org.sfa.volunteer.repository.UserCategoryRepository;
import org.sfa.volunteer.repository.UserRepository;
import org.sfa.volunteer.repository.UserSignOffReasonRepository;
import org.sfa.volunteer.repository.UserStatusRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplSearchUsersTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserStatusRepository userStatusRepository;
    @Mock
    private OrganizationRepository organizationRepository;
    @Mock
    private UserCategoryRepository userCategoryRepository;
    @Mock
    private CountryRepository countryRepository;
    @Mock
    private StateRepository stateRepository;
    @Mock
    private UserSignOffReasonRepository userSignOffReasonRepository;
    @Mock
    private UserAdditionalDetailRepository userAdditionalDetailRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id("entity-user-id")
                .firstName("Sam")
                .lastName("Ple")
                .fullName("Sam Ple")
                .primaryEmailAddress("sam.ple@example.test")
                .timeZone("UTC")
                .build();
    }

    @Test
    void searchUsers_returnsEmptyPage_withoutCallingRepository_whenQueryNull() {
        PaginationResponse<UserProfileResponse> result = userService.searchUsers(null, 0, 10);

        assertThat(result.totalItems()).isZero();
        assertThat(result.items()).isEmpty();
        verify(userRepository, never()).searchUsers(any(), any());
    }

    @Test
    void searchUsers_returnsEmptyPage_withoutCallingRepository_whenQueryBlank() {
        PaginationResponse<UserProfileResponse> result = userService.searchUsers("   ", 0, 10);

        assertThat(result.totalItems()).isZero();
        assertThat(result.items()).isEmpty();
        verify(userRepository, never()).searchUsers(any(), any());
    }

    @Test
    void searchUsers_trimsLowercasesQuery_andUsesDefaultPagination_whenArgsNull() {
        Page<User> page = new PageImpl<>(List.of(sampleUser), PageRequest.of(0, 10), 1);
        when(userRepository.searchUsers(eq("%alice%"), any(Pageable.class)))
                .thenReturn(page);

        PaginationResponse<UserProfileResponse> result = userService.searchUsers("  Alice  ", null, null);

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).id()).isEqualTo("entity-user-id");
        assertThat(result.items().get(0).firstName()).isEqualTo("Sam");
        assertThat(result.currentPage()).isZero();
        assertThat(result.pageSize()).isEqualTo(10);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(userRepository).searchUsers(eq("%alice%"), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isZero();
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(10);
    }

    @Test
    void searchUsers_respectsExplicitPageAndSize() {
        Page<User> page = new PageImpl<>(List.of(), PageRequest.of(2, 20), 0);
        when(userRepository.searchUsers(eq("%bob%"), any(Pageable.class)))
                .thenReturn(page);

        PaginationResponse<UserProfileResponse> result = userService.searchUsers("Bob", 2, 20);

        assertThat(result.items()).isEmpty();
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(userRepository).searchUsers(eq("%bob%"), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(2);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(20);
    }
}
