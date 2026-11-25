package org.sfa.volunteer.handler;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import org.sfa.volunteer.dto.common.SaayamStatusCode;
import org.sfa.volunteer.service.OrganizationService;
import org.sfa.volunteer.util.MessageSourceUtil;
import org.sfa.volunteer.util.ResponseBuilder;
import org.springframework.http.HttpMethod;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.sfa.volunteer.dto.request.UserOrganizationsRequest;

/** POST /organizations/user  body: { "userId": "..." } */
public class GetUserOrganizationsHandler extends OrganizationHandlerBase {

    public GetUserOrganizationsHandler() {
        super(HttpMethod.POST.name());
    }

    GetUserOrganizationsHandler(OrganizationService organizationService, ResponseBuilder responseBuilder,
                                MessageSourceUtil messageSourceUtil) {
        super(HttpMethod.POST.name(), organizationService, responseBuilder, messageSourceUtil);
    }

    @Override
    protected Result handle(APIGatewayProxyRequestEvent event) throws JsonProcessingException {
        UserOrganizationsRequest request = readBody(event, UserOrganizationsRequest.class);
        return ok(SaayamStatusCode.SUCCESS, new Object[]{ request.userId() },
                organizationService.getOrganizationsByUserId(request.userId()));
    }
}