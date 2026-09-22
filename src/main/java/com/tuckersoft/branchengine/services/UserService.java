package com.tuckersoft.branchengine.services;

import com.tuckersoft.branchengine.dtos.RoleUpdateRequest;
import com.tuckersoft.branchengine.dtos.UserDTO;
import com.tuckersoft.branchengine.exceptions.BadRequestException;
import com.tuckersoft.branchengine.exceptions.ResourceNotFoundException;
import com.tuckersoft.branchengine.models.Roles;
import com.tuckersoft.branchengine.models.User;
import com.tuckersoft.branchengine.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /**
     * El usuario del token. El resto de services lo usa para resolver la propiedad
     * de partidas y decisiones, que nunca sale del request body.
     */
    @Transactional(readOnly = true)
    public User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new ResourceNotFoundException("No hay un usuario autenticado");
        }
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario autenticado no encontrado"));
    }

    @Transactional(readOnly = true)
    public UserDTO getCurrentUser() {
        return toDTO(getAuthenticatedUser());
    }

    @Transactional(readOnly = true)
    public List<UserDTO> getAll() {
        return userRepository.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional
    public UserDTO updateRole(Long id, RoleUpdateRequest request) {
        if (!Roles.isValid(request.getRole())) {
            throw new BadRequestException("El rol debe ser ROLE_USER o ROLE_ADMIN");
        }

        User objetivo = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario " + id + " no encontrado"));

        // Nadie se degrada a si mismo: si no, la base se puede quedar sin administradores.
        if (objetivo.getId().equals(getAuthenticatedUser().getId())) {
            throw new BadRequestException("Un administrador no puede cambiar su propio rol");
        }

        objetivo.setRole(request.getRole());
        return toDTO(userRepository.save(objetivo));
    }

    // El DTO nunca lleva password, ni siquiera codificada.
    private UserDTO toDTO(User user) {
        return new UserDTO(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRole(),
                user.getCreatedAt());
    }
}
