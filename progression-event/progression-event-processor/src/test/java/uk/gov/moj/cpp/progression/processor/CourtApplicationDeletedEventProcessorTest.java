package uk.gov.moj.cpp.progression.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.List;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CourtApplicationDeletedEventProcessorTest {

    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<JsonEnvelope> senderJsonEnvelopeCaptor;

    @InjectMocks
    private CourtApplicationDeletedEventProcessor applicationDeletedEventProcessor;

    @Test
    public void shouldProcessDeleteApplicationForCaseRequested() {
        final String seedingHearingId = randomUUID().toString();
        final String applicationId = randomUUID().toString();
        final JsonObject hearingDeleted = createObjectBuilder()
                .add("seedingHearingId", seedingHearingId)
                .add("applicationId", applicationId)
                .build();

        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID("progression.event.delete-application-for-case-requested"),
                hearingDeleted);

        applicationDeletedEventProcessor.processDeleteApplicationForCaseRequested(event);

        verify(this.sender).send(this.senderJsonEnvelopeCaptor.capture());
        final JsonEnvelope commandEvent = this.senderJsonEnvelopeCaptor.getValue();

        assertThat(commandEvent.metadata().name(), is("progression.command.delete-application-for-case"));
        assertThat(commandEvent.payload().toString(), isJson(allOf(
                withJsonPath("$.seedingHearingId", equalTo(seedingHearingId)),
                withJsonPath("$.applicationId", equalTo(applicationId))
        )));
    }

    @Test
    public void shouldProcessDeleteCourtApplicationHearingRequested(){
        final String seedingHearingId = randomUUID().toString();
        final String applicationId = randomUUID().toString();
        final String hearingId = randomUUID().toString();
        final JsonObject hearingDeleted = createObjectBuilder()
                .add("seedingHearingId", seedingHearingId)
                .add("applicationId", applicationId)
                .add("hearingId", hearingId)
                .build();

        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID("progression.event.court-application-deleted"),
                hearingDeleted);

        applicationDeletedEventProcessor.processDeleteCourtApplicationHearingRequested(event);

        verify(this.sender).send(this.senderJsonEnvelopeCaptor.capture());
        final JsonEnvelope commandEvent = this.senderJsonEnvelopeCaptor.getValue();

        assertThat(commandEvent.metadata().name(), is("progression.command.delete-court-application-hearing"));
        assertThat(commandEvent.payload().toString(), isJson(allOf(
                withJsonPath("$.seedingHearingId", equalTo(seedingHearingId)),
                withJsonPath("$.hearingId", equalTo(hearingId)),
                withJsonPath("$.applicationId", equalTo(applicationId))
        )));
    }

    @Test
    public void shouldProcessCourtApplicationHearingDeleted() {
        final String seedingHearingId = randomUUID().toString();
        final String applicationId = randomUUID().toString();
        final String hearingId = randomUUID().toString();
        final JsonObject hearingDeleted = createObjectBuilder()
                .add("seedingHearingId", seedingHearingId)
                .add("applicationId", applicationId)
                .add("hearingId", hearingId)
                .build();

        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID("progression.event.court-application-hearing-deleted"),
                hearingDeleted);

        applicationDeletedEventProcessor.processCourtApplicationDeleted(event);

        verify(this.sender, times(2)).send(this.senderJsonEnvelopeCaptor.capture());
        final List<JsonEnvelope> commandEvents = this.senderJsonEnvelopeCaptor.getAllValues();

        assertThat(commandEvents.get(0).metadata().name(), is("public.progression.events.court-application-deleted"));
        assertThat(commandEvents.get(0).payload().toString(), isJson(allOf(
                withJsonPath("$.hearingId", equalTo(hearingId)),
                withJsonPath("$.applicationId", equalTo(applicationId))
        )));

        assertThat(commandEvents.get(1).metadata().name(), is("progression.command.remove-application-from-seedingHearing"));
        assertThat(commandEvents.get(1).payload().toString(), isJson(allOf(
                withJsonPath("$.seedingHearingId", equalTo(seedingHearingId)),
                withJsonPath("$.applicationId", equalTo(applicationId))
        )));
    }


}
