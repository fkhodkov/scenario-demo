package com.demo.scenario.repository;

import com.demo.scenario.domain.Scenario;
import com.demo.scenario.domain.ScenarioStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ScenarioRepository extends JpaRepository<Scenario, UUID> {
    List<Scenario> findByStatus(ScenarioStatus status);
    Optional<Scenario> findByTriggerTopic(String topic);
    List<Scenario> findByTriggerTopicIn(List<String> topics);
}
