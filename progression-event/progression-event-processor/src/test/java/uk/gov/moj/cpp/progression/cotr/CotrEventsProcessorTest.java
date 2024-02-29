package uk.gov.moj.cpp.progression.cotr;

import static com.google.common.io.Resources.getResource;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.nio.charset.Charset.defaultCharset;
import static java.util.Arrays.asList;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.progression.service.MetadataUtil.metadataWithNewActionName;
import static uk.gov.moj.cpp.progression.utils.FileUtil.jsonFromString;

import uk.gov.justice.core.courts.CotrPdfContent;
import uk.gov.justice.core.courts.DefendantCotrServed;
import uk.gov.justice.cpp.progression.FormDefendants;
import uk.gov.justice.cpp.progression.event.CotrCreated;
import uk.gov.justice.cpp.progression.event.ProsecutionCotrServed;
import uk.gov.justice.cpp.progression.event.ProsecutionCotrUpdated;
import uk.gov.justice.progression.event.CotrNotes;
import uk.gov.justice.progression.event.DefendantAddedToCotr;
import uk.gov.justice.progression.event.DefendantRemovedFromCotr;
import uk.gov.justice.progression.event.FurtherInfoForDefenceCotrAdded;
import uk.gov.justice.progression.event.FurtherInfoForProsecutionCotrAdded;
import uk.gov.justice.progression.event.ReviewNoteType;
import uk.gov.justice.progression.event.ReviewNotes;
import uk.gov.justice.progression.event.ReviewNotesUpdated;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.PolarQuestion;
import uk.gov.moj.cpp.progression.json.schemas.event.CotrTaskRequested;
import uk.gov.moj.cpp.progression.service.DocumentGeneratorService;
import uk.gov.moj.cpp.progression.service.MaterialService;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.service.RefDataService;
import uk.gov.moj.cpp.progression.service.UsersGroupService;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.COTRDetailsEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.COTRDetailsRepository;
import uk.gov.moj.cpp.systemusers.ServiceContextSystemUserProvider;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import com.google.common.io.Resources;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CotrEventsProcessorTest {

    public static final String SUBMISSION_ID = "submissionId";
    public static final String HAS_ALL_EVIDENCE_TO_BE_RELIED_ON_BEEN_SERVED = "hasAllEvidenceToBeReliedOnBeenServed";
    public static final String REVIEW_NOTE_DESCRIPTION = "reviewNoteDescription";
    public static final String WELSH_REVIEW_NOTE_DESCRIPTION = "welshReviewNoteDescription";
    public static final String ROLES = "roles";
    private static final String PROGRESSION_QUERY_CASEHEARINGS = "progression.query.casehearings";
    private static final String PROGRESSION_QUERY_COTR_DETAILS_PROSECUTION_CASE = "progression.query.cotr.details.prosecutioncase";
    private static final String PROGRESSION_COMMAND_SERVE_COTR = "progression.command.serve-prosecution-cotr";
    private static final String PROGRESSION_COMMAND_CREATE_COTR = "progression.command.create-cotr";
    private static final String PROGRESSION_COMMAND_UPDATE_COTR = "progression.command.update-prosecution-cotr";
    private static final String COTR_ID = "cotrId";
    private static final String CASE_ID = "caseId";
    private static final String CASE_URN = "caseUrn";
    private static final String COURT_CENTER = "courtCenter";
    private static final String HEARING_ID = "hearingId";
    private static final String HEARING_DATE = "hearingDate";
    private static final String DEFENDANT_IDS = "defendantIds";
    private static final String PROGRESSION_OPERATION_FAILED = "public.progression.cotr-operation-failed";
    public static final String REVIEW_NOTE_TYPE = "reviewNoteType";
    public static final String TASK_NAME = "taskName";
    public static final String NUMBER_OF_DAYS = "numberOfDays";
    public static final String ID = "id";

    @Mock
    private Sender sender;

    @Mock
    private DocumentGeneratorService documentGeneratorService;

    @Mock
    private MaterialService materialService;

    @Mock
    private RefDataService referenceDataService;

    @Mock
    private UsersGroupService usersGroupService;

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeArgumentCaptor;

    @InjectMocks
    private CotrEventsProcessor processor;

    @Mock
    private ProgressionService progressionService;

    @Mock
    private JsonEnvelope queryResponseEnvelope;

    @Mock
    private Requester requester;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(new ObjectMapperProducer().objectMapper());

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(new ObjectMapperProducer().objectMapper());

    @Spy
    private StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();


    @Mock
    private COTRDetailsRepository cotrDetailsRepository;

    @Mock
    private ServiceContextSystemUserProvider serviceContextSystemUserProvider;

    private static JsonObject getReviewNotes(final UUID id1, final UUID id2) {

        final JsonObject reviewNotes = createObjectBuilder()
                .add("reviewNotes", Json.createArrayBuilder()
                        .add(createObjectBuilder()
                                .add(ID, id1.toString())
                                .add(REVIEW_NOTE_TYPE, "CASE_PROGRESSION")
                                .add(REVIEW_NOTE_DESCRIPTION, "description")
                                .add(WELSH_REVIEW_NOTE_DESCRIPTION, "welshdescription")
                                .add(ROLES, "roles1, role2")
                                .add(TASK_NAME, "Some task")
                                .add(NUMBER_OF_DAYS, "2")
                                .build())
                        .add(createObjectBuilder()
                                .add(ID, id2.toString())
                                .add(REVIEW_NOTE_TYPE, "LISTING")
                                .add(REVIEW_NOTE_DESCRIPTION, "description")
                                .add(WELSH_REVIEW_NOTE_DESCRIPTION, "welshdescription")
                                .add(ROLES, "roles1, role2")
                                .add(TASK_NAME, "Some task")
                                .add(NUMBER_OF_DAYS, "2")
                                .build())
                        .build())
                .build();

        return reviewNotes;
    }

    private static JsonObject buildCotrDetails() {
        final ZonedDateTime hearingDay = ZonedDateTime.now();
        return Json.createObjectBuilder()
                .add(ID, String.valueOf(randomUUID()))
                .add("hearingDay", String.valueOf(hearingDay))
                .add("hearingId", String.valueOf(randomUUID()))
                .add("isArchived", false)
                .add("isProsecutionServed", false)
                .add("cotrDefendants", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("dateOfBirth", String.valueOf(LocalDate.now()))
                                .add("defenceFormData", "defenceFormData")
                                .add("defendantNumber", 1)
                                .add("firstName", "firstName")
                                .add(ID, String.valueOf(randomUUID()))
                                .add("isDefenceServed", false)
                                .add("lastName", "lastName")
                                .add("servedBy", "servedBy")
                                .add("servedOn", "servedOn")
                                .add("defenceAdditionalInfo", Json.createArrayBuilder()
                                        .add(Json.createObjectBuilder()
                                                .add("addedBy", String.valueOf(randomUUID()))
                                                .add("addedByName", "addedByName")
                                                .add("addedOn", String.valueOf(LocalDate.now()))
                                                .add(ID, String.valueOf(randomUUID()))
                                                .add("information", "information")
                                                .add("isCertificationReady", true)
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build();
    }

    @Test
    public void shouldTestProcessCotrCreatedEvent() throws IOException {
        final UUID hearingId = randomUUID();
        final UUID cotrId = randomUUID();
        final UUID submissionId = randomUUID();

        final String hearingDate = "2022-06-04";

        final Metadata eventEnvelopeMetadata = metadataBuilder()
                .withName("progression.event.cotr-created")
                .withId(randomUUID())
                .build();

        final List<FormDefendants> formDefendants = new ArrayList<>();
        formDefendants.add(FormDefendants.formDefendants()
                        .withDefendantId(randomUUID())
                        .withCpsDefendantId(randomUUID().toString())
                .build());

        final Envelope<CotrCreated> eventEnvelope = Envelope.envelopeFrom(eventEnvelopeMetadata,
                CotrCreated.cotrCreated()
                        .withCotrId(cotrId)
                        .withHearingId(hearingId)
                        .withHearingDate(hearingDate)
                        .withSubmissionId(submissionId)
                        .withCaseId(randomUUID())
                        .withFormDefendants(formDefendants)
                        .build());

        String caseHearingpayload = Resources.toString(getResource("progression-case-latest-hearings.json"), defaultCharset());

        final JsonObject caseHearingjsonPayload = jsonFromString(caseHearingpayload);

        when(queryResponseEnvelope.payloadAsJsonObject()).thenReturn(caseHearingjsonPayload);
        when(requester.request(any(Envelope.class))).thenReturn(queryResponseEnvelope);

        when(serviceContextSystemUserProvider.getContextSystemUserId()).thenReturn(of(UUID.randomUUID()));
        when(cotrDetailsRepository.findBy(any())).thenReturn(new COTRDetailsEntity(randomUUID(),randomUUID(),randomUUID(),false, null, null, null, null));

        final Metadata metadata = metadataWithNewActionName(eventEnvelopeMetadata, "progression.query.cotr.details.prosecutioncase");
        final Envelope envelope1 = Envelope.envelopeFrom(metadata, createCotrDetails().get());
        when(requester.requestAsAdmin(any(), any())).thenReturn(envelope1);

        processor.cotrCreated(eventEnvelope);

        ArgumentCaptor<Envelope> captor = forClass(Envelope.class);
        verify(sender, times(3)).send(captor.capture());

        List<Envelope> currentEvents = captor.getAllValues();
        assertThat(currentEvents.get(0).metadata().name(), Matchers.is("progression.command.serve-prosecution-cotr"));
        assertThat(currentEvents.get(1).metadata().name(), Matchers.is("progression.command.update-cps-defendant-id"));
        assertThat(currentEvents.get(2).metadata().name(), Matchers.is("public.progression.cotr-created"));

        assertThat(currentEvents.get(0).payload().toString(), notNullValue());
        assertThat(objectToJsonObjectConverter.convert(currentEvents.get(0).payload()).getString(COTR_ID), Matchers.is(cotrId.toString()));
    }

    @Test
    public void shouldTestProcessServeProsecutionCotrEvent() {
        final UUID hearingId = randomUUID();
        final UUID cotrId = randomUUID();

        final Metadata eventEnvelopeMetadata = metadataBuilder()
                .withName("progression.event.prosecution-cotr-served")
                .withId(randomUUID())
                .build();

        final Envelope<ProsecutionCotrServed> eventEnvelope = Envelope.envelopeFrom(eventEnvelopeMetadata, ProsecutionCotrServed.prosecutionCotrServed()
                .withCotrId(cotrId)
                .withSubmissionId(randomUUID())
                .withHearingId(hearingId).build());

        processor.serveProsecutionCotr(eventEnvelope);

        verify(this.sender).send(this.envelopeArgumentCaptor.capture());

        final Envelope<JsonObject> command = this.envelopeArgumentCaptor.getValue();

        assertThat(command.metadata().name(), is("public.progression.prosecution-cotr-served"));
        assertThat(command.payload().toString(), isJson(allOf(
                withJsonPath("$.cotrId", is(cotrId.toString())))
        ));
    }

    @Test
    public void shouldTestArchiveCotrEvent() {
        final String cotrId = randomUUID().toString();
        final JsonEnvelope jsonEnvelope = envelope()
                .with(metadataWithRandomUUID("progression.event.cotr-archived"))
                .withPayloadOf(cotrId, "cotrId")
                .build();

        processor.archiveCotr(jsonEnvelope);

        verify(this.sender).send(this.envelopeArgumentCaptor.capture());

        final Envelope<JsonObject> command = this.envelopeArgumentCaptor.getValue();
        assertThat(command.metadata().name(), is("public.progression.cotr-archived"));
        assertThat(command.payload().toString(), isJson(allOf(
                withJsonPath("$.cotrId", is(cotrId)))
        ));
    }

    @Test
    public void shouldTestcotrTaskRequestedEvent() {
        final UUID defendenId = randomUUID();
        final UUID cotrId = randomUUID();
        final UUID caseId = randomUUID();

        final Metadata eventEnvelopeMetadata = metadataBuilder()
                .withName("progression.event.cotr-task-requested")
                .withId(randomUUID())
                .build();
        when(usersGroupService.getOrganisationByType(Mockito.any())).thenReturn(randomUUID());

        final Envelope<CotrTaskRequested> eventEnvelope = Envelope.envelopeFrom(eventEnvelopeMetadata, CotrTaskRequested.cotrTaskRequested()
                .withCotrId(cotrId).build());

        processor.cotrTaskRequested(eventEnvelope);

        verify(this.sender, times(1)).send(this.envelopeArgumentCaptor.capture());

        final List<Envelope<JsonObject>> commands = this.envelopeArgumentCaptor.getAllValues();

        assertThat(commands.get(0).metadata().name(), is("public.progression.cotr-task-requested"));
        assertThat(commands.get(0).payload().toString(), isJson(allOf(
                withJsonPath("$.cotrId", is(cotrId.toString())))
        ));


    }

    @Test
    public void shouldTestProcessServeDefendantCotrEvent() {
        final UUID defendenId = randomUUID();
        final UUID cotrId = randomUUID();
        final UUID caseId = randomUUID();

        final Metadata eventEnvelopeMetadata = metadataBuilder()
                .withName("progression.event.defendant-cotr-served")
                .withId(randomUUID())
                .build();

        final Envelope<DefendantCotrServed> eventEnvelope = Envelope.envelopeFrom(eventEnvelopeMetadata, DefendantCotrServed.defendantCotrServed()
                .withCotrId(cotrId).withCaseId(caseId).withDefendantId(defendenId).withDefendantFormData("forData").withPdfContent(CotrPdfContent.cotrPdfContent().build()).build());

        processor.defendantCotrServed(eventEnvelope);

        verify(this.sender, times(2)).send(this.envelopeArgumentCaptor.capture());

        final List<Envelope<JsonObject>> commands = this.envelopeArgumentCaptor.getAllValues();

        assertThat(commands.get(1).metadata().name(), is("public.progression.serve-defendant-cotr"));
        assertThat(commands.get(1).payload().toString(), isJson(allOf(
                withJsonPath("$.cotrId", is(cotrId.toString())))
        ));

        assertThat(commands.get(0).metadata().name(), is("progression.command.add-court-document"));
        assertThat(commands.get(0).payload().toString(), isJson(allOf(
                withJsonPath("$.courtDocument.notificationType", is("cotr-form-served")),
                withJsonPath("$.courtDocument.sendToCps", is(true)))
        ));
    }

    @Test
    public void shouldRaiseDefendantAddedToCotrPublicEvent() {

        final UUID cotrId = randomUUID();
        final UUID defendantId = randomUUID();

        final Metadata eventEnvelopeMetadata = metadataBuilder()
                .withName("progression.event.defendant-added-to-cotr")
                .withId(randomUUID())
                .build();

        final Envelope<DefendantAddedToCotr> eventEnvelope = Envelope.envelopeFrom(eventEnvelopeMetadata, DefendantAddedToCotr.defendantAddedToCotr()
                .withCotrId(cotrId)
                .withDefendantId(defendantId)
                .withDefendantNumber(1)
                .build());

        processor.handleDefendantAddedToCotrEvent(eventEnvelope);

        verify(this.sender).send(this.envelopeArgumentCaptor.capture());

        final Envelope<JsonObject> command = this.envelopeArgumentCaptor.getValue();

        assertThat(command.metadata().name(), is("public.progression.defendants-changed-in-cotr"));
        assertThat(command.payload().toString(), isJson(allOf(
                withJsonPath("$.cotrId", is(cotrId.toString())),
                withJsonPath("$.defendantId", is(defendantId.toString()))
        )));
    }

    @Test
    public void shouldRaiseDefendantRemovedFromCotrPublicEvent() {

        final UUID cotrId = randomUUID();
        final UUID defendantId = randomUUID();

        final Metadata eventEnvelopeMetadata = metadataBuilder()
                .withName("progression.event.defendant-removed-from-cotr")
                .withId(randomUUID())
                .build();

        final Envelope<DefendantRemovedFromCotr> eventEnvelope = Envelope.envelopeFrom(eventEnvelopeMetadata, DefendantRemovedFromCotr.defendantRemovedFromCotr()
                .withCotrId(cotrId)
                .withDefendantId(defendantId)
                .build());

        processor.handleDefendantRemovedFromCotrEvent(eventEnvelope);

        verify(this.sender).send(this.envelopeArgumentCaptor.capture());

        final Envelope<JsonObject> command = this.envelopeArgumentCaptor.getValue();

        assertThat(command.metadata().name(), is("public.progression.defendants-changed-in-cotr"));
        assertThat(command.payload().toString(), isJson(allOf(
                withJsonPath("$.cotrId", is(cotrId.toString())),
                withJsonPath("$.defendantId", is(defendantId.toString()))
        )));
    }

    @Test
    public void shouldRaiseFurtherInfoForProsecutionCotrAddedPublicEvent() {

        final String message = "Prosecution Cotr Further Info";
        final UUID cotrId = randomUUID();

        final Metadata eventEnvelopeMetadata = metadataBuilder()
                .withName("progression.event.further-info-for-prosecution-cotr-added")
                .withId(randomUUID())
                .build();

        final Envelope<FurtherInfoForProsecutionCotrAdded> eventEnvelope = Envelope.envelopeFrom(eventEnvelopeMetadata, FurtherInfoForProsecutionCotrAdded.furtherInfoForProsecutionCotrAdded()
                .withCotrId(cotrId)
                .withMessage(message)
                .build());

        processor.handleFurtherInfoForProsecutionCotrAddedEvent(eventEnvelope);

        verify(this.sender).send(this.envelopeArgumentCaptor.capture());

        final Envelope<JsonObject> command = this.envelopeArgumentCaptor.getValue();

        assertThat(command.metadata().name(), is("public.progression.further-info-for-prosecution-cotr-added"));
        assertThat(command.payload().toString(), isJson(allOf(withJsonPath("$.cotrId", is(cotrId.toString())))
        ));
    }

    @Test
    public void shouldRaiseFurtherInfoForDefenceCotrAddedPublicEvent() {

        final String message = "Defence Cotr Further Info";
        final UUID cotrId = randomUUID();

        final Metadata eventEnvelopeMetadata = metadataBuilder()
                .withName("progression.event.further-info-for-defence-cotr-added")
                .withId(randomUUID())
                .build();

        final Envelope<FurtherInfoForDefenceCotrAdded> eventEnvelope = Envelope.envelopeFrom(eventEnvelopeMetadata, FurtherInfoForDefenceCotrAdded.furtherInfoForDefenceCotrAdded()
                .withCotrId(cotrId)
                .withCaseId(randomUUID())
                .withDefendantId(randomUUID())
                .withMessage(message)
                .withPdfContent(CotrPdfContent.cotrPdfContent().build())
                .build());

        processor.handleFurtherInfoForDefenceCotrAddedEvent(eventEnvelope);

        verify(this.sender, times(2)).send(this.envelopeArgumentCaptor.capture());

        final Envelope<JsonObject> command = this.envelopeArgumentCaptor.getValue();

        assertThat(command.metadata().name(), is("public.progression.further-info-for-defence-cotr-added"));
        assertThat(command.payload().toString(), isJson(allOf(withJsonPath("$.cotrId", is(cotrId.toString())))
        ));
    }

    @Test
    public void shouldRaiseReviewNotesUpdatedPublicEvent() {

        final UUID cotrId = randomUUID();
        final UUID caseProgressionReviewNoteId1 = randomUUID();
        final UUID caseProgressionReviewNoteId2 = randomUUID();
        final String value = "Value 1";

        when(referenceDataService.getCotrReviewNotes(Mockito.any(), Mockito.any())).thenReturn(Optional.of(getReviewNotes(caseProgressionReviewNoteId1, caseProgressionReviewNoteId2)));

        final Metadata eventEnvelopeMetadata = metadataBuilder()
                .withName("progression.event.review-notes-updated")
                .withId(randomUUID())
                .build();

        final Envelope<ReviewNotesUpdated> eventEnvelope = Envelope.envelopeFrom(eventEnvelopeMetadata, ReviewNotesUpdated.reviewNotesUpdated()
                .withCotrId(cotrId)
                .withCotrNotes(asList(CotrNotes.cotrNotes()
                        .withReviewNoteType(ReviewNoteType.CASE_PROGRESSION)
                        .withReviewNotes(asList(ReviewNotes.reviewNotes()
                                .withId(caseProgressionReviewNoteId1)
                                .withComment(value)
                                .build()))
                        .build()))
                .build());


        processor.handleReviewNotesUpdateEvent(eventEnvelope);

        verify(this.sender, times(2)).send(this.envelopeArgumentCaptor.capture());

        final List<Envelope<JsonObject>> event = this.envelopeArgumentCaptor.getAllValues();

        assertThat(event.get(0).metadata().name(), is("public.progression.cotr-review-notes-updated"));
        assertThat(event.get(0).payload().getString("cotrId"), is(cotrId.toString()));

        assertThat(event.get(1).metadata().name(), is("progression.command.request-cotr-task"));


    }

    @Test
    public void shouldHandleServeCotrReceivedPublicEvent() throws IOException {

        final JsonEnvelope jsonEnvelope = getEnvelope(PROGRESSION_QUERY_CASEHEARINGS);
        final UUID caseId = randomUUID();

        String caseHearingpayload = Resources.toString(getResource("progression-case-hearings.json"), defaultCharset());

        final JsonObject caseHearingjsonPayload = jsonFromString(caseHearingpayload);

        when(queryResponseEnvelope.payloadAsJsonObject()).thenReturn(caseHearingjsonPayload);
        when(requester.request(any(Envelope.class))).thenReturn(queryResponseEnvelope);

        String payload = Resources.toString(getResource("cps-serve-cotr-submitted.json"), defaultCharset());
        final JsonObject jsonPayload = jsonFromString(payload);
        final String submissionId  = jsonPayload.getString(SUBMISSION_ID);
        assertThat(jsonPayload.getString(HAS_ALL_EVIDENCE_TO_BE_RELIED_ON_BEEN_SERVED),Matchers.is("Y"));

        final JsonEnvelope envelope = envelopeFrom(
                metadataWithRandomUUID("public.prosecutioncasefile.cps-serve-pet-submitted"),
                jsonPayload);
        when(serviceContextSystemUserProvider.getContextSystemUserId()).thenReturn(of(UUID.randomUUID()));
        when(cotrDetailsRepository.findBy(any())).thenReturn(new COTRDetailsEntity(randomUUID(),randomUUID(),randomUUID(),false, null, null, null, null));

        final Metadata metadata = metadataWithNewActionName(envelope.metadata(), "progression.query.cotr.details.prosecutioncase");
        final Envelope envelope1 = Envelope.envelopeFrom(metadata, createCotrDetails().get());
        when(requester.requestAsAdmin(any(), any())).thenReturn(envelope1);

        //when
        processor.handleServeCotrReceivedPublicEvent(envelope);
        //Then
        ArgumentCaptor<Envelope> captor = forClass(Envelope.class);
        verify(sender, times(1)).send(captor.capture());
        List<Envelope> currentEvents = captor.getAllValues();
        assertThat(currentEvents.get(0).metadata().name(), Matchers.is(PROGRESSION_COMMAND_CREATE_COTR));

        assertThat(currentEvents.get(0).payload().toString(), notNullValue());
        assertThat(objectToJsonObjectConverter.convert(currentEvents.get(0).payload()).getString(CASE_ID), Matchers.is(notNullValue()));
        assertThat(objectToJsonObjectConverter.convert(currentEvents.get(0).payload()).getString(COTR_ID), Matchers.is(submissionId));
        assertThat(objectToJsonObjectConverter.convert(currentEvents.get(0).payload()).getString(SUBMISSION_ID), Matchers.is(notNullValue()));
        assertThat(objectToJsonObjectConverter.convert(currentEvents.get(0).payload()).getString(HEARING_ID), Matchers.is(notNullValue()));
        assertThat(objectToJsonObjectConverter.convert(currentEvents.get(0).payload()).getString(CASE_URN), Matchers.is(notNullValue()));
        assertThat(objectToJsonObjectConverter.convert(currentEvents.get(0).payload()).getString(HEARING_DATE), Matchers.is(notNullValue()));
        assertThat(objectToJsonObjectConverter.convert(currentEvents.get(0).payload()).getString(COURT_CENTER), Matchers.is(notNullValue()));
        assertThat(objectToJsonObjectConverter.convert(currentEvents.get(0).payload()).getJsonArray(DEFENDANT_IDS).size(), is(1));
    }

    @Test
    public void shouldHandleServeCotrReceivedForTheLatestHearing() throws IOException {

        String caseHearingpayload = Resources.toString(getResource("progression-case-latest-hearings.json"), defaultCharset());

        final JsonObject caseHearingjsonPayload = jsonFromString(caseHearingpayload);

        when(queryResponseEnvelope.payloadAsJsonObject()).thenReturn(caseHearingjsonPayload);
        when(requester.request(any(Envelope.class))).thenReturn(queryResponseEnvelope);


        String payload = Resources.toString(getResource("cps-serve-cotr-submitted.json"), defaultCharset());
        final JsonObject jsonPayload = jsonFromString(payload);

        final JsonEnvelope envelope = envelopeFrom(
                metadataWithRandomUUID("public.prosecutioncasefile.cps-serve-pet-submitted"),
                jsonPayload);

        when(serviceContextSystemUserProvider.getContextSystemUserId()).thenReturn(of(UUID.randomUUID()));
        when(cotrDetailsRepository.findBy(any())).thenReturn(new COTRDetailsEntity(randomUUID(),randomUUID(),randomUUID(),false, null, null, null, null));

        final Metadata metadata = metadataWithNewActionName(envelope.metadata(), "progression.query.cotr.details.prosecutioncase");
        final Envelope envelope1 = Envelope.envelopeFrom(metadata, createCotrDetails().get());when(requester.requestAsAdmin(any(), any())).thenReturn(envelope1);

        //when
        processor.handleServeCotrReceivedPublicEvent(envelope);
        //Then
        ArgumentCaptor<Envelope> captor = forClass(Envelope.class);
        verify(sender, times(1)).send(captor.capture());
        List<Envelope> currentEvents = captor.getAllValues();
        assertThat(currentEvents.get(0).metadata().name(), Matchers.is(PROGRESSION_COMMAND_CREATE_COTR));

        assertThat(currentEvents.get(0).payload().toString(), notNullValue());
        assertThat(objectToJsonObjectConverter.convert(currentEvents.get(0).payload()).getString(CASE_ID), Matchers.is(notNullValue()));
        assertThat(objectToJsonObjectConverter.convert(currentEvents.get(0).payload()).getString(COTR_ID), Matchers.is(notNullValue()));
        assertThat(objectToJsonObjectConverter.convert(currentEvents.get(0).payload()).getString(SUBMISSION_ID), Matchers.is(notNullValue()));
        assertThat(objectToJsonObjectConverter.convert(currentEvents.get(0).payload()).getString(HEARING_ID), Matchers.is(notNullValue()));
        assertThat(objectToJsonObjectConverter.convert(currentEvents.get(0).payload()).getString(CASE_URN), Matchers.is(notNullValue()));
        assertThat(objectToJsonObjectConverter.convert(currentEvents.get(0).payload()).getString(HEARING_DATE), Matchers.is(notNullValue()));
        assertThat(objectToJsonObjectConverter.convert(currentEvents.get(0).payload()).getString(COURT_CENTER), Matchers.is(notNullValue()));
        assertThat(objectToJsonObjectConverter.convert(currentEvents.get(0).payload()).getJsonArray(DEFENDANT_IDS).size(), is(1));
    }

    @Test
    public void shouldHandleServeCotrReceivedForTheLatestHearingDate() throws IOException {

        String caseHearingpayload = Resources.toString(getResource("progression-latestdate-case-hearings.json"), defaultCharset());

        final JsonObject caseHearingjsonPayload = jsonFromString(caseHearingpayload);

        when(queryResponseEnvelope.payloadAsJsonObject()).thenReturn(caseHearingjsonPayload);
        when(requester.request(any(Envelope.class))).thenReturn(queryResponseEnvelope);


        String payload = Resources.toString(getResource("cps-serve-cotr-submitted.json"), defaultCharset());
        final JsonObject jsonPayload = jsonFromString(payload);

        final JsonEnvelope envelope = envelopeFrom(
                metadataWithRandomUUID("public.prosecutioncasefile.cps-serve-pet-submitted"),
                jsonPayload);

        when(serviceContextSystemUserProvider.getContextSystemUserId()).thenReturn(of(UUID.randomUUID()));
        when(cotrDetailsRepository.findBy(any())).thenReturn(new COTRDetailsEntity(randomUUID(),randomUUID(),randomUUID(),false, null, null, null, null));

        final Metadata metadata = metadataWithNewActionName(envelope.metadata(), "progression.query.cotr.details.prosecutioncase");
        final Envelope envelope1 = Envelope.envelopeFrom(metadata, createCotrDetails().get());
        when(requester.requestAsAdmin(any(), any())).thenReturn(envelope1);

        //when
        processor.handleServeCotrReceivedPublicEvent(envelope);
        //Then
        ArgumentCaptor<Envelope> captor = forClass(Envelope.class);
        verify(sender, times(1)).send(captor.capture());
        List<Envelope> currentEvents = captor.getAllValues();
        assertThat(currentEvents.get(0).metadata().name(), Matchers.is(PROGRESSION_COMMAND_CREATE_COTR));

        assertThat(currentEvents.get(0).payload().toString(), notNullValue());
        assertThat(objectToJsonObjectConverter.convert(currentEvents.get(0).payload()).getString(CASE_ID), Matchers.is(notNullValue()));
        assertThat(objectToJsonObjectConverter.convert(currentEvents.get(0).payload()).getString(COTR_ID), Matchers.is(notNullValue()));
        assertThat(objectToJsonObjectConverter.convert(currentEvents.get(0).payload()).getString(SUBMISSION_ID), Matchers.is(notNullValue()));
        assertThat(objectToJsonObjectConverter.convert(currentEvents.get(0).payload()).getString(HEARING_ID), Matchers.is(notNullValue()));
        assertThat(objectToJsonObjectConverter.convert(currentEvents.get(0).payload()).getString(CASE_URN), Matchers.is(notNullValue()));
        assertThat(objectToJsonObjectConverter.convert(currentEvents.get(0).payload()).getString(HEARING_DATE), Matchers.is(notNullValue()));
        assertThat(objectToJsonObjectConverter.convert(currentEvents.get(0).payload()).getString(COURT_CENTER), Matchers.is(notNullValue()));
        assertThat(objectToJsonObjectConverter.convert(currentEvents.get(0).payload()).getJsonArray(DEFENDANT_IDS).size(), is(1));
    }

    @Test
    public void shouldNotHandleServeCotrReceivedPublicEvent() throws IOException {

        final JsonEnvelope jsonEnvelope = getEnvelope(PROGRESSION_QUERY_CASEHEARINGS);
        final UUID caseId = randomUUID();

        String caseHearingpayload = Resources.toString(getResource("progression-no-case-hearings.json"), defaultCharset());

        final JsonObject caseHearingjsonPayload = jsonFromString(caseHearingpayload);

        when(queryResponseEnvelope.payloadAsJsonObject()).thenReturn(caseHearingjsonPayload);
        when(requester.request(any(Envelope.class))).thenReturn(queryResponseEnvelope);

        String payload = Resources.toString(getResource("cps-serve-cotr-submitted.json"), defaultCharset());
        final JsonObject jsonPayload = jsonFromString(payload);

        final JsonEnvelope envelope = envelopeFrom(
                metadataWithRandomUUID("public.prosecutioncasefile.cps-serve-pet-submitted"),
                jsonPayload);

        when(serviceContextSystemUserProvider.getContextSystemUserId()).thenReturn(of(UUID.randomUUID()));

        //when
        processor.handleServeCotrReceivedPublicEvent(envelope);

        //Then
        ArgumentCaptor<Envelope> captor = forClass(Envelope.class);
        verify(sender, times(1)).send(captor.capture());
        List<Envelope> currentEvents = captor.getAllValues();
        assertThat(currentEvents.get(0).metadata().name(), Matchers.is(PROGRESSION_OPERATION_FAILED));
        assertThat(currentEvents.get(0).payload(), notNullValue());
    }

    @Test
    public void shouldHandleUpdateCotrReceivedPublicEvent() throws IOException {
        when(queryResponseEnvelope.payloadAsJsonObject()).thenReturn(createCotrDetails().get());
        when(requester.request(any(Envelope.class))).thenReturn(queryResponseEnvelope);
        String payload = Resources.toString(getResource("cps-update-cotr-submitted.json"), defaultCharset());
        final JsonObject jsonPayload = jsonFromString(payload);
        final JsonEnvelope envelope = envelopeFrom(
                metadataWithRandomUUID("public.prosecutioncasefile.cps-update-cotr-submitted"),
                jsonPayload);
        when(cotrDetailsRepository.findBy(any())).thenReturn(new COTRDetailsEntity(randomUUID(),randomUUID(),randomUUID(),false, null, null, null, null));
        //when
        processor.handleUpdateCotrReceivedPublicEvent(envelope);
        //Then
        ArgumentCaptor<Envelope> captor = forClass(Envelope.class);
        verify(sender, times(1)).send(captor.capture());
        List<Envelope> currentEvents = captor.getAllValues();
        assertThat(currentEvents.get(0).metadata().name(), Matchers.is(PROGRESSION_COMMAND_UPDATE_COTR));
        assertThat(currentEvents.get(0).payload().toString(), notNullValue());
        assertThat(objectToJsonObjectConverter.convert(currentEvents.get(0).payload()).getString(COTR_ID), Matchers.is(notNullValue()));
        assertThat(objectToJsonObjectConverter.convert(currentEvents.get(0).payload()).getString(SUBMISSION_ID), Matchers.is(notNullValue()));
        assertThat(objectToJsonObjectConverter.convert(currentEvents.get(0).payload()).getString(HEARING_ID), Matchers.is(notNullValue()));
    }

    @Test
    public void shouldNotHandleUpdateCotrReceivedPublicEvent() throws IOException {
        final JsonEnvelope jsonEnvelope = getEnvelope(PROGRESSION_QUERY_COTR_DETAILS_PROSECUTION_CASE);
        final UUID caseId = randomUUID();
        when(queryResponseEnvelope.payloadAsJsonObject()).thenReturn(createNoCotrDetails().get());
        when(requester.request(any(Envelope.class))).thenReturn(queryResponseEnvelope);
        String payload = Resources.toString(getResource("cps-update-cotr-submitted.json"), defaultCharset());
        final JsonObject jsonPayload = jsonFromString(payload);
        final JsonEnvelope envelope = envelopeFrom(
                metadataWithRandomUUID("public.prosecutioncasefile.cps-update-cotr-submitted"),
                jsonPayload);
        //when
        processor.handleUpdateCotrReceivedPublicEvent(envelope);
        //Then
        ArgumentCaptor<Envelope> captor = forClass(Envelope.class);
        verify(sender, times(1)).send(captor.capture());
        List<Envelope> currentEvents = captor.getAllValues();
        assertThat(currentEvents.get(0).metadata().name(), Matchers.is(PROGRESSION_OPERATION_FAILED));
        assertThat(currentEvents.get(0).payload(), notNullValue());
    }

    @Test
    public void shouldTestHandleEventProsecutionCotrUpdated() {
        final UUID hearingId = randomUUID();
        final UUID cotrId = randomUUID();
        final UUID submissionId = randomUUID();

        final Metadata eventEnvelopeMetadata = metadataBuilder()
                .withName("progression.event.prosecution-cotr-updated")
                .withId(randomUUID())
                .build();
        PolarQuestion polarQuestion = PolarQuestion.polarQuestion()
                .withAnswer("Answer")
                .withAttentionRequired(true)
                .withDetails("Details")
                .build();

        final Envelope<ProsecutionCotrUpdated> eventEnvelope = Envelope.envelopeFrom(eventEnvelopeMetadata,
                ProsecutionCotrUpdated.prosecutionCotrUpdated()
                        .withCotrId(cotrId)
                        .withHearingId(hearingId)
                        .withSubmissionId(submissionId)
                        .withCertificationDate(polarQuestion)
                        .withCertifyThatTheProsecutionIsTrialReady(polarQuestion)
                        .withFormCompletedOnBehalfOfProsecutionBy(polarQuestion)
                        .withFurtherProsecutionInformationProvidedAfterCertification(polarQuestion)
                        .build());

        processor.handleEventProsecutionCotrUpdated(eventEnvelope);

        verify(this.sender).send(this.envelopeArgumentCaptor.capture());

        final Envelope<JsonObject> command = this.envelopeArgumentCaptor.getValue();

        assertThat(command.metadata().name(), is("public.progression.cotr-updated"));

        assertThat(command.payload().toString(), isJson(allOf(
                withJsonPath("$.cotrId", is(cotrId.toString())),
                withJsonPath("$.submissionId", is(submissionId.toString())),
                withJsonPath("$.hearingId", is(hearingId.toString()))
                )
        ));
    }

    private JsonEnvelope getEnvelope(final String name) {
        return envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID()).withName(name).build(),
                Json.createObjectBuilder().build());
    }

    private String generateHearingData() {
        return "{\n" +
                "  \"form_id\": \"f8254db1-1683-483e-afb3-b87fde5a0a26\",\n" +
                "  \"description\": \"Pet Form data\",\n" +
                "  \"petDefendants\": \"[]\",\n" +
                "  \"data\":\"{\\n  \\\"field1\\\": \\\"value1\\\",\\n  \\\"field2\\\": \\\"value2\\\"\\n}\",\n" +
                "  \"version\": 1\n" +
                "}";

    }

    private Optional<JsonObject> createCotrDetails() {
        final JsonObject payload = Json.createObjectBuilder()
                .add("cotrDetails", createArrayBuilder().add(buildCotrDetails())
                        .add(buildCotrDetails())).build();
        return Optional.ofNullable(payload);
    }

    private Optional<JsonObject> createNoCotrDetails() {
        final JsonObject payload = Json.createObjectBuilder()
                .add("cotrDetails", createArrayBuilder().build()).build();
        return Optional.ofNullable(payload);


    }
}