package com.demo.scenario.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ScenarioExecutionTest {

    @Test
    void builder_setsAllFields() {
        Scenario scenario = Scenario.builder().name("s").definition("{}").status(ScenarioStatus.ACTIVE).build();

        ScenarioExecution exec = ScenarioExecution.builder()
                .scenario(scenario)
                .userId("user_001")
                .workflowId("wf-123")
                .status(ExecutionStatus.RUNNING)
                .currentNodeId("node_1")
                .build();

        assertEquals(scenario,              exec.getScenario());
        assertEquals("user_001",            exec.getUserId());
        assertEquals("wf-123",              exec.getWorkflowId());
        assertEquals(ExecutionStatus.RUNNING, exec.getStatus());
        assertEquals("node_1",              exec.getCurrentNodeId());
        assertNull(exec.getId());
        assertNull(exec.getFinishedAt());
    }

    @Test
    void setStatus_updatesStatus() {
        ScenarioExecution exec = new ScenarioExecution();
        exec.setStatus(ExecutionStatus.RUNNING);
        assertEquals(ExecutionStatus.RUNNING, exec.getStatus());

        exec.setStatus(ExecutionStatus.COMPLETED);
        assertEquals(ExecutionStatus.COMPLETED, exec.getStatus());
    }

    @Test
    void setCurrentNodeId_updatesNodeId() {
        ScenarioExecution exec = new ScenarioExecution();
        exec.setCurrentNodeId("node_5");
        assertEquals("node_5", exec.getCurrentNodeId());
    }
}
