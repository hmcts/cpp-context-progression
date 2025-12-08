package uk.gov.moj.cpp.progression.task;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.moj.cpp.jobstore.api.task.ExecutionStatus.STARTED;
import static uk.gov.moj.cpp.jobstore.persistence.Priority.MEDIUM;
import static uk.gov.moj.cpp.progression.task.Task.TaskNames.RETRY_ADD_DEFENDANT_TO_CASE_TASK;

import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.jobstore.api.task.ExecutionInfo;
import uk.gov.moj.cpp.jobstore.api.task.ExecutionStatus;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

@ExtendWith(MockitoExtension.class)
class RetryAddDefendantToCaseTaskTest {

    @Mock
    private UtcClock utcClock;

    @Mock
    private Logger logger;

    @Mock
    private Sender sender;

    @InjectMocks
    private RetryAddDefendantToCaseTask retryAddDefendantToCaseTask;

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeCaptor;

    private final ZonedDateTime now = ZonedDateTime.now();

    @BeforeEach
    public void setup() {
        when(utcClock.now()).thenReturn(now);
    }

    @Test
    void shouldExecuteCommand(){

        final UUID id = randomUUID();
        final JsonObject payload = createObjectBuilder()
                .add("metadata", createObjectBuilder().add("name", "progression.command.replay-defendants-added-to-court-proceedings")
                        .add("id", id.toString())
                        .build())
                .add("payload", createObjectBuilder().add("interval", 2).build())
                .build();


        final ExecutionInfo executionInfo = new ExecutionInfo(payload, RETRY_ADD_DEFENDANT_TO_CASE_TASK, utcClock.now(), STARTED, MEDIUM);
        final ExecutionInfo response = retryAddDefendantToCaseTask.execute(executionInfo);

        assertThat(response.getExecutionStatus(), is(ExecutionStatus.COMPLETED));

        verify(sender).send(envelopeCaptor.capture());
        assertThat(envelopeCaptor.getValue().metadata().name(), is("progression.command.replay-defendants-added-to-court-proceedings"));

    }

}
