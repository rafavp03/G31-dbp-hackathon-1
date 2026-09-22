package com.tuckersoft.branchengine.repositories;

import com.tuckersoft.branchengine.models.Playthrough;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlaythroughRepository extends JpaRepository<Playthrough, Long> {

    boolean existsByPlayerTag(String playerTag);

    /** El filtrado por dueno ocurre aqui, no en memoria. */
    List<Playthrough> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Playthrough> findAllByOrderByCreatedAtDesc();
}
