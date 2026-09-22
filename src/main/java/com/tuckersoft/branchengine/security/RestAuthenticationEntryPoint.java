package com.tuckersoft.branchengine.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuckersoft.branchengine.dtos.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

/**
 * Spring Security devuelve los 401 con cuerpo vacio: este componente les pone el
 * formato de error del enunciado.
 *
 * El ObjectMapper se inyecta (el de Spring Boot, que ya sabe serializar Instant)
 * en lugar de construir uno nuevo.
 */
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiError cuerpo = new ApiError(
                "UNAUTHORIZED",
                "Se requiere un token valido para acceder a este recurso",
                Instant.now(),
                request.getRequestURI());

        objectMapper.writeValue(response.getWriter(), cuerpo);
    }
}
