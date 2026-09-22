package com.tuckersoft.branchengine.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** currentBranches no esta aqui: lo fija el service en 0. */
@NoArgsConstructor
@Getter
@Setter
public class StoryNodeRequest {

    @NotBlank(message = "el nodeCode es obligatorio")
    @Size(min = 3, max = 40, message = "debe tener entre 3 y 40 caracteres")
    private String nodeCode;

    @NotBlank(message = "el titulo es obligatorio")
    @Size(min = 3, max = 80, message = "debe tener entre 3 y 80 caracteres")
    private String title;

    @NotBlank(message = "el sceneText es obligatorio")
    @Size(min = 10, message = "debe tener al menos 10 caracteres")
    private String sceneText;

    @NotNull(message = "la capacidad es obligatoria")
    @Min(value = 1, message = "debe ser mayor a 0")
    private Integer branchCapacity;

    private String primaryBranchCode;

    private String glitchBranchCode;
}
