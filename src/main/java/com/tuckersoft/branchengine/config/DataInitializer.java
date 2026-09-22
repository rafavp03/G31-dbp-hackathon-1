package com.tuckersoft.branchengine.config;

import com.tuckersoft.branchengine.models.Roles;
import com.tuckersoft.branchengine.models.User;
import com.tuckersoft.branchengine.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Crea al administrador al arrancar, leyendo ADMIN_NAME, ADMIN_EMAIL y ADMIN_PASSWORD
 * del .env. La contrasena se guarda codificada con BCrypt.
 *
 * Si ya existe un usuario con ese email no hace nada: no lo pisa ni le corrige el rol.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.display-name}")
    private String adminDisplayName;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        if (userRepository.existsByEmail(adminEmail)) {
            log.info("El administrador {} ya existe, no se toca", adminEmail);
            return;
        }

        User admin = new User();
        admin.setEmail(adminEmail);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setDisplayName(adminDisplayName);
        admin.setRole(Roles.ADMIN);
        admin.setCreatedAt(Instant.now());

        userRepository.save(admin);
        log.info("Administrador creado: {}", adminEmail);
    }
}
