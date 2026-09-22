package com.tuckersoft.branchengine.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** Nunca incluye password, ni siquiera codificada. */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class UserDTO {
    private Long id;
    private String email;
    private String displayName;
    private String role;
    private Instant createdAt;
}
