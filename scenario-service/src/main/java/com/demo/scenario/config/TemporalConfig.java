package com.demo.scenario.config;

import com.demo.scenario.temporal.activities.ScenarioActivitiesImpl;
import com.demo.scenario.temporal.workflows.ScenarioWorkflowImpl;
import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TemporalConfig {

    private static final Logger log = LoggerFactory.getLogger(TemporalConfig.class);
    public static final String TASK_QUEUE = "SCENARIO_TASK_QUEUE";

    @Bean
    @ConditionalOnMissingBean(WorkflowServiceStubs.class)
    public WorkflowServiceStubs workflowServiceStubs(
            @Value("${temporal.connection.target:localhost:7233}") String target) {
        log.info("Connecting to Temporal at {}", target);
        return WorkflowServiceStubs.newServiceStubs(
                WorkflowServiceStubsOptions.newBuilder()
                        .setTarget(target)
                        .build());
    }

    @Bean
    @ConditionalOnMissingBean(WorkflowClient.class)
    public WorkflowClient workflowClient(WorkflowServiceStubs stubs) {
        return WorkflowClient.newInstance(stubs);
    }

    @Bean
    public WorkerFactory workerFactory(WorkflowClient client,
                                       ScenarioActivitiesImpl activitiesImpl) {
        WorkerFactory factory = WorkerFactory.newInstance(client);
        Worker worker = factory.newWorker(TASK_QUEUE);
        worker.registerWorkflowImplementationTypes(ScenarioWorkflowImpl.class);
        worker.registerActivitiesImplementations(activitiesImpl);
        factory.start();
        log.info("Temporal worker started on task queue: {}", TASK_QUEUE);
        return factory;
    }
}
