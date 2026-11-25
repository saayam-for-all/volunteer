package org.sfa.volunteer.handler;

import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OrganizationRouterHandlerTest {

    private static RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> returning(int status) {
        return (event, context) -> new APIGatewayProxyResponseEvent().withStatusCode(status);
    }

    private final OrganizationRouterHandler router = new OrganizationRouterHandler(Map.of(
            OrganizationRouterHandler.SEARCH, returning(201),
            OrganizationRouterHandler.UPDATE, returning(202),
            OrganizationRouterHandler.LINK, returning(203)));

    @Test
    void dispatchesOnResourceTemplate() {
        assertThat(router.handleRequest(
                new APIGatewayProxyRequestEvent().withResource("/organizations/search"), null)
                .getStatusCode()).isEqualTo(201);
        assertThat(router.handleRequest(
                new APIGatewayProxyRequestEvent().withResource("/organizations/{orgId}")
                        .withPath("/organizations/ORG-1"), null)
                .getStatusCode()).isEqualTo(202);
        assertThat(router.handleRequest(
                new APIGatewayProxyRequestEvent().withResource("/organizations/link"), null)
                .getStatusCode()).isEqualTo(203);
    }

    @Test
    void unknownResourceReturns404() {
        APIGatewayProxyResponseEvent response = router.handleRequest(
                new APIGatewayProxyRequestEvent().withResource("/nope"), null);

        assertThat(response.getStatusCode()).isEqualTo(404);
        assertThat(response.getHeaders()).containsKey("Access-Control-Allow-Origin");
    }

    @Test
    void nullEventReturns404() {
        assertThat(router.handleRequest(null, null).getStatusCode()).isEqualTo(404);
    }
}