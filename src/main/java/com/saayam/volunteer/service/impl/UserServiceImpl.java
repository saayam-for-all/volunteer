package com.saayam.volunteer.service.impl;

import com.saayam.volunteer.entities.User;
import com.saayam.volunteer.repository.UserRepository;
import com.saayam.volunteer.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public Optional<User> findById(BigInteger id) {
        return userRepository.findById(id);
    }

    @Override
    public User saveUser(User newUser) {
        return userRepository.save(newUser);
    }

    @Override
    public User updateUser(User user) {
        return userRepository.save(user);
    }

    @Override
    public void deleteUser(BigInteger id) {
        userRepository.deleteById(id);
    }
}
