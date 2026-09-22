package com.tuckersoft.branchengine.repositories;

import com.tuckersoft.branchengine.models.RealityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RealityLogRepository extends JpaRepository<RealityLog, Long> {

    List<RealityLog> findByDecisionIdOrderByCreatedAtAsc(Long decisionId);
}
