package org.sfa.volunteer.service.impl;

import org.sfa.volunteer.dto.common.SaayamStatusCode;
import org.sfa.volunteer.dto.request.SaveUserSkillsRequest;
import org.sfa.volunteer.dto.response.SaveUserSkillsResponse;
import org.sfa.volunteer.exception.SkillsException;
import org.sfa.volunteer.exception.UserNotFoundException;
import org.sfa.volunteer.model.User;
import org.sfa.volunteer.model.UserSkill;
import org.sfa.volunteer.repository.HelpCategoryRepository;
import org.sfa.volunteer.repository.UserRepository;
import org.sfa.volunteer.repository.UserSkillRepository;
import org.sfa.volunteer.service.SkillsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class SkillsServiceImpl implements SkillsService {

    private static final int SKILLS_WIZARD_STAGE = 3;

    private final UserRepository userRepository;
    private final HelpCategoryRepository helpCategoryRepository;
    private final UserSkillRepository userSkillRepository;

    public SkillsServiceImpl(UserRepository userRepository,
                             HelpCategoryRepository helpCategoryRepository,
                             UserSkillRepository userSkillRepository) {
        this.userRepository = userRepository;
        this.helpCategoryRepository = helpCategoryRepository;
        this.userSkillRepository = userSkillRepository;
    }

    @Override
    public SaveUserSkillsResponse saveUserSkills(SaveUserSkillsRequest request) {
        String userId = request.userId();
        List<String> catIds = request.catIds();

        if (userId == null || userId.isBlank()) {
            throw SkillsException.badRequest(userId);
        }
        if (catIds == null || catIds.isEmpty()) {
            throw SkillsException.badRequest(userId);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // Validate all submitted cat_ids exist in help_categories
        Set<String> existingCatIds = helpCategoryRepository.findAllById(catIds)
                .stream()
                .map(c -> c.getCatId())
                .collect(Collectors.toSet());

        List<String> invalidIds = catIds.stream()
                .filter(id -> !existingCatIds.contains(id))
                .toList();

        if (!invalidIds.isEmpty()) {
            throw new SkillsException(
                    SaayamStatusCode.INVALID_SKILL_CATEGORY.toString(), userId);
        }

        // Full replace: delete existing, insert new
        userSkillRepository.deleteByUserId(userId);

        List<UserSkill> skills = catIds.stream()
                .map(catId -> UserSkill.builder()
                        .userId(userId)
                        .catId(catId)
                        .build())
                .toList();

        userSkillRepository.saveAll(skills);

        // Advance wizard stage to 3
        user.setVolunteerStage(SKILLS_WIZARD_STAGE);
        user.setVolunteerUpdateDate(ZonedDateTime.now(ZoneId.of("UTC")));
        userRepository.save(user);

        return SaveUserSkillsResponse.builder()
                .userId(userId)
                .savedCatIds(catIds)
                .wizardStage(SKILLS_WIZARD_STAGE)
                .build();
    }
}
