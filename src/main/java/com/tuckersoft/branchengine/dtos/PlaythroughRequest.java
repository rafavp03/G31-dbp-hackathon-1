package com.tuckersoft.branchengine.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** No hay campo de usuario: el dueno sale del token. */
@NoArgsConstructor
@Getter
@Setter
public class PlaythroughRequest {

    @NotBlank(message = "el playerTag es obligatorio")
    @Size(min = 2, max = 40, message = "debe tener entre 2 y 40 caracteres")
    private String playerTag;

    @NotBlank(message = "el startNodeCode es obligatorio")
    private String startNodeCode;
}
