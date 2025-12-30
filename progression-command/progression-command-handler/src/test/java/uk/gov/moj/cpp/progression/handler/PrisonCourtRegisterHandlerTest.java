package uk.gov.moj.cpp.progression.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
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
import static uk.gov.moj.cpp.progression.domain.helper.CourtRegisterHelper.getPrisonCourtRegisterStreamId;

import uk.gov.justice.core.courts.PrisonCourtRegisterFailed;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.PrisonCourtRegisterGenerated;
import uk.gov.justice.core.courts.PrisonCourtRegisterGeneratedV2;
import uk.gov.justice.core.courts.PrisonCourtRegisterRecorded;
import uk.gov.justice.core.courts.PrisonCourtRegisterSent;
import uk.gov.justice.core.courts.PrisonCourtRegisterWithoutRecipientsRecorded;
import uk.gov.justice.core.courts.prisonCourtRegisterDocument.PrisonCourtRegisterCaseOrApplication;
import uk.gov.justice.core.courts.prisonCourtRegisterDocument.PrisonCourtRegisterDefendant;
import uk.gov.justice.core.courts.RecordPrisonCourtRegisterDocumentSent;
import uk.gov.justice.core.courts.RecordPrisonCourtRegisterFailed;
import uk.gov.justice.core.courts.prisonCourtRegisterDocument.PrisonCourtRegisterDocumentRequest;
import uk.gov.justice.core.courts.prisonCourtRegisterDocument.PrisonCourtRegisterRecipient;
import uk.gov.justice.progression.courts.NotifyPrisonCourtRegister;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher;
import uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil;
import uk.gov.moj.cpp.progression.aggregate.ApplicationAggregate;
import uk.gov.moj.cpp.progression.aggregate.CourtCentreAggregate;
import uk.gov.moj.cpp.progression.test.FileUtil;

