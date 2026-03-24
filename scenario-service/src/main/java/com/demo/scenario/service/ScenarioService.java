package com.demo.scenario.service;

import com.demo.scenario.domain.*;
import com.demo.scenario.dto.ScenarioGraph;
import com.demo.scenario.repository.ScenarioExecutionRepository;
import com.demo.scenario.repository.ScenarioRepository;
import com.demo.scenario.temporal.workflows.ScenarioWorkflow;
import com.demo.scenario.temporal.workflows.ScenarioWorkflowInput;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ScenarioService {

    private static final Logger log = LoggerFactory.getLogger(ScenarioService.class);

    private final ScenarioRepository scenarioRepo;
    private final ScenarioExecutionRepository executionRepo;
    private final WorkflowClient workflowClient;
    private final ObjectMapper objectMapper;

    public ScenarioService(ScenarioRepository scenarioRepo,
                            ScenarioExecutionRepository executionRepo,
                            WorkflowClient workflowClient,
                            ObjectMapper objectMapper) {
        this.scenarioRepo  = scenarioRepo;
        this.executionRepo = executionRepo;
        this.workflowClient = workflowClient;
        this.objectMapper  = objectMapper;
    }

    @Transactional
    public Scenario create(String name, String description, String definitionJson) throws Exception {
        objectMapper.readValue(definitionJson, ScenarioGraph.class); // validate JSON

        String[] triggerInfo = extractTrigger(definitionJson);
        Scenario s = Scenario.builder()
                .name(name)
                .description(description)
                .definition(definitionJson)
                .status(ScenarioStatus.DRAFT)
                .triggerTopic(triggerInfo[0])
                .triggerEventType(triggerInfo[1])
                .build();
        return scenarioRepo.save(s);
    }

    @Transactional
    public Scenario update(UUID id, String name, String description, String definitionJson) throws Exception {
        Scenario s = scenarioRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Scenario not found: " + id));

        objectMapper.readValue(definitionJson, ScenarioGraph.class); // validate
        String[] triggerInfo = extractTrigger(definitionJson);

        s.setName(name);
        s.setDescription(description);
        s.setDefinition(definitionJson);
        s.setTriggerTopic(triggerInfo[0]);
        s.setTriggerEventType(triggerInfo[1]);
        return scenarioRepo.save(s);
    }

    @Transactional
    public Scenario activate(UUID id) {
        Scenario s = scenarioRepo.findById(id).orElseThrow();
        s.setStatus(ScenarioStatus.ACTIVE);
        return scenarioRepo.save(s);
    }

    @Transactional
    public Scenario pause(UUID id) {
        Scenario s = scenarioRepo.findById(id).orElseThrow();
        s.setStatus(ScenarioStatus.PAUSED);
        return scenarioRepo.save(s);
    }

    public List<Scenario> listAll() { return scenarioRepo.findAll(); }

    public Scenario findById(UUID id) { return scenarioRepo.findById(id).orElseThrow(); }

    @Transactional
    public ScenarioExecution startExecution(UUID scenarioId, String userId, String triggerPayload) {
        Scenario scenario = scenarioRepo.findById(scenarioId).orElseThrow();

        if (scenario.getStatus() != ScenarioStatus.ACTIVE) {
            throw new IllegalStateException("Scenario is not ACTIVE");
        }

        String workflowId = "scenario-" + scenarioId + "-user-" + userId + "-" + UUID.randomUUID();

        ScenarioExecution execution = ScenarioExecution.builder()
                .scenario(scenario)
                .userId(userId)
                .workflowId(workflowId)
                .status(ExecutionStatus.RUNNING)
                .currentNodeId("start")
                .build();
        execution = executionRepo.save(execution);

        ScenarioWorkflowInput input = new ScenarioWorkflowInput(
                scenarioId, userId, triggerPayload, execution.getId().toString());

        ScenarioWorkflow wf = workflowClient.newWorkflowStub(
                ScenarioWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setWorkflowId(workflowId)
                        .setTaskQueue("SCENARIO_TASK_QUEUE")
                        .build());

        WorkflowClient.start(wf::execute, input);
        log.info("Started workflow {} for user {} scenario {}", workflowId, userId, scenarioId);
        return execution;
    }

    public List<ScenarioExecution> listExecutions(UUID scenarioId) {
        return executionRepo.findByScenario_IdOrderByStartedAtDesc(scenarioId);
    }

    /** Returns [triggerTopic, triggerEventType] from the graph's TRIGGER node. */
    private String[] extractTrigger(String definitionJson) throws Exception {
        ScenarioGraph graph = objectMapper.readValue(definitionJson, ScenarioGraph.class);
        if (graph.getNodes() != null) {
            for (ScenarioGraph.ScenarioNode node : graph.getNodes()) {
                if ("TRIGGER".equals(node.getType()) && node.getData() != null) {
                    return new String[]{ node.getData().getTopic(), node.getData().getEventType() };
                }
            }
        }
        return new String[]{ null, null };
    }
}
