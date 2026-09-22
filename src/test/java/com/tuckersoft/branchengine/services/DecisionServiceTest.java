package com.tuckersoft.branchengine.services;

import com.tuckersoft.branchengine.dtos.DecisionDTO;
import com.tuckersoft.branchengine.dtos.DecisionRequest;
import com.tuckersoft.branchengine.events.DecisionCommittedEvent;
import com.tuckersoft.branchengine.models.*;
import com.tuckersoft.branchengine.repositories.DecisionRepository;
import com.tuckersoft.branchengine.repositories.PlaythroughRepository;
import com.tuckersoft.branchengine.repositories.RealityLogRepository;
import com.tuckersoft.branchengine.repositories.StoryNodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios del motor de decisiones.
 *
 * Todo esta mockeado: corren sin PostgreSQL y sin red. Lo que se ejercita es el
 * DecisionService de verdad, no una reimplementacion de sus reglas.
 */
@ExtendWith(MockitoExtension.class)
class DecisionServiceTest {

    private static final String NODO_ORIGEN = "NODE-CEREAL";
    private static final String NODO_PRIMARIO = "NODE-BUS";
    private static final String NODO_GLITCH = "NODE-ESPEJO";

    @Mock
    private DecisionRepository decisionRepository;
    @Mock
    private PlaythroughRepository playthroughRepository;
    @Mock
    private StoryNodeRepository storyNodeRepository;
    @Mock
    private RealityLogRepository realityLogRepository;
    @Mock
    private UserService userService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private DecisionService decisionService;

    private User dueno;
    private StoryNode origen;
    private Playthrough partida;

    @BeforeEach
    void prepararEscenario() {
        dueno = new User();
        dueno.setId(1L);
        dueno.setEmail("ada@tuckersoft.test");
        dueno.setDisplayName("Ada Lovelace");
        dueno.setRole(Roles.USER);
        dueno.setCreatedAt(Instant.now());

        origen = new StoryNode();
        origen.setId(10L);
        origen.setNodeCode(NODO_ORIGEN);
        origen.setTitle("El desayuno");
        origen.setSceneText("Stefan debe elegir entre Sugar Puffs y Frosties.");
        origen.setBranchCapacity(5);
        origen.setCurrentBranches(1);
        origen.setPrimaryBranchCode(NODO_PRIMARIO);
        origen.setGlitchBranchCode(NODO_GLITCH);
        origen.setCreatedAt(Instant.now());

        partida = new Playthrough();
        partida.setId(100L);
        partida.setPlayerTag("STEFAN-01");
        partida.setUser(dueno);
        partida.setCurrentNode(origen);
        partida.setStartNodeCode(NODO_ORIGEN);
        partida.setLucidity(100);
        partida.setControlLevel(0);
        partida.setStatus(Playthrough.ACTIVA);
        partida.setCreatedAt(Instant.now());
        partida.setUpdatedAt(Instant.now());
    }

    // ------------------------------------------------------------------ 1

    @Test
    @DisplayName("1. 'Stefan destruye la camara' es RUPTURA_CUARTA_PARED, no REBELDIA")
    void destruir_la_camara_es_ruptura_de_cuarta_pared() {
        stubsDeDecisionValida();

        DecisionDTO dto = decisionService.create(
                pedir("Stefan destruye la camara que lo estaba grabando."), null);

        // El texto cumple la regla 2 ('camara') y la 4 ('destruye'). Gana la 2
        // porque se evalua primero.
        assertEquals(BranchClassifier.RUPTURA_CUARTA_PARED, dto.getBranchType());
        assertEquals("Departamento Netflix", dto.getHandlerUnit());
        assertEquals("BREAK_FOURTH_WALL", dto.getOutcomeCode());
    }

    // ------------------------------------------------------------------ 2

    @Test
    @DisplayName("2. Un texto sin letras es ENTRADA_CORRUPTA y no modifica la partida")
    void una_entrada_sin_letras_no_modifica_la_partida() {
        when(userService.getAuthenticatedUser()).thenReturn(dueno);
        when(playthroughRepository.findById(100L)).thenReturn(Optional.of(partida));
        when(decisionRepository.save(any(Decision.class))).thenAnswer(inv -> inv.getArgument(0));

        DecisionDTO dto = decisionService.create(pedir("%%%% 01001 ### @@@ 110"), null);

        assertEquals(BranchClassifier.ENTRADA_CORRUPTA, dto.getBranchType());
        assertEquals(Decision.ERROR, dto.getStatus());
        assertNull(dto.getResolvedNodeCode());

        // La partida queda exactamente como estaba.
        assertEquals(100, partida.getLucidity());
        assertEquals(0, partida.getControlLevel());
        assertEquals(Playthrough.ACTIVA, partida.getStatus());
        assertNull(partida.getEndingCode());
        assertEquals(origen, partida.getCurrentNode());

        // Ni siquiera se guarda la partida: no hay nada que actualizar.
        verify(playthroughRepository, never()).save(any(Playthrough.class));
    }

