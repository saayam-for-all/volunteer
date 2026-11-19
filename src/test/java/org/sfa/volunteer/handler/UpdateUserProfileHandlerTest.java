package org.sfa.volunteer.handler;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.sfa.volunteer.dto.common.SaayamStatusCode;
import org.sfa.volunteer.exception.WizardProgressMutationException;
import org.sfa.volunteer.service.UserService;
import org.sfa.volunteer.util.MessageSourceUtil;
import org.sfa.volunteer.util.ResponseBuilder;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UpdateUserProfileHandlerTest {
    private static final String USER = "SID-LOCAL";
    private final UserService users = mock(UserService.class);
    private final MessageSourceUtil messages = mock(MessageSourceUtil.class);
    private final UpdateUserProfileHandler handler = new UpdateUserProfileHandler(users, new ResponseBuilder(messages), messages);
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void wizardProgressMutationReturnsActualBadRequest() throws Exception {
        when(users.updateUserProfile(eq(USER), any())).thenThrow(new WizardProgressMutationException());

        var response = handler.handleRequest(request("{\"volunteerStage\":5}"), null);

        assertThat(response.getStatusCode()).isEqualTo(400);
        var body = json.readTree(response.getBody());
        assertThat(body.path("statusCode").asInt()).isEqualTo(400);
        assertThat(body.path("saayamCode").asText()).isEqualTo(SaayamStatusCode.BAD_REQUEST.getCode());
        assertThat(body.path("message").asText()).isEqualTo("Wizard progress is managed by onboarding");
    }

    @Test
    void unrelatedProfileFailuresRetainTheirExistingStatus() throws Exception {
        when(users.updateUserProfile(eq(USER), any())).thenThrow(new IllegalArgumentException("unrelated failure"));
        when(messages.getMessage(SaayamStatusCode.INTERNAL_SERVER_ERROR.getCode(), null)).thenReturn("Internal error");

        var response = handler.handleRequest(request("{\"firstName\":\"Example\"}"), null);

        assertThat(response.getStatusCode()).isEqualTo(500);
        assertThat(json.readTree(response.getBody()).path("message").asText()).isEqualTo("Internal error");
    }

    private static APIGatewayProxyRequestEvent request(String body) {
        return new APIGatewayProxyRequestEvent().withHeaders(Map.of("Accept-Language", "en"))
                .withPathParameters(Map.of("userId", USER)).withBody(body);
    }
}
