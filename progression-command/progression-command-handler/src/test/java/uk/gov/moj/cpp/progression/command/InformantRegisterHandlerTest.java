package uk.gov.moj.cpp.progression.command;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.core.courts.InformantRegisterRecorded;
import uk.gov.justice.core.courts.informantRegisterDocument.InformantRegisterDocumentRequest;
import uk.gov.justice.core.courts.informantRegisterDocument.InformantRegisterRecipient;
import uk.gov.justice.progression.courts.GenerateInformantRegister;
import uk.gov.justice.progression.courts.InformantRegisterGenerated;
import uk.gov.justice.progression.courts.InformantRegisterNotificationIgnored;
import uk.gov.justice.progression.courts.InformantRegisterNotified;
import uk.gov.justice.progression.courts.NotifyInformantRegister;
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
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher;
import uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil;
import uk.gov.moj.cpp.progression.aggregate.ProsecutionAuthorityAggregate;
import uk.gov.moj.cpp.progression.handler.InformantRegisterHandler;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import java.util.stream.Stream;

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
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

@RunWith(MockitoJUnitRunner.class)
public class InformantRegisterHandlerTest {

    private static final String ADD_INFORMANT_REGISTER_COMMAND_NAME = "progression.command.add-informant-register";
    private static final UUID PROSECUTION_AUTHORITY_ID = randomUUID();

    @Mock
    private EventSource eventSource;

    @Mock
    private Requester requester;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @InjectMocks
    private InformantRegisterHandler informantRegisterHandler;

    private ProsecutionAuthorityAggregate aggregate;

    @Spy
    private final JsonObjectToObjectConverter jsonToObjectConverter = new JsonObjectToObjectConverter();

    @Spy
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter();

    @Spy
    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(InformantRegisterRecorded.class, InformantRegisterGenerated.class, InformantRegisterNotified.class, InformantRegisterNotificationIgnored.class);

