package com.tuckersoft.branchengine.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * handlerUnit y outcomeCode no estan aqui: se derivan del branchType en el service.
 *
 * impactLevel es String con @Pattern y no un enum: asi un valor fuera de la lista
 * sale como un 400 de validacion normal, sin depender de traducir el error de Jackson.
 */
@NoArgsConstructor
@Getter
@Setter
public class DecisionRequest {

    @NotNull(message = "el playthroughId es obligatorio")
    private Long playthroughId;

    @NotBlank(message = "el rawInput es obligatorio")
    @Size(min = 10, message = "debe tener al menos 10 caracteres")
    private String rawInput;

    @NotBlank(message = "el impactLevel es obligatorio")
    @Pattern(regexp = "LEVE|MODERADO|GRAVE|CRITICO",
            message = "debe ser LEVE, MODERADO, GRAVE o CRITICO")
    private String impactLevel;
}
