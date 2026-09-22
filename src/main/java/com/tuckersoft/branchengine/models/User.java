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
 * El analista de QA. La tabla se llama "users" porque "user" es palabra
 * reservada en PostgreSQL.
 */
@Entity
@Table(name = "users")
@NoArgsConstructor
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    /** Siempre codificada con BCrypt. Nunca sale en un DTO. */
    @Column(nullable = false)
    private String password;

    @Column(name = "display_name", nullable = false, length = 60)
    private String displayName;

    @Column(nullable = false, length = 20)
    private String role;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    // Lado inverso de User 1 --- N Playthrough. Nunca se serializa: todo sale por DTO.
    @OneToMany(mappedBy = "user")
    private Set<Playthrough> playthroughs = new HashSet<>();

    public void addPlaythrough(Playthrough playthrough) {
        playthroughs.add(playthrough);
        playthrough.setUser(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        User other = (User) o;
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }
}
