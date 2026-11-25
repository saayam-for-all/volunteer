package org.sfa.volunteer.handler;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import org.sfa.volunteer.dto.common.SaayamStatusCode;
import org.sfa.volunteer.service.OrganizationService;
import org.sfa.volunteer.util.MessageSourceUtil;
import org.sfa.volunteer.util.ResponseBuilder;
import org.springframework.http.HttpMethod;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.sfa.volunteer.dto.request.LinkOrganizationRequest;

/** POST /organizations/link  body: { "userId": "...", "orgId": "..." } */
public class LinkOrganizationHandler extends OrganizationHandlerBase {

    public LinkOrganizationHandler() {
        super(HttpMethod.POST.name());
    }

    LinkOrganizationHandler(OrganizationService organizationService, ResponseBuilder responseBuilder,
                            MessageSourceUtil messageSourceUtil) {
        super(HttpMethod.POST.name(), organizationService, responseBuilder, messageSourceUtil);
    }

    @Override
    protected Result handle(APIGatewayProxyRequestEvent event) throws JsonProcessingException {
        LinkOrganizationRequest request = readBody(event, LinkOrganizationRequest.class);
        organizationService.linkOrganization(request.userId(), request.orgId());
        return ok(SaayamStatusCode.SUCCESS, new Object[]{ request.orgId() }, null);
    }
}
