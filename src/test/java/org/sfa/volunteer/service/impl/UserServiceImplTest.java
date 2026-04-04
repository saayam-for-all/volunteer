package org.sfa.volunteer.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sfa.volunteer.repository.UserAdditionalDetailRepository;
import org.sfa.volunteer.repository.UserCategoryRepository;
import org.sfa.volunteer.repository.UserRepository;


class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserCategoryRepository userCategoryRepository;

    @Mock
    private UserAdditionalDetailRepository userAdditionalDetailRepository;

    @InjectMocks
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }
}