package org.sfa.volunteer.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import org.sfa.volunteer.util.Cors;
import java.util.Map;


/**
 * Single Lambda entry point for the organization API. API Gateway routes every
 * /organizations path here; this dispatches on the matched resource to the
 * existing handler classes, which share one Spring context.
 */
public class OrganizationRouterHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    static final String SEARCH = "/organizations/search";
    static final String CREATE = "/organizations";
    static final String UPDATE = "/organizations/{orgId}";
    static final String LINK = "/organizations/link";
    static final String USER_ORGS = "/organizations/user";

    private static final String NOT_FOUND_BODY = "{\"message\":\"Unknown route\"}";

    private final Map<String, RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent>> routes;

    public OrganizationRouterHandler() {
        this(Map.of(
                SEARCH, new SearchOrganizationHandler(),
                CREATE, new CreateOrganizationHandler(),
                UPDATE, new UpdateOrganizationHandler(),
                LINK, new LinkOrganizationHandler(),
                USER_ORGS, new GetUserOrganizationsHandler()));
    }

    OrganizationRouterHandler(
            Map<String, RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent>> routes) {
        this.routes = routes;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> target =
                event == null ? null : routes.get(event.getResource());
        if (target == null) {
            return new APIGatewayProxyResponseEvent()
                .withStatusCode(404)
                .withHeaders(Cors.headers("OPTIONS"))
                .withBody(NOT_FOUND_BODY);
        }
        return target.handleRequest(event, context);
    }
}