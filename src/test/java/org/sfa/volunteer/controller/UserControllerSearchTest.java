package org.sfa.volunteer.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sfa.volunteer.dto.common.SaayamResponse;
import org.sfa.volunteer.dto.common.SaayamStatusCode;
import org.sfa.volunteer.dto.response.PaginationResponse;
import org.sfa.volunteer.dto.response.UserProfileResponse;
import org.sfa.volunteer.service.ProfileImageStorageService;
import org.sfa.volunteer.service.UserService;
import org.sfa.volunteer.util.ResponseBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerSearchTest {

    private static final String HDR_CALLER_USER_ID = "X-Caller-UserId";

    @Mock
    private UserService userService;

    @Mock
    private ResponseBuilder responseBuilder;

    @Mock
    private ProfileImageStorageService profileImageStorageService;

    @Mock
    private HttpServletRequest httpServletRequest;

    @InjectMocks
    private UserController userController;

    private String adminCallerId;
    private PaginationResponse<UserProfileResponse> pagination;
    private SaayamResponse<PaginationResponse<UserProfileResponse>> wrapped;

    @BeforeEach
    void setUp() {
        adminCallerId = "test-admin-caller-id";
        UserProfileResponse profile = UserProfileResponse.builder()
                .id("user-1")
                .firstName("Test")
                .lastName("User")
                .emailAddress("search-result@example.test")
                .build();
        pagination = PaginationResponse.<UserProfileResponse>builder()
                .currentPage(0)
                .pageSize(10)
                .totalPages(1)
                .totalItems(1)
                .items(List.of(profile))
                .hasNextPage(false)
                .hasPreviousPage(false)
                .build();
        wrapped = SaayamResponse.success(SaayamStatusCode.SUCCESS, "ok", pagination);
    }

    @Test
    void searchUsers_returnsSuccess_whenCallerIsAdmin() {
        when(httpServletRequest.getHeader(HDR_CALLER_USER_ID)).thenReturn(adminCallerId);
        when(userService.userExists(adminCallerId)).thenReturn(true);
        when(userService.isAdminUser(adminCallerId)).thenReturn(true);
        when(userService.searchUsers("alice", 0, 10)).thenReturn(pagination);
        when(responseBuilder.buildSuccessResponse(
                eq(SaayamStatusCode.SUCCESS),
                any(Object[].class),
                eq(pagination)))
                .thenReturn(wrapped);

        SaayamResponse<PaginationResponse<UserProfileResponse>> result =
                userController.searchUsers("alice", 0, 10, httpServletRequest);

        assertThat(result).isSameAs(wrapped);
        verify(userService).searchUsers("alice", 0, 10);

        ArgumentCaptor<Object[]> argsCaptor = ArgumentCaptor.forClass(Object[].class);
        verify(responseBuilder).buildSuccessResponse(
                eq(SaayamStatusCode.SUCCESS),
                argsCaptor.capture(),
                eq(pagination));
        Object[] passed = argsCaptor.getValue();
        assertThat(passed).containsExactly("alice", 0, 10);
    }

    @Test
    void searchUsers_passesNullPaginationParams_toService() {
        when(httpServletRequest.getHeader(HDR_CALLER_USER_ID)).thenReturn(adminCallerId);
        when(userService.userExists(adminCallerId)).thenReturn(true);
        when(userService.isAdminUser(adminCallerId)).thenReturn(true);
        when(userService.searchUsers("bob", null, null)).thenReturn(pagination);
        when(responseBuilder.buildSuccessResponse(
                eq(SaayamStatusCode.SUCCESS),
                any(Object[].class),
                eq(pagination)))
                .thenReturn(wrapped);

        userController.searchUsers("bob", null, null, httpServletRequest);

        verify(userService).searchUsers("bob", null, null);
    }

    @Test
    void searchUsers_throwsForbidden_whenCallerHeaderMissing() {
        when(httpServletRequest.getHeader(HDR_CALLER_USER_ID)).thenReturn(null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> userController.searchUsers("q", 0, 10, httpServletRequest));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(ex.getReason()).isEqualTo("Missing caller identity");
        verify(userService, never()).searchUsers(any(), any(), any());
        verifyNoInteractions(responseBuilder);
    }

    @Test
    void searchUsers_throwsForbidden_whenCallerHeaderBlank() {
        when(httpServletRequest.getHeader(HDR_CALLER_USER_ID)).thenReturn("   ");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> userController.searchUsers("q", 0, 10, httpServletRequest));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(ex.getReason()).isEqualTo("Missing caller identity");
        verify(userService, never()).searchUsers(any(), any(), any());
    }

    @Test
    void searchUsers_throwsForbidden_whenCallerUserDoesNotExist() {
        when(httpServletRequest.getHeader(HDR_CALLER_USER_ID)).thenReturn(adminCallerId);
        when(userService.userExists(adminCallerId)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> userController.searchUsers("q", 0, 10, httpServletRequest));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(ex.getReason()).isEqualTo("User does not exist");
        verify(userService, never()).isAdminUser(any());
        verify(userService, never()).searchUsers(any(), any(), any());
    }

    @Test
    void searchUsers_throwsForbidden_whenCallerIsNotAdmin() {
        when(httpServletRequest.getHeader(HDR_CALLER_USER_ID)).thenReturn(adminCallerId);
        when(userService.userExists(adminCallerId)).thenReturn(true);
        when(userService.isAdminUser(adminCallerId)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> userController.searchUsers("q", 0, 10, httpServletRequest));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(ex.getReason()).isEqualTo("User is not an admin");
        verify(userService, never()).searchUsers(any(), any(), any());
    }
}
