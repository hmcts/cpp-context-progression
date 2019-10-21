package uk.gov.moj.cpp.progression.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.core.courts.*;
import uk.gov.justice.hearing.courts.Initiate;
import uk.gov.justice.services.common.converter.ListToJsonArrayConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.apache.activemq.artemis.utils.JsonLoader.createReader;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.ConfirmedDefendant;
import uk.gov.justice.core.courts.ConfirmedHearing;
import uk.gov.justice.core.courts.ConfirmedProsecutionCase;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.HearingLanguage;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.HearingUpdated;
import uk.gov.justice.core.courts.JudicialRole;
import uk.gov.justice.core.courts.JudicialRoleType;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.common.converter.ListToJsonArrayConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
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

    @Mock
    private Sender sender;

    @Spy
    private final Enveloper enveloper = createEnveloper();

    @Spy
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    @InjectMocks
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter();

    @Spy
    private ListToJsonArrayConverter listToJsonArrayConverter;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeArgumentCaptor;

    @Mock
    private ReferenceDataService referenceDataService;

    @InjectMocks
    private ProgressionService progressionService;

    @Mock
    private Function<Object, JsonEnvelope> enveloperFunction;

    @Mock
    private JsonEnvelope finalEnvelope;

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
    private static final String PROGRESSION_COMMAND_HEARING_RESULTED_UPDATED_CASE = "progression.command.hearing-resulted-update-case";
    private static final String PROGRESSION_COMMAND_PREPARE_SUMMONS_DATA = "progression.command.prepare-summons-data";
    private static final String PUBLIC_EVENT_HEARING_DETAIL_CHANGED = "public.hearing-detail-changed";


    @Before
    public void initMocks() {
        setField(this.listToJsonArrayConverter, "mapper", objectMapper);
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

        when(referenceDataService.getOrganisationUnitById(updatedHearing.getCourtCentre().getId(), envelope))
                .thenReturn(Optional.of(generateCourtCentreJson()));
        when(referenceDataService.getJudiciariesByJudiciaryIdList(Arrays.asList(JUDICIARY_ID_1, JUDICIARY_ID_2), envelope))
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
    public void testUpdateCase() {
        final JsonEnvelope envelope = getEnvelope(PROGRESSION_COMMAND_HEARING_RESULTED_UPDATED_CASE);

        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withDefendants(generateDefendantsForCase(randomUUID()))
                .withId(randomUUID()).build();

        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("prosecutionCase", objectToJsonObjectConverter.convert(prosecutionCase))
                .build();

        when(enveloper.withMetadataFrom
                (envelope, PROGRESSION_COMMAND_HEARING_RESULTED_UPDATED_CASE)).thenReturn(enveloperFunction);

        when(enveloperFunction.apply(jsonObject)).thenReturn(finalEnvelope);
        progressionService.updateCase(envelope, prosecutionCase);
        verify(sender).send(finalEnvelope);
    }



    private JsonObject generateJudiciariesJson() throws IOException {
        String jsonString = Resources.toString(Resources.getResource("referenceData.getJudiciariesByIdList.json"), Charset.defaultCharset())
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
        String jsonString = Resources.toString(Resources.getResource("referencedata.query.organisationunits.json"), Charset.defaultCharset())
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
        return Arrays.asList(
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
        return Collections.singletonList(
                ConfirmedDefendant.confirmedDefendant()
                        .withId(defendantId)
                        .build()
        );
    }

    private List<Defendant> generateDefendantsForCase(final UUID defendantId){
        return Collections.singletonList(
                Defendant.defendant()
                        .withId(defendantId)
                        .withProsecutionCaseId(CASE_ID_1)
                        .build()
        );
    }

    private List<HearingDay> generateHearingDays() {
        return Arrays.asList(
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
                .build();
    }

    private CourtCentre generateBasicCourtCentre() {
        return CourtCentre.courtCentre()
                .withId(COURT_CENTRE_ID)
                .build();
    }

    private List<JudicialRole> generateBasicJudiciaryList() {
        return Arrays.asList(
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
        final List<UUID> applicationId = Arrays.asList(randomUUID());
        final Hearing hearing = Hearing.hearing().withId(hearingId).build();
        JsonObject jsonObject = Json.createObjectBuilder()
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
    public void testShouldTransformBoxWorkApplication(){

        final UUID applicationId = UUID.randomUUID();
        final LocalDate dueDate = LocalDate.now().plusDays(2);

        final Hearing expectedHearing = Hearing.hearing()
                .withIsBoxHearing(true).withId(UUID.randomUUID())
                .withCourtApplications(Arrays.asList(CourtApplication.courtApplication()
                        .withId(applicationId)
                        .withDueDate(dueDate).build()))
                .withHearingDays(Arrays.asList(HearingDay.hearingDay()
                        .withListedDurationMinutes(10)
                        .withSittingDay(ZonedDateTimes.fromString("2019-08-12T05:27:17.210Z")).build()))
                .build();


        final HearingListingNeeds hearingListingNeeds = HearingListingNeeds.hearingListingNeeds()
                .withCourtApplications(Arrays.asList(CourtApplication.courtApplication().withDueDate(dueDate).withId(applicationId).build()))
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

}
