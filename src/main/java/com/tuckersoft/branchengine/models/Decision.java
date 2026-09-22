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
 * Lo que el jugador decidio hacer y hacia donde lo mando el motor.
 */
@Entity
@Table(name = "decisions")
@NoArgsConstructor
@Getter
@Setter
public class Decision {

    public static final String REGISTRADA = "REGISTRADA";
    public static final String PROCESANDO = "PROCESANDO";
    public static final String ESTABILIZADA = "ESTABILIZADA";
    public static final String ERROR = "ERROR";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "playthrough_id", nullable = false)
    private Playthrough playthrough;

    /** Nodo de ORIGEN: el currentNode de la partida al momento de decidir. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "node_id", nullable = false)
    private StoryNode node;

    @Column(name = "raw_input", nullable = false, columnDefinition = "TEXT")
    private String rawInput;

    @Column(name = "branch_type", nullable = false, length = 30)
    private String branchType;

    @Column(name = "impact_level", nullable = false, length = 20)
    private String impactLevel;

    /** Se deriva del branchType, no llega en el request. */
    @Column(name = "handler_unit", nullable = false, length = 40)
    private String handlerUnit;

    /** Se deriva del branchType, no llega en el request. */
    @Column(name = "outcome_code", nullable = false, length = 40)
    private String outcomeCode;

    /** Puede guardar un codigo cuyo StoryNode no existe. Nulo si la entrada fue corrupta. */
    @Column(name = "resolved_node_code", length = 40)
    private String resolvedNodeCode;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Lado inverso de Decision 1 --- N RealityLog.
    @OneToMany(mappedBy = "decision")
    private Set<RealityLog> realityLogs = new HashSet<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        Decision other = (Decision) o;
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }
}
