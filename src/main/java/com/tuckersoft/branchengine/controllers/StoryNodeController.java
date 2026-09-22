package com.tuckersoft.branchengine.controllers;

import com.tuckersoft.branchengine.dtos.StoryNodeDTO;
import com.tuckersoft.branchengine.dtos.StoryNodeRequest;
import com.tuckersoft.branchengine.services.StoryNodeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/nodes")
@RequiredArgsConstructor
public class StoryNodeController {

    private final StoryNodeService storyNodeService;

    /** Solo ROLE_ADMIN: la regla vive en el SecurityFilterChain. */
    @PostMapping
    public ResponseEntity<StoryNodeDTO> create(@Valid @RequestBody StoryNodeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(storyNodeService.create(request));
    }

    /** Array simple, no paginado. */
    @GetMapping
    public ResponseEntity<List<StoryNodeDTO>> getAll() {
        return ResponseEntity.ok(storyNodeService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<StoryNodeDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(storyNodeService.getById(id));
    }
}
