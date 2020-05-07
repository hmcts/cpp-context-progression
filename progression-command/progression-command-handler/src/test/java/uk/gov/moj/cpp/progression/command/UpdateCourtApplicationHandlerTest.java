package uk.gov.moj.cpp.progression.command;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtApplicationRespondent;
import uk.gov.justice.core.courts.CourtApplicationUpdated;
import uk.gov.justice.core.courts.UpdateCourtApplication;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher;
import uk.gov.moj.cpp.progression.aggregate.ApplicationAggregate;
import uk.gov.moj.cpp.progression.handler.UpdateCourtApplicationHandler;

import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class UpdateCourtApplicationHandlerTest {

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            CourtApplicationUpdated.class);

    @InjectMocks
    private UpdateCourtApplicationHandler updateCourtApplicationHandler;

    private ApplicationAggregate aggregate;

    private static final UUID APPLICATION_ID = UUID.randomUUID();

    @Before
    public void setup() {
        aggregate = new ApplicationAggregate();
        setField(aggregate, "courtApplication", getCourtApplication());
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(aggregate);
    }

    private CourtApplication getCourtApplication() {
        return CourtApplication.courtApplication()
                .withId(UUID.randomUUID())
                .withApplicationStatus(ApplicationStatus.LISTED)
                .withApplicant(CourtApplicationParty.courtApplicationParty()
                        .withId(UUID.randomUUID())
                        .build())
                .withRespondents(Arrays.asList(CourtApplicationRespondent.courtApplicationRespondent()
                        .withPartyDetails(CourtApplicationParty.courtApplicationParty()
                                .withId(UUID.randomUUID())
                                .build())
                        .build()))
                .build();
    }

    @Test
    public void shouldHandleCommand() {
        assertThat(new UpdateCourtApplicationHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handle")
                        .thatHandles("progression.command.update-court-application")
                ));
    }

    @Test
    public void shouldProcessCommand() throws Exception {

        CourtApplication courtApplication = CourtApplication.courtApplication()
                .withId(APPLICATION_ID)
                .build();

        final UpdateCourtApplication updateCourtApplication = UpdateCourtApplication.updateCourtApplication()
                .withCourtApplication(courtApplication)
                .build();
        aggregate.updateCourtApplication(courtApplication);

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.update-court-application")
                .withId(UUID.randomUUID())
                .build();

        final Envelope<UpdateCourtApplication> envelope = envelopeFrom(metadata, updateCourtApplication);
        updateCourtApplicationHandler.handle(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.court-application-updated"),
                        JsonEnvelopePayloadMatcher.payload().isJson(anyOf(
                                withJsonPath("$.courtApplication", notNullValue()))
                        ))

                )
        );
    }
}
