package com.bakery.autoBackery.dto;


import com.bakery.autoBackery.model.Role;
import lombok.Data;

@Data
public class RegisterRequest {
    private String username;
    private String email;
    private String password;
    private Role role;
}
