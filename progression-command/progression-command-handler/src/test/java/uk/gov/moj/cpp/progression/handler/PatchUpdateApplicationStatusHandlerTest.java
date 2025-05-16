package uk.gov.moj.cpp.progression.handler;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.ApplicationStatus.IN_PROGRESS;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationStatusUpdated;
import uk.gov.justice.core.courts.PatchUpdateApplicationStatus;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.progression.aggregate.ApplicationAggregate;

import java.util.List;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PatchUpdateApplicationStatusHandlerTest {

    @Mock
    private EventSource eventSource;

    @Mock
    private AggregateService aggregateService;

    @Mock
    private EventStream eventStream;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(CourtApplicationStatusUpdated.class);

    @InjectMocks
    private PatchUpdateApplicationStatusHandler handler;

    @Test
    void shouldHandleWhenApplicationStatusIsNotProvided() throws EventStreamException {
        final UUID applicationId = randomUUID();
        when(eventSource.getStreamById(applicationId)).thenReturn(eventStream);
        final ApplicationAggregate applicationAggregate = new ApplicationAggregate();
        setField(applicationAggregate, "courtApplication", CourtApplication.courtApplication().withId(applicationId).build());
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(applicationAggregate);

        final MetadataBuilder metadataBuilder = metadataFrom(metadataWithRandomUUID("progression.command.patch-update-application-status")
                .withUserId(randomUUID().toString())
                .build());
        final Envelope<PatchUpdateApplicationStatus> envelope = envelopeFrom(metadataBuilder, PatchUpdateApplicationStatus.patchUpdateApplicationStatus()
                .withId(applicationId)
                .build());

        handler.handle(envelope);

        final List<JsonEnvelope> eventList = verifyAppendAndGetArgumentFrom(eventStream).toList();
        assertThat(eventList.size(), is(1));
        final JsonEnvelope event = eventList.get(0);
        assertThat(event.metadata().name(), is("progression.event.court-application-status-updated"));
        final JsonObject payload = event.payload().asJsonObject().getJsonObject("courtApplication");
        assertThat(payload.getString("id"), is(applicationId.toString()));
        assertThat(payload.getString("applicationStatus"), is(IN_PROGRESS.toString()));

    }

    @ParameterizedTest
    @EnumSource(ApplicationStatus.class)
    void shouldHandleWhenApplicationStatusIsProvided(final ApplicationStatus applicationStatus) throws EventStreamException {
        final UUID applicationId = randomUUID();
        when(eventSource.getStreamById(applicationId)).thenReturn(eventStream);
        final ApplicationAggregate applicationAggregate = new ApplicationAggregate();
        setField(applicationAggregate, "courtApplication", CourtApplication.courtApplication().withId(applicationId).build());
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(applicationAggregate);

        final MetadataBuilder metadataBuilder = metadataFrom(metadataWithRandomUUID("progression.command.patch-update-application-status")
                .withUserId(randomUUID().toString())
                .build());
        final Envelope<PatchUpdateApplicationStatus> envelope = envelopeFrom(metadataBuilder, PatchUpdateApplicationStatus.patchUpdateApplicationStatus()
                .withId(applicationId)
                .withApplicationStatus(applicationStatus)
                .build());

        handler.handle(envelope);

        final List<JsonEnvelope> eventList = verifyAppendAndGetArgumentFrom(eventStream).toList();
        assertThat(eventList.size(), is(1));
        final JsonEnvelope event = eventList.get(0);
        assertThat(event.metadata().name(), is("progression.event.court-application-status-updated"));
        final JsonObject payload = event.payload().asJsonObject();
        final JsonObject courtApplication = payload.getJsonObject("courtApplication");
        assertThat(courtApplication.getString("id"), is(applicationId.toString()));
        assertThat(courtApplication.getString("applicationStatus"), is(applicationStatus.toString()));

    }


}