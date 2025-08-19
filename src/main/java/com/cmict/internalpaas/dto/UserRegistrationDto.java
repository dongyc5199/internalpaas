package com.cmict.internalpaas.dto;

import lombok.Data;

@Data // Lombok注解，自动生成Getter, Setter, toString等方法
public class UserRegistrationDto {
    private String username;
    private String password;
}
