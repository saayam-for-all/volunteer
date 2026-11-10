package org.sfa.volunteer.service.impl;

import java.util.List;

import org.sfa.volunteer.model.UserOrgMap;
import org.sfa.volunteer.repository.UserOrgMapRepository;
import org.sfa.volunteer.service.UserOrgMapService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserOrgMapServiceImpl implements UserOrgMapService {

    private final UserOrgMapRepository userOrgMapRepository;

    @Autowired
    public UserOrgMapServiceImpl(UserOrgMapRepository userOrgMapRepository) {
        this.userOrgMapRepository = userOrgMapRepository;
    }

    @Override
    public UserOrgMap linkUserToOrg(String userId, String orgId) {
        // A volunteer can belong to multiple organizations, so we create a new
        // membership. The composite key (user_id, org_id) prevents exact duplicates.
        UserOrgMap userOrgMap = UserOrgMap.builder()
                .userId(userId)
                .orgId(orgId)
                .userRole("VOLUNTEER")
                .build();

        return userOrgMapRepository.save(userOrgMap);
    }
}