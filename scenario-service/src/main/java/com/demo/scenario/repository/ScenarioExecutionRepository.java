package com.demo.scenario.repository;

import com.demo.scenario.domain.ScenarioExecution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import com.demo.scenario.domain.ExecutionStatus;
import java.util.UUID;

public interface ScenarioExecutionRepository extends JpaRepository<ScenarioExecution, UUID> {
    Optional<ScenarioExecution> findByWorkflowId(String workflowId);
    List<ScenarioExecution> findByScenario_IdOrderByStartedAtDesc(UUID scenarioId);
    List<ScenarioExecution> findByUserId(String userId);
    boolean existsByScenario_IdAndUserIdAndStatus(UUID scenarioId, String userId, ExecutionStatus status);
}
