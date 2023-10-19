package uk.gov.moj.cpp.progression.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.CourtRegisterRecorded;
import uk.gov.justice.core.courts.courtRegisterDocument.CourtRegisterDocumentRequest;
import uk.gov.justice.core.courts.courtRegisterDocument.CourtRegisterRecipient;
import uk.gov.justice.progression.courts.CourtRegisterGenerated;
import uk.gov.justice.progression.courts.CourtRegisterNotificationIgnored;
import uk.gov.justice.progression.courts.CourtRegisterNotified;
import uk.gov.justice.progression.courts.GenerateCourtRegister;
import uk.gov.justice.progression.courts.NotifyCourtRegister;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher;
import uk.gov.moj.cpp.progression.aggregate.CourtCentreAggregate;
import uk.gov.moj.cpp.progression.command.GenerateCourtRegisterByDate;

import java.time.LocalDate;
import java.util.Collections;
import java.util.UUID;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CourtRegisterHandlerTest {
    private static final String ADD_COURT_REGISTER_COMMAND_NAME = "progression.command.add-court-register";
    private static final UUID COURT_CENTRE_ID = randomUUID();

    @Mock
    private EventSource eventSource;
    @Mock
    private EventStream eventStream;
    @Mock
    private AggregateService aggregateService;
    @InjectMocks
    private CourtRegisterHandler courtRegisterHandler;
    @Mock
    private JsonEnvelope envelope;
    @Mock
    private Requester requester;
    private CourtCentreAggregate aggregate;
    @Spy
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
    @Spy
    private final JsonObjectToObjectConverter jsonToObjectConverter = new JsonObjectToObjectConverter(objectMapper);

    @Spy
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);

    @Spy
    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();
    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(CourtRegisterRecorded.class, CourtRegisterGenerated.class, CourtRegisterNotified.class, CourtRegisterNotificationIgnored.class);

    @Before
    public void setup() {
        aggregate = new CourtCentreAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CourtCentreAggregate.class)).thenReturn(aggregate);
        setField(this.jsonToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldHandleCommand() {
        assertThat(courtRegisterHandler, isHandler(COMMAND_HANDLER)
                .with(method("handleAddCourtRegister")
                        .thatHandles(ADD_COURT_REGISTER_COMMAND_NAME)));
    }

    @Test
    public void shouldProcessCommand() throws Exception {

        final Envelope<CourtRegisterDocumentRequest> envelope = createCourtRegisterDocumentRequestHandlerEnvelope();

        courtRegisterHandler.handleAddCourtRegister(envelope);

        verifyCourtRegisterDocumentRequestHandlerResults();
    }

    @Test
    public void shouldHandleGenerateRequest() throws EventStreamException {
        final Envelope<GenerateCourtRegister> generateCourtRegisterEnvelope = prepareGenerateCourtRegisterEnvelope();
        final UUID courtCentreId = UUID.randomUUID();
        final JsonEnvelope queryEnvelope = mock(JsonEnvelope.class);
        final CourtRegisterDocumentRequest courtRegisterDocumentRequest = new CourtRegisterDocumentRequest.Builder().withCourtCentreId(courtCentreId).build();
        final JsonArray jsonValues = Json.createArrayBuilder().add(Json.createObjectBuilder().add("courtCentreId", courtCentreId.toString())
                .add("payload", objectToJsonObjectConverter.convert(courtRegisterDocumentRequest).toString())
                .build()).build();
        final JsonObject jsonObject = Json.createObjectBuilder().add("courtRegisterDocumentRequests", jsonValues).build();
        when(queryEnvelope.payloadAsJsonObject()).thenReturn(jsonObject);
        when(requester.request(any(Envelope.class))).thenReturn(queryEnvelope);
        courtRegisterHandler.handleGenerateCourtRegister(generateCourtRegisterEnvelope);
        verifyCourtRegisterGenerateDocumentResults();

    }

    @Test
    public void shouldHandleNotifyCourtCentre() throws EventStreamException {
        final UUID courtCentreId = randomUUID();
        final UUID systemDocGeneratorId = randomUUID();
        final CourtRegisterRecipient recipient = CourtRegisterRecipient.courtRegisterRecipient().withRecipientName("John").build();
        aggregate.setCourtRegisterRecipients(Collections.singletonList(recipient));
        final Envelope<NotifyCourtRegister> notifyCourtRegisterEnvelope = prepareNotifyCourtCentreEnvelope(courtCentreId, systemDocGeneratorId);
        courtRegisterHandler.handleNotifyCourtCentre(notifyCourtRegisterEnvelope);
        verifyNotifyCourtHandlerResults();
    }

    @Test
    public void shouldHandleGenerateCourtRegisterByDateRequest() throws EventStreamException {
        final Envelope<GenerateCourtRegisterByDate> generateCourtRegisterEnvelope = prepareGenerateCourtRegisterByDateEnvelope();
        final UUID courtCentreId = UUID.randomUUID();
        final JsonEnvelope queryEnvelope = mock(JsonEnvelope.class);
        final CourtRegisterDocumentRequest courtRegisterDocumentRequest = new CourtRegisterDocumentRequest.Builder().withCourtCentreId(courtCentreId).build();
        final JsonArray jsonValues = Json.createArrayBuilder().add(Json.createObjectBuilder().add("courtCentreId", courtCentreId.toString())
                .add("payload", objectToJsonObjectConverter.convert(courtRegisterDocumentRequest).toString())
                .build()).build();
        final JsonObject jsonObject = Json.createObjectBuilder().add("courtRegisterDocumentRequests", jsonValues).build();
        when(queryEnvelope.payloadAsJsonObject()).thenReturn(jsonObject);
        when(requester.request(any(Envelope.class))).thenReturn(queryEnvelope);
        courtRegisterHandler.handleGenerateCourtRegisterByDate(generateCourtRegisterEnvelope);
        verifyCourtRegisterGenerateDocumentResults();

    }


    private Envelope prepareNotifyCourtCentreEnvelope(final UUID courtCentreId, final UUID systemDocGeneratorId) {
        final NotifyCourtRegister generateCourtRegister = NotifyCourtRegister.notifyCourtRegister()
                .withCourtCentreId(courtCentreId).withSystemDocGeneratorId(systemDocGeneratorId).build();
        final JsonEnvelope requestEnvelope = JsonEnvelope.envelopeFrom(
                metadataWithRandomUUID("progression.notify-court-register").withUserId(UUID.randomUUID().toString()),
                createObjectBuilder().build());
        return Enveloper.envelop(generateCourtRegister)
                .withName("progression.command.notify-court-register")
                .withMetadataFrom(requestEnvelope);
    }

    private Envelope prepareGenerateCourtRegisterByDateEnvelope() {
        final GenerateCourtRegisterByDate generateCourtRegisterByDate = GenerateCourtRegisterByDate.generateCourtRegisterByDate().withRegisterDate(LocalDate.now().toString()).build();
        final JsonEnvelope requestEnvelope = JsonEnvelope.envelopeFrom(
                metadataWithRandomUUID("progression.generate-court-register-by-date").withUserId(UUID.randomUUID().toString()),
                createObjectBuilder().add("requestDate", LocalDate.now().toString()).build());
        return Enveloper.envelop(generateCourtRegisterByDate)
                .withName("progression.command.generate-court-register")
                .withMetadataFrom(requestEnvelope);
    }

    private void verifyNotifyCourtHandlerResults() throws EventStreamException {
        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.court-register-notified"),
                        JsonEnvelopePayloadMatcher.payload().isJson(anyOf(
                                withJsonPath("courtCentreId", is(notNullValue()))
                                )
                        ))
                )
        );
    }

    private Envelope prepareGenerateCourtRegisterEnvelope() {
        final GenerateCourtRegister generateCourtRegister = GenerateCourtRegister.generateCourtRegister().withRegisterDate(LocalDate.now()).build();
        final JsonEnvelope requestEnvelope = JsonEnvelope.envelopeFrom(
                metadataWithRandomUUID("progression.generate-court-register").withUserId(UUID.randomUUID().toString()),
                createObjectBuilder().build());
        return Enveloper.envelop(generateCourtRegister)
                .withName("progression.command.generate-court-register")
                .withMetadataFrom(requestEnvelope);
    }

    private void verifyCourtRegisterGenerateDocumentResults() throws EventStreamException {
        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.court-register-generated"),
                        JsonEnvelopePayloadMatcher.payload().isJson(anyOf(
                                withJsonPath("$.courtRegisterDocumentRequests.length()", is(greaterThan(0)))
                                )
                        ))

                )
        );
    }

    private void verifyCourtRegisterDocumentRequestHandlerResults() throws EventStreamException {
        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.court-register-recorded"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("courtRegister.courtCentreId", is(COURT_CENTRE_ID.toString()))
                                )
                        ))

                )
        );
    }

    private Envelope<CourtRegisterDocumentRequest> createCourtRegisterDocumentRequestHandlerEnvelope() {
        CourtRegisterDocumentRequest courtRegisterDocumentRequest = CourtRegisterDocumentRequest.
                courtRegisterDocumentRequest().
                withCourtCentreId(COURT_CENTRE_ID).
                build();


        final JsonEnvelope requestEnvelope = JsonEnvelope.envelopeFrom(
                metadataWithRandomUUID("usersgroups.get-user-details").withUserId(COURT_CENTRE_ID.toString()),
                createObjectBuilder().build());


        return Enveloper.envelop(courtRegisterDocumentRequest)
                .withName("usersgroups.get-user-details")
                .withMetadataFrom(requestEnvelope);
    }
}
