package com.tuckersoft.branchengine.controllers;

import com.tuckersoft.branchengine.dtos.DecisionDTO;
import com.tuckersoft.branchengine.dtos.DecisionRequest;
import com.tuckersoft.branchengine.dtos.PageResponse;
import com.tuckersoft.branchengine.dtos.RealityLogDTO;
import com.tuckersoft.branchengine.services.DecisionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/decisions")
@RequiredArgsConstructor
public class DecisionController {

    private final DecisionService decisionService;

    /**
     * La cabecera X-Bandersnatch-Simulate es opcional y se pasa tal cual al evento.
     * Un valor desconocido no cambia nada: nunca produce un 400.
     */
    @PostMapping
    public ResponseEntity<DecisionDTO> create(
            @Valid @RequestBody DecisionRequest request,
            @RequestHeader(value = "X-Bandersnatch-Simulate", required = false) String simulate) {
        return ResponseEntity.status(HttpStatus.CREATED).body(decisionService.create(request, simulate));
    }

    @GetMapping
    public ResponseEntity<PageResponse<DecisionDTO>> search(
            @RequestParam(required = false) String branchType,
            @RequestParam(required = false) String impactLevel,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long playthroughId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(
                decisionService.search(branchType, impactLevel, status, playthroughId, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DecisionDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(decisionService.getById(id));
    }

    /** La auditoria de los envios. Dueno o admin: 403 para el resto. */
    @GetMapping("/{id}/reality-logs")
    public ResponseEntity<List<RealityLogDTO>> getRealityLogs(@PathVariable Long id) {
        return ResponseEntity.ok(decisionService.getRealityLogs(id));
    }
}
