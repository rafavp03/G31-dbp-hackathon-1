package com.tuckersoft.branchengine.services;

import com.tuckersoft.branchengine.dtos.PathStepDTO;
import com.tuckersoft.branchengine.dtos.PlaythroughDTO;
import com.tuckersoft.branchengine.dtos.PlaythroughPathDTO;
import com.tuckersoft.branchengine.dtos.PlaythroughRequest;
import com.tuckersoft.branchengine.exceptions.BadRequestException;
import com.tuckersoft.branchengine.exceptions.ConflictException;
import com.tuckersoft.branchengine.exceptions.ResourceNotFoundException;
import com.tuckersoft.branchengine.models.*;
import com.tuckersoft.branchengine.repositories.DecisionRepository;
import com.tuckersoft.branchengine.repositories.PlaythroughRepository;
import com.tuckersoft.branchengine.repositories.StoryNodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlaythroughService {

    private final PlaythroughRepository playthroughRepository;
    private final StoryNodeRepository storyNodeRepository;
    private final DecisionRepository decisionRepository;
    private final UserService userService;

    @Transactional
    public PlaythroughDTO create(PlaythroughRequest request) {
        // 1. El dueno sale del token, nunca del request.
        User owner = userService.getAuthenticatedUser();

        // 2. El nodo de arranque tiene que existir.
        StoryNode node = storyNodeRepository.findByNodeCode(request.getStartNodeCode())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El nodo " + request.getStartNodeCode() + " no existe"));

        // 3. La etiqueta del jugador es unica.
        if (playthroughRepository.existsByPlayerTag(request.getPlayerTag())) {
            throw new ConflictException("El playerTag " + request.getPlayerTag() + " ya existe");
        }

        // 4. No se puede arrancar en un nodo lleno.
        if (node.getCurrentBranches() >= node.getBranchCapacity()) {
            throw new BadRequestException(
                    "El nodo " + node.getNodeCode() + " ya no admite mas partidas");
        }

        // 5. Valores iniciales fijados aqui, no en el request.
        Instant ahora = Instant.now();
        Playthrough playthrough = new Playthrough();
        playthrough.setPlayerTag(request.getPlayerTag());
        playthrough.setUser(owner);
        playthrough.setCurrentNode(node);
        playthrough.setStartNodeCode(node.getNodeCode());
        playthrough.setLucidity(100);
        playthrough.setControlLevel(0);
        playthrough.setStatus(Playthrough.ACTIVA);
        playthrough.setEndingCode(null);
        playthrough.setCreatedAt(ahora);
        playthrough.setUpdatedAt(ahora);

        // 6. La partida ocupa una rama del nodo, en la misma transaccion.
        node.setCurrentBranches(node.getCurrentBranches() + 1);
        storyNodeRepository.save(node);

        return toDTO(playthroughRepository.save(playthrough));
    }

    /** Un ROLE_USER ve solo las suyas; el administrador las ve todas. */
    @Transactional(readOnly = true)
    public List<PlaythroughDTO> getAll() {
        User actual = userService.getAuthenticatedUser();

        List<Playthrough> partidas = esAdmin(actual)
                ? playthroughRepository.findAllByOrderByCreatedAtDesc()
                : playthroughRepository.findByUserIdOrderByCreatedAtDesc(actual.getId());

        return partidas.stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public PlaythroughDTO getById(Long id) {
        return toDTO(findConAccesoDeLectura(id));
    }

    @Transactional(readOnly = true)
    public PlaythroughPathDTO getPath(Long id) {
        Playthrough partida = findConAccesoDeLectura(id);

        // Solo los pasos que movieron la historia, en orden cronologico.
        List<Decision> decisiones = decisionRepository
                .findByPlaythroughIdAndResolvedNodeCodeIsNotNullOrderByCreatedAtAsc(id);

        List<PathStepDTO> steps = new ArrayList<>();
        int orden = 1;
        for (Decision d : decisiones) {
            steps.add(new PathStepDTO(
                    orden++,
                    d.getId(),
                    d.getNode().getNodeCode(),
                    d.getResolvedNodeCode(),
                    d.getBranchType(),
                    d.getImpactLevel(),
                    d.getCreatedAt()));
        }

        return new PlaythroughPathDTO(
                partida.getId(),
                partida.getPlayerTag(),
                partida.getStatus(),
                partida.getEndingCode(),
                partida.getStartNodeCode(),
                partida.getCurrentNode().getNodeCode(),
                steps);
    }

    /**
     * Para LEER vale el dueno o el administrador: el administrador supervisa.
     * La regla de ESCRITURA es otra y vive en el DecisionService.
     */
    private Playthrough findConAccesoDeLectura(Long id) {
        Playthrough partida = playthroughRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Partida " + id + " no encontrada"));

        User actual = userService.getAuthenticatedUser();
        if (!esAdmin(actual) && !partida.getUser().getId().equals(actual.getId())) {
            throw new AccessDeniedException("Esa partida no te pertenece");
        }
        return partida;
    }

    private boolean esAdmin(User user) {
        return Roles.ADMIN.equals(user.getRole());
    }

    PlaythroughDTO toDTO(Playthrough playthrough) {
        return new PlaythroughDTO(
                playthrough.getId(),
                playthrough.getPlayerTag(),
                playthrough.getUser().getEmail(),
                playthrough.getStartNodeCode(),
                playthrough.getCurrentNode().getNodeCode(),
                playthrough.getLucidity(),
                playthrough.getControlLevel(),
                playthrough.getStatus(),
                playthrough.getEndingCode(),
                playthrough.getCreatedAt(),
                playthrough.getUpdatedAt());
    }
}
