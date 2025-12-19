package uk.gov.moj.cpp.progression.task;

import static uk.gov.moj.cpp.jobstore.api.task.ExecutionInfo.executionInfo;
import static uk.gov.moj.cpp.jobstore.api.task.ExecutionStatus.COMPLETED;
import static uk.gov.moj.cpp.progression.task.Task.TaskNames.RETRY_ADD_DEFENDANT_TO_CASE_TASK;

import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.core.annotation.FrameworkComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.jobstore.api.task.ExecutableTask;
import uk.gov.moj.cpp.jobstore.api.task.ExecutionInfo;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@uk.gov.moj.cpp.jobstore.api.annotation.Task(RETRY_ADD_DEFENDANT_TO_CASE_TASK)
@ApplicationScoped
public class RetryAddDefendantToCaseTask implements ExecutableTask {
    private static final Logger LOGGER = LoggerFactory.getLogger(RetryAddDefendantToCaseTask.class);

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
        final JsonObject metadata = jobData.getJsonObject(METADATA);
        final JsonObject payload = jobData.getJsonObject(PAYLOAD);


        LOGGER.info("{} task started at {}", metadata.getString("name"), utcClock.now());

        sender.send(Envelope.envelopeFrom(
                Envelope.metadataFrom(metadata),
                payload
        ));
        LOGGER.info("{} task completed at {}", metadata.getString("name"), utcClock.now());

        return executionInfo()
                .withExecutionStatus(COMPLETED)
                .build();
    }
}
