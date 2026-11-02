package org.sfa.volunteer.handler;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import org.sfa.volunteer.dto.common.SaayamStatusCode;
import org.sfa.volunteer.service.OrganizationService;
import org.sfa.volunteer.util.MessageSourceUtil;
import org.sfa.volunteer.util.ResponseBuilder;
import org.springframework.http.HttpMethod;

/** POST /organizations/{orgId}/users/{userId} */
public class LinkOrganizationHandler extends OrganizationHandlerBase {

    public LinkOrganizationHandler() {
        super(HttpMethod.POST.name());
    }

    LinkOrganizationHandler(OrganizationService organizationService, ResponseBuilder responseBuilder,
                            MessageSourceUtil messageSourceUtil) {
        super(HttpMethod.POST.name(), organizationService, responseBuilder, messageSourceUtil);
    }

    @Override
    protected Result handle(APIGatewayProxyRequestEvent event) {
        String orgId = requirePathParam(event, ORG_ID_PARAM);
        String userId = requirePathParam(event, USER_ID_PARAM);
        organizationService.linkOrganization(userId, orgId);
        return ok(SaayamStatusCode.SUCCESS, new Object[]{ orgId }, null);
    }
}
