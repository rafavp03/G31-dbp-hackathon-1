package com.tuckersoft.branchengine.services;

import com.tuckersoft.branchengine.dtos.AuthResponse;
import com.tuckersoft.branchengine.dtos.LoginRequest;
import com.tuckersoft.branchengine.dtos.RegisterRequest;
import com.tuckersoft.branchengine.exceptions.ConflictException;
import com.tuckersoft.branchengine.models.Roles;
import com.tuckersoft.branchengine.models.User;
import com.tuckersoft.branchengine.repositories.UserRepository;
import com.tuckersoft.branchengine.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("El email " + request.getEmail() + " ya esta registrado");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setDisplayName(request.getDisplayName());
        // El rol se fija aqui, nunca se copia del request: evita la escalada de privilegios.
        user.setRole(Roles.USER);
        user.setCreatedAt(Instant.now());

        return toAuthResponse(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        // Si el email no existe o la clave no coincide, sale BadCredentialsException:
        // los dos casos son 401 y no se revela cual de los dos fallo.
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Credenciales invalidas"));

        return toAuthResponse(user);
    }

    private AuthResponse toAuthResponse(User user) {
        return new AuthResponse(
                jwtService.generateToken(user.getEmail()),
                "Bearer",
                user.getEmail(),
                user.getDisplayName(),
                user.getRole());
    }
}
