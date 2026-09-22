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
 * Una partida de prueba. Pertenece al usuario que la creo, que sale del token y
 * nunca del request body.
 */
@Entity
@Table(name = "playthroughs")
@NoArgsConstructor
@Getter
@Setter
public class Playthrough {

    public static final String ACTIVA = "ACTIVA";
    public static final String FINALIZADA = "FINALIZADA";

    public static final String ENDING_PAC_SYMBOL = "ENDING_PAC_SYMBOL";
    public static final String ENDING_WHITE_BEAR = "ENDING_WHITE_BEAR";
    public static final String ENDING_NETFLIX_CUT = "ENDING_NETFLIX_CUT";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_tag", nullable = false, unique = true, length = 40)
    private String playerTag;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** El nodo donde arranco. No cambia nunca. */
    @Column(name = "start_node_code", nullable = false, length = 40)
    private String startNodeCode;

    /** Se actualiza en cada decision que mueve la historia. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "current_node_id", nullable = false)
    private StoryNode currentNode;

    @Column(nullable = false)
    private Integer lucidity;

    @Column(name = "control_level", nullable = false)
    private Integer controlLevel;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "ending_code", length = 40)
    private String endingCode;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Lado inverso de Playthrough 1 --- N Decision.
    @OneToMany(mappedBy = "playthrough")
    private Set<Decision> decisions = new HashSet<>();

    public boolean estaActiva() {
        return ACTIVA.equals(status);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        Playthrough other = (Playthrough) o;
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }
}
