package uk.gov.moj.cpp.progression.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
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

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.PrisonCourtRegisterGenerated;
import uk.gov.justice.core.courts.PrisonCourtRegisterRecorded;
import uk.gov.justice.core.courts.PrisonCourtRegisterWithoutRecipientsRecorded;
import uk.gov.justice.core.courts.prisonCourtRegisterDocument.PrisonCourtRegisterCaseOrApplication;
import uk.gov.justice.core.courts.prisonCourtRegisterDocument.PrisonCourtRegisterDefendant;
import uk.gov.justice.core.courts.prisonCourtRegisterDocument.PrisonCourtRegisterDocumentRequest;
import uk.gov.justice.core.courts.prisonCourtRegisterDocument.PrisonCourtRegisterRecipient;
import uk.gov.justice.core.courts.prisonCourtRegisterDocument.RecordPrisonCourtRegisterDocumentGenerated;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher;
import uk.gov.justice.services.test.utils.framework.api.JsonObjectConvertersFactory;
import uk.gov.moj.cpp.progression.aggregate.ApplicationAggregate;
import uk.gov.moj.cpp.progression.aggregate.CourtCentreAggregate;
import uk.gov.moj.cpp.progression.test.FileUtil;

import java.util.UUID;
import java.util.stream.Stream;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PrisonCourtRegisterHandlerTest {

    private static final String ADD_PRISON_COURT_REGISTER_COMMAND_NAME = "progression.command.add-prison-court-register";
    private static final String RECORD_PRISON_COURT_REGISTER_GENERATED_COMMAND_NAME = "progression.command.record-prison-court-register-generated";
    private static final UUID COURT_CENTRE_ID = randomUUID();
    private static final UUID FILE_ID = randomUUID();
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
    private PrisonCourtRegisterHandler prisonCourtRegisterHandler;

    @Spy
    private final JsonObjectToObjectConverter jsonToObjectConverter = new JsonObjectConvertersFactory().jsonObjectToObjectConverter();

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(PrisonCourtRegisterRecorded.class, PrisonCourtRegisterWithoutRecipientsRecorded.class, PrisonCourtRegisterGenerated.class);

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
        when(eventSource.getStreamById(COURT_CENTRE_ID)).thenReturn(eventStream);
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
        when(eventSource.getStreamById(COURT_CENTRE_ID)).thenReturn(eventStream);
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
        when(eventSource.getStreamById(COURT_CENTRE_ID)).thenReturn(eventStream);
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
        when(eventSource.getStreamById(COURT_CENTRE_ID)).thenReturn(eventStream);
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

    @Test
    public void shouldCreateRegisterGeneratedEvent() throws Exception {

        final CourtCentreAggregate courtCentreAggregate = new CourtCentreAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CourtCentreAggregate.class)).thenReturn(courtCentreAggregate);

        prisonCourtRegisterHandler.handleRecordDocumentGenerated(buildRecordRegisterGeneratedEnvelope());

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata().withName("progression.event.prison-court-register-generated"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.courtCentreId", is(COURT_CENTRE_ID.toString())),
                                withJsonPath("$.fileId", is(FILE_ID.toString()))
                        )))));
    }

    private Envelope<PrisonCourtRegisterDocumentRequest> buildEnvelopeWithoutRecipients() {
        return buildEnvelope(null);
    }

    private Envelope<PrisonCourtRegisterDocumentRequest> buildEnvelope(PrisonCourtRegisterRecipient registerRecipient) {

        final PrisonCourtRegisterDocumentRequest.Builder builder = PrisonCourtRegisterDocumentRequest.prisonCourtRegisterDocumentRequest().withCourtCentreId(COURT_CENTRE_ID);

        if (registerRecipient != null) {
            builder.withRecipients(asList(registerRecipient));
        }

        final PrisonCourtRegisterDocumentRequest prisonCourtRegisterDocumentRequest = builder
                .withDefendant(PrisonCourtRegisterDefendant.prisonCourtRegisterDefendant()
                        .withMasterDefendantId(MASTER_DEFENDANT_ID)
                        .withProsecutionCasesOrApplications(asList(PrisonCourtRegisterCaseOrApplication.prisonCourtRegisterCaseOrApplication().withCourtApplicationId(APPLICATION_ID).build()))
                        .build())
                .build();

        return envelope(ADD_PRISON_COURT_REGISTER_COMMAND_NAME, prisonCourtRegisterDocumentRequest);
    }

    private Envelope<RecordPrisonCourtRegisterDocumentGenerated> buildRecordRegisterGeneratedEnvelope() {

        final RecordPrisonCourtRegisterDocumentGenerated recordPrisonCourtRegisterDocumentGenerated = RecordPrisonCourtRegisterDocumentGenerated.recordPrisonCourtRegisterDocumentGenerated()
                .withCourtCentreId(COURT_CENTRE_ID)
                .withFileId(FILE_ID)
                .build();

        return envelope(RECORD_PRISON_COURT_REGISTER_GENERATED_COMMAND_NAME, recordPrisonCourtRegisterDocumentGenerated);
    }

    private <T> Envelope<T> envelope(final String name, final T t) {
        final MetadataBuilder metadataBuilder = metadataFrom(metadataWithRandomUUID(name).withUserId(randomUUID().toString()).build());
        return envelopeFrom(metadataBuilder, t);
    }
}
