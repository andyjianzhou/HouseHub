package com.housing.backend.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class UserCreateDTO {
    private String email;
    private String password;
    private String firstName;
    private String lastName;

}