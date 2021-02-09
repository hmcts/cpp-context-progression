package uk.gov.moj.cpp.progression.service;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.apache.activemq.artemis.utils.JsonLoader.createObjectBuilder;
import static org.apache.activemq.artemis.utils.JsonLoader.createReader;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.BoxworkApplicationReferred;
import uk.gov.justice.core.courts.Category;
import uk.gov.justice.core.courts.ConfirmedDefendant;
import uk.gov.justice.core.courts.ConfirmedHearing;
import uk.gov.justice.core.courts.ConfirmedOffence;
import uk.gov.justice.core.courts.ConfirmedProsecutionCase;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantsToRemove;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingConfirmed;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.HearingLanguage;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.HearingUpdated;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JudicialRole;
import uk.gov.justice.core.courts.JudicialRoleType;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.NextHearing;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.OffencesToRemove;
import uk.gov.justice.core.courts.Plea;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCasesToRemove;
import uk.gov.justice.core.courts.SeedingHearing;
import uk.gov.justice.core.courts.UpdateHearingForPartialAllocation;
import uk.gov.justice.core.courts.Verdict;
import uk.gov.justice.hearing.courts.Initiate;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ListToJsonArrayConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ProgressionServiceTest {

    private static final UUID CASE_ID_1 = UUID.randomUUID();
    private static final UUID CASE_ID_2 = UUID.randomUUID();
    private static final UUID DEFENDANT_ID_1 = UUID.randomUUID();
    private static final UUID DEFENDANT_ID_2 = UUID.randomUUID();
    private static final UUID COURT_CENTRE_ID = UUID.randomUUID();
    private static final UUID JUDICIARY_ID_1 = UUID.randomUUID();
    private static final UUID JUDICIARY_ID_2 = UUID.randomUUID();
    private static final String HEARING_DATE_1 = "2018-06-01T10:00:00.000Z";
    private static final String HEARING_DATE_2 = "2018-06-04T10:00:00.000Z";
    private static final String HEARING_DATE_3 = "2018-07-01T10:00:00.000Z";
    private static final String COURT_CENTRE_NAME = STRING.next();
    private static final String JUDICIARY_TITLE_1 = STRING.next();
    private static final String JUDICIARY_FIRST_NAME_1 = STRING.next();
    private static final String JUDICIARY_LAST_NAME_1 = STRING.next();
    private static final String JUDICIARY_TITLE_2 = STRING.next();
    private static final String JUDICIARY_FIRST_NAME_2 = STRING.next();
    private static final String JUDICIARY_LAST_NAME_2 = STRING.next();
    private static final String JUDICIARY_TYPE_1 = "RECORDER";
    private static final String JUDICIARY_TYPE_2 = "MAGISTRATE";
    private static final String PROGRESSION_COMMAND_CREATE_HEARING_APPLICATION_LINK = "progression.command.create-hearing-application-link";
    private static final String PROGRESSION_COMMAND_CREATE_HEARING_PROSECUTION_CASE_LINK = "progression.command-link-prosecution-cases-to-hearing";
    private static final String PROGRESSION_COMMAND_HEARING_RESULTED_UPDATED_CASE = "progression.command.hearing-resulted-update-case";
    private static final String PROGRESSION_COMMAND_HEARING_CONFIRMED_UPDATE_CASE_STATUS = "progression.command.hearing-confirmed-update-case-status";
    private static final String PROGRESSION_COMMAND_PREPARE_SUMMONS_DATA = "progression.command.prepare-summons-data";
    private static final String PROGRESSION_UPDATE_DEFENDANT_LISTING_STATUS_COMMAND = "progression.command.update-defendant-listing-status";
    private static final String PUBLIC_EVENT_HEARING_DETAIL_CHANGED = "public.hearing-detail-changed";
    private static final String PROGRESSION_LIST_UNSCHEDULED_HEARING_COMMAND = "progression.command.list-unscheduled-hearing";
    private static final String PROGRESSION_COMMAND_RECORD_UNSCHEDULED_HEARING = "progression.command.record-unscheduled-hearing";
    public static final String PROGRESSION_COMMAND_PREPARE_SUMMONS_DATA_FOR_EXTENDED_HEARING = "progression.command.prepare-summons-data-for-extended-hearing";
    private static final String PROGRESSION_COMMAND_UPDATE_HEARING_FOR_PARTIAL_ALLOCATION = "progression.command.update-hearing-for-partial-allocation";
    public static final String PROGRESSION_QUERY_PROSECUTION_CASES = "progression.query.prosecutioncase";
    public static final String PROGRESSION_COMMAND_UPDATE_DEFENDANT_AS_YOUTH = "progression.command.update-defendant-for-prosecution-case";
    private static final String EMPTY = "";
    @Spy
    private final Enveloper enveloper = createEnveloper();
    @Spy
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
    @Spy
    @InjectMocks
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);
    @Spy
    @InjectMocks
    private JsonObjectToObjectConverter jsonObjectConverter =  new JsonObjectToObjectConverter(objectMapper);
    @Mock
    private Sender sender;
    @Spy
    private ListToJsonArrayConverter listToJsonArrayConverter;
    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeArgumentCaptor;
    @Captor
    private ArgumentCaptor<Envelope> envelopeCaptor;
    @Mock
    private Requester requester;
    @Mock
    private ReferenceDataService referenceDataService;
    @Mock
    private ListingService listingService;
    @InjectMocks
    private ProgressionService progressionService;
    @Mock
    private Function<Object, JsonEnvelope> enveloperFunction;
    @Mock
    private JsonEnvelope finalEnvelope;

    @Before
    public void initMocks() {
        setField(this.listToJsonArrayConverter, "mapper", objectMapper);
        setField(this.jsonObjectConverter, "objectMapper", objectMapper);
        setField(this.listToJsonArrayConverter, "stringToJsonObjectConverter", new StringToJsonObjectConverter());
    }


    @Test
    public void shouldSendPrepareSummonsCommand() {
        final ConfirmedHearing confirmedHearing = generateConfirmedHearingForPrepareSummons();
        final JsonEnvelope prepareSummonsEnvelope = getEnvelope(PROGRESSION_COMMAND_PREPARE_SUMMONS_DATA);

        progressionService.prepareSummonsData(prepareSummonsEnvelope, confirmedHearing);

        verify(sender).send(envelopeArgumentCaptor.capture());

        assertThat(envelopeArgumentCaptor.getValue(), jsonEnvelope(
                metadata().withName(PROGRESSION_COMMAND_PREPARE_SUMMONS_DATA),
                payloadIsJson(
                        allOf(
                                withJsonPath("$.courtCentre.id", is(confirmedHearing.getCourtCentre().getId().toString())),
                                withJsonPath("$.courtCentre.name", is(confirmedHearing.getCourtCentre().getName())),
                                withJsonPath("$.courtCentre.roomId", is(confirmedHearing.getCourtCentre().getRoomId().toString())),
                                withJsonPath("$.courtCentre.roomName", is(confirmedHearing.getCourtCentre().getRoomName())),
                                withJsonPath("$.courtCentre.welshName", is(confirmedHearing.getCourtCentre().getWelshName())),
                                withJsonPath("$.courtCentre.welshRoomName", is(confirmedHearing.getCourtCentre().getWelshRoomName())),
                                withJsonPath("$.hearingDateTime", is(HEARING_DATE_1)),
                                withJsonPath("$.confirmedProsecutionCaseIds[0].id", is(CASE_ID_1.toString())),
                                withJsonPath("$.confirmedProsecutionCaseIds[0].confirmedDefendantIds[0]", is(DEFENDANT_ID_1.toString())),
                                withJsonPath("$.confirmedProsecutionCaseIds[1].id", is(CASE_ID_2.toString())),
                                withJsonPath("$.confirmedProsecutionCaseIds[1].confirmedDefendantIds[0]", is(DEFENDANT_ID_2.toString()))

                        )
                )
                )
        );

    }

    @Test
    public void testPublishHearingDetailChangedPublicEvent() throws Exception {
        final HearingUpdated hearingUpdated = generateHearingUpdated();
        final ConfirmedHearing updatedHearing = hearingUpdated.getUpdatedHearing();
        final JsonEnvelope envelope = getEnvelope(PUBLIC_EVENT_HEARING_DETAIL_CHANGED);

        when(referenceDataService.getOrganisationUnitById(updatedHearing.getCourtCentre().getId(), envelope, requester))
                .thenReturn(Optional.of(generateCourtCentreJson()));
        when(referenceDataService.getJudiciariesByJudiciaryIdList(asList(JUDICIARY_ID_1, JUDICIARY_ID_2), envelope, requester))
                .thenReturn(Optional.of(generateJudiciariesJson()));

        progressionService.publishHearingDetailChangedPublicEvent(envelope, hearingUpdated);

        verify(sender).send(envelopeArgumentCaptor.capture());

        assertThat(envelopeArgumentCaptor.getValue(), jsonEnvelope(
                metadata().withName(PUBLIC_EVENT_HEARING_DETAIL_CHANGED),
                payloadIsJson(
                        allOf(
                                withJsonPath("$.hearing.id", is(updatedHearing.getId().toString())),
                                withJsonPath("$.hearing.type.description", is(updatedHearing.getType().getDescription())),
                                withJsonPath("$.hearing.type.id", is(updatedHearing.getType().getId().toString())),
                                withJsonPath("$.hearing.jurisdictionType", is(updatedHearing.getJurisdictionType().toString())),
                                withJsonPath("$.hearing.reportingRestrictionReason", is(updatedHearing.getReportingRestrictionReason())),
                                withJsonPath("$.hearing.hearingLanguage", is(updatedHearing.getHearingLanguage().toString())),
                                withJsonPath("$.hearing.hearingDays[0].sittingDay", is(HEARING_DATE_1)),
                                withJsonPath("$.hearing.hearingDays[1].sittingDay", is(HEARING_DATE_2)),
                                withJsonPath("$.hearing.hearingDays[2].sittingDay", is(HEARING_DATE_3)),
                                withJsonPath("$.hearing.courtCentre.id", is(COURT_CENTRE_ID.toString())),
                                withJsonPath("$.hearing.courtCentre.name", is(COURT_CENTRE_NAME)),
                                withJsonPath("$.hearing.judiciary[0].judicialId", is(JUDICIARY_ID_1.toString())),
                                withJsonPath("$.hearing.judiciary[0].title", is(JUDICIARY_TITLE_1)),
                                withJsonPath("$.hearing.judiciary[0].firstName", is(JUDICIARY_FIRST_NAME_1)),
                                withJsonPath("$.hearing.judiciary[0].lastName", is(JUDICIARY_LAST_NAME_1)),
                                withJsonPath("$.hearing.judiciary[0].judicialRoleType.judiciaryType", is(updatedHearing.getJudiciary().get(0).getJudicialRoleType().getJudiciaryType())),
                                withJsonPath("$.hearing.judiciary[0].isDeputy", is(updatedHearing.getJudiciary().get(0).getIsDeputy())),
                                withJsonPath("$.hearing.judiciary[0].isBenchChairman", is(updatedHearing.getJudiciary().get(0).getIsBenchChairman())),
                                withJsonPath("$.hearing.judiciary[1].judicialId", is(JUDICIARY_ID_2.toString())),
                                withJsonPath("$.hearing.judiciary[1].title", is(JUDICIARY_TITLE_2)),
                                withJsonPath("$.hearing.judiciary[1].firstName", is(JUDICIARY_FIRST_NAME_2)),
                                withJsonPath("$.hearing.judiciary[1].lastName", is(JUDICIARY_LAST_NAME_2)),
                                withJsonPath("$.hearing.judiciary[1].judicialRoleType.judiciaryType", is(updatedHearing.getJudiciary().get(1).getJudicialRoleType().getJudiciaryType())),
                                withJsonPath("$.hearing.judiciary[1].isDeputy", is(updatedHearing.getJudiciary().get(1).getIsDeputy())),
                                withJsonPath("$.hearing.judiciary[1].isBenchChairman", is(updatedHearing.getJudiciary().get(1).getIsBenchChairman()))
                        )
                )
                )
        );
    }

    @Test
    public void shouldListUnscheduledHearing() throws Exception {
        final Hearing hearing = Hearing.hearing().withId(UUID.randomUUID()).build();
        final JsonEnvelope envelope = getEnvelope(PROGRESSION_LIST_UNSCHEDULED_HEARING_COMMAND);
        progressionService.listUnscheduledHearings(envelope, hearing);
        verify(sender).send(envelopeCaptor.capture());
        assertThat(envelopeCaptor.getValue().metadata().name(), is(PROGRESSION_LIST_UNSCHEDULED_HEARING_COMMAND));
        JsonObject jsonObject = (JsonObject) envelopeCaptor.getValue().payload();
        assertThat(jsonObject.getJsonObject("hearing").getString("id"), is(hearing.getId().toString()));
    }

    public void shouldRecordUnlistedHearing() {
        final UUID hearingId = randomUUID();
        final UUID unscheduledHearingId = randomUUID();

        final Hearing hearing = Hearing.hearing().withId(unscheduledHearingId).build();
        final JsonEnvelope envelope = getEnvelope(PROGRESSION_COMMAND_RECORD_UNSCHEDULED_HEARING);
        progressionService.recordUnlistedHearing(envelope, hearingId, asList(hearing));
        verify(sender).send(envelopeCaptor.capture());
        assertThat(envelopeCaptor.getValue().metadata().name(), is(PROGRESSION_COMMAND_RECORD_UNSCHEDULED_HEARING));
        JsonObject jsonObject = (JsonObject) envelopeCaptor.getValue().payload();
        assertThat(jsonObject.getString("hearingId"), is(hearingId.toString()));
        assertThat(jsonObject.getString("unscheduledHearingIds.length()"), is(unscheduledHearingId.toString()));
        assertThat(jsonObject.getString("unscheduledHearingIds[0]"), is(unscheduledHearingId.toString()));
    }

    @Test
    public void shouldSendUpdateDefendantListingStatusForUnscheduledListing(){
        final List<Hearing> hearings = Arrays.asList(Hearing.hearing().withId(UUID.randomUUID()).build());
        final JsonEnvelope envelope = getEnvelope(PROGRESSION_UPDATE_DEFENDANT_LISTING_STATUS_COMMAND);
        progressionService.sendUpdateDefendantListingStatusForUnscheduledListing(envelope, hearings);
        verify(sender).send(envelopeCaptor.capture());
        assertThat(envelopeCaptor.getValue().metadata().name(), is(PROGRESSION_UPDATE_DEFENDANT_LISTING_STATUS_COMMAND));
        JsonObject jsonObject = (JsonObject) envelopeCaptor.getValue().payload();
        assertThat(jsonObject.getJsonObject("hearing").getString("id"), is(hearings.get(0).getId().toString()));
    }

    @Test
    public void testUpdateCase() {
        final JsonEnvelope envelope = getEnvelope(PROGRESSION_COMMAND_HEARING_RESULTED_UPDATED_CASE);

        final UUID caseId = randomUUID();
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withDefendants(generateDefendantsForCase(randomUUID()))
                .withId(caseId).build();
        final CourtApplication courtApplication = CourtApplication.courtApplication()
                .withLinkedCaseId(caseId)
                .withJudicialResults(singletonList(JudicialResult.judicialResult().withCategory(Category.FINAL).build()))
                .build();
        final List<CourtApplication> courtApplications = singletonList(courtApplication);

        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("prosecutionCase", objectToJsonObjectConverter.convert(prosecutionCase))
                .add("courtApplications", listToJsonArrayConverter.convert(courtApplications))
                .build();

        when(enveloper.withMetadataFrom
                (envelope, PROGRESSION_COMMAND_HEARING_RESULTED_UPDATED_CASE)).thenReturn(enveloperFunction);

        when(enveloperFunction.apply(jsonObject)).thenReturn(finalEnvelope);

        progressionService.updateCase(envelope, prosecutionCase, courtApplications);
        verify(sender).send(finalEnvelope);
    }

    @Test
    public void testUpdateCaseStatus() {
        final JsonEnvelope envelope = getEnvelope(PROGRESSION_COMMAND_HEARING_CONFIRMED_UPDATE_CASE_STATUS);
        final UUID prosecutionCaseId = randomUUID();
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withCaseStatus("INACTIVE")
                .withDefendants(generateDefendantsForCase(randomUUID()))
                .withId(prosecutionCaseId)
                .build();

        final UUID courtApplicationId = randomUUID();
        final CourtApplication courtApplication = CourtApplication.courtApplication()
                .withLinkedCaseId(prosecutionCaseId)
                .withId(courtApplicationId)
                .withApplicationStatus(ApplicationStatus.IN_PROGRESS)
                .build();
        final Hearing hearing = Hearing.hearing()
                .withProsecutionCases(singletonList(prosecutionCase))
                .withCourtApplications(singletonList(courtApplication))
                .build();


        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("prosecutionCase", objectToJsonObjectConverter.convert(prosecutionCase))
                .add("caseStatus", "ACTIVE")
                .build();

        when(enveloper.withMetadataFrom
                (envelope, PROGRESSION_COMMAND_HEARING_CONFIRMED_UPDATE_CASE_STATUS)).thenReturn(enveloperFunction);

        when(enveloperFunction.apply(jsonObject)).thenReturn(finalEnvelope);
        progressionService.updateCaseStatus(envelope, hearing, singletonList(courtApplicationId));
        verify(sender).send(finalEnvelope);
    }

    @Test
    public void testShouldNotRaiseEventForUpdateCaseStatus() {
        final JsonEnvelope envelope = getEnvelope(PROGRESSION_COMMAND_HEARING_CONFIRMED_UPDATE_CASE_STATUS);
        final UUID prosecutionCaseId = randomUUID();
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withCaseStatus("LISTED")
                .withDefendants(generateDefendantsForCase(randomUUID()))
                .withId(prosecutionCaseId)
                .build();

        final UUID courtApplicationId = randomUUID();
        final CourtApplication courtApplication = CourtApplication.courtApplication()
                .withLinkedCaseId(prosecutionCaseId)
                .withId(courtApplicationId)
                .withApplicationStatus(ApplicationStatus.IN_PROGRESS)
                .build();
        final Hearing hearing = Hearing.hearing()
                .withProsecutionCases(singletonList(prosecutionCase))
                .withCourtApplications(singletonList(courtApplication))
                .build();


        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("prosecutionCase", objectToJsonObjectConverter.convert(prosecutionCase))
                .add("caseStatus", "SJP_REFERRAL")
                .build();

        when(enveloper.withMetadataFrom
                (envelope, PROGRESSION_COMMAND_HEARING_CONFIRMED_UPDATE_CASE_STATUS)).thenReturn(enveloperFunction);

        when(enveloperFunction.apply(jsonObject)).thenReturn(finalEnvelope);
        progressionService.updateCaseStatus(envelope, hearing, singletonList(courtApplicationId));
        verifyNoMoreInteractions(sender);
    }


    private JsonObject generateJudiciariesJson() throws IOException {
        final String jsonString = Resources.toString(Resources.getResource("referenceData.getJudiciariesByIdList.json"), Charset.defaultCharset())
                .replace("JUDICIARY_ID_1", JUDICIARY_ID_1.toString())
                .replace("JUDICIARY_TITLE_1", JUDICIARY_TITLE_1)
                .replace("JUDICIARY_FIRST_NAME_1", JUDICIARY_FIRST_NAME_1)
                .replace("JUDICIARY_LAST_NAME_1", JUDICIARY_LAST_NAME_1)
                .replace("JUDICIARY_ID_2", JUDICIARY_ID_2.toString())
                .replace("JUDICIARY_TITLE_2", JUDICIARY_TITLE_2)
                .replace("JUDICIARY_FIRST_NAME_2", JUDICIARY_FIRST_NAME_2)
                .replace("JUDICIARY_LAST_NAME_2", JUDICIARY_LAST_NAME_2)
                .replace("JUDICIARY_TYPE_1", JUDICIARY_TYPE_1)
                .replace("JUDICIARY_TYPE_2", JUDICIARY_TYPE_2);

        return returnAsJson(jsonString);
    }


    private JsonObject generateCourtCentreJson() throws IOException {
        final String jsonString = Resources.toString(Resources.getResource("referencedata.query.organisationunits.json"), Charset.defaultCharset())
                .replace("COURT_CENTRE_ID", COURT_CENTRE_ID.toString())
                .replace("COURT_CENTRE_NAME", COURT_CENTRE_NAME);

        return returnAsJson(jsonString);
    }

    private JsonObject returnAsJson(final String jsonString) {
        try (JsonReader jsonReader = createReader(new StringReader(jsonString))) {
            return jsonReader.readObject();
        }
    }


    private HearingUpdated generateHearingUpdated() {
        return HearingUpdated.hearingUpdated()
                .withUpdatedHearing(generateConfirmedHearingForHearingUpdated())
                .build();
    }

    private ConfirmedHearing generateConfirmedHearingForHearingUpdated() {
        return ConfirmedHearing.confirmedHearing()
                .withId(randomUUID())
                .withType(generateHearingType())
                .withJurisdictionType(JurisdictionType.MAGISTRATES)
                .withReportingRestrictionReason(STRING.next())
                .withHearingLanguage(HearingLanguage.ENGLISH)
                .withHearingDays(generateHearingDays())
                .withCourtCentre(generateBasicCourtCentre())
                .withJudiciary(generateBasicJudiciaryList())
                .withProsecutionCases(generateProsecutionCases())
                .build();
    }

    private HearingType generateHearingType() {
        return HearingType.hearingType()
                .withId(randomUUID())
                .withDescription("Sentence")
                .build();
    }


    private ConfirmedHearing generateConfirmedHearingForPrepareSummons() {
        return ConfirmedHearing.confirmedHearing()
                .withId(randomUUID())
                .withCourtCentre(generateFullCourtCentre())
                .withHearingDays(generateHearingDays())
                .withProsecutionCases(generateProsecutionCases())
                .build();
    }

    private List<ConfirmedProsecutionCase> generateProsecutionCases() {
        return asList(
                ConfirmedProsecutionCase.confirmedProsecutionCase()
                        .withId(CASE_ID_1)
                        .withDefendants(generateDefendants(DEFENDANT_ID_1))
                        .build(),
                ConfirmedProsecutionCase.confirmedProsecutionCase()
                        .withId(CASE_ID_2)
                        .withDefendants(generateDefendants(DEFENDANT_ID_2))
                        .build()
        );
    }

    private List<ConfirmedDefendant> generateDefendants(final UUID defendantId) {
        return singletonList(
                ConfirmedDefendant.confirmedDefendant()
                        .withId(defendantId)
                        .build()
        );
    }

    private List<Defendant> generateDefendantsForCase(final UUID defendantId) {
        return singletonList(
                Defendant.defendant()
                        .withId(defendantId)
                        .withProsecutionCaseId(CASE_ID_1)
                        .build()
        );
    }

    private List<HearingDay> generateHearingDays() {
        return asList(
                HearingDay.hearingDay()
                        .withSittingDay(ZonedDateTimes.fromString(HEARING_DATE_1))
                        .build(),
                HearingDay.hearingDay()
                        .withSittingDay(ZonedDateTimes.fromString(HEARING_DATE_2))
                        .build(),
                HearingDay.hearingDay()
                        .withSittingDay(ZonedDateTimes.fromString(HEARING_DATE_3))
                        .build()
        );
    }

    private JsonEnvelope getEnvelope(final String name) {
        return JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID()).withName(name).build(),
                Json.createObjectBuilder().build());
    }

    private CourtCentre generateFullCourtCentre() {
        return CourtCentre.courtCentre()
                .withId(UUID.fromString("89b10041-b44d-43c8-9b1e-d1b9fee15c93"))
                .withName("00ObpXuu51")
                .withRoomId(UUID.fromString("d7020fe0-cd97-4ce0-84c2-fd00ff0bc48a"))
                .withRoomName("JK2Y7hu0Tc")
                .withWelshName("3IpJDfdfhS")
                .withWelshRoomName("hm60SAXokc")
                .withAddress(Address.address()
                        .withAddress1("Address1")
                        .build())
                .build();
    }

    private CourtCentre generateBasicCourtCentre() {
        return CourtCentre.courtCentre()
                .withId(COURT_CENTRE_ID)
                .build();
    }

    private List<JudicialRole> generateBasicJudiciaryList() {
        return asList(
                JudicialRole.judicialRole()
                        .withIsDeputy(Boolean.TRUE)
                        .withJudicialId(JUDICIARY_ID_1)
                        .withJudicialRoleType(
                                JudicialRoleType.judicialRoleType()
                                        .withJudicialRoleTypeId(randomUUID())
                                        .withJudiciaryType(JUDICIARY_TYPE_1)
                                        .build()
                        )
                        .withIsBenchChairman(Boolean.TRUE)
                        .build(),
                JudicialRole.judicialRole()
                        .withIsDeputy(Boolean.TRUE)
                        .withJudicialId(JUDICIARY_ID_2)
                        .withJudicialRoleType(
                                JudicialRoleType.judicialRoleType()
                                        .withJudicialRoleTypeId(randomUUID())
                                        .withJudiciaryType(JUDICIARY_TYPE_2)
                                        .build()
                        )
                        .withIsBenchChairman(Boolean.TRUE)
                        .build()
        );
    }

    @Test
    public void testCreateHearingApplicationLink() {
        final JsonEnvelope envelope = getEnvelope(PROGRESSION_COMMAND_CREATE_HEARING_APPLICATION_LINK);
        final UUID hearingId = randomUUID();
        final List<UUID> applicationId = asList(randomUUID());
        final Hearing hearing = Hearing.hearing().withId(hearingId).build();
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("hearing", objectToJsonObjectConverter.convert(hearing))
                .add("hearingListingStatus", "HEARING_INITIALISED")
                .add("applicationId", applicationId.get(0).toString())
                .build();
        when(enveloper.withMetadataFrom
                (envelope, PROGRESSION_COMMAND_CREATE_HEARING_APPLICATION_LINK)).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(jsonObject)).thenReturn(finalEnvelope);
        progressionService.linkApplicationsToHearing(envelope, hearing, applicationId, HearingListingStatus.HEARING_INITIALISED);
        verify(sender).send(finalEnvelope);
    }


    @Test
    public void testCreateHearingProsecutionCaseLink() {
        final JsonEnvelope envelope = getEnvelope(PROGRESSION_COMMAND_CREATE_HEARING_PROSECUTION_CASE_LINK);
        final UUID hearingId = randomUUID();
        final List<UUID> caseIds = asList(randomUUID());
        final Hearing hearing = Hearing.hearing().withId(hearingId).build();
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("hearingId", hearingId.toString())
                .add("caseId", caseIds.get(0).toString())
                .build();
        when(enveloper.withMetadataFrom
                (envelope, PROGRESSION_COMMAND_CREATE_HEARING_PROSECUTION_CASE_LINK)).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(jsonObject)).thenReturn(finalEnvelope);
        progressionService.linkProsecutionCasesToHearing(envelope, hearingId, caseIds);
        verify(sender).send(finalEnvelope);
    }

    @Test
    public void testShouldTransformBoxWorkApplication() {

        final UUID applicationId = UUID.randomUUID();
        final LocalDate dueDate = LocalDate.now().plusDays(2);

        final Hearing expectedHearing = Hearing.hearing()
                .withIsBoxHearing(true).withId(UUID.randomUUID())
                .withCourtApplications(asList(CourtApplication.courtApplication()
                        .withId(applicationId)
                        .withDueDate(dueDate).build()))
                .withHearingDays(asList(HearingDay.hearingDay()
                        .withListedDurationMinutes(10)
                        .withSittingDay(ZonedDateTimes.fromString("2019-08-12T05:27:17.210Z")).build()))
                .build();


        final HearingListingNeeds hearingListingNeeds = HearingListingNeeds.hearingListingNeeds()
                .withCourtApplications(asList(CourtApplication.courtApplication().withDueDate(dueDate).withId(applicationId).build()))
                .withListedStartDateTime(ZonedDateTimes.fromString("2019-08-12T05:27:17.210Z"))
                .build();

        final BoxworkApplicationReferred boxworkApplicationReferred = BoxworkApplicationReferred.boxworkApplicationReferred()
                .withHearingRequest(hearingListingNeeds).build();

        final Hearing actualHearing = progressionService.transformBoxWorkApplication(boxworkApplicationReferred);

        assertThat(actualHearing.getCourtApplications().get(0).getId(), CoreMatchers.is(expectedHearing.getCourtApplications().get(0).getId()));
        assertThat(actualHearing.getHearingDays().get(0).getSittingDay(), CoreMatchers.is(expectedHearing.getHearingDays().get(0).getSittingDay()));
        assertThat(actualHearing.getHearingDays().get(0).getListedDurationMinutes(), CoreMatchers.is(expectedHearing.getHearingDays().get(0).getListedDurationMinutes()));
        assertThat(actualHearing.getCourtApplications().get(0).getDueDate(), CoreMatchers.is(expectedHearing.getCourtApplications().get(0).getDueDate()));
        assertThat(actualHearing.getIsBoxHearing(), CoreMatchers.is(expectedHearing.getIsBoxHearing()));

    }

    @Test
    public void shouldSendPrepareSummonsForExtendedHearingCommand() {

        final ConfirmedHearing confirmedHearing = generateConfirmedHearingForPrepareSummons();
        final HearingConfirmed hearingConfirmed = HearingConfirmed.hearingConfirmed()
                .withConfirmedHearing(confirmedHearing)
                .build();
        final JsonEnvelope prepareSummonsEnvelope = getEnvelope(PROGRESSION_COMMAND_PREPARE_SUMMONS_DATA_FOR_EXTENDED_HEARING);
        progressionService.prepareSummonsDataForExtendHearing(prepareSummonsEnvelope, hearingConfirmed);
        verify(sender).send(envelopeArgumentCaptor.capture());

        assertThat(envelopeArgumentCaptor.getValue(), jsonEnvelope(
                metadata().withName(PROGRESSION_COMMAND_PREPARE_SUMMONS_DATA_FOR_EXTENDED_HEARING),
                payloadIsJson(
                        allOf(
                                withJsonPath("$.confirmedHearing.courtCentre.id", is(confirmedHearing.getCourtCentre().getId().toString())),
                                withJsonPath("$.confirmedHearing.courtCentre.name", is(confirmedHearing.getCourtCentre().getName())),
                                withJsonPath("$.confirmedHearing.courtCentre.roomId", is(confirmedHearing.getCourtCentre().getRoomId().toString())),
                                withJsonPath("$.confirmedHearing.courtCentre.roomName", is(confirmedHearing.getCourtCentre().getRoomName())),
                                withJsonPath("$.confirmedHearing.courtCentre.welshName", is(confirmedHearing.getCourtCentre().getWelshName())),
                                withJsonPath("$.confirmedHearing.courtCentre.welshRoomName", is(confirmedHearing.getCourtCentre().getWelshRoomName())),
                                withJsonPath("$.confirmedHearing.prosecutionCases[0].id", is(CASE_ID_1.toString())),
                                withJsonPath("$.confirmedHearing.prosecutionCases[0].defendants[0].id", is(DEFENDANT_ID_1.toString())),
                                withJsonPath("$.confirmedHearing.prosecutionCases[1].id", is(CASE_ID_2.toString())),
                                withJsonPath("$.confirmedHearing.prosecutionCases[1].defendants[0].id", is(DEFENDANT_ID_2.toString()))

                        )
                )
                )
        );

    }

    @Test
    public void shouldUpdateHearingForPartialAllocation() {
        final UUID hearingId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID offenceId = randomUUID();
        final UUID defendantId = randomUUID();

        final UpdateHearingForPartialAllocation updateHearingForPartialAllocation = UpdateHearingForPartialAllocation.updateHearingForPartialAllocation()
                .withHearingId(hearingId)
                .withProsecutionCasesToRemove(asList(ProsecutionCasesToRemove.prosecutionCasesToRemove()
                        .withCaseId(caseId)
                        .withDefendantsToRemove(asList(DefendantsToRemove.defendantsToRemove()
                                .withDefendantId(defendantId)
                                .withOffencesToRemove(asList(OffencesToRemove.offencesToRemove()
                                        .withOffenceId(offenceId)
                                        .build()))
                                .build()))
                        .build()))
                .build();
        final JsonEnvelope envelope = getEnvelope(PROGRESSION_COMMAND_UPDATE_HEARING_FOR_PARTIAL_ALLOCATION);

        progressionService.updateHearingForPartialAllocation(envelope, updateHearingForPartialAllocation);
        verify(sender).send(envelopeArgumentCaptor.capture());

        assertThat(envelopeArgumentCaptor.getValue(), jsonEnvelope(
                metadata().withName(PROGRESSION_COMMAND_UPDATE_HEARING_FOR_PARTIAL_ALLOCATION),
                payloadIsJson(
                        allOf(
                                withJsonPath("$.hearingId", is(hearingId.toString())),
                                withJsonPath("$.prosecutionCasesToRemove[0].caseId", is(caseId.toString())),
                                withJsonPath("$.prosecutionCasesToRemove[0].defendantsToRemove[0].defendantId", is(defendantId.toString())),
                                withJsonPath("$.prosecutionCasesToRemove[0].defendantsToRemove[0].offencesToRemove[0].offenceId", is(offenceId.toString()))

                        )
                )
                )
        );
    }

    @Test
    public void shouldTransformProsecutionCaseWhenProsecutionCaseQueryGetDifferentResult() {

        final UUID caseId = randomUUID();
        final UUID defendant1 = randomUUID();
        final UUID defendant2 = randomUUID();
        final UUID defendant1sOffence1 = randomUUID();
        final UUID defendant1sOffence2 = randomUUID();
        final UUID defendant2sOffence1 = randomUUID();
        final UUID defendant2sOffence2 = randomUUID();
        final List<ConfirmedProsecutionCase> confirmedProsecutionCases = Arrays.asList(ConfirmedProsecutionCase.confirmedProsecutionCase()
                .withId(caseId)
                .withDefendants(asList(ConfirmedDefendant.confirmedDefendant()
                        .withId(defendant2)
                        .withOffences(asList(ConfirmedOffence.confirmedOffence()
                                .withId(defendant2sOffence1)
                                .build()))
                        .build()))
                .build());

        when(enveloper.withMetadataFrom
                (finalEnvelope, PROGRESSION_QUERY_PROSECUTION_CASES)).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(any())).thenReturn(finalEnvelope);
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("prosecutionCase", objectToJsonObjectConverter.convert(
                        buildProsecutionCasesWithTwoDefendantsOffences(caseId, defendant1, defendant2, defendant1sOffence1, defendant1sOffence2, defendant2sOffence1, defendant2sOffence2, false)
                )).build();
        when(finalEnvelope.payloadAsJsonObject()).thenReturn(jsonObject);
        when(requester.requestAsAdmin(any())).thenReturn(finalEnvelope);

        List<ProsecutionCase> prosecutionCases = progressionService.transformProsecutionCase(confirmedProsecutionCases, LocalDate.now(), finalEnvelope);

        assertThat(prosecutionCases.get(0).getCpsOrganisation(), is("A01"));
        assertThat(1, is(prosecutionCases.get(0).getDefendants().size()));
        assertThat(defendant2, is(prosecutionCases.get(0).getDefendants().get(0).getId()));
        assertThat(1, is(prosecutionCases.get(0).getDefendants().get(0).getOffences().size()));
        assertThat(defendant2sOffence1, is(prosecutionCases.get(0).getDefendants().get(0).getOffences().get(0).getId()));
    }

    @Test
    public void shouldCallOnlyUpdateDefendantYouthForProsecutionCaseForAllocatedWhenOneOfDefendantsAllOffencesNotAllocated() {

        final UUID caseId = randomUUID();
        final UUID defendant1 = randomUUID();
        final UUID defendant2 = randomUUID();
        final UUID defendant1sOffence1 = randomUUID();
        final UUID defendant1sOffence2 = randomUUID();
        final UUID defendant2sOffence1 = randomUUID();
        final UUID defendant2sOffence2 = randomUUID();
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withId(caseId)
                .withDefendants(asList(Defendant.defendant()
                        .withId(defendant2)
                        .withIsYouth(true)
                        .withOffences(asList(Offence.offence()
                                .withId(defendant2sOffence1)
                                .build()))
                        .build())
                )
                .build();

        final Initiate hearingInitiate = Initiate.initiate()
                .withHearing(Hearing.hearing()
                        .withProsecutionCases(Arrays.asList(prosecutionCase))
                        .build())
                .build();

        final List<ProsecutionCase> deltaProsecutionCases = Arrays.asList(ProsecutionCase.prosecutionCase()
                .withId(caseId)
                .withDefendants(asList(Defendant.defendant()
                        .withId(defendant2)
                        .withIsYouth(true)
                        .withOffences(asList(
                                Offence.offence()
                                        .withId(defendant2sOffence2)
                                        .build()))
                        .build()))
                .build());

        when(enveloper.withMetadataFrom
                (finalEnvelope, PROGRESSION_QUERY_PROSECUTION_CASES)).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(any())).thenReturn(finalEnvelope);
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("prosecutionCase", objectToJsonObjectConverter.convert(
                        buildProsecutionCasesWithTwoDefendantsOffences(caseId, defendant1, defendant2, defendant1sOffence1, defendant1sOffence2, defendant2sOffence1, defendant2sOffence2, false)
                )).build();
        when(finalEnvelope.payloadAsJsonObject()).thenReturn(jsonObject);
        when(requester.requestAsAdmin(any())).thenReturn(finalEnvelope);
        when(enveloper.withMetadataFrom
                (finalEnvelope, PROGRESSION_COMMAND_UPDATE_DEFENDANT_AS_YOUTH)).thenReturn(enveloperFunction);


        progressionService.updateDefendantYouthForProsecutionCase(finalEnvelope, hearingInitiate, deltaProsecutionCases);

        verify(sender).send(envelopeArgumentCaptor.capture());
    }

    @Test
    public void shouldCallUpdateDefendantYouthForProsecutionCaseWhenOneOfDefendantsAtLeastOneOffencesAllocated() {

        final UUID caseId = randomUUID();
        final UUID defendant1 = randomUUID();
        final UUID defendant2 = randomUUID();
        final UUID defendant1sOffence1 = randomUUID();
        final UUID defendant1sOffence2 = randomUUID();
        final UUID defendant2sOffence1 = randomUUID();
        final UUID defendant2sOffence2 = randomUUID();
        final ProsecutionCase prosecutionCase = buildProsecutionCasesWithTwoDefendantsOffences(caseId, defendant1, defendant2, defendant1sOffence1, defendant1sOffence2, defendant2sOffence1, defendant2sOffence2, true);

        final Initiate hearingInitiate = Initiate.initiate()
                .withHearing(Hearing.hearing()
                        .withProsecutionCases(Arrays.asList(prosecutionCase))
                        .build())
                .build();
        final List<ProsecutionCase> deltaProsecutionCases = Arrays.asList(ProsecutionCase.prosecutionCase()
                .withId(caseId)
                .withDefendants(asList(Defendant.defendant()
                        .withId(defendant2)
                        .withIsYouth(true)
                        .withOffences(asList(Offence.offence()
                                .withId(defendant2sOffence1)
                                .build()))
                        .build()))
                .build());

        when(enveloper.withMetadataFrom
                (finalEnvelope, PROGRESSION_QUERY_PROSECUTION_CASES)).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(any())).thenReturn(finalEnvelope);
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("prosecutionCase", objectToJsonObjectConverter.convert(
                        buildProsecutionCasesWithTwoDefendantsOffences(caseId, defendant1, defendant2, defendant1sOffence1, defendant1sOffence2, defendant2sOffence1, defendant2sOffence2, false)
                )).build();
        when(finalEnvelope.payloadAsJsonObject()).thenReturn(jsonObject);
        when(requester.requestAsAdmin(any())).thenReturn(finalEnvelope);

        when(enveloper.withMetadataFrom
                (finalEnvelope, PROGRESSION_COMMAND_UPDATE_DEFENDANT_AS_YOUTH)).thenReturn(enveloperFunction);


        progressionService.updateDefendantYouthForProsecutionCase(finalEnvelope, hearingInitiate, deltaProsecutionCases);

        verify(sender, times(2)).send(envelopeArgumentCaptor.capture());


    }

    @Test
    public void shouldCallUpdateDefendantYouthForProsecutionCaseWhenFullAllocated() {

        final UUID caseId = randomUUID();
        final UUID defendant1 = randomUUID();
        final UUID defendant2 = randomUUID();
        final UUID defendant1sOffence1 = randomUUID();
        final UUID defendant1sOffence2 = randomUUID();
        final UUID defendant2sOffence1 = randomUUID();
        final UUID defendant2sOffence2 = randomUUID();
        final ProsecutionCase prosecutionCase = buildProsecutionCasesWithTwoDefendantsOffences(caseId, defendant1, defendant2, defendant1sOffence1, defendant1sOffence2, defendant2sOffence1, defendant2sOffence2, true);

        final Initiate hearingInitiate = Initiate.initiate()
                .withHearing(Hearing.hearing()
                        .withProsecutionCases(Arrays.asList(prosecutionCase))
                        .build())
                .build();
        final List<ProsecutionCase> deltaProsecutionCases = emptyList();

        when(enveloper.withMetadataFrom
                (finalEnvelope, PROGRESSION_QUERY_PROSECUTION_CASES)).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(any())).thenReturn(finalEnvelope);
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("prosecutionCase", objectToJsonObjectConverter.convert(
                        buildProsecutionCasesWithTwoDefendantsOffences(caseId, defendant1, defendant2, defendant1sOffence1, defendant1sOffence2, defendant2sOffence1, defendant2sOffence2, false)
                )).build();
        when(finalEnvelope.payloadAsJsonObject()).thenReturn(jsonObject);
        when(requester.requestAsAdmin(any())).thenReturn(finalEnvelope);

        when(enveloper.withMetadataFrom
                (finalEnvelope, PROGRESSION_COMMAND_UPDATE_DEFENDANT_AS_YOUTH)).thenReturn(enveloperFunction);


        progressionService.updateDefendantYouthForProsecutionCase(finalEnvelope, hearingInitiate, deltaProsecutionCases);

        verify(sender, times(2)).send(envelopeArgumentCaptor.capture());


    }

    @Test
    public void shouldRemovePleaWhenMovingFromMagsToCrownAndGuiltyTypeNo() {

        final UUID caseId = randomUUID();
        final UUID defendant1 = randomUUID();
        final UUID defendant1sOffence1 = randomUUID();

        final List<ConfirmedProsecutionCase> confirmedProsecutionCases = Arrays.asList(ConfirmedProsecutionCase.confirmedProsecutionCase()
                .withId(caseId)
                .withDefendants(asList(ConfirmedDefendant.confirmedDefendant()
                        .withId(defendant1)
                        .withOffences(asList(ConfirmedOffence.confirmedOffence()
                                .withId(defendant1sOffence1)
                                .build()))
                        .build()))
                .build());

        final SeedingHearing seedingHearing = SeedingHearing.seedingHearing()
                .withJurisdictionType(JurisdictionType.MAGISTRATES)
                .build();

        when(enveloper.withMetadataFrom
                (finalEnvelope, PROGRESSION_QUERY_PROSECUTION_CASES)).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(any())).thenReturn(finalEnvelope);
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("prosecutionCase", objectToJsonObjectConverter.convert(
                        buildProsecutionCaseWithDefendantWithOffenceWithPlea(caseId, defendant1, defendant1sOffence1, JurisdictionType.CROWN)
                )).build();
        when(finalEnvelope.payloadAsJsonObject()).thenReturn(jsonObject);
        when(requester.requestAsAdmin(any())).thenReturn(finalEnvelope);
        when(referenceDataService.getPleaType(any(),any())).thenReturn(Optional.of(
                Json.createObjectBuilder().add("pleaTypeGuiltyFlag", "No").build()
        ));

        List<ProsecutionCase> prosecutionCases = progressionService.transformProsecutionCase(confirmedProsecutionCases, LocalDate.now(), finalEnvelope, seedingHearing);

        assertThat(prosecutionCases.get(0).getCpsOrganisation(), is("A01"));
        assertThat(1, is(prosecutionCases.get(0).getDefendants().size()));
        assertThat(1, is(prosecutionCases.get(0).getDefendants().get(0).getOffences().size()));
        assertThat(prosecutionCases.get(0).getDefendants().get(0).getOffences().get(0).getPlea(), is(nullValue()));
    }

    @Test
    public void shouldNotRemovePleaWhenMovingFromCrownToMagsAndGuiltyTypeNo() {

        final UUID caseId = randomUUID();
        final UUID defendant1 = randomUUID();
        final UUID defendant1sOffence1 = randomUUID();

        final List<ConfirmedProsecutionCase> confirmedProsecutionCases = Arrays.asList(ConfirmedProsecutionCase.confirmedProsecutionCase()
                .withId(caseId)
                .withDefendants(asList(ConfirmedDefendant.confirmedDefendant()
                        .withId(defendant1)
                        .withOffences(asList(ConfirmedOffence.confirmedOffence()
                                .withId(defendant1sOffence1)
                                .build()))
                        .build()))
                .build());

        final SeedingHearing seedingHearing = SeedingHearing.seedingHearing()
                .withJurisdictionType(JurisdictionType.CROWN)
                .build();

        when(enveloper.withMetadataFrom
                (finalEnvelope, PROGRESSION_QUERY_PROSECUTION_CASES)).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(any())).thenReturn(finalEnvelope);
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("prosecutionCase", objectToJsonObjectConverter.convert(
                        buildProsecutionCaseWithDefendantWithOffenceWithPlea(caseId, defendant1, defendant1sOffence1, JurisdictionType.MAGISTRATES)
                )).build();
        when(finalEnvelope.payloadAsJsonObject()).thenReturn(jsonObject);
        when(requester.requestAsAdmin(any())).thenReturn(finalEnvelope);
        when(referenceDataService.getPleaType(any(),any())).thenReturn(Optional.of(
                Json.createObjectBuilder().add("pleaTypeGuiltyFlag", "No").build()
        ));

        List<ProsecutionCase> prosecutionCases = progressionService.transformProsecutionCase(confirmedProsecutionCases, LocalDate.now(), finalEnvelope, seedingHearing);

        assertThat(prosecutionCases.get(0).getCpsOrganisation(), is("A01"));
        assertThat(1, is(prosecutionCases.get(0).getDefendants().size()));
        assertThat(1, is(prosecutionCases.get(0).getDefendants().get(0).getOffences().size()));
        assertThat(prosecutionCases.get(0).getDefendants().get(0).getOffences().get(0).getPlea(), is(notNullValue()));
    }

    @Test
    public void shouldNotRemovePleaWhenMovingFromMagsToMagsAndGuiltyTypeNo() {

        final UUID caseId = randomUUID();
        final UUID defendant1 = randomUUID();
        final UUID defendant1sOffence1 = randomUUID();

        final List<ConfirmedProsecutionCase> confirmedProsecutionCases = Arrays.asList(ConfirmedProsecutionCase.confirmedProsecutionCase()
                .withId(caseId)
                .withDefendants(asList(ConfirmedDefendant.confirmedDefendant()
                        .withId(defendant1)
                        .withOffences(asList(ConfirmedOffence.confirmedOffence()
                                .withId(defendant1sOffence1)
                                .build()))
                        .build()))
                .build());

        final SeedingHearing seedingHearing = SeedingHearing.seedingHearing()
                .withJurisdictionType(JurisdictionType.MAGISTRATES)
                .build();

        when(enveloper.withMetadataFrom
                (finalEnvelope, PROGRESSION_QUERY_PROSECUTION_CASES)).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(any())).thenReturn(finalEnvelope);
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("prosecutionCase", objectToJsonObjectConverter.convert(
                        buildProsecutionCaseWithDefendantWithOffenceWithPlea(caseId, defendant1, defendant1sOffence1, JurisdictionType.MAGISTRATES)
                )).build();
        when(finalEnvelope.payloadAsJsonObject()).thenReturn(jsonObject);
        when(requester.requestAsAdmin(any())).thenReturn(finalEnvelope);
        when(referenceDataService.getPleaType(any(),any())).thenReturn(Optional.of(
                Json.createObjectBuilder().add("pleaTypeGuiltyFlag", "No").build()
        ));

        List<ProsecutionCase> prosecutionCases = progressionService.transformProsecutionCase(confirmedProsecutionCases, LocalDate.now(), finalEnvelope, seedingHearing);

        assertThat(prosecutionCases.get(0).getCpsOrganisation(), is("A01"));
        assertThat(1, is(prosecutionCases.get(0).getDefendants().size()));
        assertThat(1, is(prosecutionCases.get(0).getDefendants().get(0).getOffences().size()));
        assertThat(prosecutionCases.get(0).getDefendants().get(0).getOffences().get(0).getPlea(), is(notNullValue()));
    }

    @Test
    public void shouldNotRemovePleaWhenMovingFromMagsToCrownAndGuiltyTypeYes() {

        final UUID caseId = randomUUID();
        final UUID defendant1 = randomUUID();
        final UUID defendant1sOffence1 = randomUUID();

        final List<ConfirmedProsecutionCase> confirmedProsecutionCases = Arrays.asList(ConfirmedProsecutionCase.confirmedProsecutionCase()
                .withId(caseId)
                .withDefendants(asList(ConfirmedDefendant.confirmedDefendant()
                        .withId(defendant1)
                        .withOffences(asList(ConfirmedOffence.confirmedOffence()
                                .withId(defendant1sOffence1)
                                .build()))
                        .build()))
                .build());

        final SeedingHearing seedingHearing = SeedingHearing.seedingHearing()
                .withJurisdictionType(JurisdictionType.MAGISTRATES)
                .build();

        when(enveloper.withMetadataFrom
                (finalEnvelope, PROGRESSION_QUERY_PROSECUTION_CASES)).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(any())).thenReturn(finalEnvelope);
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("prosecutionCase", objectToJsonObjectConverter.convert(
                        buildProsecutionCaseWithDefendantWithOffenceWithPlea(caseId, defendant1, defendant1sOffence1,JurisdictionType.CROWN)
                )).build();
        when(finalEnvelope.payloadAsJsonObject()).thenReturn(jsonObject);
        when(requester.requestAsAdmin(any())).thenReturn(finalEnvelope);
        when(referenceDataService.getPleaType(any(),any())).thenReturn(Optional.of(
                Json.createObjectBuilder().add("pleaTypeGuiltyFlag", "Yes").build()
        ));

        List<ProsecutionCase> prosecutionCases = progressionService.transformProsecutionCase(confirmedProsecutionCases, LocalDate.now(), finalEnvelope, seedingHearing);

        assertThat(prosecutionCases.get(0).getCpsOrganisation(), is("A01"));
        assertThat(1, is(prosecutionCases.get(0).getDefendants().size()));
        assertThat(1, is(prosecutionCases.get(0).getDefendants().get(0).getOffences().size()));
        assertThat(prosecutionCases.get(0).getDefendants().get(0).getOffences().get(0).getPlea(), is(notNullValue()));
    }

    @Test
    public void shouldNotRemovePleaWhenMovingFromMagsToCrownAndGuiltyTypeNoAndHasVerdict() {

        final UUID caseId = randomUUID();
        final UUID defendant1 = randomUUID();
        final UUID defendant1sOffence1 = randomUUID();

        final List<ConfirmedProsecutionCase> confirmedProsecutionCases = Arrays.asList(ConfirmedProsecutionCase.confirmedProsecutionCase()
                .withId(caseId)
                .withDefendants(asList(ConfirmedDefendant.confirmedDefendant()
                        .withId(defendant1)
                        .withOffences(asList(ConfirmedOffence.confirmedOffence()
                                .withId(defendant1sOffence1)
                                .build()))
                        .build()))
                .build());

        final SeedingHearing seedingHearing = SeedingHearing.seedingHearing()
                .withJurisdictionType(JurisdictionType.MAGISTRATES)
                .build();

        when(enveloper.withMetadataFrom
                (finalEnvelope, PROGRESSION_QUERY_PROSECUTION_CASES)).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(any())).thenReturn(finalEnvelope);
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("prosecutionCase", objectToJsonObjectConverter.convert(
                        buildProsecutionCaseWithDefendantWithOffenceWithPleaWithVerdict(caseId, defendant1, defendant1sOffence1, JurisdictionType.CROWN)
                )).build();
        when(finalEnvelope.payloadAsJsonObject()).thenReturn(jsonObject);
        when(requester.requestAsAdmin(any())).thenReturn(finalEnvelope);
        when(referenceDataService.getPleaType(any(),any())).thenReturn(Optional.of(
                Json.createObjectBuilder().add("pleaTypeGuiltyFlag", "No").build()
        ));

        List<ProsecutionCase> prosecutionCases = progressionService.transformProsecutionCase(confirmedProsecutionCases, LocalDate.now(), finalEnvelope, seedingHearing);

        assertThat(prosecutionCases.get(0).getCpsOrganisation(), is("A01"));
        assertThat(1, is(prosecutionCases.get(0).getDefendants().size()));
        assertThat(1, is(prosecutionCases.get(0).getDefendants().get(0).getOffences().size()));
        assertThat(prosecutionCases.get(0).getDefendants().get(0).getOffences().get(0).getPlea(), is(notNullValue()));
    }

    @Test
    public void shouldTransformCourtCentre() {
        final UUID courtCentreId = randomUUID();
        final String address1 = "ADDRESS1";
        final JsonObject courtCentreJson = createObjectBuilder()
                .add("oucodeL3Name", "Lavender Hill Magistrates Court")
                .add("address1", address1)
                .build();
        when(referenceDataService.getOrganisationUnitById(courtCentreId, finalEnvelope, requester)).thenReturn(Optional.of(courtCentreJson));
        final ConfirmedHearing confirmedHearing = ConfirmedHearing.confirmedHearing()
                .withId(randomUUID())
                .withHearingDays(asList(HearingDay.hearingDay()
                        .withSittingDay(ZonedDateTime.now()).build()))
                .withCourtCentre(CourtCentre.courtCentre()
                        .withId(courtCentreId).build())
                .build();

        final Hearing hearing = progressionService.transformConfirmedHearing(confirmedHearing, finalEnvelope);
        assertThat(hearing.getCourtCentre().getAddress().getAddress1(), is(address1));
        assertThat(hearing.getCourtCentre().getAddress().getAddress2(), is(EMPTY));
        assertThat(hearing.getCourtCentre().getAddress().getAddress3(), is(EMPTY));
        assertThat(hearing.getCourtCentre().getAddress().getAddress4(), is(EMPTY));
        assertThat(hearing.getCourtCentre().getAddress().getAddress5(), is(EMPTY));
        assertThat(hearing.getCourtCentre().getAddress().getPostcode(), nullValue());
    }

    private ProsecutionCase buildProsecutionCasesWithTwoDefendantsOffences(UUID caseId, UUID defendant1, UUID defendant2, UUID defendant1sOffence1, UUID defendant1sOffence2, UUID defendant2sOffence1, UUID defendant2sOffence2, boolean youth) {
        return ProsecutionCase.prosecutionCase()
                .withId(caseId)
                .withCpsOrganisation("A01")
                .withDefendants(asList(Defendant.defendant()
                                .withId(defendant1)
                                .withIsYouth(youth)
                                .withOffences(asList(Offence.offence()
                                                .withId(defendant1sOffence1)
                                                .build(),
                                        Offence.offence()
                                                .withId(defendant1sOffence2)
                                                .build()))
                                .build(),
                        Defendant.defendant()
                                .withId(defendant2)
                                .withIsYouth(youth)
                                .withOffences(asList(Offence.offence()
                                                .withId(defendant2sOffence1)
                                                .build(),
                                        Offence.offence()
                                                .withId(defendant2sOffence2)
                                                .build()))
                                .build()))
                .build();
    }

    private ProsecutionCase buildProsecutionCaseWithDefendantWithOffenceWithPlea(UUID caseId, UUID defendant, UUID offence, JurisdictionType jurisdictionType) {
        return ProsecutionCase.prosecutionCase()
                .withId(caseId)
                .withCpsOrganisation("A01")
                .withDefendants(asList(Defendant.defendant()
                                .withId(defendant)
                                .withIsYouth(Boolean.FALSE)
                                .withOffences(asList(Offence.offence()
                                                .withId(offence)
                                                .withPlea(Plea.plea().withPleaValue("NOT_GUILTY").build())
                                                .withJudicialResults(asList(
                                                        JudicialResult.judicialResult()
                                                                .withNextHearing(NextHearing.nextHearing().withJurisdictionType(JurisdictionType.MAGISTRATES).build())
                                                                .build(),
                                                        JudicialResult.judicialResult()
                                                                .build(),
                                                        JudicialResult.judicialResult()
                                                                .withNextHearing(NextHearing.nextHearing().withJurisdictionType(jurisdictionType).build())
                                                                .build()))
                                                .build()
                                        ))
                                .build()
                      ))
                .build();
    }

    private ProsecutionCase buildProsecutionCaseWithDefendantWithOffenceWithPleaWithVerdict(UUID caseId, UUID defendant, UUID offence, JurisdictionType jurisdictionType) {
        return ProsecutionCase.prosecutionCase()
                .withId(caseId)
                .withCpsOrganisation("A01")
                .withDefendants(asList(Defendant.defendant()
                        .withId(defendant)
                        .withIsYouth(Boolean.FALSE)
                        .withOffences(asList(Offence.offence()
                                .withId(offence)
                                .withPlea(Plea.plea().withPleaValue("NOT_GUILTY").build())
                                .withVerdict(Verdict.verdict().build())
                                .withJudicialResults(asList(
                                        JudicialResult.judicialResult()
                                                .withNextHearing(NextHearing.nextHearing().withJurisdictionType(JurisdictionType.MAGISTRATES).build())
                                                .build(),
                                        JudicialResult.judicialResult()
                                                .build(),
                                        JudicialResult.judicialResult()
                                                .withNextHearing(NextHearing.nextHearing().withJurisdictionType(jurisdictionType).build())
                                                .build()))
                                .build()
                        ))
                        .build()
                ))
                .build();
    }

}
