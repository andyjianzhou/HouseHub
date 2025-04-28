package com.housing.backend.service;

import com.housing.backend.dto.UserCreateDTO;
import com.housing.backend.dto.UserDTO;
import com.housing.backend.dto.UserLoginDTO;
import com.housing.backend.model.User;
import com.housing.backend.repository.UserRepository;
import com.housing.backend.security.JwtUtil;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service // Marks this class as a service component, meaning it holds business logic
// and can be injected into other components.
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public UserService(UserRepository userRepository, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();  // Encrypts passwords
        this.jwtUtil = jwtUtil;
    }

    // Fetch all users and return them as DTOs
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(user -> convertToDTO(user))
                .collect(Collectors.toList());
    }

    // Fetch a user by ID
    public UserDTO getUserById(Long id) {
        return userRepository.findById(id).map(user -> convertToDTO(user)).orElse(null);
    }

    // Register a new user
    public boolean createUser(UserCreateDTO UserCreateDTO) {
        // Check if email already exists
        if (userRepository.existsByEmail(UserCreateDTO.getEmail())) {
            return false;  // Email already registered
        }

        // Create a new user object
        User newUser = new User();
        newUser.setEmail(UserCreateDTO.getEmail());
        newUser.setFirstName(UserCreateDTO.getFirstName());
        newUser.setLastName(UserCreateDTO.getLastName());
        newUser.setPassword(passwordEncoder.encode(UserCreateDTO.getPassword())); // Encrypt password

        // Save user in database
        userRepository.save(newUser);
        return true;
    }

    // Validate user login
    public String validateUserLogin(UserLoginDTO userLoginDTO) {
        return userRepository.findByEmail(userLoginDTO.getEmail())
                .filter(user -> passwordEncoder.matches(userLoginDTO.getPassword(), user.getPassword()))
                .map(user -> jwtUtil.generateToken(user.getEmail())) // Return JWT on success
                .orElse(null);
    }

    public boolean updateUser(Long id, UserDTO userDTO) {
        return userRepository.findById(id).map(existingUser -> {
            // Update only the fields that are not null
            if (userDTO.getEmail() != null) existingUser.setEmail(userDTO.getEmail());
            if (userDTO.getFirstName() != null) existingUser.setFirstName(userDTO.getFirstName());
            if (userDTO.getLastName() != null) existingUser.setLastName(userDTO.getLastName());
            // Save updated user
            userRepository.save(existingUser);
            return true;
        }).orElse(false); // If user is not found, return false
    }

    public boolean changePassword (Long id, String newPassword) {
        return userRepository.findById(id).map(existingUser -> {
            existingUser.setPassword(passwordEncoder.encode(newPassword)); // Encrypt new password
            userRepository.save(existingUser);
            return true;
        }).orElse(false); // If user is not found, return false
    }

    // Convert User entity to UserDTO
    private UserDTO convertToDTO(User user) {
        return new UserDTO(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName()
        );
    }
}