package com.restapi.crud.firstcrud.Repo;

import com.restapi.crud.firstcrud.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface User_repo extends JpaRepository<User, Long> {
}
