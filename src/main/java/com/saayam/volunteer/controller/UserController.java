package com.saayam.volunteer.controller;
import com.saayam.volunteer.entities.User;
import com.saayam.volunteer.service.impl.UserServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/volunteer")
public class UserController {

    @Autowired
    private UserServiceImpl userService;

    @PostMapping
    public User saveUser(@RequestBody User user){
        return userService.saveUser(user);
    }

    @GetMapping
    public List<User> getAllUsers(){
        return userService.findAllUsers();
    }

    @GetMapping("/{id}")
    public Optional<User> getUser(@PathVariable("id") BigInteger id){
        return userService.findById(id);
    }

    @PutMapping
    public User updateUser(@RequestBody User user){
        return userService.updateUser(user);
    }

    @DeleteMapping
    public void deleteUser(@RequestParam BigInteger id){
        userService.deleteUser(id);
    }

}
