package uk.gov.moj.cpp.progression.task;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.moj.cpp.jobstore.api.task.ExecutionInfo.executionInfo;
import static uk.gov.moj.cpp.jobstore.api.task.ExecutionStatus.COMPLETED;
import static uk.gov.moj.cpp.progression.task.Task.TaskNames.GENERATE_OPA_NOTICE_TASK;

import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.core.annotation.FrameworkComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.moj.cpp.jobstore.api.task.ExecutableTask;
import uk.gov.moj.cpp.jobstore.api.task.ExecutionInfo;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@uk.gov.moj.cpp.jobstore.api.annotation.Task(GENERATE_OPA_NOTICE_TASK)
@ApplicationScoped
public class GenerateOpaNoticeTask implements ExecutableTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenerateOpaNoticeTask.class);

    private static final String METADATA= "metadata";
    private static final String PAYLOAD= "payload";

    @Inject
    private UtcClock utcClock;

    @Inject
    @FrameworkComponent("EVENT_PROCESSOR")
    private Sender sender;

    @Override
    public ExecutionInfo execute(final ExecutionInfo executionInfo) {
        final JsonObject jobData = executionInfo.getJobData();
        final String commandName = jobData.getString(METADATA);
        final JsonObject payload = jobData.getJsonObject(PAYLOAD);

        LOGGER.info("{} task started at {}", commandName, utcClock.now());

        sendCommandWith(commandName, payload);
        LOGGER.info("{} task completed at {}", commandName, utcClock.now());

        return executionInfo()
                .withExecutionStatus(COMPLETED)
                .build();
    }

    private void sendCommandWith(final String commandName, final JsonObject payload) {
        sender.send(envelopeFrom(
                metadataBuilder()
                        .createdAt(utcClock.now())
                        .withName(commandName)
                        .withId(randomUUID())
                        .build(),
                payload));
    }
}
