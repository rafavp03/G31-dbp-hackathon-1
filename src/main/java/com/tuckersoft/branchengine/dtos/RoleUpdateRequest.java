package com.tuckersoft.branchengine.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class RoleUpdateRequest {

    @NotBlank(message = "el rol es obligatorio")
    private String role;
}
