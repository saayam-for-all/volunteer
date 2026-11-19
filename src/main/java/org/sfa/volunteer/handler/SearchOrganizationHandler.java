package org.sfa.volunteer.handler;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import org.sfa.volunteer.dto.common.SaayamStatusCode;
import org.sfa.volunteer.dto.response.OrganizationResponse;
import org.sfa.volunteer.service.OrganizationService;
import org.sfa.volunteer.util.MessageSourceUtil;
import org.sfa.volunteer.util.ResponseBuilder;
import org.springframework.http.HttpMethod;

import java.util.List;

/** GET /organizations/search?name=... */
public class SearchOrganizationHandler extends OrganizationHandlerBase {

    private static final String NAME_PARAM = "name";

    public SearchOrganizationHandler() {
        super(HttpMethod.GET.name());
    }

    SearchOrganizationHandler(OrganizationService organizationService, ResponseBuilder responseBuilder,
                              MessageSourceUtil messageSourceUtil) {
        super(HttpMethod.GET.name(), organizationService, responseBuilder, messageSourceUtil);
    }

    @Override
    protected Result handle(APIGatewayProxyRequestEvent event) {
        String name = requireQueryParam(event, NAME_PARAM);
        List<OrganizationResponse> results = organizationService.searchByName(name);
        return ok(SaayamStatusCode.SUCCESS, new Object[]{ name }, results);
    }
}
