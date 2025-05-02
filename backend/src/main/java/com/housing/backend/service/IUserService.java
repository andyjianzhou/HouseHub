package com.housing.backend.service;

import java.util.List;

import com.housing.backend.dto.UserCreateDTO;
import com.housing.backend.dto.UserDTO;
import com.housing.backend.dto.UserLoginDTO;

public interface IUserService {

    // Fetch all users and return them as DTOs
    List<UserDTO> getAllUsers();
    // Fetch a user by ID
    public UserDTO getUserById(Long id);
    // Register a new user
    boolean createUser(UserCreateDTO UserCreateDTO);
    // Validate user login
    String validateUserLogin(UserLoginDTO userLoginDTO);
    boolean updateUser(Long id, UserDTO userDTO);
    boolean changePassword (Long id, String newPassword);
}
