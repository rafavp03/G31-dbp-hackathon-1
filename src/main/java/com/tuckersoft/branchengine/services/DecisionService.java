package com.tuckersoft.branchengine.services;

import com.tuckersoft.branchengine.dtos.DecisionDTO;
import com.tuckersoft.branchengine.dtos.DecisionRequest;
import com.tuckersoft.branchengine.dtos.PageResponse;
import com.tuckersoft.branchengine.dtos.RealityLogDTO;
import com.tuckersoft.branchengine.events.DecisionCommittedEvent;
import com.tuckersoft.branchengine.exceptions.ConflictException;
import com.tuckersoft.branchengine.exceptions.ResourceNotFoundException;
import com.tuckersoft.branchengine.models.*;
import com.tuckersoft.branchengine.repositories.DecisionRepository;
import com.tuckersoft.branchengine.repositories.PlaythroughRepository;
import com.tuckersoft.branchengine.repositories.RealityLogRepository;
import com.tuckersoft.branchengine.repositories.StoryNodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DecisionService {

    private final DecisionRepository decisionRepository;
    private final PlaythroughRepository playthroughRepository;
    private final StoryNodeRepository storyNodeRepository;
    private final RealityLogRepository realityLogRepository;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public DecisionDTO create(DecisionRequest request, String simulate) {
        User actual = userService.getAuthenticatedUser();

        Playthrough partida = playthroughRepository.findById(request.getPlaythroughId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Partida " + request.getPlaythroughId() + " no encontrada"));

        // Supervisar no es jugar: para ESCRIBIR la regla de propiedad no tiene
        // excepcion, ni siquiera para el administrador.
        if (!partida.getUser().getId().equals(actual.getId())) {
            throw new AccessDeniedException("Esa partida no te pertenece");
        }

        if (!partida.estaActiva()) {
            throw new ConflictException("La partida " + partida.getId() + " ya esta FINALIZADA");
        }

        Instant ahora = Instant.now();
        StoryNode origen = partida.getCurrentNode();
        String branchType = BranchClassifier.clasificar(request.getRawInput());

        Decision decision = new Decision();
        decision.setPlaythrough(partida);
        decision.setNode(origen);
        decision.setRawInput(request.getRawInput());
        decision.setBranchType(branchType);
        decision.setImpactLevel(request.getImpactLevel());
        decision.setHandlerUnit(BranchClassifier.handlerUnit(branchType));
        decision.setOutcomeCode(BranchClassifier.outcomeCode(branchType));
        decision.setCreatedAt(ahora);
        decision.setUpdatedAt(ahora);

        // Las entradas corruptas se descartan: se guarda la decision y nada mas.
        // No se tocan los stats, no se mueve el nodo y no se publica el evento.
        if (BranchClassifier.ENTRADA_CORRUPTA.equals(branchType)) {
            decision.setResolvedNodeCode(null);
            decision.setStatus(Decision.ERROR);
            return toDTO(decisionRepository.save(decision), partida);
        }

        // Paso 1: los stats, con los limites estrictos de 0 a 100.
        int lucidity = acotar(partida.getLucidity() + ImpactLevel.deltaLucidity(request.getImpactLevel()));
        int controlLevel = acotar(partida.getControlLevel() + ImpactLevel.deltaControl(request.getImpactLevel()));
        partida.setLucidity(lucidity);
        partida.setControlLevel(controlLevel);

        // Paso 2: el nodo destino. Se guarda aunque ese nodo no exista.
        boolean porGlitch = BranchClassifier.RUPTURA_CUARTA_PARED.equals(branchType)
                || ImpactLevel.CRITICO.equals(request.getImpactLevel());
        String destino = porGlitch ? origen.getGlitchBranchCode() : origen.getPrimaryBranchCode();
        decision.setResolvedNodeCode(destino);

        // Paso 3: el estado de la partida, evaluado EN ESTE ORDEN.
        if (controlLevel >= 100) {
            finalizar(partida, Playthrough.ENDING_PAC_SYMBOL);
        } else if (lucidity <= 0) {
            finalizar(partida, Playthrough.ENDING_WHITE_BEAR);
        } else {
            Optional<StoryNode> nodoDestino = destino == null
                    ? Optional.empty()
                    : storyNodeRepository.findByNodeCode(destino);

            if (nodoDestino.isEmpty()) {
                finalizar(partida, Playthrough.ENDING_NETFLIX_CUT);
            } else {
                // El unico caso en el que la partida se mueve.
                partida.setCurrentNode(nodoDestino.get());
            }
        }
        partida.setUpdatedAt(ahora);
        playthroughRepository.save(partida);

        decision.setStatus(Decision.REGISTRADA);
        Decision guardada = decisionRepository.save(decision);

        // El correo ocurre despues del commit, en otro hilo: aqui solo se publica.
        eventPublisher.publishEvent(new DecisionCommittedEvent(
                guardada.getId(),
                partida.getUser().getEmail(),
                partida.getUser().getDisplayName(),
                partida.getPlayerTag(),
                guardada.getBranchType(),
                guardada.getImpactLevel(),
                guardada.getHandlerUnit(),
                guardada.getOutcomeCode(),
                origen.getNodeCode(),
                guardada.getResolvedNodeCode(),
                partida.getStatus(),
                partida.getLucidity(),
                partida.getControlLevel(),
                partida.getEndingCode(),
                guardada.getRawInput(),
                guardada.getCreatedAt(),
                simulate));

        return toDTO(guardada, partida);
    }

    @Transactional(readOnly = true)
    public PageResponse<DecisionDTO> search(String branchType, String impactLevel, String status,
                                            Long playthroughId, int page, int size) {
        User actual = userService.getAuthenticatedUser();
        // El administrador ve todas; un ROLE_USER queda limitado a las suyas.
        Long ownerId = esAdmin(actual) ? null : actual.getId();

        Page<Decision> pagina = decisionRepository.buscar(
                ownerId, branchType, impactLevel, status, playthroughId, PageRequest.of(page, size));

        return PageResponse.of(pagina, d -> toDTO(d, d.getPlaythrough()));
    }

    @Transactional(readOnly = true)
    public DecisionDTO getById(Long id) {
        Decision decision = findConAccesoDeLectura(id);
        return toDTO(decision, decision.getPlaythrough());
    }

    /** El aislamiento entre usuarios tambien aplica a los recursos hijos. */
    @Transactional(readOnly = true)
    public List<RealityLogDTO> getRealityLogs(Long decisionId) {
        findConAccesoDeLectura(decisionId);

        return realityLogRepository.findByDecisionIdOrderByCreatedAtAsc(decisionId).stream()
                .map(log -> new RealityLogDTO(
                        log.getId(),
                        decisionId,
                        log.getRecipientEmail(),
                        log.getSubject(),
                        log.getLogStatus(),
                        log.getErrorMessage(),
                        log.getSentAt(),
                        log.getCreatedAt()))
                .toList();
    }

    /** Leer vale para el dueno y para el administrador. */
    Decision findConAccesoDeLectura(Long id) {
        Decision decision = decisionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Decision " + id + " no encontrada"));

        User actual = userService.getAuthenticatedUser();
        if (!esAdmin(actual) && !decision.getPlaythrough().getUser().getId().equals(actual.getId())) {
            throw new AccessDeniedException("Esa decision no te pertenece");
        }
        return decision;
    }

    private void finalizar(Playthrough partida, String endingCode) {
        // La partida termina y su currentNode NO cambia.
        partida.setStatus(Playthrough.FINALIZADA);
        partida.setEndingCode(endingCode);
    }

    private int acotar(int valor) {
        return Math.max(0, Math.min(100, valor));
    }

    private boolean esAdmin(User user) {
        return Roles.ADMIN.equals(user.getRole());
    }

    private DecisionDTO toDTO(Decision decision, Playthrough partida) {
        return new DecisionDTO(
                decision.getId(),
                partida.getId(),
                partida.getPlayerTag(),
                decision.getNode().getNodeCode(),
                decision.getResolvedNodeCode(),
                decision.getRawInput(),
                decision.getBranchType(),
                decision.getImpactLevel(),
                decision.getHandlerUnit(),
                decision.getOutcomeCode(),
                decision.getStatus(),
                // El estado de la partida DESPUES de aplicar la decision.
                partida.getStatus(),
                partida.getLucidity(),
                partida.getControlLevel(),
                partida.getEndingCode(),
                decision.getCreatedAt(),
                decision.getUpdatedAt());
    }
}
