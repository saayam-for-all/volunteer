package com.restapi.crud.firstcrud.controller;

import com.restapi.crud.firstcrud.Model.User;
import com.restapi.crud.firstcrud.Repo.User_repo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class Apicontroller {

    @Autowired
    private User_repo urepo;

    @GetMapping(value = "/")
    public String getpage(){
        return "Hello";
    }


    public List<User> getUsers(){
        return urepo.findAll();
    }
}
