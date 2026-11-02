package org.sfa.volunteer.handler;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sfa.volunteer.dto.common.SaayamStatusCode;
import org.sfa.volunteer.dto.response.OrganizationDetailsResponse;
import org.sfa.volunteer.dto.response.OrganizationResponse;
import org.sfa.volunteer.exception.OrganizationNotFoundException;
import org.sfa.volunteer.service.OrganizationService;
import org.sfa.volunteer.util.MessageSourceUtil;
import org.sfa.volunteer.util.ResponseBuilder;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationHandlersTest {

    private static final String GET = "GET";
    private static final String POST = "POST";
    private static final String PUT = "PUT";
    private static final String OPTIONS = "OPTIONS";
    private static final String ORG_ID = "ORG-1";
    private static final String USER_ID = "USER-1";
    private static final String ORG_ID_PARAM = "orgId";
    private static final String USER_ID_PARAM = "userId";
    private static final String NAME_PARAM = "name";
    private static final String SEARCH_TERM = "red";
    private static final String NOT_JSON = "{not json";

    @Mock
    private OrganizationService organizationService;

    @Mock
    private ResponseBuilder responseBuilder;

    @Mock
    private MessageSourceUtil messageSourceUtil;

    private SearchOrganizationHandler search() {
        return new SearchOrganizationHandler(organizationService, responseBuilder, messageSourceUtil);
    }

    private CreateOrganizationHandler create() {
        return new CreateOrganizationHandler(organizationService, responseBuilder, messageSourceUtil);
    }

    private UpdateOrganizationHandler update() {
        return new UpdateOrganizationHandler(organizationService, responseBuilder, messageSourceUtil);
    }

    private LinkOrganizationHandler link() {
        return new LinkOrganizationHandler(organizationService, responseBuilder, messageSourceUtil);
    }

    private GetUserOrganizationsHandler userOrganizations() {
        return new GetUserOrganizationsHandler(organizationService, responseBuilder, messageSourceUtil);
    }

    private static APIGatewayProxyRequestEvent event(String method) {
        return new APIGatewayProxyRequestEvent().withHttpMethod(method);
    }

    @Test
    void options_returns204WithCorsHeaders() {
        APIGatewayProxyResponseEvent response = search().handleRequest(event(OPTIONS), null);

        assertThat(response.getStatusCode()).isEqualTo(204);
        assertThat(response.getHeaders()).containsKey("Access-Control-Allow-Origin");
        verifyNoInteractions(organizationService);
    }

    @Test
    void search_returns200() {
        List<OrganizationResponse> results = List.of();
        when(organizationService.searchByName(SEARCH_TERM)).thenReturn(results);

        APIGatewayProxyResponseEvent response = search().handleRequest(
                event(GET).withQueryStringParameters(Map.of(NAME_PARAM, SEARCH_TERM)), null);

        assertThat(response.getStatusCode()).isEqualTo(200);
        verify(responseBuilder).buildSuccessResponse(
                eq(SaayamStatusCode.SUCCESS), any(Object[].class), eq(results));
    }

    @Test
    void search_missingName_returns400() {
        APIGatewayProxyResponseEvent response = search().handleRequest(event(GET), null);

        assertThat(response.getStatusCode()).isEqualTo(400);
        verify(responseBuilder).buildErrorResponse(
                eq(400), eq(SaayamStatusCode.BAD_REQUEST), eq("name is required"));
        verifyNoInteractions(organizationService);
    }

    @Test
    void search_unexpectedError_returns500() {
        when(organizationService.searchByName(SEARCH_TERM)).thenThrow(new RuntimeException("boom"));

        APIGatewayProxyResponseEvent response = search().handleRequest(
                event(GET).withQueryStringParameters(Map.of(NAME_PARAM, SEARCH_TERM)), null);

        assertThat(response.getStatusCode()).isEqualTo(500);
        verify(responseBuilder).buildErrorResponse(
                eq(500), eq(SaayamStatusCode.INTERNAL_SERVER_ERROR), any());
    }

    @Test
    void link_returns200_andPassesUserIdFirst() {
        APIGatewayProxyResponseEvent response = link().handleRequest(
                event(POST).withPathParameters(Map.of(ORG_ID_PARAM, ORG_ID, USER_ID_PARAM, USER_ID)), null);

        assertThat(response.getStatusCode()).isEqualTo(200);
        verify(organizationService).linkOrganization(USER_ID, ORG_ID);
    }

    @Test
    void link_unknownOrg_returns404() {
        doThrow(new OrganizationNotFoundException(ORG_ID))
                .when(organizationService).linkOrganization(USER_ID, ORG_ID);

        APIGatewayProxyResponseEvent response = link().handleRequest(
                event(POST).withPathParameters(Map.of(ORG_ID_PARAM, ORG_ID, USER_ID_PARAM, USER_ID)), null);

        assertThat(response.getStatusCode()).isEqualTo(404);
        verify(responseBuilder).buildErrorResponse(
                eq(404), eq(SaayamStatusCode.ORGANIZATION_NOT_FOUND), any());
    }

    @Test
    void link_missingUserId_returns400() {
        APIGatewayProxyResponseEvent response = link().handleRequest(
                event(POST).withPathParameters(Map.of(ORG_ID_PARAM, ORG_ID)), null);

        assertThat(response.getStatusCode()).isEqualTo(400);
        verify(responseBuilder).buildErrorResponse(
                eq(400), eq(SaayamStatusCode.BAD_REQUEST), eq("userId is required"));
        verifyNoInteractions(organizationService);
    }

    @Test
    void userOrganizations_returns200() {
        List<OrganizationDetailsResponse> organizations = List.of();
        when(organizationService.getOrganizationsByUserId(USER_ID)).thenReturn(organizations);

        APIGatewayProxyResponseEvent response = userOrganizations().handleRequest(
                event(GET).withPathParameters(Map.of(USER_ID_PARAM, USER_ID)), null);

        assertThat(response.getStatusCode()).isEqualTo(200);
        verify(responseBuilder).buildSuccessResponse(
                eq(SaayamStatusCode.SUCCESS), any(Object[].class), eq(organizations));
    }

    @Test
    void create_invalidJson_returns400() {
        APIGatewayProxyResponseEvent response = create().handleRequest(
                event(POST).withBody(NOT_JSON), null);

        assertThat(response.getStatusCode()).isEqualTo(400);
        verify(responseBuilder).buildErrorResponse(
                eq(400), eq(SaayamStatusCode.BAD_REQUEST), eq("Invalid request body"));
        verifyNoInteractions(organizationService);
    }

    @Test
    void create_missingBody_returns400() {
        APIGatewayProxyResponseEvent response = create().handleRequest(event(POST), null);

        assertThat(response.getStatusCode()).isEqualTo(400);
        verify(responseBuilder).buildErrorResponse(
                eq(400), eq(SaayamStatusCode.BAD_REQUEST), eq("Request body is required"));
    }

    @Test
    void update_missingOrgId_returns400() {
        APIGatewayProxyResponseEvent response = update().handleRequest(event(PUT), null);

        assertThat(response.getStatusCode()).isEqualTo(400);
        verify(responseBuilder).buildErrorResponse(
                eq(400), eq(SaayamStatusCode.BAD_REQUEST), eq("orgId is required"));
        verifyNoInteractions(organizationService);
    }
}