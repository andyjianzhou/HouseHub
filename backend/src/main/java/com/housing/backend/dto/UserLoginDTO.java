package com.housing.backend.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class UserLoginDTO {
    private String email;
    private String password;
}
