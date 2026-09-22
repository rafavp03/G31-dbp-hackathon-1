package com.tuckersoft.branchengine.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Sin @Email a proposito: un email mal formado en el login es un problema de
 * credenciales (401), no de validacion (400).
 */
@NoArgsConstructor
@Getter
@Setter
public class LoginRequest {

    @NotBlank(message = "el email es obligatorio")
    private String email;

    @NotBlank(message = "la clave es obligatoria")
    private String password;
}
