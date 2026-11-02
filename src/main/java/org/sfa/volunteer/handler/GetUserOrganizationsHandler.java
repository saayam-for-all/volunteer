package org.sfa.volunteer.handler;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import org.sfa.volunteer.dto.common.SaayamStatusCode;
import org.sfa.volunteer.service.OrganizationService;
import org.sfa.volunteer.util.MessageSourceUtil;
import org.sfa.volunteer.util.ResponseBuilder;
import org.springframework.http.HttpMethod;

/** GET /organizations/users/{userId} */
public class GetUserOrganizationsHandler extends OrganizationHandlerBase {

    public GetUserOrganizationsHandler() {
        super(HttpMethod.GET.name());
    }

    GetUserOrganizationsHandler(OrganizationService organizationService, ResponseBuilder responseBuilder,
                                MessageSourceUtil messageSourceUtil) {
        super(HttpMethod.GET.name(), organizationService, responseBuilder, messageSourceUtil);
    }

    @Override
    protected Result handle(APIGatewayProxyRequestEvent event) {
        String userId = requirePathParam(event, USER_ID_PARAM);
        return ok(SaayamStatusCode.SUCCESS, new Object[]{ userId },
                organizationService.getOrganizationsByUserId(userId));
    }
}