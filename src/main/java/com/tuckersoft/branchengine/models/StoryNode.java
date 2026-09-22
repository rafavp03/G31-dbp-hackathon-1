package com.tuckersoft.branchengine.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.Hibernate;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * Una escena de Bandersnatch.
 *
 * primaryBranchCode y glitchBranchCode son Strings, no llaves foraneas: los nodos
 * se crean en cualquier orden y pueden apuntar a nodos que todavia no existen.
 * La resolucion ocurre al decidir, no al crear.
 */
@Entity
@Table(name = "story_nodes")
@NoArgsConstructor
@Getter
@Setter
public class StoryNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "node_code", nullable = false, unique = true, length = 40)
    private String nodeCode;

    @Column(nullable = false, length = 80)
    private String title;

    @Column(name = "scene_text", nullable = false, columnDefinition = "TEXT")
    private String sceneText;

    @Column(name = "branch_capacity", nullable = false)
    private Integer branchCapacity;

    /** Lo fija el service en 0: nunca llega en el request. */
    @Column(name = "current_branches", nullable = false)
    private Integer currentBranches;

    @Column(name = "primary_branch_code", length = 40)
    private String primaryBranchCode;

    @Column(name = "glitch_branch_code", length = 40)
    private String glitchBranchCode;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    // Lado inverso de StoryNode 1 --- N Playthrough (el nodo actual de cada partida).
    @OneToMany(mappedBy = "currentNode")
    private Set<Playthrough> playthroughs = new HashSet<>();

    // Lado inverso de StoryNode 1 --- N Decision (el nodo de origen de cada decision).
    @OneToMany(mappedBy = "node")
    private Set<Decision> decisions = new HashSet<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        StoryNode other = (StoryNode) o;
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }
}