    // ------------------------------------------------------------------ 3

    @Test
    @DisplayName("3. CRITICO baja 40 la lucidez, sube 45 el control y respeta los limites")
    void el_impacto_critico_mueve_los_stats_y_respeta_los_limites() {
        stubsDeDecisionValida();

        DecisionDTO dto = decisionService.create(
                pedir("Stefan sigue adelante con lo que el guion le indica.", ImpactLevel.CRITICO), null);

        assertEquals(60, dto.getLucidity());
        assertEquals(45, dto.getControlLevel());
        assertEquals(Playthrough.ACTIVA, dto.getPlaythroughStatus());

        // Con la lucidez ya baja, el siguiente CRITICO la deja en 0 y nunca negativa.
        partida.setLucidity(30);
        partida.setControlLevel(0);
        partida.setStatus(Playthrough.ACTIVA);

        DecisionDTO alLimite = decisionService.create(
                pedir("Stefan sigue adelante con lo que el guion le indica.", ImpactLevel.CRITICO), null);

        assertEquals(0, alLimite.getLucidity(), "100 - 40 - 40 daria negativo: se topa en 0");
        assertEquals(45, alLimite.getControlLevel());
    }

    // ------------------------------------------------------------------ 4

    @Test
    @DisplayName("4. Con controlLevel en 100 el final es ENDING_PAC_SYMBOL aunque la lucidez sea 0")
    void el_final_por_control_gana_al_final_por_lucidez() {
        when(userService.getAuthenticatedUser()).thenReturn(dueno);
        when(playthroughRepository.findById(100L)).thenReturn(Optional.of(partida));
        when(decisionRepository.save(any(Decision.class))).thenAnswer(inv -> inv.getArgument(0));

        // Esta decision deja lucidity en 0 Y controlLevel en 100 a la vez.
        partida.setLucidity(20);
        partida.setControlLevel(80);

        DecisionDTO dto = decisionService.create(
                pedir("Stefan sigue adelante con lo que el guion le indica.", ImpactLevel.CRITICO), null);

        assertEquals(0, dto.getLucidity());
        assertEquals(100, dto.getControlLevel());
        assertEquals(Playthrough.FINALIZADA, dto.getPlaythroughStatus());
        assertEquals(Playthrough.ENDING_PAC_SYMBOL, dto.getEndingCode(),
                "El control se evalua antes que la lucidez: si sale ENDING_WHITE_BEAR, "
                        + "las condiciones estan al reves");
    }

    // ------------------------------------------------------------------ 5

    @Test
    @DisplayName("5. El evento se publica una vez en una decision valida y ninguna en una corrupta")
    void el_evento_se_publica_solo_en_las_decisiones_validas() {
        stubsDeDecisionValida();

        decisionService.create(pedir("Stefan acepta la oferta y se queda en Tuckersoft."), null);
        verify(eventPublisher, times(1)).publishEvent(any(DecisionCommittedEvent.class));

        // La entrada corrupta se descarta antes de publicar nada: sin evento no hay
        // correo ni RealityLog.
        clearInvocations(eventPublisher);
        decisionService.create(pedir("### 0101 @@@ 11"), null);
        verify(eventPublisher, never()).publishEvent(any(DecisionCommittedEvent.class));
    }

    // ------------------------------------------------------------------ apoyo

    /** Stubs de una decision que sigue el camino completo y encuentra su nodo destino. */
    private void stubsDeDecisionValida() {
        StoryNode destino = new StoryNode();
        destino.setId(11L);
        destino.setNodeCode(NODO_PRIMARIO);

        when(userService.getAuthenticatedUser()).thenReturn(dueno);
        when(playthroughRepository.findById(100L)).thenReturn(Optional.of(partida));
        when(decisionRepository.save(any(Decision.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(playthroughRepository.save(any(Playthrough.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient().when(storyNodeRepository.findByNodeCode(anyString()))
                .thenReturn(Optional.of(destino));
    }

    private DecisionRequest pedir(String rawInput) {
        return pedir(rawInput, ImpactLevel.LEVE);
    }

    private DecisionRequest pedir(String rawInput, String impactLevel) {
        DecisionRequest request = new DecisionRequest();
        request.setPlaythroughId(100L);
        request.setRawInput(rawInput);
        request.setImpactLevel(impactLevel);
        return request;
    }
}