    @Before
    public void setup() {
        aggregate = new ProsecutionAuthorityAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ProsecutionAuthorityAggregate.class)).thenReturn(aggregate);
        ReflectionUtil.setField(this.jsonToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldHandleCommand() {
        assertThat(new InformantRegisterHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handleAddInformantRegisterToEventStream")
                        .thatHandles("progression.command.add-informant-register")
                ));
    }

    @Test
    public void shouldProcessCommand() throws Exception {

        informantRegisterHandler.handleAddInformantRegisterToEventStream(buildEnvelope());

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata().withName("progression.event.informant-register-recorded"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.prosecutionAuthorityId", is(PROSECUTION_AUTHORITY_ID.toString())),
                                withJsonPath("$.informantRegister", notNullValue())
                                )
                        ))
                )
        );
    }

    @Test
    public void generateInformantRegister() throws EventStreamException {
        final Envelope<GenerateInformantRegister> generateInformantRegisterEnvelope = prepareEnvelope();
        final UUID prosecutionAuthorityId = UUID.randomUUID();
        final JsonEnvelope queryEnvelope = mock(JsonEnvelope.class);
        final InformantRegisterDocumentRequest informantRegisterDocumentRequest = InformantRegisterDocumentRequest.informantRegisterDocumentRequest().withProsecutionAuthorityId(prosecutionAuthorityId).build();
        final JsonArray jsonValues = Json.createArrayBuilder().add(Json.createObjectBuilder().add("prosecutionAuthorityId", PROSECUTION_AUTHORITY_ID.toString())
                .add("payload", objectToJsonObjectConverter.convert(informantRegisterDocumentRequest).toString())
                .build()).build();
        final JsonObject jsonObject = Json.createObjectBuilder().add("informantRegisterDocumentRequests", jsonValues).build();
        when(queryEnvelope.payloadAsJsonObject()).thenReturn(jsonObject);
        when(requester.request(any(Envelope.class))).thenReturn(queryEnvelope);
        informantRegisterHandler.handleGenerateInformantRegister(generateInformantRegisterEnvelope);
        verifyInformantRegisterDocumentRequestHandlerResults();

    }

    @Test
    public void notifyInformantRegister() throws EventStreamException {
        final UUID materialId = randomUUID();
        final Envelope<NotifyInformantRegister> notifyInformantRegisterEnvelope = prepareNotificationEnvelope(materialId);

        final InformantRegisterRecipient recipient = InformantRegisterRecipient.informantRegisterRecipient().withRecipientName("John").build();
        aggregate.setInformantRegisterRecipients(Collections.singletonList(recipient));
        informantRegisterHandler.handleNotifyInformantRegister(notifyInformantRegisterEnvelope);
        verifyNotifyInformantRegisterDocumentRequestHandlerResults();
    }

    @Test
    public void generateInformantRegisterByDate() throws EventStreamException {
        final Envelope<GenerateInformantRegisterByDate> generateInformantRegisterEnvelope = prepareGenerateInformantRegisterByDateEnvelope();
        final UUID prosecutionAuthorityId = UUID.randomUUID();
        final JsonEnvelope queryEnvelope = mock(JsonEnvelope.class);
        final InformantRegisterDocumentRequest informantRegisterDocumentRequest = InformantRegisterDocumentRequest.informantRegisterDocumentRequest().withProsecutionAuthorityId(prosecutionAuthorityId).build();
        final JsonArray jsonValues = Json.createArrayBuilder().add(Json.createObjectBuilder().add("prosecutionAuthorityId", PROSECUTION_AUTHORITY_ID.toString())
                .add("payload", objectToJsonObjectConverter.convert(informantRegisterDocumentRequest).toString())
                .build()).build();
        final JsonObject jsonObject = Json.createObjectBuilder().add("informantRegisterDocumentRequests", jsonValues).build();
        when(queryEnvelope.payloadAsJsonObject()).thenReturn(jsonObject);
        when(requester.request(any(Envelope.class))).thenReturn(queryEnvelope);
        informantRegisterHandler.handleGenerateInformantRegisterByDate(generateInformantRegisterEnvelope);
        verifyInformantRegisterDocumentRequestHandlerResults();

    }


    private Envelope prepareNotificationEnvelope(final UUID fileId) {
        final NotifyInformantRegister notifyInformantRegister = NotifyInformantRegister.notifyInformantRegister()
                .withProsecutionAuthorityId(PROSECUTION_AUTHORITY_ID)
                .withFileId(fileId)
                .withTemplateId("template Id")
                .build();
        final JsonEnvelope requestEnvelope = JsonEnvelope.envelopeFrom(
                metadataWithRandomUUID("progression.notify-informant-register").withUserId(UUID.randomUUID().toString()),
                createObjectBuilder().build());
        return Enveloper.envelop(notifyInformantRegister)
                .withName("progression.command.notify-informant-register")
                .withMetadataFrom(requestEnvelope);
    }

    private void verifyNotifyInformantRegisterDocumentRequestHandlerResults() throws EventStreamException {
        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.informant-register-notified"),
                        JsonEnvelopePayloadMatcher.payload().isJson(anyOf(
                                withJsonPath("prosecutionAuthorityId", is(PROSECUTION_AUTHORITY_ID.toString()))
                                )
                        ))
                )
        );
    }

    private Envelope prepareEnvelope() {
        final GenerateInformantRegister generateInformantRegister = GenerateInformantRegister.generateInformantRegister().withRegisterDate(LocalDate.now()).build();
        final JsonEnvelope requestEnvelope = JsonEnvelope.envelopeFrom(
                metadataWithRandomUUID("progression.generate-informant-register").withUserId(UUID.randomUUID().toString()),
                createObjectBuilder().add("registerDate", LocalDate.now().toString()).build());
        return Enveloper.envelop(generateInformantRegister)
                .withName("progression.command.generate-informant-register")
                .withMetadataFrom(requestEnvelope);
    }

    private Envelope prepareGenerateInformantRegisterByDateEnvelope() {
        final GenerateInformantRegisterByDate generateInformantRegister = GenerateInformantRegisterByDate
                .generateInformantRegisterByDate()
                .withRegisterDate(LocalDate.now().toString())
                .withProsecutionAuthorities(Arrays.asList("TFL", "TVL"))
                .build();
        final JsonEnvelope requestEnvelope = JsonEnvelope.envelopeFrom(
                metadataWithRandomUUID("progression.generate-informant-register-by-date").withUserId(UUID.randomUUID().toString()),
                createObjectBuilder().add("registerDate", LocalDate.now().toString())
                        .add("prosecutionAuthorityCode", Arrays.asList("TFL", "TVL").toString()).build());
        return Enveloper.envelop(generateInformantRegister)
                .withName("progression.command.generate-informant-register-by-date")
                .withMetadataFrom(requestEnvelope);
    }

    private void verifyInformantRegisterDocumentRequestHandlerResults() throws EventStreamException {
        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.informant-register-generated"),
                        JsonEnvelopePayloadMatcher.payload().isJson(anyOf(
                                withJsonPath("$.informantRegisterDocumentRequests.length()", is(greaterThan(0)))
                                )
                        ))

                )
        );
    }

    private Envelope<InformantRegisterDocumentRequest> buildEnvelope() {

        final InformantRegisterDocumentRequest informantRegisterDocumentRequest = InformantRegisterDocumentRequest.informantRegisterDocumentRequest()
                .withProsecutionAuthorityId(PROSECUTION_AUTHORITY_ID)
                .build();

        return envelope(ADD_INFORMANT_REGISTER_COMMAND_NAME, informantRegisterDocumentRequest);
    }

    private <T> Envelope<T> envelope(final String name, final T t) {
        final MetadataBuilder metadataBuilder = metadataFrom(metadataWithRandomUUID(name).withUserId(randomUUID().toString()).build());
        return envelopeFrom(metadataBuilder, t);
    }
}
