package com.tuckersoft.branchengine.repositories;

import com.tuckersoft.branchengine.models.Decision;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DecisionRepository extends JpaRepository<Decision, Long> {

    /** El recorrido de una partida: solo los pasos que movieron la historia. */
    List<Decision> findByPlaythroughIdAndResolvedNodeCodeIsNotNullOrderByCreatedAtAsc(Long playthroughId);

    /**
     * Listado con filtros opcionales, resueltos en la consulta y no en memoria.
     *
     * ownerId nulo significa "sin restriccion de dueno", que es el caso del
     * administrador; para un ROLE_USER siempre llega su propio id.
     */
    @Query("""
            SELECT d FROM Decision d
            WHERE (:ownerId IS NULL OR d.playthrough.user.id = :ownerId)
              AND (:branchType IS NULL OR d.branchType = :branchType)
              AND (:impactLevel IS NULL OR d.impactLevel = :impactLevel)
              AND (:status IS NULL OR d.status = :status)
              AND (:playthroughId IS NULL OR d.playthrough.id = :playthroughId)
            """)
    Page<Decision> buscar(@Param("ownerId") Long ownerId,
                          @Param("branchType") String branchType,
                          @Param("impactLevel") String impactLevel,
                          @Param("status") String status,
                          @Param("playthroughId") Long playthroughId,
                          Pageable pageable);
}
