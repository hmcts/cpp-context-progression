package uk.gov.moj.cpp.progression.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.codehaus.groovy.runtime.InvokerHelper.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.courtRegisterDocument.CourtRegisterCaseOrApplication.courtRegisterCaseOrApplication;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.progression.domain.helper.CourtRegisterHelper.getCourtRegisterStreamId;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtRegisterRecorded;
import uk.gov.justice.core.courts.courtRegisterDocument.CourtRegisterDefendant;
import uk.gov.justice.core.courts.courtRegisterDocument.CourtRegisterDocumentRequest;
import uk.gov.justice.core.courts.courtRegisterDocument.CourtRegisterRecipient;
import uk.gov.justice.progression.courts.CourtRegisterGenerated;
import uk.gov.justice.progression.courts.CourtRegisterNotificationIgnored;
import uk.gov.justice.progression.courts.CourtRegisterNotified;
import uk.gov.justice.progression.courts.CourtRegisterNotifiedV2;
import uk.gov.justice.progression.courts.GenerateCourtRegister;
import uk.gov.justice.progression.courts.NotifyCourtRegister;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
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
import uk.gov.justice.services.test.utils.framework.api.JsonObjectConvertersFactory;
import uk.gov.moj.cpp.progression.aggregate.ApplicationAggregate;
import uk.gov.moj.cpp.progression.aggregate.CourtCentreAggregate;
import uk.gov.moj.cpp.progression.command.GenerateCourtRegisterByDate;
import uk.gov.moj.cpp.progression.test.FileUtil;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CourtRegisterHandlerTest {
    private static final String ADD_COURT_REGISTER_COMMAND_NAME = "progression.command.add-court-register";
    private static final UUID COURT_CENTRE_ID = randomUUID();
    private static final ZonedDateTime REGISTER_DATE = ZonedDateTime.parse("2024-10-24T22:23:12.414Z");
    private static final UUID APPLICATION_ID = randomUUID();
    private static final UUID MASTER_DEFENDANT_ID = randomUUID();

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;
    @Mock
    private ApplicationAggregate applicationAggregate;
    @Mock
    private EventStream applicationEventStream;

    @InjectMocks
    private CourtRegisterHandler courtRegisterHandler;

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private Requester requester;

    @Spy
    private final JsonObjectToObjectConverter jsonToObjectConverter = new JsonObjectConvertersFactory().jsonObjectToObjectConverter();

    @Spy
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new JsonObjectConvertersFactory().objectToJsonObjectConverter();

    @Spy
    private final StringToJsonObjectConverter stringToJsonObjectConverter = new JsonObjectConvertersFactory().stringToJsonObjectConverter();
    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(CourtRegisterRecorded.class, CourtRegisterGenerated.class, CourtRegisterNotified.class, CourtRegisterNotifiedV2.class, CourtRegisterNotificationIgnored.class);
    @Captor
    private ArgumentCaptor<UUID> courtRegisterStreamIdCaptor;


    @Test
    public void shouldHandleCommand() {
        assertThat(courtRegisterHandler, isHandler(COMMAND_HANDLER)
                .with(method("handleAddCourtRegister")
                        .thatHandles(ADD_COURT_REGISTER_COMMAND_NAME)));
    }

    @Test
    public void shouldProcessCommand() throws Exception {
        final JsonObject jsonObject = FileUtil.jsonFromString(FileUtil
                .getPayload("json/progression.event.court-application-for-applicant.json")
                .replaceAll("%APPLICATION_ID%", APPLICATION_ID.toString()));

        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(applicationAggregate);

        when(eventSource.getStreamById(getCourtRegisterStreamId(COURT_CENTRE_ID.toString(), REGISTER_DATE.toLocalDate().toString()))).thenReturn(eventStream);

        final Envelope<CourtRegisterDocumentRequest> envelope = createCourtRegisterDocumentRequestHandlerEnvelope(createCourtRegisterDocumentRequest());

        courtRegisterHandler.handleAddCourtRegister(envelope);

        verifyCourtRegisterDocumentRequestHandlerResults();
    }

    @Test
    public void shouldHandleGenerateRequest() throws EventStreamException {
        final Envelope<GenerateCourtRegister> generateCourtRegisterEnvelope = prepareGenerateCourtRegisterEnvelope();
        final UUID courtCentreId = randomUUID();
        final ZonedDateTime registerDate = ZonedDateTime.parse("2024-10-24T22:23:12.414Z");
        final JsonEnvelope queryEnvelope = mock(JsonEnvelope.class);
        final CourtRegisterDocumentRequest courtRegisterDocumentRequest = getCourtRegisterDocumentRequest(courtCentreId, registerDate);
        final JsonArray jsonValues = JsonObjects.createArrayBuilder().add(JsonObjects.createObjectBuilder()
                .add("courtCentreId", courtCentreId.toString())
                .add("registerDate", registerDate.toLocalDate().toString())
                .add("payload", objectToJsonObjectConverter.convert(courtRegisterDocumentRequest).toString())
                .build()).build();
        final JsonObject jsonObject = JsonObjects.createObjectBuilder().add("courtRegisterDocumentRequests", jsonValues).build();
        when(queryEnvelope.payloadAsJsonObject()).thenReturn(jsonObject);
        when(requester.request(any(Envelope.class))).thenReturn(queryEnvelope);

        when(eventSource.getStreamById(any())).thenReturn(eventStream);

        courtRegisterHandler.handleGenerateCourtRegister(generateCourtRegisterEnvelope);
        verifyCourtRegisterGenerateDocumentResults();
    }

    @Test
    public void shouldHandleGenerateRequestWhenMultipleCourtCentreRequestsInDifferentRegisterDate() throws EventStreamException {
        final Envelope<GenerateCourtRegister> generateCourtRegisterEnvelope = prepareGenerateCourtRegisterEnvelope();
        final UUID courtCentreId = randomUUID();
        final ZonedDateTime registerDate1 = ZonedDateTime.parse("2024-10-24T22:23:12.414Z");
        final ZonedDateTime registerDate2 = ZonedDateTime.parse("2024-10-25T12:03:12.414Z");

        final CourtRegisterDocumentRequest courtRegisterDocumentRequest1 = getCourtRegisterDocumentRequest(courtCentreId, registerDate1);
        final CourtRegisterDocumentRequest courtRegisterDocumentRequest2 = getCourtRegisterDocumentRequest(courtCentreId, registerDate2);
        final JsonArray jsonValues = JsonObjects.createArrayBuilder()
                .add(JsonObjects.createObjectBuilder()
                        .add("courtCentreId", courtCentreId.toString())
                        .add("registerDate", registerDate1.toLocalDate().toString())
                        .add("payload", objectToJsonObjectConverter.convert(courtRegisterDocumentRequest1).toString())
                        .build())
                .add(JsonObjects.createObjectBuilder()
                        .add("courtCentreId", courtCentreId.toString())
                        .add("registerDate", registerDate2.toLocalDate().toString())
                        .add("payload", objectToJsonObjectConverter.convert(courtRegisterDocumentRequest2).toString())
                        .build())
                .build();
        final JsonObject jsonObject = JsonObjects.createObjectBuilder().add("courtRegisterDocumentRequests", jsonValues).build();

        final JsonEnvelope queryEnvelope = mock(JsonEnvelope.class);
        when(queryEnvelope.payloadAsJsonObject()).thenReturn(jsonObject);
        when(requester.request(any(Envelope.class))).thenReturn(queryEnvelope);

        final EventStream eventStream1 = mock(EventStream.class);
        final EventStream eventStream2 = mock(EventStream.class);

        when(eventSource.getStreamById(courtRegisterStreamIdCaptor.capture())).thenReturn(eventStream1, eventStream2);

        courtRegisterHandler.handleGenerateCourtRegister(generateCourtRegisterEnvelope);
        assertStreamEnvelopeForCourtRegisterGenerated(verifyAppendAndGetArgumentFrom(eventStream1), 1);
        assertStreamEnvelopeForCourtRegisterGenerated(verifyAppendAndGetArgumentFrom(eventStream2), 1);

        //assert streamIds generated are unique
        final List<UUID> courtRegisterStreamIdList = courtRegisterStreamIdCaptor.getAllValues();
        assertThat(courtRegisterStreamIdList.size(), is(2));
        assertThat(courtRegisterStreamIdList.get(0).equals(courtRegisterStreamIdList.get(1)), is(false));
    }

    @Test
    public void shouldHandleGenerateRequestWhenMultipleCourtCentreRequestsInSameRegisterDate() throws EventStreamException {
        final Envelope<GenerateCourtRegister> generateCourtRegisterEnvelope = prepareGenerateCourtRegisterEnvelope();
        final UUID courtCentreId = randomUUID();
        final ZonedDateTime registerDate = ZonedDateTime.parse("2024-10-24T22:23:12.414Z");

        final CourtRegisterDocumentRequest courtRegisterDocumentRequest1 = getCourtRegisterDocumentRequest(courtCentreId, registerDate);
        final CourtRegisterDocumentRequest courtRegisterDocumentRequest2 = getCourtRegisterDocumentRequest(courtCentreId, registerDate);
        final JsonArray jsonValues = JsonObjects.createArrayBuilder()
                .add(JsonObjects.createObjectBuilder()
                        .add("courtCentreId", courtCentreId.toString())
                        .add("registerDate", registerDate.toLocalDate().toString())
                        .add("payload", objectToJsonObjectConverter.convert(courtRegisterDocumentRequest1).toString())
                        .build())
                .add(JsonObjects.createObjectBuilder()
                        .add("courtCentreId", courtCentreId.toString())
                        .add("registerDate", registerDate.toLocalDate().toString())
                        .add("payload", objectToJsonObjectConverter.convert(courtRegisterDocumentRequest2).toString())
                        .build())
                .build();
        final JsonObject jsonObject = JsonObjects.createObjectBuilder().add("courtRegisterDocumentRequests", jsonValues).build();

        final JsonEnvelope queryEnvelope = mock(JsonEnvelope.class);
        when(queryEnvelope.payloadAsJsonObject()).thenReturn(jsonObject);
        when(requester.request(any(Envelope.class))).thenReturn(queryEnvelope);

        when(eventSource.getStreamById(courtRegisterStreamIdCaptor.capture())).thenReturn(eventStream);

        courtRegisterHandler.handleGenerateCourtRegister(generateCourtRegisterEnvelope);
        assertThat(verifyAppendAndGetArgumentFrom(eventStream), streamContaining(
                        jsonEnvelope(
                                metadata()
                                        .withName("progression.event.court-register-generated"),
                                JsonEnvelopePayloadMatcher.payload().isJson(anyOf(
                                                withJsonPath("$.courtRegisterDocumentRequests.length()", is(equalTo(2)))
                                        )
                                ))
                )
        );

        //assert streamIds generated are unique
        final List<UUID> courtRegisterStreamIdList = courtRegisterStreamIdCaptor.getAllValues();
        assertThat(courtRegisterStreamIdList.size(), is(1));
    }

    private static CourtRegisterDocumentRequest getCourtRegisterDocumentRequest(final UUID courtCentreId, final ZonedDateTime registerDate) {
        return new CourtRegisterDocumentRequest.Builder()
                .withCourtCentreId(courtCentreId).withRegisterDate(registerDate).build();
    }

    @Test
    public void shouldHandleNotifyCourtCentre() throws EventStreamException {
        final UUID courtCentreId = randomUUID();
        final UUID systemDocGeneratorId = randomUUID();
        final ZonedDateTime registerDate = ZonedDateTime.parse("2024-10-25T22:23:12.414Z");

        final CourtRegisterRecipient recipient = CourtRegisterRecipient.courtRegisterRecipient().withRecipientName("John").build();

        final CourtCentreAggregate courtCentreAggregate = new CourtCentreAggregate();
        courtCentreAggregate.apply(new CourtRegisterRecorded(courtCentreId, CourtRegisterDocumentRequest.courtRegisterDocumentRequest()
                .withCourtCentreId(courtCentreId)
                .withRegisterDate(registerDate)
                .build()));
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CourtCentreAggregate.class)).thenReturn(courtCentreAggregate);

        courtCentreAggregate.setCourtRegisterRecipients(Collections.singletonList(recipient));
        final UUID courtRegisterId = getCourtRegisterStreamId(courtCentreId.toString(), registerDate.toLocalDate().toString());
        final Envelope<NotifyCourtRegister> notifyCourtRegisterEnvelope = prepareNotifyCourtCentreEnvelope(courtRegisterId, systemDocGeneratorId);

        courtRegisterHandler.handleNotifyCourtCentre(notifyCourtRegisterEnvelope);
        verifyNotifyCourtHandlerResults();
    }

    @Test
    public void shouldHandleGenerateCourtRegisterByDateRequest() throws EventStreamException {
        final Envelope<GenerateCourtRegisterByDate> generateCourtRegisterEnvelope = prepareGenerateCourtRegisterByDateEnvelope();
        final UUID courtCentreId = randomUUID();
        final ZonedDateTime registerDate = ZonedDateTime.parse("2024-10-24T22:23:12.414Z");
        final JsonEnvelope queryEnvelope = mock(JsonEnvelope.class);
        final CourtRegisterDocumentRequest courtRegisterDocumentRequest = getCourtRegisterDocumentRequest(courtCentreId, registerDate);
        final JsonArray jsonValues = JsonObjects.createArrayBuilder().add(JsonObjects.createObjectBuilder()
                .add("courtCentreId", courtCentreId.toString())
                .add("registerDate", registerDate.toLocalDate().toString())
                .add("payload", objectToJsonObjectConverter.convert(courtRegisterDocumentRequest).toString())
                .build()).build();
        final JsonObject jsonObject = JsonObjects.createObjectBuilder().add("courtRegisterDocumentRequests", jsonValues).build();
        when(queryEnvelope.payloadAsJsonObject()).thenReturn(jsonObject);
        when(requester.request(any(Envelope.class))).thenReturn(queryEnvelope);

        when(eventSource.getStreamById(any())).thenReturn(eventStream);

        courtRegisterHandler.handleGenerateCourtRegisterByDate(generateCourtRegisterEnvelope);
        verifyCourtRegisterGenerateDocumentResults();

    }

    @Test
    public void shouldGetRecordedEventForApplicant() throws Exception {
        final JsonObject jsonObject = FileUtil.jsonFromString(FileUtil
                .getPayload("json/progression.event.court-application-for-applicant.json")
                .replaceAll("%APPLICATION_ID%", APPLICATION_ID.toString()));

        final CourtApplication courtApplication = jsonToObjectConverter.convert(jsonObject, CourtApplication.class);
        when(applicationAggregate.getCourtApplication()).thenReturn(courtApplication);

        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(applicationAggregate);

        when(eventSource.getStreamById(getCourtRegisterStreamId(COURT_CENTRE_ID.toString(), REGISTER_DATE.toLocalDate().toString()))).thenReturn(eventStream);

        courtRegisterHandler.handleAddCourtRegister(createCourtRegisterDocumentRequestHandlerEnvelope(createCourtRegisterDocumentRequest()));

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata().withName("progression.event.court-register-recorded"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.courtCentreId", is(COURT_CENTRE_ID.toString())),
                                withJsonPath("$.courtRegister", notNullValue()),
                                withJsonPath("$.courtRegister.courtCentreId", is(COURT_CENTRE_ID.toString())),
                                withJsonPath("$.courtRegister.defendantType", is("Applicant"))
                        )))));
    }

    @Test
    public void shouldGetRecordedEventForApplicantWhenCourtApplicantoionIsNull() throws Exception {
        final JsonObject jsonObject = FileUtil.jsonFromString(FileUtil
                .getPayload("json/progression.event.court-application-for-applicant.json")
                .replaceAll("%APPLICATION_ID%", APPLICATION_ID.toString()));

        final CourtApplication courtApplication = jsonToObjectConverter.convert(jsonObject, CourtApplication.class);
        when(applicationAggregate.getCourtApplication()).thenReturn(null);

        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(applicationAggregate);

        when(eventSource.getStreamById(getCourtRegisterStreamId(COURT_CENTRE_ID.toString(), REGISTER_DATE.toLocalDate().toString()))).thenReturn(eventStream);

        courtRegisterHandler.handleAddCourtRegister(createCourtRegisterDocumentRequestHandlerEnvelope(createCourtRegisterDocumentRequest()));

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata().withName("progression.event.court-register-recorded"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.courtCentreId", is(COURT_CENTRE_ID.toString())),
                                withJsonPath("$.courtRegister", notNullValue()),
                                withJsonPath("$.courtRegister.courtCentreId", is(COURT_CENTRE_ID.toString())),
                                withJsonPath("$.courtRegister.defendantType", is("Applicant"))
                        )))));
    }

    @Mock
    private CourtApplication courtApplication;
    @Mock
    private CourtApplicationParty courtApplicationParty;

    @Test
    public void shouldGetRecordedEventForApplicantWhenApplicantIsNull() throws Exception {
        when(applicationAggregate.getCourtApplication()).thenReturn(courtApplication);
        when(courtApplication.getApplicant()).thenReturn(null);

        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(applicationAggregate);

        when(eventSource.getStreamById(getCourtRegisterStreamId(COURT_CENTRE_ID.toString(), REGISTER_DATE.toLocalDate().toString()))).thenReturn(eventStream);

        courtRegisterHandler.handleAddCourtRegister(createCourtRegisterDocumentRequestHandlerEnvelope(createCourtRegisterDocumentRequest()));

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata().withName("progression.event.court-register-recorded"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.courtCentreId", is(COURT_CENTRE_ID.toString())),
                                withJsonPath("$.courtRegister", notNullValue()),
                                withJsonPath("$.courtRegister.courtCentreId", is(COURT_CENTRE_ID.toString())),
                                withJsonPath("$.courtRegister.defendantType", is("Applicant"))
                        )))));
    }

    @Test
    public void shouldGetRecordedEventForApplicantWhenMasterDefendantIsNull() throws Exception {
        when(applicationAggregate.getCourtApplication()).thenReturn(courtApplication);
        when(courtApplication.getApplicant()).thenReturn(courtApplicationParty);
        when(courtApplicationParty.getMasterDefendant()).thenReturn(null);

        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(applicationAggregate);

        when(eventSource.getStreamById(getCourtRegisterStreamId(COURT_CENTRE_ID.toString(), REGISTER_DATE.toLocalDate().toString()))).thenReturn(eventStream);

        courtRegisterHandler.handleAddCourtRegister(createCourtRegisterDocumentRequestHandlerEnvelope(createCourtRegisterDocumentRequest()));

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata().withName("progression.event.court-register-recorded"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.courtCentreId", is(COURT_CENTRE_ID.toString())),
                                withJsonPath("$.courtRegister", notNullValue()),
                                withJsonPath("$.courtRegister.courtCentreId", is(COURT_CENTRE_ID.toString())),
                                withJsonPath("$.courtRegister.defendantType", is("Applicant"))
                        )))));
    }

    @Test
    public void shouldGetRecordedEventForAppellant() throws Exception {
        final JsonObject jsonObject = FileUtil.jsonFromString(FileUtil
                .getPayload("json/progression.event.court-application-for-appellant.json")
                .replaceAll("%APPLICATION_ID%", APPLICATION_ID.toString()));

        final CourtApplication courtApplication = jsonToObjectConverter.convert(jsonObject, CourtApplication.class);
        when(applicationAggregate.getCourtApplication()).thenReturn(courtApplication);

        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(applicationAggregate);

        when(eventSource.getStreamById(getCourtRegisterStreamId(COURT_CENTRE_ID.toString(), REGISTER_DATE.toLocalDate().toString()))).thenReturn(eventStream);

        courtRegisterHandler.handleAddCourtRegister(createCourtRegisterDocumentRequestHandlerEnvelope(createCourtRegisterDocumentRequest()));

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata().withName("progression.event.court-register-recorded"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.courtCentreId", is(COURT_CENTRE_ID.toString())),
                                withJsonPath("$.courtRegister", notNullValue()),
                                withJsonPath("$.courtRegister.courtCentreId", is(COURT_CENTRE_ID.toString())),
                                withJsonPath("$.courtRegister.defendantType", is("Appellant"))
                        )))));
    }

    @Test
    public void shouldGetRecordedEventForRespondent() throws Exception {
        final JsonObject jsonObject = FileUtil.jsonFromString(FileUtil
                .getPayload("json/progression.event.court-application-for-respondent.json")
                .replaceAll("%APPLICATION_ID%", APPLICATION_ID.toString())
                .replaceAll("%MASTER_DEFENDANT_ID%", MASTER_DEFENDANT_ID.toString()));

        final CourtApplication courtApplication = jsonToObjectConverter.convert(jsonObject, CourtApplication.class);
        when(applicationAggregate.getCourtApplication()).thenReturn(courtApplication);

        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(applicationAggregate);

        when(eventSource.getStreamById(getCourtRegisterStreamId(COURT_CENTRE_ID.toString(), REGISTER_DATE.toLocalDate().toString()))).thenReturn(eventStream);

        courtRegisterHandler.handleAddCourtRegister(createCourtRegisterDocumentRequestHandlerEnvelope(createCourtRegisterDocumentRequest()));

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata().withName("progression.event.court-register-recorded"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.courtCentreId", is(COURT_CENTRE_ID.toString())),
                                withJsonPath("$.courtRegister", notNullValue()),
                                withJsonPath("$.courtRegister.courtCentreId", is(COURT_CENTRE_ID.toString())),
                                withJsonPath("$.courtRegister.defendantType", is("Respondent"))
                        )))));
    }

    @Test
    public void shouldGetRecordedEventForDefendantWhenApplicationIdIsNull() throws Exception {
        final JsonObject jsonObject = FileUtil.jsonFromString(FileUtil
                .getPayload("json/progression.event.court-application-for-respondent.json")
                .replaceAll("%APPLICATION_ID%", APPLICATION_ID.toString())
                .replaceAll("%MASTER_DEFENDANT_ID%", MASTER_DEFENDANT_ID.toString()));

        final CourtApplication courtApplication = jsonToObjectConverter.convert(jsonObject, CourtApplication.class);

        when(eventSource.getStreamById(getCourtRegisterStreamId(COURT_CENTRE_ID.toString(), REGISTER_DATE.toLocalDate().toString()))).thenReturn(eventStream);


        courtRegisterHandler.handleAddCourtRegister(createCourtRegisterDocumentRequestHandlerEnvelope(createCourtRegisterDocumentRequestWithNullApplicationId()));

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata().withName("progression.event.court-register-recorded"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.courtCentreId", is(COURT_CENTRE_ID.toString())),
                                withJsonPath("$.courtRegister", notNullValue()),
                                withJsonPath("$.courtRegister.courtCentreId", is(COURT_CENTRE_ID.toString())),
                                withJsonPath("$.courtRegister.defendantType", is(""))
                        )))));
    }

    private Envelope prepareNotifyCourtCentreEnvelope(final UUID courtRegisterId, final UUID systemDocGeneratorId) {
        final NotifyCourtRegister generateCourtRegister = NotifyCourtRegister.notifyCourtRegister()
                .withCourtRegisterId(courtRegisterId).withSystemDocGeneratorId(systemDocGeneratorId).build();
        final JsonEnvelope requestEnvelope = JsonEnvelope.envelopeFrom(
                metadataWithRandomUUID("progression.notify-court-register").withUserId(randomUUID().toString()),
                createObjectBuilder().build());
        return Enveloper.envelop(generateCourtRegister)
                .withName("progression.command.notify-court-register")
                .withMetadataFrom(requestEnvelope);
    }

    private Envelope prepareGenerateCourtRegisterByDateEnvelope() {
        final GenerateCourtRegisterByDate generateCourtRegisterByDate = GenerateCourtRegisterByDate.generateCourtRegisterByDate().withRegisterDate(LocalDate.now().toString()).build();
        final JsonEnvelope requestEnvelope = JsonEnvelope.envelopeFrom(
                metadataWithRandomUUID("progression.generate-court-register-by-date").withUserId(randomUUID().toString()),
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
                                        .withName("progression.event.court-register-notified-v2"),
                                JsonEnvelopePayloadMatcher.payload().isJson(anyOf(
                                                withJsonPath("courtCentreId", is(notNullValue())),
                                                withJsonPath("registerDate", is(notNullValue()))
                                        )
                                ))
                )
        );
    }

    private Envelope prepareGenerateCourtRegisterEnvelope() {
        final GenerateCourtRegister generateCourtRegister = GenerateCourtRegister.generateCourtRegister().withRegisterDate(LocalDate.now()).build();
        final JsonEnvelope requestEnvelope = JsonEnvelope.envelopeFrom(
                metadataWithRandomUUID("progression.generate-court-register").withUserId(randomUUID().toString()),
                createObjectBuilder().build());
        return Enveloper.envelop(generateCourtRegister)
                .withName("progression.command.generate-court-register")
                .withMetadataFrom(requestEnvelope);
    }

    private void verifyCourtRegisterGenerateDocumentResults() throws EventStreamException {
        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        assertStreamEnvelopeForCourtRegisterGenerated(envelopeStream, 1);
    }

    private static void assertStreamEnvelopeForCourtRegisterGenerated(final Stream<JsonEnvelope> envelopeStream, final int requestCount) {
        assertThat(envelopeStream, streamContaining(
                        jsonEnvelope(
                                metadata()
                                        .withName("progression.event.court-register-generated"),
                                JsonEnvelopePayloadMatcher.payload().isJson(anyOf(
                                                withJsonPath("$.courtRegisterDocumentRequests.length()", is(equalTo(requestCount)))
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

    private Envelope<CourtRegisterDocumentRequest> createCourtRegisterDocumentRequestHandlerEnvelope(CourtRegisterDocumentRequest courtRegisterDocumentRequest) {
        final JsonEnvelope requestEnvelope = JsonEnvelope.envelopeFrom(
                metadataWithRandomUUID("usersgroups.get-user-details").withUserId(COURT_CENTRE_ID.toString()),
                createObjectBuilder().build());

        return Enveloper.envelop(courtRegisterDocumentRequest)
                .withName("usersgroups.get-user-details")
                .withMetadataFrom(requestEnvelope);
    }

    private CourtRegisterDocumentRequest createCourtRegisterDocumentRequest() {
        return CourtRegisterDocumentRequest.
                courtRegisterDocumentRequest()
                .withCourtCentreId(COURT_CENTRE_ID)
                .withRegisterDate(REGISTER_DATE)
                .withDefendants(Arrays.asList(CourtRegisterDefendant.courtRegisterDefendant().withMasterDefendantId(MASTER_DEFENDANT_ID)
                                .withProsecutionCasesOrApplications(asList(courtRegisterCaseOrApplication()
                                        .withCourtApplicationId(randomUUID())
                                        .build())).build(),
                        CourtRegisterDefendant.courtRegisterDefendant().withMasterDefendantId(randomUUID()).build()))
                .build();
    }

    private CourtRegisterDocumentRequest createCourtRegisterDocumentRequestWithNullApplicationId() {
        return CourtRegisterDocumentRequest.
                courtRegisterDocumentRequest()
                .withCourtCentreId(COURT_CENTRE_ID)
                .withRegisterDate(REGISTER_DATE)
                .withCourtApplicationId(null)
                .withDefendants(Arrays.asList(CourtRegisterDefendant.courtRegisterDefendant().withMasterDefendantId(MASTER_DEFENDANT_ID).build(),
                        CourtRegisterDefendant.courtRegisterDefendant().withMasterDefendantId(randomUUID()).build()))
                .build();
    }
}
