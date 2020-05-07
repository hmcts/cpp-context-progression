package uk.gov.moj.cpp.progression.command;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
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

import uk.gov.justice.core.courts.AddCourtApplicationToCase;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationAddedToCase;
import uk.gov.justice.core.courts.CourtApplicationCreated;
import uk.gov.justice.core.courts.CreateCourtApplication;
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
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import uk.gov.moj.cpp.progression.handler.CreateCourtApplicationHandler;

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
public class CreateCourtApplicationHandlerTest {

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            CourtApplicationCreated.class, CourtApplicationAddedToCase.class);

    @InjectMocks
    private CreateCourtApplicationHandler createCourtApplicationHandler;


    private CaseAggregate aggregate;

    private ApplicationAggregate applicationAggregate;

    private static final UUID CASE_ID = UUID.fromString("5002d600-af66-11e8-b568-0800200c9a66");


    @Before
    public void setup() {
        aggregate = new CaseAggregate();
        applicationAggregate = new ApplicationAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(aggregate);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(applicationAggregate);
    }

    @Test
    public void shouldHandleCommand() {
        assertThat(new CreateCourtApplicationHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handle")
                        .thatHandles("progression.command.create-court-application")
                ));
    }

    @Test
    public void shouldHandleCommandCourtApplicationAddedToCase() {
        assertThat(new CreateCourtApplicationHandler(), isHandler(COMMAND_HANDLER)
                .with(method("courtApplicationAddedToCase")
                        .thatHandles("progression.command.add-court-application-to-case")
                ));
    }

    @Test
    public void shouldProcessCommand() throws Exception {
        final CreateCourtApplication createCourtApplication =
                CreateCourtApplication.createCourtApplication()
                        .withApplication(CourtApplication.courtApplication()
                                .withLinkedCaseId(CASE_ID).build()).build();
        aggregate.apply(createCourtApplication.getApplication());


        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.create-court-application")
                .withId(UUID.randomUUID())
                .build();

        final Envelope<CreateCourtApplication> envelope = envelopeFrom(metadata, createCourtApplication);

        createCourtApplicationHandler.handle(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.court-application-created"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.courtApplication", notNullValue()))
                        ).isJson(allOf(
                                withJsonPath("$.courtApplication.parentApplicationId", nullValue())))
                                .isJson(allOf(
                                        withJsonPath("$.courtApplication.linkedCaseId", notNullValue())))

                )
        ));
    }


    @Test
    public void shouldProcessSubApplicationCommand() throws Exception {

        final UUID parentApplicationId = UUID.randomUUID();

        final CreateCourtApplication createCourtApplication =
                CreateCourtApplication.createCourtApplication()
                        .withApplication(CourtApplication.courtApplication()
                                .withLinkedCaseId(CASE_ID)
                                .withParentApplicationId(parentApplicationId).build()).build();
        aggregate.apply(createCourtApplication.getApplication());


        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.create-court-application")
                .withId(parentApplicationId)
                .build();

        final Envelope<CreateCourtApplication> envelope = envelopeFrom(metadata, createCourtApplication);

        createCourtApplicationHandler.handle(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.court-application-created"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.courtApplication", notNullValue()))
                        ).isJson(allOf(
                                withJsonPath("$.courtApplication.parentApplicationId", equalTo(parentApplicationId))))
                                .isJson(allOf(
                                        withJsonPath("$.courtApplication.linkedCaseId", notNullValue())))

                )

        ));
    }

    @Test
    public void shouldProcessCommandCourtApplicationAddedToCase() throws Exception {
        final AddCourtApplicationToCase addCourtApplicationToCase =
                AddCourtApplicationToCase.addCourtApplicationToCase()
                        .withCourtApplication(CourtApplication.courtApplication()
                                .withId(UUID.randomUUID()).build())
                        .build();
        aggregate.apply(addCourtApplicationToCase.getCourtApplication());


        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.add-court-application-to-case")
                .withId(UUID.randomUUID())
                .build();

        final Envelope<AddCourtApplicationToCase> envelope = envelopeFrom(metadata, addCourtApplicationToCase);

        createCourtApplicationHandler.courtApplicationAddedToCase(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.court-application-added-to-case"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.courtApplication", notNullValue()))
                        ).isJson(allOf(
                                withJsonPath("$.courtApplication.id", notNullValue())))

                )
        ));
    }

}
