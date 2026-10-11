package org.sfa.volunteer.handler;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.sfa.volunteer.dto.common.SaayamStatusCode;
import org.sfa.volunteer.dto.request.CreateOrganizationRequest;
import org.sfa.volunteer.dto.response.OrganizationResponse;
import org.sfa.volunteer.service.OrganizationService;
import org.sfa.volunteer.util.MessageSourceUtil;
import org.sfa.volunteer.util.ResponseBuilder;
import org.springframework.http.HttpMethod;

/** POST /organizations */
public class CreateOrganizationHandler extends OrganizationHandlerBase {

    public CreateOrganizationHandler() {
        super(HttpMethod.POST.name());
    }

    CreateOrganizationHandler(OrganizationService organizationService, ResponseBuilder responseBuilder,
                              MessageSourceUtil messageSourceUtil) {
        super(HttpMethod.POST.name(), organizationService, responseBuilder, messageSourceUtil);
    }

    @Override
    protected Result handle(APIGatewayProxyRequestEvent event) throws JsonProcessingException {
        CreateOrganizationRequest request = readBody(event, CreateOrganizationRequest.class);
        OrganizationResponse organization = organizationService.createOrganization(request);
        return created(SaayamStatusCode.ORGANIZATION_CREATED, NO_ARGS, organization);
    }
}
