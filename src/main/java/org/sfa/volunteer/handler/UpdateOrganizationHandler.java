package org.sfa.volunteer.handler;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.sfa.volunteer.dto.common.SaayamStatusCode;
import org.sfa.volunteer.dto.request.UpdateOrganizationRequest;
import org.sfa.volunteer.dto.response.OrganizationDetailsResponse;
import org.sfa.volunteer.service.OrganizationService;
import org.sfa.volunteer.util.MessageSourceUtil;
import org.sfa.volunteer.util.ResponseBuilder;
import org.springframework.http.HttpMethod;

/** PUT /organizations/{orgId} */
public class UpdateOrganizationHandler extends OrganizationHandlerBase {

    public UpdateOrganizationHandler() {
        super(HttpMethod.PUT.name());
    }

    UpdateOrganizationHandler(OrganizationService organizationService, ResponseBuilder responseBuilder,
                              MessageSourceUtil messageSourceUtil) {
        super(HttpMethod.PUT.name(), organizationService, responseBuilder, messageSourceUtil);
    }

    @Override
    protected Result handle(APIGatewayProxyRequestEvent event) throws JsonProcessingException {
        String orgId = requirePathParam(event, ORG_ID_PARAM);
        UpdateOrganizationRequest request = readBody(event, UpdateOrganizationRequest.class);
        OrganizationDetailsResponse updated = organizationService.updateOrganization(orgId, request);
        return ok(SaayamStatusCode.ORGANIZATION_UPDATED, new Object[]{ orgId }, updated);
    }
}