import javax.json.JsonObject;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PrisonCourtRegisterHandlerTest {

    private static final String ADD_PRISON_COURT_REGISTER_COMMAND_NAME = "progression.command.add-prison-court-register";
    private static final String NOTIFY_PRISON_COURT_REGISTER_COMMAND_NAME = "progression.command.notify-prison-court-register";
    private static final String RECORD_PRISON_COURT_REGISTER_SENT_COMMAND_NAME = "progression.command.record-prison-court-register-sent";
    private static final String PRISON_COURT_REGISTER_FAILED_COMMAND_NAME = "progression.command.record-prison-court-register-failed";
    private static final UUID COURT_CENTRE_ID = randomUUID();
    private static final UUID PAYLOAD_FILE_ID = randomUUID();
    private static final ZonedDateTime HEARING_DATE = ZonedDateTime.parse("2024-10-24T22:23:12.414Z");
    private static final UUID FILE_ID = randomUUID();
    private static final UUID APPLICATION_ID = randomUUID();
    private static final UUID MASTER_DEFENDANT_ID = randomUUID();
    private static final UUID ID = randomUUID();

    private static final UUID SYSTEM_DOCUMENT_ID = randomUUID();

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
    private PrisonCourtRegisterHandler prisonCourtRegisterHandler;

    private static CourtCentreAggregate aggregator;
    private static final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    private static final JsonObjectToObjectConverter jsonToObjectConverter = new JsonObjectToObjectConverter(objectMapper);

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            PrisonCourtRegisterRecorded.class,
            PrisonCourtRegisterWithoutRecipientsRecorded.class,
            PrisonCourtRegisterGenerated.class,
            PrisonCourtRegisterGeneratedV2.class,
            PrisonCourtRegisterSent.class,
            PrisonCourtRegisterFailed.class);

    @BeforeAll
    public static void setup() {
        ReflectionUtil.setField(jsonToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldHandleCommand() {
        assertThat(new PrisonCourtRegisterHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handleAddPrisonCourtRegister")
                        .thatHandles(ADD_PRISON_COURT_REGISTER_COMMAND_NAME)
                ));
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

        final CourtCentreAggregate courtCentreAggregate = new CourtCentreAggregate();
        when(eventSource.getStreamById(getPrisonCourtRegisterStreamId(COURT_CENTRE_ID.toString(), HEARING_DATE.toLocalDate().toString()))).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CourtCentreAggregate.class)).thenReturn(courtCentreAggregate);

        prisonCourtRegisterHandler.handleAddPrisonCourtRegister(buildEnvelope(new PrisonCourtRegisterRecipient("emailAddress1", null, "emailTemplate", "recipientName")));

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata().withName("progression.event.prison-court-register-recorded"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.courtCentreId", is(COURT_CENTRE_ID.toString())),
                                withJsonPath("$.prisonCourtRegister", notNullValue()),
                                withJsonPath("$.prisonCourtRegister.courtCentreId", is(COURT_CENTRE_ID.toString())),
                                withJsonPath("$.prisonCourtRegister.defendant.prosecutionCasesOrApplications[0].courtApplicationId", is(APPLICATION_ID.toString())),
                                withJsonPath("$.defendantType", is("Applicant"))
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

        final CourtCentreAggregate courtCentreAggregate = new CourtCentreAggregate();
        when(eventSource.getStreamById(getPrisonCourtRegisterStreamId(COURT_CENTRE_ID.toString(), HEARING_DATE.toLocalDate().toString()))).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CourtCentreAggregate.class)).thenReturn(courtCentreAggregate);

        prisonCourtRegisterHandler.handleAddPrisonCourtRegister(buildEnvelope(new PrisonCourtRegisterRecipient("emailAddress1", null, "emailTemplate", "recipientName")));

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata().withName("progression.event.prison-court-register-recorded"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.courtCentreId", is(COURT_CENTRE_ID.toString())),
                                withJsonPath("$.prisonCourtRegister", notNullValue()),
                                withJsonPath("$.prisonCourtRegister.courtCentreId", is(COURT_CENTRE_ID.toString())),
                                withJsonPath("$.prisonCourtRegister.defendant.prosecutionCasesOrApplications[0].courtApplicationId", is(APPLICATION_ID.toString())),
                                withJsonPath("$.defendantType", is("Appellant"))
                        )))));
    }

    @Test
    public void shouldCreateRegisterGeneratedEvents() throws Exception {
        aggregator = new CourtCentreAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CourtCentreAggregate.class)).thenReturn(aggregator);

        prisonCourtRegisterHandler.recordPrisonCourtRegisterDocumentSent(buildRecordPrisonCourtRegisterSentEnvelope());

        prisonCourtRegisterHandler.handleNotifyCourtCentre(buildRecordRegisterGeneratedEnvelope());

        ArgumentCaptor<Stream> argumentCaptor = ArgumentCaptor.forClass(Stream.class);

        Mockito.verify(eventStream, times(2)).append(argumentCaptor.capture());
        Stream<JsonEnvelope> envelopeStream0 = argumentCaptor.getValue();
        JsonEnvelopeMatcher matcher1 = jsonEnvelope(
                metadata().withName("progression.event.prison-court-register-generated"),
                JsonEnvelopePayloadMatcher.payload().isJson(
                        allOf(withJsonPath("$.courtCentreId", is(COURT_CENTRE_ID.toString())),
                                withJsonPath("$.fileId", is(SYSTEM_DOCUMENT_ID.toString())))));
        JsonEnvelopeMatcher matcher2 = jsonEnvelope(
                metadata().withName("progression.event.prison-court-register-generated-v2"),
                JsonEnvelopePayloadMatcher.payload().isJson(
                        allOf(withJsonPath("$.courtCentreId", is(COURT_CENTRE_ID.toString())),
                                withJsonPath("$.fileId", is(SYSTEM_DOCUMENT_ID.toString())))));
        assertThat(envelopeStream0, streamContaining(matcher1, matcher2));
    }

    @Test
    public void shouldRecordPrisonCourtRegisterDocumentSent() throws Exception {

        aggregator = new CourtCentreAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CourtCentreAggregate.class)).thenReturn(aggregator);

        prisonCourtRegisterHandler.recordPrisonCourtRegisterDocumentSent(buildRecordPrisonCourtRegisterSentEnvelope());

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata().withName("progression.event.prison-court-register-sent"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.courtCentreId", is(COURT_CENTRE_ID.toString())),
                                withJsonPath("$.payloadFileId", is(PAYLOAD_FILE_ID.toString()))
                        )))));
    }

    @Test
    public void shouldHandlePrisonCourtRegisterFailed() throws Exception {

        aggregator = new CourtCentreAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CourtCentreAggregate.class)).thenReturn(aggregator);

        prisonCourtRegisterHandler.handlePrisonCourtRegisterFailed(buildPrisonCourtRegisterFailedEnvelope());

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata().withName("progression.event.prison-court-register-failed"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.courtCentreId", is(COURT_CENTRE_ID.toString())),
                                withJsonPath("$.payloadFileId", is(PAYLOAD_FILE_ID.toString())),
                                withJsonPath("$.reason", is("Test"))
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

        final CourtCentreAggregate courtCentreAggregate = new CourtCentreAggregate();
        when(eventSource.getStreamById(getPrisonCourtRegisterStreamId(COURT_CENTRE_ID.toString(), HEARING_DATE.toLocalDate().toString()))).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CourtCentreAggregate.class)).thenReturn(courtCentreAggregate);

        prisonCourtRegisterHandler.handleAddPrisonCourtRegister(buildEnvelope(new PrisonCourtRegisterRecipient("emailAddress1", null, "emailTemplate", "recipientName")));

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata().withName("progression.event.prison-court-register-recorded"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.courtCentreId", is(COURT_CENTRE_ID.toString())),
                                withJsonPath("$.prisonCourtRegister", notNullValue()),
                                withJsonPath("$.prisonCourtRegister.courtCentreId", is(COURT_CENTRE_ID.toString())),
                                withJsonPath("$.prisonCourtRegister.defendant.prosecutionCasesOrApplications[0].courtApplicationId", is(APPLICATION_ID.toString())),
                                withJsonPath("$.defendantType", is("Respondent"))
                        )))));
    }

    @Test
    public void shouldGetRecordedEventForRespondentOrganisation() throws Exception {
        final JsonObject jsonObject = FileUtil.jsonFromString(FileUtil
                .getPayload("json/progression.event.court-application-for-respondent-organisation.json")
                .replaceAll("%APPLICATION_ID%", APPLICATION_ID.toString())
                .replaceAll("%MASTER_DEFENDANT_ID%", MASTER_DEFENDANT_ID.toString()));

        final CourtApplication courtApplication = jsonToObjectConverter.convert(jsonObject, CourtApplication.class);
        when(applicationAggregate.getCourtApplication()).thenReturn(courtApplication);

        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(applicationAggregate);

        final CourtCentreAggregate courtCentreAggregate = new CourtCentreAggregate();
        when(eventSource.getStreamById(getPrisonCourtRegisterStreamId(COURT_CENTRE_ID.toString(), HEARING_DATE.toLocalDate().toString()))).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CourtCentreAggregate.class)).thenReturn(courtCentreAggregate);

        prisonCourtRegisterHandler.handleAddPrisonCourtRegister(buildEnvelope(new PrisonCourtRegisterRecipient("emailAddress1", null, "emailTemplate", "recipientName")));

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata().withName("progression.event.prison-court-register-recorded"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.courtCentreId", is(COURT_CENTRE_ID.toString())),
                                withJsonPath("$.prisonCourtRegister", notNullValue()),
                                withJsonPath("$.prisonCourtRegister.courtCentreId", is(COURT_CENTRE_ID.toString())),
                                withJsonPath("$.prisonCourtRegister.defendant.prosecutionCasesOrApplications[0].courtApplicationId", is(APPLICATION_ID.toString())),
                                withJsonPath("$.defendantType", is("Respondent"))
                        )))));
    }

    @Test
    public void shouldGetRecordedWithoutRecipientsEvent() throws Exception {
        final JsonObject jsonObject = FileUtil.jsonFromString(FileUtil
                .getPayload("json/progression.event.court-application-for-applicant.json")
                .replaceAll("%APPLICATION_ID%", APPLICATION_ID.toString()));

        final CourtApplication courtApplication = jsonToObjectConverter.convert(jsonObject, CourtApplication.class);
        when(applicationAggregate.getCourtApplication()).thenReturn(courtApplication);

        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(applicationAggregate);

        final CourtCentreAggregate courtCentreAggregate = new CourtCentreAggregate();
        when(eventSource.getStreamById(getPrisonCourtRegisterStreamId(COURT_CENTRE_ID.toString(), HEARING_DATE.toLocalDate().toString()))).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CourtCentreAggregate.class)).thenReturn(courtCentreAggregate);

        prisonCourtRegisterHandler.handleAddPrisonCourtRegister(buildEnvelopeWithoutRecipients());

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata().withName("progression.event.prison-court-register-without-recipients-recorded"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.courtCentreId", is(COURT_CENTRE_ID.toString())),
                                withJsonPath("$.prisonCourtRegister", notNullValue()),
                                withJsonPath("$.prisonCourtRegister.courtCentreId", is(COURT_CENTRE_ID.toString()))
                        )))));
    }

    private Envelope<PrisonCourtRegisterDocumentRequest> buildEnvelopeWithoutRecipients() {
        return buildEnvelope(null);
    }

    private Envelope<PrisonCourtRegisterDocumentRequest> buildEnvelope(PrisonCourtRegisterRecipient registerRecipient) {

        final PrisonCourtRegisterDocumentRequest.Builder builder = PrisonCourtRegisterDocumentRequest.prisonCourtRegisterDocumentRequest()
                .withCourtCentreId(COURT_CENTRE_ID).withHearingDate(HEARING_DATE);

        if (registerRecipient != null) {
            builder.withRecipients(Arrays.asList(registerRecipient));
        }

        final PrisonCourtRegisterDocumentRequest prisonCourtRegisterDocumentRequest = builder
                .withDefendant(PrisonCourtRegisterDefendant.prisonCourtRegisterDefendant()
                        .withMasterDefendantId(MASTER_DEFENDANT_ID)
                        .withProsecutionCasesOrApplications(asList(PrisonCourtRegisterCaseOrApplication.prisonCourtRegisterCaseOrApplication().withCourtApplicationId(APPLICATION_ID).build()))
                        .build())
                .build();

        return envelope(ADD_PRISON_COURT_REGISTER_COMMAND_NAME, prisonCourtRegisterDocumentRequest);
    }

    private Envelope<NotifyPrisonCourtRegister> buildRecordRegisterGeneratedEnvelope() {

        final NotifyPrisonCourtRegister notifyPrisonCourtRegister = NotifyPrisonCourtRegister.notifyPrisonCourtRegister()
                .withCourtCentreId(COURT_CENTRE_ID)
                .withPayloadFileId(PAYLOAD_FILE_ID)
                .withSystemDocGeneratorId(SYSTEM_DOCUMENT_ID)
                .withId(ID)
                .build();

        return envelope(NOTIFY_PRISON_COURT_REGISTER_COMMAND_NAME, notifyPrisonCourtRegister);
    }

    private Envelope<RecordPrisonCourtRegisterDocumentSent> buildRecordPrisonCourtRegisterSentEnvelope() {

        final RecordPrisonCourtRegisterDocumentSent recordPrisonCourtRegisterDocumentSent = RecordPrisonCourtRegisterDocumentSent
                .recordPrisonCourtRegisterDocumentSent()
                .withCourtCentreId(COURT_CENTRE_ID)
                .withPayloadFileId(PAYLOAD_FILE_ID)
                .build();

        return envelope(RECORD_PRISON_COURT_REGISTER_SENT_COMMAND_NAME, recordPrisonCourtRegisterDocumentSent);
    }

    private Envelope<RecordPrisonCourtRegisterFailed> buildPrisonCourtRegisterFailedEnvelope() {

        final RecordPrisonCourtRegisterFailed recordPrisonCourtRegisterFailed = RecordPrisonCourtRegisterFailed
                .recordPrisonCourtRegisterFailed()
                .withCourtCentreId(COURT_CENTRE_ID)
                .withPayloadFileId(PAYLOAD_FILE_ID)
                .withReason("Test")
                .build();

        return envelope(PRISON_COURT_REGISTER_FAILED_COMMAND_NAME, recordPrisonCourtRegisterFailed);
    }


    private <T> Envelope<T> envelope(final String name, final T t) {
        final MetadataBuilder metadataBuilder = metadataFrom(metadataWithRandomUUID(name).withUserId(randomUUID().toString()).build());
        return envelopeFrom(metadataBuilder, t);
    }
}
