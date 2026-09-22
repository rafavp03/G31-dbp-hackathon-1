package com.tuckersoft.branchengine.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * No existe un campo 'role' a proposito: si el JSON lo trae, Jackson lo descarta.
 * El rol se fija en el service, nunca se copia del request.
 */
@NoArgsConstructor
@Getter
@Setter
public class RegisterRequest {

    @NotBlank(message = "el email es obligatorio")
    @Email(message = "debe tener formato de email")
    private String email;

    @NotBlank(message = "la clave es obligatoria")
    @Size(min = 6, message = "debe tener al menos 6 caracteres")
    private String password;

    @NotBlank(message = "el nombre es obligatorio")
    @Size(min = 3, max = 60, message = "debe tener entre 3 y 60 caracteres")
    private String displayName;
}
