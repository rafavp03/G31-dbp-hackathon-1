package com.tuckersoft.branchengine.services;

import com.tuckersoft.branchengine.dtos.StoryNodeDTO;
import com.tuckersoft.branchengine.dtos.StoryNodeRequest;
import com.tuckersoft.branchengine.exceptions.ConflictException;
import com.tuckersoft.branchengine.exceptions.ResourceNotFoundException;
import com.tuckersoft.branchengine.models.StoryNode;
import com.tuckersoft.branchengine.repositories.StoryNodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StoryNodeService {

    private final StoryNodeRepository storyNodeRepository;

    @Transactional
    public StoryNodeDTO create(StoryNodeRequest request) {
        if (storyNodeRepository.existsByNodeCode(request.getNodeCode())) {
            throw new ConflictException("El nodeCode " + request.getNodeCode() + " ya existe");
        }

        StoryNode node = new StoryNode();
        node.setNodeCode(request.getNodeCode());
        node.setTitle(request.getTitle());
        node.setSceneText(request.getSceneText());
        node.setBranchCapacity(request.getBranchCapacity());
        // Los dos valores que no llegan del request.
        node.setCurrentBranches(0);
        node.setCreatedAt(Instant.now());
        // Strings sueltos: pueden apuntar a nodos que todavia no existen.
        node.setPrimaryBranchCode(request.getPrimaryBranchCode());
        node.setGlitchBranchCode(request.getGlitchBranchCode());

        return toDTO(storyNodeRepository.save(node));
    }

    @Transactional(readOnly = true)
    public List<StoryNodeDTO> getAll() {
        return storyNodeRepository.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public StoryNodeDTO getById(Long id) {
        return toDTO(findById(id));
    }

    /** Para los services que necesitan la entidad, no el DTO. */
    @Transactional(readOnly = true)
    public StoryNode findById(Long id) {
        return storyNodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nodo " + id + " no encontrado"));
    }

    private StoryNodeDTO toDTO(StoryNode node) {
        return new StoryNodeDTO(
                node.getId(),
                node.getNodeCode(),
                node.getTitle(),
                node.getSceneText(),
                node.getBranchCapacity(),
                node.getCurrentBranches(),
                node.getPrimaryBranchCode(),
                node.getGlitchBranchCode(),
                node.getCreatedAt());
    }
}
