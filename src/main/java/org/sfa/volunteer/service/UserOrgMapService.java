package org.sfa.volunteer.service;

import org.sfa.volunteer.model.UserOrgMap;

public interface UserOrgMapService {

    UserOrgMap linkUserToOrg(String userId, String orgId);
}