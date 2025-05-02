package com.housing.backend.controller;

import org.apache.catalina.connector.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.housing.backend.dto.UserCreateDTO;
import com.housing.backend.dto.UserDTO;
import com.housing.backend.dto.UserLoginDTO;
import com.housing.backend.service.UserService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // Fetch all users
    @GetMapping
    public List<UserDTO> getAllUsers() {
        return userService.getAllUsers();
    }

    // Fetch a user by ID
    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        UserDTO user = userService.getUserById(id);
        return (user != null) ? ResponseEntity.ok(user) : ResponseEntity.notFound().build();
    }

    // Register a new user (Using UserDTO instead)
    // @RequestBody converts JSON to Java object which in this case is UserCreateDTO
    @PostMapping
    public ResponseEntity<String> createUser(@RequestBody UserCreateDTO UserCreateDTO) {
        boolean created = userService.createUser(UserCreateDTO);
        return created ? ResponseEntity.ok("User registered successfully") : ResponseEntity.badRequest().body("Email already exists");
    }

    // User login (Using UserDTO instead)
    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody UserLoginDTO userLoginDTO) {
        String token = userService.validateUserLogin(userLoginDTO);

        if (token != null) {
            Map<String, String> response = new HashMap<>();
            response.put("token", token);
            return ResponseEntity.ok().body(response);
        }
        return ResponseEntity.status(401).body("Invalid email or password");
    }
}