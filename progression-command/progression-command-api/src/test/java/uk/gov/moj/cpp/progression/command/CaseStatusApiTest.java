package uk.gov.moj.cpp.progression.command;

import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum.INACTIVE;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.spi.DefaultJsonEnvelopeProvider;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CaseStatusApiTest {

    @Mock
    private Sender sender;

    @InjectMocks
    private CaseStatusApi caseStatusApi;

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> commandCaptor;


    @Test
    void shouldInactiveProsecutionCase() {
        final String caseId = randomUUID().toString();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.inactive-case-bdf")
                .withId(fromString(caseId))
                .withUserId(randomUUID().toString())
                .build();

        final JsonEnvelope command = new DefaultJsonEnvelopeProvider().envelopeFrom(metadata, createObjectBuilder()
                .add("prosecutionCaseId", caseId).build());

        caseStatusApi.handleCaseInactiveViaBdf(command);

        verify(sender, times(1)).send(commandCaptor.capture());
        assertThat(INACTIVE.name(), is(commandCaptor.getValue().payload().getString("caseStatus")));
        assertThat("progression.command.update-case-status-bdf", is(commandCaptor.getValue().metadata().asJsonObject().getString("name")));
    }

    @Test
    void shouldUpdateCaseStatus() {
        final String caseId = randomUUID().toString();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.inactive-case-bdf")
                .withId(fromString(caseId))
                .withUserId(randomUUID().toString())
                .build();

        final JsonEnvelope command = new DefaultJsonEnvelopeProvider().envelopeFrom(metadata, createObjectBuilder()
                .add("prosecutionCaseId", caseId)
                .add("caseStatus", "EJECTED")
                .add("notes", "legal")
                .build());

        caseStatusApi.handleUpdateCaseStatusBdf(command);

        verify(sender, times(1)).send(commandCaptor.capture());
        assertThat(caseId, is(commandCaptor.getValue().payload().getString("prosecutionCaseId")));
        assertThat("EJECTED", is(commandCaptor.getValue().payload().getString("caseStatus")));
        assertThat("legal", is(commandCaptor.getValue().payload().getString("notes")));
        assertThat("progression.command.update-case-status-bdf", is(commandCaptor.getValue().metadata().asJsonObject().getString("name")));
    }

}
