package com.housing.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity // Marks this class as a database entity
@Table(name = "users") // Maps this class to the users table in the database
@Getter @Setter // Lombok annotations to generate getters and setters
@NoArgsConstructor // Generates a no-argument constructor
@AllArgsConstructor // Generates a constructor with all fields
public class User {

    @Id  // Marks this field as the primary key
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // Auto-increment ID
    private Long id;

    @Column(nullable = false, unique = true)  // Unique & required
    @NonNull
    private String email;

    @Column(nullable = false)
    @NonNull
    private String password;

    @Column(nullable = false)
    @NonNull
    private String firstName;

    @Column(nullable = false)
    @NonNull
    private String lastName;

    @Column(nullable = false, updatable = false)
    @NonNull
    private LocalDateTime createdAt = LocalDateTime.now();  // Timestamp
}