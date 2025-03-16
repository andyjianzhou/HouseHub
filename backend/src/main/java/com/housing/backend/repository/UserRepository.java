package com.housing.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.housing.backend.model.User;

import java.util.Optional;

// so spring treats this kind of like ORM

@Repository // Marks this as a Spring-managed component
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}