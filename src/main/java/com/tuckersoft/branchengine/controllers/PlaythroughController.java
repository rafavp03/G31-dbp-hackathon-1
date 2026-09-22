package com.tuckersoft.branchengine.controllers;

import com.tuckersoft.branchengine.dtos.PlaythroughDTO;
import com.tuckersoft.branchengine.dtos.PlaythroughPathDTO;
import com.tuckersoft.branchengine.dtos.PlaythroughRequest;
import com.tuckersoft.branchengine.services.PlaythroughService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/playthroughs")
@RequiredArgsConstructor
public class PlaythroughController {

    private final PlaythroughService playthroughService;

    @PostMapping
    public ResponseEntity<PlaythroughDTO> create(@Valid @RequestBody PlaythroughRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(playthroughService.create(request));
    }

    /** Array simple: las del usuario, o todas si es administrador. */
    @GetMapping
    public ResponseEntity<List<PlaythroughDTO>> getAll() {
        return ResponseEntity.ok(playthroughService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlaythroughDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(playthroughService.getById(id));
    }

    @GetMapping("/{id}/path")
    public ResponseEntity<PlaythroughPathDTO> getPath(@PathVariable Long id) {
        return ResponseEntity.ok(playthroughService.getPath(id));
    }
}
