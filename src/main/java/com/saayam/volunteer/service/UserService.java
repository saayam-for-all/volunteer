package com.saayam.volunteer.service;
import com.saayam.volunteer.entities.User;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;


public interface UserService {

    List<User> findAllUsers();
    Optional<User> findById(BigInteger id);
    User saveUser(User newUser);
    User updateUser(User user);
    void deleteUser(BigInteger id);

}
