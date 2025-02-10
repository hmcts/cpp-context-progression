package uk.gov.moj.cpp.progression.service;

import static com.google.common.io.Resources.getResource;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.nio.charset.Charset.defaultCharset;
import static java.util.Arrays.asList;
import static java.util.Collections.EMPTY_LIST;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static javax.json.Json.createReader;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.CourtApplication.courtApplication;
import static uk.gov.justice.core.courts.CourtApplicationCase.courtApplicationCase;
import static uk.gov.justice.core.courts.CourtApplicationType.courtApplicationType;
import static uk.gov.justice.core.courts.DefendantJudicialResult.defendantJudicialResult;
import static uk.gov.justice.core.courts.JudicialResult.judicialResult;
import static uk.gov.justice.listing.events.PublicListingNewDefendantAddedForCourtProceedings.publicListingNewDefendantAddedForCourtProceedings;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.ApplicantCounsel;
import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.ApprovalRequest;
import uk.gov.justice.core.courts.BoxworkApplicationReferred;
import uk.gov.justice.core.courts.CompanyRepresentative;
import uk.gov.justice.core.courts.ConfirmedDefendant;
import uk.gov.justice.core.courts.ConfirmedHearing;
import uk.gov.justice.core.courts.ConfirmedOffence;
import uk.gov.justice.core.courts.ConfirmedProsecutionCase;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationPartyAttendance;
import uk.gov.justice.core.courts.CourtApplicationPartyCounsel;
import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.CourtOrder;
import uk.gov.justice.core.courts.CourtOrderOffence;
import uk.gov.justice.core.courts.CrackedIneffectiveTrial;
import uk.gov.justice.core.courts.DefenceCounsel;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantAttendance;
import uk.gov.justice.core.courts.DefendantJudicialResult;
import uk.gov.justice.core.courts.DefendantsToRemove;
import uk.gov.justice.core.courts.FutureSummonsHearing;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingCaseNote;
import uk.gov.justice.core.courts.HearingConfirmed;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.HearingLanguage;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.HearingUpdatedProcessed;
import uk.gov.justice.core.courts.InitiateApplicationForCaseRequested;
import uk.gov.justice.core.courts.InterpreterIntermediary;
import uk.gov.justice.core.courts.JudicialResultCategory;
import uk.gov.justice.core.courts.JudicialRole;
import uk.gov.justice.core.courts.JudicialRoleType;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.LinkType;
import uk.gov.justice.core.courts.ListHearingRequest;
import uk.gov.justice.core.courts.LjaDetails;
import uk.gov.justice.core.courts.NextHearing;
import uk.gov.justice.core.courts.NextHearingsRequested;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.OffencesToRemove;
import uk.gov.justice.core.courts.Plea;
import uk.gov.justice.core.courts.PrepareSummonsData;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCasesToRemove;
import uk.gov.justice.core.courts.ProsecutionCounsel;
import uk.gov.justice.core.courts.ReferralReason;
import uk.gov.justice.core.courts.RespondentCounsel;
import uk.gov.justice.core.courts.SeedingHearing;
import uk.gov.justice.core.courts.UpdateHearingForPartialAllocation;
import uk.gov.justice.core.courts.Verdict;
import uk.gov.justice.core.courts.YouthCourt;
import uk.gov.justice.hearing.courts.Initiate;
import uk.gov.justice.listing.courts.ListNextHearingsV3;
import uk.gov.justice.listing.events.PublicListingNewDefendantAddedForCourtProceedings;
import uk.gov.justice.progression.courts.BookingReferenceCourtScheduleIds;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ListToJsonArrayConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.json.JsonSchemaValidator;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.exception.DataValidationException;
import uk.gov.moj.cpp.progression.processor.exceptions.CourtApplicationAndCaseNotFoundException;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"squid:S1607"})
public class ProgressionServiceTest {

    private static final UUID CASE_ID_1 = randomUUID();
    private static final UUID CASE_ID_2 = randomUUID();
    private static final UUID DEFENDANT_ID_1 = randomUUID();
    private static final UUID DEFENDANT_ID_2 = randomUUID();
    private static final UUID COURT_CENTRE_ID = randomUUID();
    private static final UUID JUDICIARY_ID_1 = randomUUID();
    private static final UUID JUDICIARY_ID_2 = randomUUID();
    public static final String PROGRESSION_COMMAND_PREPARE_SUMMONS_DATA_FOR_EXTENDED_HEARING = "progression.command.prepare-summons-data-for-extended-hearing";
    public static final String PROGRESSION_QUERY_PROSECUTION_CASES = "progression.query.prosecutioncase-v2";
    public static final String PROGRESSION_COMMAND_UPDATE_DEFENDANT_AS_YOUTH = "progression.command.update-defendant-for-prosecution-case";
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
    private static final String PROGRESSION_UPDATE_DEFENDANT_LISTING_STATUS_COMMAND_V3 = "progression.command.update-defendant-listing-status-v3";
    private static final String PUBLIC_EVENT_HEARING_DETAIL_CHANGED = "public.hearing-detail-changed";
    private static final String PROGRESSION_LIST_UNSCHEDULED_HEARING_COMMAND = "progression.command.list-unscheduled-hearing";
    private static final String PROGRESSION_COMMAND_RECORD_UNSCHEDULED_HEARING = "progression.command.record-unscheduled-hearing";
    private static final String PROGRESSION_COMMAND_UPDATE_HEARING_FOR_PARTIAL_ALLOCATION = "progression.command.update-hearing-for-partial-allocation";
    private static final String SENT_FOR_LISTING = "SENT_FOR_LISTING";
    private static final String APPLICATION_AAAG = "progression.query.application.aaag.json";

    private static final String PROGRESSION_CREATE_HEARING_FOR_APPLICATION_COMMAND = "progression.command.create-hearing-for-application";
    private static final String HEARING_LISTING_STATUS = "hearingListingStatus";

    private static final String PROGRESSION_GENERATE_PUBLIC_LIST_OPA_NOTICE = "progression.event.opa-public-list-notice-requested";
    private static final String PROGRESSION_GENERATE_PRESS_LIST_OPA_NOTICE = "progression.event.opa-press-list-notice-requested";
    private static final String PROGRESSION_GENERATE_RESULT_LIST_OPA_NOTICE = "progression.event.opa-result-list-notice-requested";

    private static final String PROGRESSION_QUERY_PUBLIC_LIST_OPA_NOTICES = "progression.query.public-list-opa-notices";
    private static final String PROGRESSION_QUERY_PRESS_LIST_OPA_NOTICES = "progression.query.press-list-opa-notices";
    private static final String PROGRESSION_QUERY_RESULT_LIST_OPA_NOTICES = "progression.query.result-list-opa-notices";


    private static final String EMPTY = "";


    private static final String ESTIMATED_DURATION = "1 week";
    public static final String PROGRESSION_EVENT_NEXT_HEARINGS_REQUESTED = "progression.event.next-hearings-requested";

    @Spy
    private final Enveloper enveloper = createEnveloper();
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
    @Spy
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(new ObjectMapperProducer().objectMapper());
    @Spy
    private JsonObjectToObjectConverter jsonObjectConverter = new JsonObjectToObjectConverter(new ObjectMapperProducer().objectMapper());
    @Spy
    private StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();
    @Spy
    private ObjectToJsonValueConverter objectToJsonValueConverter = new ObjectToJsonValueConverter(new ObjectMapperProducer().objectMapper());
    @Mock
    private Sender sender;
    @Spy
    private ListToJsonArrayConverter<CourtApplication> listToJsonArrayConverter;
    @Spy
    private ListToJsonArrayConverter<DefendantJudicialResult> resultListToJsonArrayConverter;
    @Spy
    private ListToJsonArrayConverter<ListHearingRequest> hearingRequestListToJsonArrayConverter;
    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeArgumentCaptor;
    @Captor
    private ArgumentCaptor<Envelope<PrepareSummonsData>> typedEnvelopeArgumentCaptor;
    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeCaptor;
    @Mock
    private Requester requester;
    @Mock
    private RefDataService referenceDataService;
    @Mock
    private ListingService listingService;
    @Spy
    @InjectMocks
    private ProgressionService progressionService;
    @Mock
    private Function<Object, JsonEnvelope> enveloperFunction;
    @Mock
    private JsonEnvelope finalEnvelope;
    @Mock
    private JsonSchemaValidator jsonSchemaValidator;

    @BeforeEach
    public void initMocks() {
        setField(this.listToJsonArrayConverter, "mapper", objectMapper);
        setField(this.jsonObjectConverter, "objectMapper", objectMapper);
        setField(this.listToJsonArrayConverter, "stringToJsonObjectConverter", new StringToJsonObjectConverter());
        setField(this.resultListToJsonArrayConverter, "mapper", objectMapper);
        setField(this.resultListToJsonArrayConverter, "stringToJsonObjectConverter", new StringToJsonObjectConverter());
        setField(this.hearingRequestListToJsonArrayConverter, "mapper", objectMapper);
        setField(this.hearingRequestListToJsonArrayConverter, "stringToJsonObjectConverter", new StringToJsonObjectConverter());
    }

    @Test
    public void shouldSendPrepareSummonsCommand() {
        final ConfirmedHearing confirmedHearing = generateConfirmedHearingForPrepareSummons();
        final JsonEnvelope prepareSummonsEnvelope = getEnvelope(PROGRESSION_COMMAND_PREPARE_SUMMONS_DATA);

        progressionService.prepareSummonsData(prepareSummonsEnvelope, confirmedHearing);

        verify(sender).send(envelopeArgumentCaptor.capture());

        assertThat(envelopeArgumentCaptor.getValue(), jsonEnvelope(metadata().withName(PROGRESSION_COMMAND_PREPARE_SUMMONS_DATA), payloadIsJson(allOf(withJsonPath("$.courtCentre.id", is(confirmedHearing.getCourtCentre().getId().toString())), withJsonPath("$.courtCentre.name", is(confirmedHearing.getCourtCentre().getName())), withJsonPath("$.courtCentre.roomId", is(confirmedHearing.getCourtCentre().getRoomId().toString())), withJsonPath("$.courtCentre.roomName", is(confirmedHearing.getCourtCentre().getRoomName())), withJsonPath("$.courtCentre.welshName", is(confirmedHearing.getCourtCentre().getWelshName())), withJsonPath("$.courtCentre.welshRoomName", is(confirmedHearing.getCourtCentre().getWelshRoomName())), withJsonPath("$.hearingDateTime", is(HEARING_DATE_1)), withJsonPath("$.confirmedProsecutionCaseIds[0].id", is(CASE_ID_1.toString())), withJsonPath("$.confirmedProsecutionCaseIds[0].confirmedDefendantIds[0]", is(DEFENDANT_ID_1.toString())), withJsonPath("$.confirmedProsecutionCaseIds[1].id", is(CASE_ID_2.toString())), withJsonPath("$.confirmedProsecutionCaseIds[1].confirmedDefendantIds[0]", is(DEFENDANT_ID_2.toString()))

        ))));
    }

    @Test
    public void shouldSendPrepareSummonsCommandWhenDefendantAddedToCourtProceedingInListing() {
        final PublicListingNewDefendantAddedForCourtProceedings publicEventPayload = getDefendantAddedPayload();
        final Envelope<PublicListingNewDefendantAddedForCourtProceedings> prepareSummonsEnvelope = getTypedEnvelope(PROGRESSION_COMMAND_PREPARE_SUMMONS_DATA, publicEventPayload);

        progressionService.prepareSummonsDataForAddedDefendant(prepareSummonsEnvelope);

        verify(sender).send(typedEnvelopeArgumentCaptor.capture());

        final PrepareSummonsData payload = typedEnvelopeArgumentCaptor.getValue().payload();
        assertThat(typedEnvelopeArgumentCaptor.getValue().metadata().name(), is(PROGRESSION_COMMAND_PREPARE_SUMMONS_DATA));
        assertThat(payload.getHearingDateTime(), is(publicEventPayload.getHearingDateTime()));
        assertThat(payload.getHearingId(), is(publicEventPayload.getHearingId()));
        assertThat(payload.getConfirmedProsecutionCaseIds().get(0).getId(), is(publicEventPayload.getCaseId()));
        assertThat(payload.getConfirmedProsecutionCaseIds().get(0).getConfirmedDefendantIds().get(0), is(publicEventPayload.getDefendantId()));
        assertThat(payload.getCourtCentre().getId(), is(publicEventPayload.getCourtCentre().getId()));
        assertThat(payload.getCourtCentre().getRoomId(), is(publicEventPayload.getCourtCentre().getRoomId()));
    }

    @Test
    public void testPublishHearingDetailChangedPublicEvent() throws Exception {
        final HearingUpdatedProcessed hearingUpdatedProcessed = generateHearingUpdated();
        final ConfirmedHearing updatedHearing = hearingUpdatedProcessed.getConfirmedHearing();
        final JsonEnvelope envelope = getEnvelope(PUBLIC_EVENT_HEARING_DETAIL_CHANGED);

        when(referenceDataService.getOrganisationUnitById(updatedHearing.getCourtCentre().getId(), envelope, requester)).thenReturn(of(generateCourtCentreJson()));
        when(referenceDataService.getJudiciariesByJudiciaryIdList(asList(JUDICIARY_ID_1, JUDICIARY_ID_2), envelope, requester)).thenReturn(of(generateJudiciariesJson()));

        progressionService.publishHearingDetailChangedPublicEvent(envelope, hearingUpdatedProcessed.getConfirmedHearing());

        verify(sender).send(envelopeArgumentCaptor.capture());

        assertThat(envelopeArgumentCaptor.getValue(), jsonEnvelope(metadata().withName(PUBLIC_EVENT_HEARING_DETAIL_CHANGED), payloadIsJson(allOf(withJsonPath("$.hearing.id", is(updatedHearing.getId().toString())), withJsonPath("$.hearing.type.description", is(updatedHearing.getType().getDescription())), withJsonPath("$.hearing.type.id", is(updatedHearing.getType().getId().toString())), withJsonPath("$.hearing.jurisdictionType", is(updatedHearing.getJurisdictionType().toString())), withJsonPath("$.hearing.reportingRestrictionReason", is(updatedHearing.getReportingRestrictionReason())), withJsonPath("$.hearing.hearingLanguage", is(updatedHearing.getHearingLanguage().toString())), withJsonPath("$.hearing.hearingDays[0].sittingDay", is(HEARING_DATE_1)), withJsonPath("$.hearing.hearingDays[1].sittingDay", is(HEARING_DATE_2)), withJsonPath("$.hearing.hearingDays[2].sittingDay", is(HEARING_DATE_3)), withJsonPath("$.hearing.courtCentre.id", is(COURT_CENTRE_ID.toString())), withJsonPath("$.hearing.courtCentre.name", is(COURT_CENTRE_NAME)), withJsonPath("$.hearing.judiciary[0].judicialId", is(JUDICIARY_ID_1.toString())), withJsonPath("$.hearing.judiciary[0].title", is(JUDICIARY_TITLE_1)), withJsonPath("$.hearing.judiciary[0].firstName", is(JUDICIARY_FIRST_NAME_1)), withJsonPath("$.hearing.judiciary[0].lastName", is(JUDICIARY_LAST_NAME_1)), withJsonPath("$.hearing.judiciary[0].judicialRoleType.judiciaryType", is(updatedHearing.getJudiciary().get(0).getJudicialRoleType().getJudiciaryType())), withJsonPath("$.hearing.judiciary[0].isDeputy", is(updatedHearing.getJudiciary().get(0).getIsDeputy())), withJsonPath("$.hearing.judiciary[0].isBenchChairman", is(updatedHearing.getJudiciary().get(0).getIsBenchChairman())), withJsonPath("$.hearing.judiciary[1].judicialId", is(JUDICIARY_ID_2.toString())), withJsonPath("$.hearing.judiciary[1].title", is(JUDICIARY_TITLE_2)), withJsonPath("$.hearing.judiciary[1].firstName", is(JUDICIARY_FIRST_NAME_2)), withJsonPath("$.hearing.judiciary[1].lastName", is(JUDICIARY_LAST_NAME_2)), withJsonPath("$.hearing.judiciary[1].judicialRoleType.judiciaryType", is(updatedHearing.getJudiciary().get(1).getJudicialRoleType().getJudiciaryType())), withJsonPath("$.hearing.judiciary[1].isDeputy", is(updatedHearing.getJudiciary().get(1).getIsDeputy())), withJsonPath("$.hearing.judiciary[1].isBenchChairman", is(updatedHearing.getJudiciary().get(1).getIsBenchChairman()))))));
    }

    @Test
    public void testPublishHearingDetailChangedPublicEventShouldNotRaiseExceptionWhenJudiciaryNotFoundInRefData() throws Exception {
        final HearingUpdatedProcessed hearingUpdatedProcessed = generateHearingUpdated();
        final ConfirmedHearing updatedHearing = hearingUpdatedProcessed.getConfirmedHearing();
        final JsonEnvelope envelope = getEnvelope(PUBLIC_EVENT_HEARING_DETAIL_CHANGED);

        when(referenceDataService.getOrganisationUnitById(updatedHearing.getCourtCentre().getId(), envelope, requester))
                .thenReturn(of(generateCourtCentreJson()));
        when(referenceDataService.getJudiciariesByJudiciaryIdList(asList(JUDICIARY_ID_1, JUDICIARY_ID_2), envelope, requester))
                .thenReturn(of(generateJudiciariesJsonWithMissingJudiciary()));

        progressionService.publishHearingDetailChangedPublicEvent(envelope, hearingUpdatedProcessed.getConfirmedHearing());

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
                                        withJsonPath("$.hearing.judiciary[1].judicialRoleType.judiciaryType", is(updatedHearing.getJudiciary().get(1).getJudicialRoleType().getJudiciaryType())),
                                        withJsonPath("$.hearing.judiciary[1].isDeputy", is(updatedHearing.getJudiciary().get(1).getIsDeputy())),
                                        withJsonPath("$.hearing.judiciary[1].isBenchChairman", is(updatedHearing.getJudiciary().get(1).getIsBenchChairman()))
                                )
                        )
                )
        );
    }

    @Test
    public void testPublishHearingDetailChangedPublicEventShouldNotRaiseExceptionWhenNoJudiciaryNotFoundInRefData() throws Exception {
        final HearingUpdatedProcessed hearingUpdatedProcessed = generateHearingUpdated();
        final ConfirmedHearing updatedHearing = hearingUpdatedProcessed.getConfirmedHearing();
        final JsonEnvelope envelope = getEnvelope(PUBLIC_EVENT_HEARING_DETAIL_CHANGED);

        when(referenceDataService.getOrganisationUnitById(updatedHearing.getCourtCentre().getId(), envelope, requester))
                .thenReturn(of(generateCourtCentreJson()));
        when(referenceDataService.getJudiciariesByJudiciaryIdList(asList(JUDICIARY_ID_1, JUDICIARY_ID_2), envelope, requester))
                .thenReturn(Optional.ofNullable(null));

        progressionService.publishHearingDetailChangedPublicEvent(envelope, hearingUpdatedProcessed.getConfirmedHearing());

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
                                        withJsonPath("$.hearing.courtCentre.name", is(COURT_CENTRE_NAME))
                                )
                        )
                )
        );
    }

    @Test
    public void shouldListUnscheduledHearing() throws Exception {
        final Hearing hearing = Hearing.hearing().withId(randomUUID()).build();
        final JsonEnvelope envelope = getEnvelope(PROGRESSION_LIST_UNSCHEDULED_HEARING_COMMAND);
        progressionService.listUnscheduledHearings(envelope, hearing);
        verify(sender).send(envelopeCaptor.capture());
        assertThat(envelopeCaptor.getValue().metadata().name(), is(PROGRESSION_LIST_UNSCHEDULED_HEARING_COMMAND));
        JsonObject jsonObject = envelopeCaptor.getValue().payload();
        assertThat(jsonObject.getJsonObject("hearing").getString("id"), is(hearing.getId().toString()));
    }

    @Test
    public void shouldReturnMixHearingWhenUpdateHearingForHearingUpdatedCalled() throws IOException {
        final ConfirmedHearing confirmedHearing = ConfirmedHearing.confirmedHearing().withHearingDays(singletonList(HearingDay.hearingDay().withCourtRoomId(randomUUID()).build())).withCourtCentre(CourtCentre.courtCentre().withId(randomUUID()).build()).withJurisdictionType(JurisdictionType.CROWN).withId(randomUUID()).withHearingLanguage(HearingLanguage.ENGLISH).withReportingRestrictionReason("reportingRestrictionReason").withType(HearingType.hearingType().withId(randomUUID()).build()).build();
        final JsonEnvelope jsonEnvelope = getEnvelope(PROGRESSION_LIST_UNSCHEDULED_HEARING_COMMAND);
        final Hearing hearing = Hearing.hearing().withIsVacatedTrial(true).withProsecutionCases(singletonList(ProsecutionCase.prosecutionCase().withId(randomUUID()).build())).withIsBoxHearing(false).withHearingDays(singletonList(HearingDay.hearingDay().withCourtRoomId(randomUUID()).build())).withId(randomUUID()).withType(HearingType.hearingType().withId(randomUUID()).build()).withHearingLanguage(HearingLanguage.WELSH).withCourtApplications(singletonList(courtApplication().withId(randomUUID()).build())).withPanel("panel").withCourtCentre(CourtCentre.courtCentre().withId(randomUUID()).withRoomId(randomUUID()).build()).withApplicantCounsels(singletonList(ApplicantCounsel.applicantCounsel().withId(randomUUID()).build())).withApplicationPartyCounsels(singletonList(CourtApplicationPartyCounsel.courtApplicationPartyCounsel().withId(randomUUID()).build())).withApprovalsRequested(singletonList(ApprovalRequest.approvalRequest().withHearingId(randomUUID()).build())).withCompanyRepresentatives(singletonList(CompanyRepresentative.companyRepresentative().withId(randomUUID()).build())).withCourtApplicationPartyAttendance(singletonList(CourtApplicationPartyAttendance.courtApplicationPartyAttendance().withCourtApplicationPartyId(randomUUID()).build())).withCrackedIneffectiveTrial(CrackedIneffectiveTrial.crackedIneffectiveTrial().withId(randomUUID()).build()).withDefenceCounsels(singletonList(DefenceCounsel.defenceCounsel().withId(randomUUID()).build())).withDefendantAttendance(singletonList(DefendantAttendance.defendantAttendance().withDefendantId(randomUUID()).build())).withDefendantJudicialResults(singletonList(defendantJudicialResult().withMasterDefendantId(randomUUID()).build())).withReportingRestrictionReason("ReportingRestrictionReason").withEarliestNextHearingDate(ZonedDateTime.now()).withDefendantReferralReasons(singletonList(ReferralReason.referralReason().withId(randomUUID()).build())).withHasSharedResults(true).withHearingCaseNotes(singletonList(HearingCaseNote.hearingCaseNote().withId(randomUUID()).build())).withIntermediaries(singletonList(InterpreterIntermediary.interpreterIntermediary().withId(randomUUID()).build())).withIsEffectiveTrial(true).withIsSJPHearing(false).withIsVirtualBoxHearing(false).withJudiciary(singletonList(JudicialRole.judicialRole().withJudicialId(randomUUID()).build())).withJurisdictionType(JurisdictionType.MAGISTRATES).withProsecutionCounsels(singletonList(ProsecutionCounsel.prosecutionCounsel().withId(randomUUID()).build())).withRespondentCounsels(singletonList(RespondentCounsel.respondentCounsel().withId(randomUUID()).build())).withSeedingHearing(SeedingHearing.seedingHearing().withSeedingHearingId(randomUUID()).build()).withShadowListedOffences(singletonList(randomUUID())).withYouthCourt(YouthCourt.youthCourt().withYouthCourtId(randomUUID()).build()).withYouthCourtDefendantIds(singletonList(randomUUID())).build();

        when(referenceDataService.getOrganisationUnitById(confirmedHearing.getCourtCentre().getId(), jsonEnvelope, requester)).thenReturn(of(generateCourtCentreJson()));

        final Hearing mixHearing = progressionService.updateHearingForHearingUpdated(confirmedHearing, jsonEnvelope, hearing);

        assertThat(mixHearing.getIsVacatedTrial(), is(true));
        assertThat(mixHearing.getProsecutionCases().get(0).getId(), is(hearing.getProsecutionCases().get(0).getId()));
        assertThat(mixHearing.getIsBoxHearing(), is(hearing.getIsBoxHearing()));
        assertThat(mixHearing.getHearingDays().get(0).getCourtRoomId(), is(confirmedHearing.getHearingDays().get(0).getCourtRoomId()));
        assertThat(mixHearing.getId(), is(confirmedHearing.getId()));
        assertThat(mixHearing.getType().getId(), is(confirmedHearing.getType().getId()));
        assertThat(mixHearing.getHearingLanguage(), is(HearingLanguage.ENGLISH));
        assertThat(mixHearing.getCourtApplications().get(0).getId(), is(hearing.getCourtApplications().get(0).getId()));
        assertThat(mixHearing.getPanel(), is("panel"));
        assertThat(mixHearing.getCourtCentre().getId(), is(confirmedHearing.getCourtCentre().getId()));
        assertThat(mixHearing.getApplicantCounsels().get(0).getId(), is(hearing.getApplicantCounsels().get(0).getId()));
        assertThat(mixHearing.getApplicationPartyCounsels().get(0).getId(), is(hearing.getApplicationPartyCounsels().get(0).getId()));
        assertThat(mixHearing.getApprovalsRequested().get(0).getHearingId(), is(hearing.getApprovalsRequested().get(0).getHearingId()));
        assertThat(mixHearing.getCompanyRepresentatives().get(0).getId(), is(hearing.getCompanyRepresentatives().get(0).getId()));
        assertThat(mixHearing.getCourtApplicationPartyAttendance().get(0).getCourtApplicationPartyId(), is(hearing.getCourtApplicationPartyAttendance().get(0).getCourtApplicationPartyId()));
        assertThat(mixHearing.getCrackedIneffectiveTrial().getId(), is(hearing.getCrackedIneffectiveTrial().getId()));
        assertThat(mixHearing.getDefenceCounsels().get(0).getId(), is(hearing.getDefenceCounsels().get(0).getId()));
        assertThat(mixHearing.getDefendantAttendance().get(0).getDefendantId(), is(hearing.getDefendantAttendance().get(0).getDefendantId()));
        assertThat(mixHearing.getDefendantJudicialResults().get(0).getMasterDefendantId(), is(hearing.getDefendantJudicialResults().get(0).getMasterDefendantId()));
        assertThat(mixHearing.getReportingRestrictionReason(), is("reportingRestrictionReason"));
        assertThat(mixHearing.getEarliestNextHearingDate(), is(hearing.getEarliestNextHearingDate()));
        assertThat(mixHearing.getDefendantReferralReasons().get(0).getId(), is(hearing.getDefendantReferralReasons().get(0).getId()));
        assertThat(mixHearing.getHasSharedResults(), is(false));
        assertThat(mixHearing.getHearingCaseNotes().get(0).getId(), is(hearing.getHearingCaseNotes().get(0).getId()));
        assertThat(mixHearing.getIntermediaries().get(0).getId(), is(hearing.getIntermediaries().get(0).getId()));
        assertThat(mixHearing.getIsEffectiveTrial(), is(true));
        assertThat(mixHearing.getIsSJPHearing(), is(false));
        assertThat(mixHearing.getIsVirtualBoxHearing(), is(false));
        assertThat(mixHearing.getJudiciary(), is(nullValue()));
        assertThat(mixHearing.getJurisdictionType(), is(JurisdictionType.CROWN));
        assertThat(mixHearing.getProsecutionCounsels().get(0).getId(), is(hearing.getProsecutionCounsels().get(0).getId()));
        assertThat(mixHearing.getRespondentCounsels().get(0).getId(), is(hearing.getRespondentCounsels().get(0).getId()));
        assertThat(mixHearing.getSeedingHearing().getSeedingHearingId(), is(hearing.getSeedingHearing().getSeedingHearingId()));
        assertThat(mixHearing.getShadowListedOffences().get(0), is(hearing.getShadowListedOffences().get(0)));
        assertThat(mixHearing.getYouthCourt().getYouthCourtId(), is(hearing.getYouthCourt().getYouthCourtId()));
        assertThat(mixHearing.getYouthCourtDefendantIds().get(0), is(hearing.getYouthCourtDefendantIds().get(0)));

    }

    public void shouldRecordUnlistedHearing() {
        final UUID hearingId = randomUUID();
        final UUID unscheduledHearingId = randomUUID();

        final Hearing hearing = Hearing.hearing().withId(unscheduledHearingId).build();
        final JsonEnvelope envelope = getEnvelope(PROGRESSION_COMMAND_RECORD_UNSCHEDULED_HEARING);
        progressionService.recordUnlistedHearing(envelope, hearingId, asList(hearing));
        verify(sender).send(envelopeCaptor.capture());
        assertThat(envelopeCaptor.getValue().metadata().name(), is(PROGRESSION_COMMAND_RECORD_UNSCHEDULED_HEARING));
        JsonObject jsonObject = envelopeCaptor.getValue().payload();
        assertThat(jsonObject.getString("hearingId"), is(hearingId.toString()));
        assertThat(jsonObject.getString("unscheduledHearingIds.length()"), is(unscheduledHearingId.toString()));
        assertThat(jsonObject.getString("unscheduledHearingIds[0]"), is(unscheduledHearingId.toString()));
    }

    @Test
    public void shouldSendUpdateDefendantListingStatusForUnscheduledListing() {
        final List<Hearing> hearings = Arrays.asList(Hearing.hearing().withProsecutionCases(asList(ProsecutionCase.prosecutionCase().withId(randomUUID()).build())).withId(randomUUID()).build());
        final JsonEnvelope envelope = getEnvelope(PROGRESSION_UPDATE_DEFENDANT_LISTING_STATUS_COMMAND);
        progressionService.sendUpdateDefendantListingStatusForUnscheduledListing(envelope, hearings, new HashSet<>());
        verify(sender, times(2)).send(envelopeCaptor.capture());
        assertThat(envelopeCaptor.getAllValues().get(0).metadata().name(), is(PROGRESSION_UPDATE_DEFENDANT_LISTING_STATUS_COMMAND));
        JsonObject jsonObject = (JsonObject) envelopeCaptor.getAllValues().get(0).payload();
        assertThat(jsonObject.getJsonObject("hearing").getString("id"), is(hearings.get(0).getId().toString()));

        assertThat(envelopeCaptor.getAllValues().get(1).metadata().name(), is("progression.command-link-prosecution-cases-to-hearing"));
        jsonObject = (JsonObject) envelopeCaptor.getAllValues().get(1).payload();
        assertThat(jsonObject.getString("hearingId"), is(hearings.get(0).getId().toString()));
        assertThat(jsonObject.getString("caseId"), is(hearings.get(0).getProsecutionCases().get(0).getId().toString()));

    }

    @Test
    public void shouldSendUpdateCaseCommand() {
        final JsonEnvelope envelope = getEnvelope(PROGRESSION_COMMAND_HEARING_RESULTED_UPDATED_CASE);

        final UUID caseId = randomUUID();
        final DefendantJudicialResult defendantJudicialResult = defendantJudicialResult().withJudicialResult(judicialResult().withCategory(JudicialResultCategory.FINAL).build()).withMasterDefendantId(randomUUID()).build();
        final List<DefendantJudicialResult> defendantJudicialResults = singletonList(defendantJudicialResult);
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase().withDefendants(generateDefendantsForCase(randomUUID())).withId(caseId).build();
        final CourtCentre courtCentre = CourtCentre.courtCentre().withId(randomUUID()).withCode("code").build();
        final CourtApplication courtApplication = courtApplication()
                .withCourtApplicationCases(singletonList(courtApplicationCase()
                        .withProsecutionCaseId(caseId).build()))
                .withJudicialResults(singletonList(judicialResult().withCategory(JudicialResultCategory.FINAL).build())).build();
        final List<CourtApplication> courtApplications = singletonList(courtApplication);

        final UUID hearingId = randomUUID();
        final HearingType hearingType = HearingType.hearingType().withDescription("Trial").build();
        final JsonObject jsonObject = Json.createObjectBuilder().add("prosecutionCase", objectToJsonObjectConverter.convert(prosecutionCase)).add("courtApplications", listToJsonArrayConverter.convert(courtApplications))
                .add("defendantJudicialResults", resultListToJsonArrayConverter.convert(defendantJudicialResults)).add("courtCentre", objectToJsonObjectConverter.convert(courtCentre))
                .add("hearingId", hearingId.toString())
                .add("hearingType", "Trial")
                .add("jurisdictionType", "CROWN")
                .add("isBoxHearing", Boolean.FALSE)
                .add("remitResultIds", createArrayBuilder().build())
                .build();

        when(enveloper.withMetadataFrom(envelope, PROGRESSION_COMMAND_HEARING_RESULTED_UPDATED_CASE)).thenReturn(enveloperFunction);
        when(referenceDataService.getResultIdsByActionCode("REM", requester)).thenReturn(emptyList());
        when(enveloperFunction.apply(jsonObject)).thenReturn(finalEnvelope);


        progressionService.updateCase(envelope, prosecutionCase, courtApplications,
                defendantJudicialResults, courtCentre, hearingId, hearingType, JurisdictionType.CROWN, Boolean.FALSE);

        verify(sender).send(finalEnvelope);
    }

    @Test
    public void testUpdateCaseStatus() {
        final JsonEnvelope envelope = getEnvelope(PROGRESSION_COMMAND_HEARING_CONFIRMED_UPDATE_CASE_STATUS);
        final UUID prosecutionCaseId = randomUUID();
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase().withCaseStatus("INACTIVE").withDefendants(generateDefendantsForCase(randomUUID())).withId(prosecutionCaseId).build();

        final UUID courtApplicationId = randomUUID();
        final CourtApplication courtApplication = courtApplication().withCourtApplicationCases(singletonList(courtApplicationCase().withProsecutionCaseId(prosecutionCaseId).build())).withId(courtApplicationId).withApplicationStatus(ApplicationStatus.IN_PROGRESS).build();
        final Hearing hearing = Hearing.hearing().withProsecutionCases(singletonList(prosecutionCase)).withCourtApplications(singletonList(courtApplication)).build();


        final JsonObject jsonObject = createObjectBuilder().add("prosecutionCase", objectToJsonObjectConverter.convert(prosecutionCase)).add("caseStatus", "ACTIVE").build();

        when(enveloper.withMetadataFrom(envelope, PROGRESSION_COMMAND_HEARING_CONFIRMED_UPDATE_CASE_STATUS)).thenReturn(enveloperFunction);

        when(enveloperFunction.apply(jsonObject)).thenReturn(finalEnvelope);
        progressionService.updateCaseStatus(envelope, hearing, singletonList(courtApplicationId));
        verify(sender).send(finalEnvelope);
    }

    @Test
    public void testShouldNotRaiseEventForUpdateCaseStatus() {
        final JsonEnvelope envelope = getEnvelope(PROGRESSION_COMMAND_HEARING_CONFIRMED_UPDATE_CASE_STATUS);
        final UUID prosecutionCaseId = randomUUID();
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase().withCaseStatus("LISTED").withDefendants(generateDefendantsForCase(randomUUID())).withId(prosecutionCaseId).build();

        final UUID courtApplicationId = randomUUID();
        final CourtApplication courtApplication = courtApplication().withCourtApplicationCases(singletonList(courtApplicationCase().withProsecutionCaseId(prosecutionCaseId).build())).withId(courtApplicationId).withApplicationStatus(ApplicationStatus.IN_PROGRESS).build();
        final Hearing hearing = Hearing.hearing().withProsecutionCases(singletonList(prosecutionCase)).withCourtApplications(singletonList(courtApplication)).build();


        final JsonObject jsonObject = createObjectBuilder().add("prosecutionCase", objectToJsonObjectConverter.convert(prosecutionCase)).add("caseStatus", "SJP_REFERRAL").build();

        progressionService.updateCaseStatus(envelope, hearing, singletonList(courtApplicationId));
        verifyNoMoreInteractions(sender);
    }

    @Test
    public void shouldCreateCourtCenterWithCourtRoomOuCode() {
        final JsonEnvelope envelope = getEnvelope(PROGRESSION_COMMAND_HEARING_CONFIRMED_UPDATE_CASE_STATUS);
        final String address1 = "ADDRESS1";
        final UUID courtCentreId = randomUUID();
        final UUID courtRoomId = randomUUID();
        final String oucode = STRING.next();
        final JsonObject courtCentreJson = createObjectBuilder().add("oucodeL3Name", "Lavender Hill Magistrates Court").add("address1", address1).add("oucode", oucode).add("lja", "ljaCode").build();
        final JsonObject courtRoomOuJson = createObjectBuilder().add("ouCourtRoomCodes", createArrayBuilder().add("B46IR03").build()).build();
        final JsonObject courtRoomJson = createObjectBuilder().add("courtrooms", createArrayBuilder().add(createObjectBuilder().add("id", courtRoomId.toString()).add("courtroomName", "room name 1").build()).build()).build();
        final CourtCentre courtCentre = CourtCentre.courtCentre().withId(courtCentreId).withRoomId(courtRoomId).build();
        when(referenceDataService.getOrganisationUnitById(courtCentreId, envelope, requester)).thenReturn(of(courtCentreJson));
        when(referenceDataService.getOuCourtRoomCode(courtRoomId.toString(), requester)).thenReturn(of(courtRoomOuJson));
        when(referenceDataService.getCourtCentreWithCourtRoomsById(courtCentreId, envelope, requester)).thenReturn(of(courtRoomJson));

        final CourtCentre result = progressionService.transformCourtCentre(courtCentre, envelope);

        assertThat(result.getCourtHearingLocation(), is("B46IR03"));

    }

    @Test
    public void shouldCreateCourtCenterWithCourtRoomOuCodeWhenDefinitionNotExist() {
        final JsonEnvelope envelope = getEnvelope(PROGRESSION_COMMAND_HEARING_CONFIRMED_UPDATE_CASE_STATUS);
        final String address1 = "ADDRESS1";
        final UUID courtCentreId = randomUUID();
        final UUID courtRoomId = randomUUID();
        final String oucode = STRING.next();
        final JsonObject courtCentreJson = createObjectBuilder().add("oucodeL3Name", "Lavender Hill Magistrates Court").add("address1", address1).add("oucode", oucode).add("lja", "ljaCode").build();
        final JsonObject courtRoomOuJson = createObjectBuilder().add("ouCourtRoomCodes", createArrayBuilder().build()).build();
        final JsonObject courtRoomJson = createObjectBuilder().add("courtrooms", createArrayBuilder().add(createObjectBuilder().add("id", courtRoomId.toString()).add("courtroomName", "room name 1").build()).build()).build();
        final CourtCentre courtCentre = CourtCentre.courtCentre().withId(courtCentreId).withRoomId(courtRoomId).build();
        when(referenceDataService.getOrganisationUnitById(courtCentreId, envelope, requester)).thenReturn(of(courtCentreJson));
        when(referenceDataService.getOuCourtRoomCode(courtRoomId.toString(), requester)).thenReturn(of(courtRoomOuJson));
        when(referenceDataService.getCourtCentreWithCourtRoomsById(courtCentreId, envelope, requester)).thenReturn(of(courtRoomJson));

        final CourtCentre result = progressionService.transformCourtCentre(courtCentre, envelope);

        assertThat(result.getCourtHearingLocation(), is(oucode));

    }

    @Test
    public void shouldGetApplicationDetails() {

       final UUID applicationId = randomUUID();
        JsonEnvelope requestEnvelope = getUserEnvelope(APPLICATION_AAAG);
        when(requester.request(any(), any(Class.class))).thenReturn(requestEnvelope);
        JsonObject courtApplication = progressionService.retrieveApplication(requestEnvelope, applicationId);

        assertThat(courtApplication, is(notNullValue()));
        assertThat(courtApplication.getJsonObject("applicantDetails").getString("name"), is(notNullValue()));
        assertThat(courtApplication.getJsonArray("respondentDetails").size(), is(greaterThan(0)));
    }

    private JsonEnvelope getUserEnvelope(String fileName) {
        return envelopeFrom(
                Envelope.metadataBuilder().
                        withName("progression.query.application.aaag").
                        withId(randomUUID()),
                createReader(getClass().getClassLoader().
                                getResourceAsStream(fileName)).
                        readObject()
        );
    }

    private JsonObject generateJudiciariesJson() throws IOException {
        final String jsonString = Resources.toString(Resources.getResource("referenceData.getJudiciariesByIdList.json"), Charset.defaultCharset()).replace("JUDICIARY_ID_1", JUDICIARY_ID_1.toString()).replace("JUDICIARY_TITLE_1", JUDICIARY_TITLE_1).replace("JUDICIARY_FIRST_NAME_1", JUDICIARY_FIRST_NAME_1).replace("JUDICIARY_LAST_NAME_1", JUDICIARY_LAST_NAME_1).replace("JUDICIARY_ID_2", JUDICIARY_ID_2.toString()).replace("JUDICIARY_TITLE_2", JUDICIARY_TITLE_2).replace("JUDICIARY_FIRST_NAME_2", JUDICIARY_FIRST_NAME_2).replace("JUDICIARY_LAST_NAME_2", JUDICIARY_LAST_NAME_2).replace("JUDICIARY_TYPE_1", JUDICIARY_TYPE_1).replace("JUDICIARY_TYPE_2", JUDICIARY_TYPE_2);

        return returnAsJson(jsonString);
    }

    private JsonObject generateJudiciariesJsonWithMissingJudiciary() throws IOException {
        final String jsonString = Resources.toString(Resources.getResource("referenceData.getJudiciariesByIdListWithJudiciaryNotFoundInRefData.json"), Charset.defaultCharset())
                .replace("JUDICIARY_ID_1", JUDICIARY_ID_1.toString())
                .replace("JUDICIARY_TITLE_1", JUDICIARY_TITLE_1)
                .replace("JUDICIARY_FIRST_NAME_1", JUDICIARY_FIRST_NAME_1)
                .replace("JUDICIARY_LAST_NAME_1", JUDICIARY_LAST_NAME_1)
                .replace("JUDICIARY_TYPE_1", JUDICIARY_TYPE_1);

        return returnAsJson(jsonString);
    }

    private JsonObject generateCourtCentreJson() throws IOException {
        final String jsonString = Resources.toString(Resources.getResource("referencedata.query.organisationunits.json"), Charset.defaultCharset()).replace("COURT_CENTRE_ID", COURT_CENTRE_ID.toString()).replace("COURT_CENTRE_NAME", COURT_CENTRE_NAME);

        return returnAsJson(jsonString);
    }

    private JsonObject returnAsJson(final String jsonString) {
        try (JsonReader jsonReader = createReader(new StringReader(jsonString))) {
            return jsonReader.readObject();
        }
    }

    private HearingUpdatedProcessed generateHearingUpdated() {
        return HearingUpdatedProcessed.hearingUpdatedProcessed().withConfirmedHearing(generateConfirmedHearingForHearingUpdated()).build();
    }

    private ConfirmedHearing generateConfirmedHearingForHearingUpdated() {
        return ConfirmedHearing.confirmedHearing().withId(randomUUID()).withType(generateHearingType()).withJurisdictionType(JurisdictionType.MAGISTRATES).withReportingRestrictionReason(STRING.next()).withHearingLanguage(HearingLanguage.ENGLISH).withHearingDays(generateHearingDays()).withCourtCentre(generateBasicCourtCentre()).withJudiciary(generateBasicJudiciaryList()).withProsecutionCases(generateProsecutionCases()).build();
    }

    private HearingType generateHearingType() {
        return HearingType.hearingType().withId(randomUUID()).withDescription("Sentence").build();
    }

    private ConfirmedHearing generateConfirmedHearingForPrepareSummons() {
        return ConfirmedHearing.confirmedHearing().withId(randomUUID()).withCourtCentre(generateFullCourtCentre()).withHearingDays(generateHearingDays()).withProsecutionCases(generateProsecutionCases()).build();
    }

    private List<ConfirmedProsecutionCase> generateProsecutionCases() {
        return asList(ConfirmedProsecutionCase.confirmedProsecutionCase().withId(CASE_ID_1).withDefendants(generateDefendants(DEFENDANT_ID_1)).build(), ConfirmedProsecutionCase.confirmedProsecutionCase().withId(CASE_ID_2).withDefendants(generateDefendants(DEFENDANT_ID_2)).build());
    }

    private List<ConfirmedDefendant> generateDefendants(final UUID defendantId) {
        return singletonList(ConfirmedDefendant.confirmedDefendant().withId(defendantId).build());
    }

    private List<Defendant> generateDefendantsForCase(final UUID defendantId) {
        return singletonList(Defendant.defendant().withId(defendantId).withProsecutionCaseId(CASE_ID_1).build());
    }

    private List<Defendant> generateDefendantsForCaseWithOffences(final UUID defendantId, final List<Offence> offenceList) {
        return singletonList(Defendant.defendant().withId(defendantId).withProsecutionCaseId(CASE_ID_1).withOffences(offenceList).build());
    }

    private Offence getOffenceWithNewJudicialResults(final UUID offenceId, final boolean isNewAmendment) {
        return Offence.offence().withId(offenceId)
                .withJudicialResults(asList(judicialResult().withIsNewAmendment(isNewAmendment).withJudicialResultId(randomUUID()).withCategory(JudicialResultCategory.ANCILLARY).build(),
                        judicialResult().withIsNewAmendment(isNewAmendment).withJudicialResultId(randomUUID()).withCategory(JudicialResultCategory.FINAL).build()))
                .build();
    }

    private List<Offence> getOffenceWithExistingJudicialResults(final UUID offenceId) {
        return singletonList(Offence.offence().withId(offenceId)
                .withJudicialResults(asList(judicialResult().withIsNewAmendment(Boolean.TRUE).withJudicialResultId(randomUUID()).withCategory(JudicialResultCategory.ANCILLARY).build(),
                                            judicialResult().withIsNewAmendment(Boolean.TRUE).withJudicialResultId(randomUUID()).withCategory(JudicialResultCategory.FINAL).build()))
                .build());
    }

    private List<HearingDay> generateHearingDays() {
        return asList(HearingDay.hearingDay().withSittingDay(ZonedDateTimes.fromString(HEARING_DATE_1)).build(), HearingDay.hearingDay().withSittingDay(ZonedDateTimes.fromString(HEARING_DATE_2)).build(), HearingDay.hearingDay().withSittingDay(ZonedDateTimes.fromString(HEARING_DATE_3)).build());
    }

    private JsonEnvelope getEnvelope(final String name) {
        return envelopeFrom(metadataBuilder().withId(randomUUID()).withName(name).build(), createObjectBuilder().build());
    }

    private <T> Envelope<T> getTypedEnvelope(final String name, final T payload) {
        return envelopeFrom(metadataBuilder().withId(randomUUID()).withName(name).build(), payload);
    }

    private CourtCentre generateFullCourtCentre() {
        return CourtCentre.courtCentre().withId(UUID.fromString("89b10041-b44d-43c8-9b1e-d1b9fee15c93")).withName("00ObpXuu51").withRoomId(UUID.fromString("d7020fe0-cd97-4ce0-84c2-fd00ff0bc48a")).withRoomName("JK2Y7hu0Tc").withWelshName("3IpJDfdfhS").withWelshRoomName("hm60SAXokc").withAddress(Address.address().withAddress1("Address1").build()).build();
    }

    private CourtCentre generateBasicCourtCentre() {
        return CourtCentre.courtCentre().withId(COURT_CENTRE_ID).build();
    }

    private List<JudicialRole> generateBasicJudiciaryList() {
        return asList(JudicialRole.judicialRole().withIsDeputy(Boolean.TRUE).withJudicialId(JUDICIARY_ID_1).withJudicialRoleType(JudicialRoleType.judicialRoleType().withJudicialRoleTypeId(randomUUID()).withJudiciaryType(JUDICIARY_TYPE_1).build()).withIsBenchChairman(Boolean.TRUE).build(), JudicialRole.judicialRole().withIsDeputy(Boolean.TRUE).withJudicialId(JUDICIARY_ID_2).withJudicialRoleType(JudicialRoleType.judicialRoleType().withJudicialRoleTypeId(randomUUID()).withJudiciaryType(JUDICIARY_TYPE_2).build()).withIsBenchChairman(Boolean.TRUE).build());
    }

    @Test
    public void testCreateHearingApplicationLink() {
        final JsonEnvelope envelope = getEnvelope(PROGRESSION_COMMAND_CREATE_HEARING_APPLICATION_LINK);
        final UUID hearingId = randomUUID();
        final List<UUID> applicationId = asList(randomUUID());
        final Hearing hearing = Hearing.hearing().withId(hearingId).build();
        final JsonObject jsonObject = createObjectBuilder().add("hearing", objectToJsonObjectConverter.convert(hearing)).add("hearingListingStatus", "HEARING_INITIALISED").add("applicationId", applicationId.get(0).toString()).build();
        when(enveloper.withMetadataFrom(envelope, PROGRESSION_COMMAND_CREATE_HEARING_APPLICATION_LINK)).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(jsonObject)).thenReturn(finalEnvelope);
        progressionService.linkApplicationsToHearing(envelope, hearing, applicationId, HearingListingStatus.HEARING_INITIALISED);
        verify(sender).send(finalEnvelope);
    }

    @Test
    public void testCreateHearingApplicationLinkWithDuplicateApplicationIds() {
        final JsonEnvelope envelope = getEnvelope(PROGRESSION_COMMAND_CREATE_HEARING_APPLICATION_LINK);
        final UUID hearingId = randomUUID();
        final UUID applicationId = randomUUID();
        final List<UUID> applicationIds = asList(applicationId, applicationId, applicationId);
        final Hearing hearing = Hearing.hearing().withId(hearingId).build();
        final JsonObject jsonObject = createObjectBuilder().add("hearing", objectToJsonObjectConverter.convert(hearing)).add("hearingListingStatus", "HEARING_INITIALISED").add("applicationId", applicationId.toString()).build();
        when(enveloper.withMetadataFrom(envelope, PROGRESSION_COMMAND_CREATE_HEARING_APPLICATION_LINK)).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(jsonObject)).thenReturn(finalEnvelope);
        progressionService.linkApplicationsToHearing(envelope, hearing, applicationIds, HearingListingStatus.HEARING_INITIALISED);
        verify(sender).send(finalEnvelope);
    }

    @Test
    public void testlinkApplicationToHearingWithDuplicateApplicationsInHearing() {
        final JsonEnvelope envelope = getEnvelope(PROGRESSION_COMMAND_CREATE_HEARING_APPLICATION_LINK);
        final UUID hearingId = randomUUID();
        final UUID applicationId = randomUUID();
        final Hearing hearing = Hearing.hearing().withId(hearingId).withCourtApplications(Arrays.asList(courtApplication().withId(applicationId).withType(courtApplicationType().withLinkType(LinkType.LINKED).build()).build(), courtApplication().withId(applicationId).withType(courtApplicationType().withLinkType(LinkType.LINKED).build()).build())).build();
        final JsonObject jsonObject = createObjectBuilder().add("hearing", objectToJsonObjectConverter.convert(hearing)).add("hearingListingStatus", "HEARING_INITIALISED").add("applicationId", applicationId.toString()).build();
        when(enveloper.withMetadataFrom(envelope, PROGRESSION_COMMAND_CREATE_HEARING_APPLICATION_LINK)).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(jsonObject)).thenReturn(finalEnvelope);
        progressionService.linkApplicationToHearing(envelope, hearing, HearingListingStatus.HEARING_INITIALISED);
        verify(sender).send(finalEnvelope);
    }


    @Test
    public void testCreateHearingProsecutionCaseLink() {
        final JsonEnvelope envelope = getEnvelope(PROGRESSION_COMMAND_CREATE_HEARING_PROSECUTION_CASE_LINK);
        final UUID hearingId = randomUUID();
        final List<UUID> caseIds = asList(randomUUID());
        final Hearing hearing = Hearing.hearing().withId(hearingId).build();
        final JsonObject jsonObject = createObjectBuilder().add("hearingId", hearingId.toString()).add("caseId", caseIds.get(0).toString()).build();
        when(enveloper.withMetadataFrom(envelope, PROGRESSION_COMMAND_CREATE_HEARING_PROSECUTION_CASE_LINK)).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(jsonObject)).thenReturn(finalEnvelope);
        progressionService.linkProsecutionCasesToHearing(envelope, hearingId, caseIds);
        verify(sender).send(finalEnvelope);
    }

    @Test
    public void testShouldTransformBoxWorkApplication() {

        final UUID applicationId = UUID.randomUUID();

        final Hearing expectedHearing = Hearing.hearing().withIsBoxHearing(true).withId(UUID.randomUUID()).withCourtApplications(asList(courtApplication().withId(applicationId).build())).withHearingDays(asList(HearingDay.hearingDay().withListedDurationMinutes(10).withSittingDay(ZonedDateTimes.fromString("2019-08-12T05:27:17.210Z")).build())).build();


        final HearingListingNeeds hearingListingNeeds = HearingListingNeeds.hearingListingNeeds().withCourtApplications(asList(courtApplication().withId(applicationId).build())).withListedStartDateTime(ZonedDateTimes.fromString("2019-08-12T05:27:17.210Z")).build();

        final BoxworkApplicationReferred boxworkApplicationReferred = BoxworkApplicationReferred.boxworkApplicationReferred().withHearingRequest(hearingListingNeeds).build();

        final Hearing actualHearing = progressionService.transformBoxWorkApplication(boxworkApplicationReferred);

        assertThat(actualHearing.getCourtApplications().get(0).getId(), CoreMatchers.is(expectedHearing.getCourtApplications().get(0).getId()));
        assertThat(actualHearing.getHearingDays().get(0).getSittingDay(), CoreMatchers.is(expectedHearing.getHearingDays().get(0).getSittingDay()));
        assertThat(actualHearing.getHearingDays().get(0).getListedDurationMinutes(), CoreMatchers.is(expectedHearing.getHearingDays().get(0).getListedDurationMinutes()));
        assertThat(actualHearing.getIsBoxHearing(), CoreMatchers.is(expectedHearing.getIsBoxHearing()));

    }

    @Test
    public void shouldSendPrepareSummonsForExtendedHearingCommand() {

        final ConfirmedHearing confirmedHearing = generateConfirmedHearingForPrepareSummons();
        final HearingConfirmed hearingConfirmed = HearingConfirmed.hearingConfirmed().withConfirmedHearing(confirmedHearing).build();
        final JsonEnvelope prepareSummonsEnvelope = getEnvelope(PROGRESSION_COMMAND_PREPARE_SUMMONS_DATA_FOR_EXTENDED_HEARING);
        progressionService.prepareSummonsDataForExtendHearing(prepareSummonsEnvelope, hearingConfirmed);
        verify(sender).send(envelopeArgumentCaptor.capture());

        assertThat(envelopeArgumentCaptor.getValue(), jsonEnvelope(metadata().withName(PROGRESSION_COMMAND_PREPARE_SUMMONS_DATA_FOR_EXTENDED_HEARING), payloadIsJson(allOf(withJsonPath("$.confirmedHearing.courtCentre.id", is(confirmedHearing.getCourtCentre().getId().toString())), withJsonPath("$.confirmedHearing.courtCentre.name", is(confirmedHearing.getCourtCentre().getName())), withJsonPath("$.confirmedHearing.courtCentre.roomId", is(confirmedHearing.getCourtCentre().getRoomId().toString())), withJsonPath("$.confirmedHearing.courtCentre.roomName", is(confirmedHearing.getCourtCentre().getRoomName())), withJsonPath("$.confirmedHearing.courtCentre.welshName", is(confirmedHearing.getCourtCentre().getWelshName())), withJsonPath("$.confirmedHearing.courtCentre.welshRoomName", is(confirmedHearing.getCourtCentre().getWelshRoomName())), withJsonPath("$.confirmedHearing.prosecutionCases[0].id", is(CASE_ID_1.toString())), withJsonPath("$.confirmedHearing.prosecutionCases[0].defendants[0].id", is(DEFENDANT_ID_1.toString())), withJsonPath("$.confirmedHearing.prosecutionCases[1].id", is(CASE_ID_2.toString())), withJsonPath("$.confirmedHearing.prosecutionCases[1].defendants[0].id", is(DEFENDANT_ID_2.toString()))

        ))));

    }

    @Test
    public void shouldUpdateHearingForPartialAllocation() {
        final UUID hearingId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID offenceId = randomUUID();
        final UUID defendantId = randomUUID();

        final UpdateHearingForPartialAllocation updateHearingForPartialAllocation = UpdateHearingForPartialAllocation.updateHearingForPartialAllocation().withHearingId(hearingId).withProsecutionCasesToRemove(asList(ProsecutionCasesToRemove.prosecutionCasesToRemove().withCaseId(caseId).withDefendantsToRemove(asList(DefendantsToRemove.defendantsToRemove().withDefendantId(defendantId).withOffencesToRemove(asList(OffencesToRemove.offencesToRemove().withOffenceId(offenceId).build())).build())).build())).build();
        final JsonEnvelope envelope = getEnvelope(PROGRESSION_COMMAND_UPDATE_HEARING_FOR_PARTIAL_ALLOCATION);

        progressionService.updateHearingForPartialAllocation(envelope, updateHearingForPartialAllocation);
        verify(sender).send(envelopeArgumentCaptor.capture());

        assertThat(envelopeArgumentCaptor.getValue(), jsonEnvelope(metadata().withName(PROGRESSION_COMMAND_UPDATE_HEARING_FOR_PARTIAL_ALLOCATION), payloadIsJson(allOf(withJsonPath("$.hearingId", is(hearingId.toString())), withJsonPath("$.prosecutionCasesToRemove[0].caseId", is(caseId.toString())), withJsonPath("$.prosecutionCasesToRemove[0].defendantsToRemove[0].defendantId", is(defendantId.toString())), withJsonPath("$.prosecutionCasesToRemove[0].defendantsToRemove[0].offencesToRemove[0].offenceId", is(offenceId.toString()))

        ))));
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
        final List<ConfirmedProsecutionCase> confirmedProsecutionCases = Arrays.asList(ConfirmedProsecutionCase.confirmedProsecutionCase().withId(caseId).withDefendants(asList(ConfirmedDefendant.confirmedDefendant().withId(defendant2).withOffences(asList(ConfirmedOffence.confirmedOffence().withId(defendant2sOffence1).build())).build())).build());

        when(enveloper.withMetadataFrom(finalEnvelope, PROGRESSION_QUERY_PROSECUTION_CASES)).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(any())).thenReturn(finalEnvelope);
        final JsonObject jsonObject = createObjectBuilder().add("prosecutionCase", objectToJsonObjectConverter.convert(buildProsecutionCasesWithTwoDefendantsOffences(caseId, defendant1, defendant2, defendant1sOffence1, defendant1sOffence2, defendant2sOffence1, defendant2sOffence2, false))).build();
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
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase().withId(caseId).withDefendants(asList(Defendant.defendant().withId(defendant2).withIsYouth(true).withOffences(asList(Offence.offence().withId(defendant2sOffence1).build())).build())).build();

        final Initiate hearingInitiate = Initiate.initiate().withHearing(Hearing.hearing().withProsecutionCases(Arrays.asList(prosecutionCase)).build()).build();

        final List<ProsecutionCase> deltaProsecutionCases = Arrays.asList(ProsecutionCase.prosecutionCase().withId(caseId).withDefendants(asList(Defendant.defendant().withId(defendant2).withIsYouth(true).withOffences(asList(Offence.offence().withId(defendant2sOffence2).build())).build())).build());

        when(enveloper.withMetadataFrom(finalEnvelope, PROGRESSION_QUERY_PROSECUTION_CASES)).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(any())).thenReturn(finalEnvelope);
        final JsonObject jsonObject = createObjectBuilder().add("prosecutionCase", objectToJsonObjectConverter.convert(buildProsecutionCasesWithTwoDefendantsOffences(caseId, defendant1, defendant2, defendant1sOffence1, defendant1sOffence2, defendant2sOffence1, defendant2sOffence2, false))).build();
        when(finalEnvelope.payloadAsJsonObject()).thenReturn(jsonObject);
        when(requester.requestAsAdmin(any())).thenReturn(finalEnvelope);
        when(enveloper.withMetadataFrom(finalEnvelope, PROGRESSION_COMMAND_UPDATE_DEFENDANT_AS_YOUTH)).thenReturn(enveloperFunction);


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

        final Initiate hearingInitiate = Initiate.initiate().withHearing(Hearing.hearing().withProsecutionCases(Arrays.asList(prosecutionCase)).build()).build();
        final List<ProsecutionCase> deltaProsecutionCases = Arrays.asList(ProsecutionCase.prosecutionCase().withId(caseId).withDefendants(asList(Defendant.defendant().withId(defendant2).withIsYouth(true).withOffences(asList(Offence.offence().withId(defendant2sOffence1).build())).build())).build());

        when(enveloper.withMetadataFrom(finalEnvelope, PROGRESSION_QUERY_PROSECUTION_CASES)).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(any())).thenReturn(finalEnvelope);
        final JsonObject jsonObject = createObjectBuilder().add("prosecutionCase", objectToJsonObjectConverter.convert(buildProsecutionCasesWithTwoDefendantsOffences(caseId, defendant1, defendant2, defendant1sOffence1, defendant1sOffence2, defendant2sOffence1, defendant2sOffence2, false))).build();
        when(finalEnvelope.payloadAsJsonObject()).thenReturn(jsonObject);
        when(requester.requestAsAdmin(any())).thenReturn(finalEnvelope);

        when(enveloper.withMetadataFrom(finalEnvelope, PROGRESSION_COMMAND_UPDATE_DEFENDANT_AS_YOUTH)).thenReturn(enveloperFunction);


        progressionService.updateDefendantYouthForProsecutionCase(finalEnvelope, hearingInitiate, deltaProsecutionCases);

        verify(sender, times(2)).send(envelopeArgumentCaptor.capture());


    }

    @Test
    public void shouldNotCallUpdateCommandWhenOldYouthIsNullAndNewValueIsFalse() {
        final UUID caseId = randomUUID();
        final UUID defendant1 = randomUUID();
        final UUID defendant2 = randomUUID();
        final UUID defendant1sOffence1 = randomUUID();
        final UUID defendant1sOffence2 = randomUUID();
        final UUID defendant2sOffence1 = randomUUID();
        final UUID defendant2sOffence2 = randomUUID();
        final ProsecutionCase prosecutionCase = buildProsecutionCasesWithTwoDefendantsOffences(caseId, defendant1, defendant2, defendant1sOffence1, defendant1sOffence2, defendant2sOffence1, defendant2sOffence2, false);

        final Initiate hearingInitiate = Initiate.initiate().withHearing(Hearing.hearing().withProsecutionCases(Arrays.asList(prosecutionCase)).build()).build();
        final List<ProsecutionCase> deltaProsecutionCases = Arrays.asList(ProsecutionCase.prosecutionCase().withId(caseId).withDefendants(asList(Defendant.defendant().withId(defendant2).withIsYouth(false).withOffences(asList(Offence.offence().withId(defendant2sOffence1).build())).build())).build());

        when(enveloper.withMetadataFrom(finalEnvelope, PROGRESSION_QUERY_PROSECUTION_CASES)).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(any())).thenReturn(finalEnvelope);
        final JsonObject jsonObject = createObjectBuilder().add("prosecutionCase", objectToJsonObjectConverter.convert(buildProsecutionCasesWithTwoDefendantsOffences(caseId, defendant1, defendant2, defendant1sOffence1, defendant1sOffence2, defendant2sOffence1, defendant2sOffence2, null))).build();
        when(finalEnvelope.payloadAsJsonObject()).thenReturn(jsonObject);
        when(requester.requestAsAdmin(any())).thenReturn(finalEnvelope);

        progressionService.updateDefendantYouthForProsecutionCase(finalEnvelope, hearingInitiate, deltaProsecutionCases);

        verify(sender, never()).send(envelopeArgumentCaptor.capture());
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

        final Initiate hearingInitiate = Initiate.initiate().withHearing(Hearing.hearing().withProsecutionCases(Arrays.asList(prosecutionCase)).build()).build();
        final List<ProsecutionCase> deltaProsecutionCases = emptyList();

        when(enveloper.withMetadataFrom(finalEnvelope, PROGRESSION_QUERY_PROSECUTION_CASES)).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(any())).thenReturn(finalEnvelope);
        final JsonObject jsonObject = createObjectBuilder().add("prosecutionCase", objectToJsonObjectConverter.convert(buildProsecutionCasesWithTwoDefendantsOffences(caseId, defendant1, defendant2, defendant1sOffence1, defendant1sOffence2, defendant2sOffence1, defendant2sOffence2, false))).build();
        when(finalEnvelope.payloadAsJsonObject()).thenReturn(jsonObject);
        when(requester.requestAsAdmin(any())).thenReturn(finalEnvelope);

        when(enveloper.withMetadataFrom(finalEnvelope, PROGRESSION_COMMAND_UPDATE_DEFENDANT_AS_YOUTH)).thenReturn(enveloperFunction);


        progressionService.updateDefendantYouthForProsecutionCase(finalEnvelope, hearingInitiate, deltaProsecutionCases);

        verify(sender, times(2)).send(envelopeArgumentCaptor.capture());


    }

    @Test
    public void shouldRemovePleaWhenMovingFromMagsToCrownAndGuiltyTypeNo() {

        final UUID caseId = randomUUID();
        final UUID defendant1 = randomUUID();
        final UUID defendant1sOffence1 = randomUUID();

        final List<ConfirmedProsecutionCase> confirmedProsecutionCases = Arrays.asList(ConfirmedProsecutionCase.confirmedProsecutionCase().withId(caseId).withDefendants(asList(ConfirmedDefendant.confirmedDefendant().withId(defendant1).withOffences(asList(ConfirmedOffence.confirmedOffence().withId(defendant1sOffence1).build())).build())).build());

        final SeedingHearing seedingHearing = SeedingHearing.seedingHearing().withJurisdictionType(JurisdictionType.MAGISTRATES).build();

        when(enveloper.withMetadataFrom(finalEnvelope, PROGRESSION_QUERY_PROSECUTION_CASES)).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(any())).thenReturn(finalEnvelope);
        final JsonObject jsonObject = createObjectBuilder().add("prosecutionCase", objectToJsonObjectConverter.convert(buildProsecutionCaseWithDefendantWithOffenceWithPlea(caseId, defendant1, defendant1sOffence1, JurisdictionType.CROWN))).build();
        when(finalEnvelope.payloadAsJsonObject()).thenReturn(jsonObject);
        when(requester.requestAsAdmin(any())).thenReturn(finalEnvelope);
        when(referenceDataService.getPleaType(any(), any())).thenReturn(of(createObjectBuilder().add("pleaTypeGuiltyFlag", "No").build()));

        List<ProsecutionCase> prosecutionCases = progressionService.transformProsecutionCase(confirmedProsecutionCases, LocalDate.now(), finalEnvelope, seedingHearing);

        assertThat(prosecutionCases.get(0).getCpsOrganisation(), is("A01"));
        assertThat(prosecutionCases.get(0).getTrialReceiptType(), is("Voluntary bill"));
        assertThat(1, is(prosecutionCases.get(0).getDefendants().size()));
        assertThat(1, is(prosecutionCases.get(0).getDefendants().get(0).getOffences().size()));
        assertThat(prosecutionCases.get(0).getDefendants().get(0).getOffences().get(0).getPlea(), is(nullValue()));
    }

    @Test
    public void shouldIncreaseListingNumbersFromProsecutionCaseWhenListingNumberDoesNotExist() {

        final UUID caseId = randomUUID();
        final UUID defendant1 = randomUUID();
        final UUID defendant1sOffence1 = randomUUID();

        final List<ConfirmedProsecutionCase> confirmedProsecutionCases = Arrays.asList(ConfirmedProsecutionCase.confirmedProsecutionCase().withId(caseId).withDefendants(asList(ConfirmedDefendant.confirmedDefendant().withId(defendant1).withOffences(asList(ConfirmedOffence.confirmedOffence().withId(defendant1sOffence1).build())).build())).build());

        final SeedingHearing seedingHearing = SeedingHearing.seedingHearing().withJurisdictionType(JurisdictionType.MAGISTRATES).build();

        when(enveloper.withMetadataFrom(finalEnvelope, PROGRESSION_QUERY_PROSECUTION_CASES)).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(any())).thenReturn(finalEnvelope);
        final JsonObject jsonObject = createObjectBuilder().add("prosecutionCase", objectToJsonObjectConverter.convert(buildProsecutionCaseWithDefendantWithOffenceWithPlea(caseId, defendant1, defendant1sOffence1, JurisdictionType.CROWN))).build();
        when(finalEnvelope.payloadAsJsonObject()).thenReturn(jsonObject);
        when(requester.requestAsAdmin(any())).thenReturn(finalEnvelope);
        when(referenceDataService.getPleaType(any(), any())).thenReturn(of(createObjectBuilder().add("pleaTypeGuiltyFlag", "No").build()));

        List<ProsecutionCase> prosecutionCases = progressionService.transformProsecutionCase(confirmedProsecutionCases, LocalDate.now(), finalEnvelope, seedingHearing);

        assertThat(prosecutionCases.get(0).getCpsOrganisation(), is("A01"));
        assertThat(prosecutionCases.get(0).getTrialReceiptType(), is("Voluntary bill"));
        assertThat(1, is(prosecutionCases.get(0).getDefendants().size()));
        assertThat(1, is(prosecutionCases.get(0).getDefendants().get(0).getOffences().size()));
        assertThat(prosecutionCases.get(0).getDefendants().get(0).getOffences().get(0).getPlea(), is(nullValue()));
        assertThat(prosecutionCases.get(0).getDefendants().get(0).getOffences().get(0).getListingNumber(), is(1));
    }

    @Test
    public void shouldIncreaseListingNumbersFromProsecutionCaseWhenListingNumberExists() {

        final UUID caseId = randomUUID();
        final UUID defendant1 = randomUUID();
        final UUID defendant1sOffence1 = randomUUID();

        final List<ConfirmedProsecutionCase> confirmedProsecutionCases = Arrays.asList(ConfirmedProsecutionCase.confirmedProsecutionCase().withId(caseId).withDefendants(asList(ConfirmedDefendant.confirmedDefendant().withId(defendant1).withOffences(asList(ConfirmedOffence.confirmedOffence().withId(defendant1sOffence1).build())).build())).build());

        final SeedingHearing seedingHearing = SeedingHearing.seedingHearing().withJurisdictionType(JurisdictionType.MAGISTRATES).build();

        when(enveloper.withMetadataFrom(finalEnvelope, PROGRESSION_QUERY_PROSECUTION_CASES)).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(any())).thenReturn(finalEnvelope);
        final JsonObject jsonObject = createObjectBuilder().add("prosecutionCase", objectToJsonObjectConverter.convert(buildProsecutionCaseWithDefendantWithOffenceWithPlea(caseId, defendant1, defendant1sOffence1, JurisdictionType.CROWN, 1))).build();
        when(finalEnvelope.payloadAsJsonObject()).thenReturn(jsonObject);
        when(requester.requestAsAdmin(any())).thenReturn(finalEnvelope);
        when(referenceDataService.getPleaType(any(), any())).thenReturn(of(createObjectBuilder().add("pleaTypeGuiltyFlag", "No").build()));

        List<ProsecutionCase> prosecutionCases = progressionService.transformProsecutionCase(confirmedProsecutionCases, LocalDate.now(), finalEnvelope, seedingHearing);

        assertThat(prosecutionCases.get(0).getCpsOrganisation(), is("A01"));
        assertThat(prosecutionCases.get(0).getTrialReceiptType(), is("Voluntary bill"));
        assertThat(1, is(prosecutionCases.get(0).getDefendants().size()));
        assertThat(1, is(prosecutionCases.get(0).getDefendants().get(0).getOffences().size()));
        assertThat(prosecutionCases.get(0).getDefendants().get(0).getOffences().get(0).getPlea(), is(nullValue()));
        assertThat(prosecutionCases.get(0).getDefendants().get(0).getOffences().get(0).getListingNumber(), is(2));
    }

    @Test
    public void shouldNotRemovePleaWhenMovingFromCrownToMagsAndGuiltyTypeNo() {

        final UUID caseId = randomUUID();
        final UUID defendant1 = randomUUID();
        final UUID defendant1sOffence1 = randomUUID();

        final List<ConfirmedProsecutionCase> confirmedProsecutionCases = Arrays.asList(ConfirmedProsecutionCase.confirmedProsecutionCase().withId(caseId).withDefendants(asList(ConfirmedDefendant.confirmedDefendant().withId(defendant1).withOffences(asList(ConfirmedOffence.confirmedOffence().withId(defendant1sOffence1).build())).build())).build());

        final SeedingHearing seedingHearing = SeedingHearing.seedingHearing().withJurisdictionType(JurisdictionType.CROWN).build();

        when(enveloper.withMetadataFrom(finalEnvelope, PROGRESSION_QUERY_PROSECUTION_CASES)).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(any())).thenReturn(finalEnvelope);
        final JsonObject jsonObject = createObjectBuilder().add("prosecutionCase", objectToJsonObjectConverter.convert(buildProsecutionCaseWithDefendantWithOffenceWithPlea(caseId, defendant1, defendant1sOffence1, JurisdictionType.MAGISTRATES))).build();
        when(finalEnvelope.payloadAsJsonObject()).thenReturn(jsonObject);
        when(requester.requestAsAdmin(any())).thenReturn(finalEnvelope);

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

        final List<ConfirmedProsecutionCase> confirmedProsecutionCases = Arrays.asList(ConfirmedProsecutionCase.confirmedProsecutionCase().withId(caseId).withDefendants(asList(ConfirmedDefendant.confirmedDefendant().withId(defendant1).withOffences(asList(ConfirmedOffence.confirmedOffence().withId(defendant1sOffence1).build())).build())).build());

        final SeedingHearing seedingHearing = SeedingHearing.seedingHearing().withJurisdictionType(JurisdictionType.MAGISTRATES).build();

        when(enveloper.withMetadataFrom(finalEnvelope, PROGRESSION_QUERY_PROSECUTION_CASES)).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(any())).thenReturn(finalEnvelope);
        final JsonObject jsonObject = createObjectBuilder().add("prosecutionCase", objectToJsonObjectConverter.convert(buildProsecutionCaseWithDefendantWithOffenceWithPlea(caseId, defendant1, defendant1sOffence1, JurisdictionType.MAGISTRATES))).build();
        when(finalEnvelope.payloadAsJsonObject()).thenReturn(jsonObject);
        when(requester.requestAsAdmin(any())).thenReturn(finalEnvelope);

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

        final List<ConfirmedProsecutionCase> confirmedProsecutionCases = Arrays.asList(ConfirmedProsecutionCase.confirmedProsecutionCase().withId(caseId).withDefendants(asList(ConfirmedDefendant.confirmedDefendant().withId(defendant1).withOffences(asList(ConfirmedOffence.confirmedOffence().withId(defendant1sOffence1).build())).build())).build());

        final SeedingHearing seedingHearing = SeedingHearing.seedingHearing().withJurisdictionType(JurisdictionType.MAGISTRATES).build();

        when(enveloper.withMetadataFrom(finalEnvelope, PROGRESSION_QUERY_PROSECUTION_CASES)).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(any())).thenReturn(finalEnvelope);
        final JsonObject jsonObject = createObjectBuilder().add("prosecutionCase", objectToJsonObjectConverter.convert(buildProsecutionCaseWithDefendantWithOffenceWithPlea(caseId, defendant1, defendant1sOffence1, JurisdictionType.CROWN))).build();
        when(finalEnvelope.payloadAsJsonObject()).thenReturn(jsonObject);
        when(requester.requestAsAdmin(any())).thenReturn(finalEnvelope);
        when(referenceDataService.getPleaType(any(), any())).thenReturn(of(createObjectBuilder().add("pleaTypeGuiltyFlag", "Yes").build()));

        List<ProsecutionCase> prosecutionCases = progressionService.transformProsecutionCase(confirmedProsecutionCases, LocalDate.now(), finalEnvelope, seedingHearing);

        assertThat(prosecutionCases.get(0).getCpsOrganisation(), is("A01"));
        assertThat(1, is(prosecutionCases.get(0).getDefendants().size()));
        assertThat(1, is(prosecutionCases.get(0).getDefendants().get(0).getOffences().size()));
        assertThat(prosecutionCases.get(0).getDefendants().get(0).getOffences().get(0).getPlea(), is(notNullValue()));
        assertThat(prosecutionCases.get(0).getDefendants().get(0).getOffences().get(0).getJudicialResults(), is(nullValue()));
    }

    @Test
    public void shouldNotRemovePleaWhenMovingFromMagsToCrownAndGuiltyTypeNoAndHasVerdict() {

        final UUID caseId = randomUUID();
        final UUID defendant1 = randomUUID();
        final UUID defendant1sOffence1 = randomUUID();

        final List<ConfirmedProsecutionCase> confirmedProsecutionCases = Arrays.asList(ConfirmedProsecutionCase.confirmedProsecutionCase().withId(caseId).withDefendants(asList(ConfirmedDefendant.confirmedDefendant().withId(defendant1).withOffences(asList(ConfirmedOffence.confirmedOffence().withId(defendant1sOffence1).build())).build())).build());

        final SeedingHearing seedingHearing = SeedingHearing.seedingHearing().withJurisdictionType(JurisdictionType.MAGISTRATES).build();

        when(enveloper.withMetadataFrom(finalEnvelope, PROGRESSION_QUERY_PROSECUTION_CASES)).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(any())).thenReturn(finalEnvelope);
        final JsonObject jsonObject = createObjectBuilder().add("prosecutionCase", objectToJsonObjectConverter.convert(buildProsecutionCaseWithDefendantWithOffenceWithPleaWithVerdict(caseId, defendant1, defendant1sOffence1, JurisdictionType.CROWN))).build();
        when(finalEnvelope.payloadAsJsonObject()).thenReturn(jsonObject);
        when(requester.requestAsAdmin(any())).thenReturn(finalEnvelope);

        List<ProsecutionCase> prosecutionCases = progressionService.transformProsecutionCase(confirmedProsecutionCases, LocalDate.now(), finalEnvelope, seedingHearing);

        assertThat(prosecutionCases.get(0).getCpsOrganisation(), is("A01"));
        assertThat(1, is(prosecutionCases.get(0).getDefendants().size()));
        assertThat(1, is(prosecutionCases.get(0).getDefendants().get(0).getOffences().size()));
        assertThat(prosecutionCases.get(0).getDefendants().get(0).getOffences().get(0).getPlea(), is(notNullValue()));
        assertThat(prosecutionCases.get(0).getDefendants().get(0).getOffences().get(0).getJudicialResults(), is(Matchers.nullValue()));
    }

    @Test
    public void shouldTransformProsecutionCaseInMultiCaseDefendantScenarioFromConfirmedHearing() {

        final Optional<JsonObject> confirmedHearingJsonObject = Optional.of(getJsonObjectResponseFromJsonResource("public.listing.hearing-confirmed-11SS0342023.json"));
        final JsonEnvelope confirmedJsonEnvelope = envelopeFrom(metadataWithRandomUUID("public.listing.hearing-confirmed"),
                confirmedHearingJsonObject.get());
        final HearingConfirmed hearingConfirmed = jsonObjectConverter.convert(confirmedJsonEnvelope.payloadAsJsonObject(), HearingConfirmed.class);
        final ConfirmedHearing confirmedHearing = hearingConfirmed.getConfirmedHearing();

        final List<ConfirmedProsecutionCase> confirmedProsecutionCases = confirmedHearing.getProsecutionCases();
        final LocalDate earliestHearingDate = ProgressionService.getEarliestDate(confirmedHearing.getHearingDays()).toLocalDate();

        final Optional<JsonObject> prosecutionCaseJsonObject = Optional.of(getJsonObjectResponseFromJsonResource("progression.prosecution-case-11SS0342023.json"));
        final JsonObject jsonObject = Json.createObjectBuilder().add("prosecutionCase", prosecutionCaseJsonObject.get()).build();

        when(enveloper.withMetadataFrom(confirmedJsonEnvelope, PROGRESSION_QUERY_PROSECUTION_CASES)).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(any())).thenReturn(confirmedJsonEnvelope);

        when(finalEnvelope.payloadAsJsonObject()).thenReturn(jsonObject);
        when(requester.requestAsAdmin(any())).thenReturn(finalEnvelope);

        List<ProsecutionCase> prosecutionCases = progressionService.transformProsecutionCase(confirmedProsecutionCases, earliestHearingDate, confirmedJsonEnvelope, null);

        assertThat(2, is(prosecutionCases.size()));
        assertThat(1, is(prosecutionCases.get(0).getDefendants().size()));
        assertThat(9, is(prosecutionCases.get(0).getDefendants().get(0).getOffences().size()));
        assertThat(prosecutionCases.get(0).getDefendants().get(0).getOffences().get(0).getPlea(), is(notNullValue()));
        assertThat(prosecutionCases.get(0).getDefendants().get(0).getOffences().get(0).getJudicialResults(), is(Matchers.nullValue()));
    }

    @Test
    public void shouldInitiateNewCourtApplication(){
        final JsonEnvelope event = getJsonEnvelop("progression.event.initiate-application-for-case-requested");
        final JsonObject jsonObject = getJsonObjectResponseFromJsonResource("progression.event.initiate-application-for-case-requested.json");
        final InitiateApplicationForCaseRequested initiateApplicationForCaseRequested= jsonObjectConverter.convert(jsonObject, InitiateApplicationForCaseRequested.class);
        final CourtApplicationType courtApplicationType = courtApplicationType().withType("type").build();
        when(referenceDataService.retrieveApplicationType(any(), any())).thenReturn(courtApplicationType);
        progressionService.initiateNewCourtApplication(event, initiateApplicationForCaseRequested);

        verify(sender).send(envelopeCaptor.capture());

        assertThat(envelopeCaptor.getValue().metadata().name(), is("progression.command.initiate-court-proceedings-for-application"));
        final JsonObject resultCommand = (JsonObject) envelopeCaptor.getValue().payload();

        JsonObject expectedCommand = getJsonObjectResponseFromJsonResource("progression.command.initiate-court-proceedings-for-application.json");
        assertThat(resultCommand, is(stringToJsonObjectConverter.convert(expectedCommand.toString().replaceAll("TODAY",LocalDate.now().toString()))));

    }

    @Test
    public void shouldTransformCourtCentre() {
        final UUID courtCentreId = randomUUID();
        final String address1 = "ADDRESS1";
        final String oucode = STRING.next();
        final JsonObject courtCentreJson = createObjectBuilder().add("oucodeL3Name", "Lavender Hill Magistrates Court").add("address1", address1).add("oucode", oucode).add("lja", "ljaCode").build();
        when(referenceDataService.getOrganisationUnitById(courtCentreId, finalEnvelope, requester)).thenReturn(of(courtCentreJson));
        when(referenceDataService.getLjaDetails(any(), any(), any())).thenReturn(LjaDetails.ljaDetails().withLjaCode("nationalCourtCode").withLjaName("name").withWelshLjaName("welshName").build());
        final ConfirmedHearing confirmedHearing = ConfirmedHearing.confirmedHearing().withId(randomUUID()).withEstimatedDuration(ESTIMATED_DURATION).withHearingDays(asList(HearingDay.hearingDay().withSittingDay(ZonedDateTime.now()).build())).withCourtCentre(CourtCentre.courtCentre().withId(courtCentreId).build()).build();

        final Hearing hearing = progressionService.transformConfirmedHearing(confirmedHearing, finalEnvelope);
        assertThat(hearing.getCourtCentre().getCode(), is(oucode));
        assertThat(hearing.getCourtCentre().getAddress().getAddress1(), is(address1));
        assertThat(hearing.getCourtCentre().getAddress().getAddress2(), is(EMPTY));
        assertThat(hearing.getCourtCentre().getAddress().getAddress3(), is(EMPTY));
        assertThat(hearing.getCourtCentre().getAddress().getAddress4(), is(EMPTY));
        assertThat(hearing.getCourtCentre().getAddress().getAddress5(), is(EMPTY));
        assertThat(hearing.getCourtCentre().getAddress().getPostcode(), nullValue());
        assertThat(hearing.getCourtCentre().getLja().getLjaCode(), is("nationalCourtCode"));
        assertThat(hearing.getCourtCentre().getLja().getLjaName(), is("name"));
        assertThat(hearing.getCourtCentre().getLja().getWelshLjaName(), is("welshName"));
        assertThat(hearing.getEstimatedDuration(), is(ESTIMATED_DURATION));
    }

    @Test
    public void shouldTransformToHearingFromConfirmedHearing() {
        final UUID courtCentreId = randomUUID();
        final JurisdictionType jurisdictionType = JurisdictionType.CROWN;
        final HearingType hearingType = HearingType.hearingType().withId(randomUUID()).build();
        final String address1 = "ADDRESS1";
        final String oucode = STRING.next();
        final JsonObject courtCentreJson = createObjectBuilder().add("oucodeL3Name", "Lavender Hill Magistrates Court").add("address1", address1).add("oucode", oucode).add("lja", "ljaCode").build();
        when(referenceDataService.getOrganisationUnitById(courtCentreId, finalEnvelope, requester)).thenReturn(of(courtCentreJson));
        when(referenceDataService.getLjaDetails(any(), any(), any())).thenReturn(LjaDetails.ljaDetails().withLjaCode("nationalCourtCode").withLjaName("name").withWelshLjaName("welshName").build());

        doReturn(EMPTY_LIST).when(progressionService).transformProsecutionCase(any(), any(), any(), any());

        final ConfirmedHearing confirmedHearing = ConfirmedHearing.confirmedHearing().withId(randomUUID()).withType(hearingType).withHearingDays(asList(HearingDay.hearingDay().withSittingDay(ZonedDateTime.now()).build())).withCourtCentre(CourtCentre.courtCentre().withId(courtCentreId).build()).withJurisdictionType(jurisdictionType).withProsecutionCases(generateProsecutionCases()).build();

        Hearing hearing = progressionService.transformToHearingFrom(confirmedHearing, finalEnvelope);

        assertThat(hearing.getType(), is(hearingType));
        assertThat(hearing.getJurisdictionType(), is(jurisdictionType));
        assertThat(hearing.getProsecutionCases(), is(EMPTY_LIST));
        assertThat(hearing.getCourtCentre().getId(), is(courtCentreId));
    }

    @Test
    public void shouldPopulateHearingToProbationCaseworker() {
        final JsonEnvelope envelope = getEnvelope("progression.command.populate-hearing-to-probation-caseworker");
        final UUID hearingId = randomUUID();
        progressionService.populateHearingToProbationCaseworker(envelope, hearingId);

        verify(sender).send(envelopeCaptor.capture());

        assertThat(envelopeCaptor.getValue().metadata().name(), is("progression.command.populate-hearing-to-probation-caseworker"));
        JsonObject jsonObject = envelopeCaptor.getValue().payload();
        assertThat(jsonObject.size(), is(1));
        assertThat(jsonObject.getString("hearingId"), is(hearingId.toString()));
    }

    @Test
    public void shouldRemoveJudicialResultFromApplication() {
        final UUID applicationId = randomUUID();
        final ConfirmedHearing confirmedHearing = ConfirmedHearing.confirmedHearing().withCourtApplicationIds(singletonList(applicationId)).build();

        final CourtApplication applicationInProgression = courtApplication().withJudicialResults(singletonList(judicialResult().build())).withFutureSummonsHearing(FutureSummonsHearing.futureSummonsHearing().build()).withCourtApplicationCases(singletonList(courtApplicationCase().withOffences(singletonList(Offence.offence().withJudicialResults(singletonList(judicialResult().build())).build())).build())).withCourtOrder(CourtOrder.courtOrder().withCourtOrderOffences(singletonList(CourtOrderOffence.courtOrderOffence().withOffence(Offence.offence().withJudicialResults(singletonList(judicialResult().build())).build()).build())).build()).build();

        when(requester.requestAsAdmin(any())).thenReturn(finalEnvelope);
        final JsonObject jsonObject = createObjectBuilder().add("courtApplication", objectToJsonObjectConverter.convert(applicationInProgression)).build();
        when(finalEnvelope.payloadAsJsonObject()).thenReturn(jsonObject);

        final JsonEnvelope jsonEnvelope = getEnvelope("progression.command.populate-hearing-to-probation-caseworker");
        List<CourtApplication> newApplications = progressionService.extractCourtApplications(confirmedHearing, jsonEnvelope);


        assertThat(newApplications.get(0).getJudicialResults(), is(nullValue()));
        assertThat(newApplications.get(0).getCourtApplicationCases().get(0).getOffences().get(0).getJudicialResults(), is(nullValue()));
        assertThat(newApplications.get(0).getCourtOrder().getCourtOrderOffences().get(0).getOffence().getJudicialResults(), is(nullValue()));
    }

    @Test
    public void shouldDeleteJudicialResultsFromApplicationWhenNewHearingForListing() {

        final JsonEnvelope jsonEnvelope = getEnvelope(PROGRESSION_EVENT_NEXT_HEARINGS_REQUESTED);
        final List<HearingListingNeeds> hearings = singletonList(HearingListingNeeds.hearingListingNeeds().withListedStartDateTime(ZonedDateTimes.fromString("2019-08-12T05:27:17.210Z")).withId(randomUUID()).withCourtApplications(singletonList(courtApplication().withJudicialResults(singletonList(judicialResult().build())).withId(randomUUID()).withCourtOrder(CourtOrder.courtOrder().withCourtOrderOffences(singletonList(CourtOrderOffence.courtOrderOffence().withOffence(Offence.offence().withJudicialResults(singletonList(judicialResult().build())).build()).build())).build()).withCourtApplicationCases(singletonList(courtApplicationCase().withOffences(singletonList(Offence.offence().withJudicialResults(singletonList(judicialResult().build())).build())).build())).build())).build());

        progressionService.updateHearingListingStatusToSentForListing(jsonEnvelope, hearings, null);

        verify(sender).send(envelopeArgumentCaptor.capture());
        assertThat(envelopeArgumentCaptor.getValue(), jsonEnvelope(metadata().withName(PROGRESSION_CREATE_HEARING_FOR_APPLICATION_COMMAND), payloadIsJson(allOf(withoutJsonPath("$.hearing.courtApplications[0].judicialResults"), withoutJsonPath("$.hearing.courtApplications[0].courtOrder.courtOrderOffences[0].offence.judicialResults"), withoutJsonPath("$.hearing.courtApplications[0].courtApplicationCases[0].offences[0].judicialResults")))));

    }

    @Test
    public void shouldDeleteJudicialResultsFromApplicationWhenNewHearingForListingWithoutCourtApplicationCase() {

        final JsonEnvelope jsonEnvelope = getEnvelope(PROGRESSION_EVENT_NEXT_HEARINGS_REQUESTED);
        final List<HearingListingNeeds> hearings = singletonList(HearingListingNeeds.hearingListingNeeds().withListedStartDateTime(ZonedDateTimes.fromString("2019-08-12T05:27:17.210Z")).withId(randomUUID()).withCourtApplications(singletonList(courtApplication().withJudicialResults(singletonList(judicialResult().build())).withId(randomUUID()).withCourtOrder(CourtOrder.courtOrder().withCourtOrderOffences(singletonList(CourtOrderOffence.courtOrderOffence().withOffence(Offence.offence().withJudicialResults(singletonList(judicialResult().build())).build()).build())).build()).build())).build());

        progressionService.updateHearingListingStatusToSentForListing(jsonEnvelope, hearings, null);

        verify(sender).send(envelopeArgumentCaptor.capture());
        assertThat(envelopeArgumentCaptor.getValue(), jsonEnvelope(metadata().withName(PROGRESSION_CREATE_HEARING_FOR_APPLICATION_COMMAND), payloadIsJson(allOf(withoutJsonPath("$.hearing.courtApplications[0].judicialResults"), withoutJsonPath("$.hearing.courtApplications[0].courtOrder.courtOrderOffences[0].offence.judicialResults")))));

    }


    @Test
    public void shouldDeleteJudicialResultsFromApplicationWhenNewHearingForListingWithoutCourtOrderOffencesAndCourtApplicationCases() {

        final JsonEnvelope jsonEnvelope = getEnvelope(PROGRESSION_EVENT_NEXT_HEARINGS_REQUESTED);
        final List<HearingListingNeeds> hearings = singletonList(HearingListingNeeds.hearingListingNeeds().withListedStartDateTime(ZonedDateTimes.fromString("2019-08-12T05:27:17.210Z")).withId(randomUUID()).withCourtApplications(singletonList(courtApplication().withJudicialResults(singletonList(judicialResult().build())).withId(randomUUID()).withCourtOrder(CourtOrder.courtOrder().build()).build())).build());

        progressionService.updateHearingListingStatusToSentForListing(jsonEnvelope, hearings, null);

        verify(sender).send(envelopeArgumentCaptor.capture());
        assertThat(envelopeArgumentCaptor.getValue(), jsonEnvelope(metadata().withName(PROGRESSION_CREATE_HEARING_FOR_APPLICATION_COMMAND), payloadIsJson(allOf(withoutJsonPath("$.hearing.courtApplications[0].judicialResults"), withoutJsonPath("$.hearing.courtApplications[0].courtOrder.courtOrderOffences[0].offence.judicialResults")))));

    }

    @Test
    public void transformProsecutionCaseShouldThrowExceptionIfProsecutionCaseDoesNotExist() {
        final UUID caseId = randomUUID();
        final UUID defendant1 = randomUUID();
        final UUID defendant1sOffence1 = randomUUID();
        final List<ConfirmedProsecutionCase> confirmedProsecutionCases = singletonList(ConfirmedProsecutionCase.confirmedProsecutionCase().withId(caseId).withDefendants(singletonList(ConfirmedDefendant.confirmedDefendant().withId(defendant1).withOffences(singletonList(ConfirmedOffence.confirmedOffence().withId(defendant1sOffence1).build())).build())).build());

        final LocalDate earliestHearingDate = LocalDate.now();
        final JsonEnvelope jsonEnvelope = getEnvelope(PROGRESSION_EVENT_NEXT_HEARINGS_REQUESTED);
        final SeedingHearing seedingHearing = SeedingHearing.seedingHearing().withSeedingHearingId(randomUUID()).withJurisdictionType(JurisdictionType.MAGISTRATES).build();

        assertThrows(CourtApplicationAndCaseNotFoundException.class, () -> progressionService.transformProsecutionCase(
                confirmedProsecutionCases,
                earliestHearingDate,
                jsonEnvelope,
                seedingHearing));
    }

    @Test
    public void transformProsecutionCaseShouldThrowExceptionIfProsecutionCaseEmpty() {
        final List<ConfirmedProsecutionCase> confirmedProsecutionCases = singletonList(ConfirmedProsecutionCase.confirmedProsecutionCase()
                .withId(randomUUID())
                .build());
        final JsonObject jsonObject = createObjectBuilder().build();
        when(enveloper.withMetadataFrom(any(), any())).thenReturn(enveloperFunction);
        when(finalEnvelope.payloadAsJsonObject()).thenReturn(jsonObject);
        when(requester.requestAsAdmin(any())).thenReturn(finalEnvelope);

        final LocalDate earliestHearingDate = LocalDate.now();
        final JsonEnvelope jsonEnvelope = getEnvelope(PROGRESSION_EVENT_NEXT_HEARINGS_REQUESTED);
        final SeedingHearing seedingHearing = SeedingHearing.seedingHearing().withSeedingHearingId(randomUUID()).withJurisdictionType(JurisdictionType.MAGISTRATES).build();

        assertThrows(CourtApplicationAndCaseNotFoundException.class, () -> progressionService.transformProsecutionCase(
                confirmedProsecutionCases,
                earliestHearingDate,
                jsonEnvelope,
                seedingHearing));
    }


    @Test
    public void shouldTransformCourtCentre1() {
        final UUID courtCentreId = randomUUID();
        final UUID defendantId = fromString("96ec1814-cfcd-4ef4-ba18-315a6c48659f");
        final String address1 = "ADDRESS1";
        final String oucode = STRING.next();
        final JsonObject courtCentreJson = createObjectBuilder().add("oucodeL3Name", "Lavender Hill Magistrates Court").add("address1", address1).add("oucode", oucode).add("lja", "ljaCode").build();

        when(enveloper.withMetadataFrom(finalEnvelope, PROGRESSION_QUERY_PROSECUTION_CASES)).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(any())).thenReturn(finalEnvelope);
        final JsonObject jsonObject = getPayload("caseQueryApiWithCourtOrdersExpectedResponse.json");
        when(finalEnvelope.payloadAsJsonObject()).thenReturn(jsonObject);
        when(requester.requestAsAdmin(any())).thenReturn(finalEnvelope);

        when(referenceDataService.getOrganisationUnitById(courtCentreId, finalEnvelope, requester)).thenReturn(of(courtCentreJson));
        when(referenceDataService.getLjaDetails(any(), any(), any())).thenReturn(LjaDetails.ljaDetails().withLjaCode("nationalCourtCode").withLjaName("name").withWelshLjaName("welshName").build());
        final ConfirmedHearing confirmedHearing = ConfirmedHearing.confirmedHearing().withId(randomUUID()).withHearingDays(singletonList(HearingDay.hearingDay().withSittingDay(ZonedDateTime.now()).build())).withCourtCentre(CourtCentre.courtCentre().withId(courtCentreId).build()).withProsecutionCases(singletonList(ConfirmedProsecutionCase.confirmedProsecutionCase().withId(defendantId).withDefendants(singletonList(ConfirmedDefendant.confirmedDefendant().withId(defendantId).withOffences(singletonList(ConfirmedOffence.confirmedOffence().withId(randomUUID()).build())).build())).build())).build();

        final Hearing hearing = progressionService.transformConfirmedHearing(confirmedHearing, finalEnvelope);
        assertThat(hearing.getCourtCentre().getCode(), is(oucode));
        assertThat(hearing.getCourtCentre().getAddress().getAddress1(), is(address1));
        assertThat(hearing.getCourtCentre().getAddress().getAddress2(), is(EMPTY));
        assertThat(hearing.getCourtCentre().getAddress().getAddress3(), is(EMPTY));
        assertThat(hearing.getCourtCentre().getAddress().getAddress4(), is(EMPTY));
        assertThat(hearing.getCourtCentre().getAddress().getAddress5(), is(EMPTY));
        assertThat(hearing.getCourtCentre().getAddress().getPostcode(), nullValue());
        assertThat(hearing.getCourtCentre().getLja().getLjaCode(), is("nationalCourtCode"));
        assertThat(hearing.getCourtCentre().getLja().getLjaName(), is("name"));
        assertThat(hearing.getCourtCentre().getLja().getWelshLjaName(), is("welshName"));
    }

    @Test
    public void shouldCreateNextHearingAndUpdateHearingListingStatusToSentForListingOnNextHearingRequested() {
        final JsonEnvelope jsonEnvelope = getEnvelope(PROGRESSION_EVENT_NEXT_HEARINGS_REQUESTED);
        final UUID hearingId = randomUUID();
        final UUID caseId = randomUUID();
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withId(caseId)
                .build();
        final LocalDate hearingDay = LocalDate.now();
        final UUID seedingHearingId = randomUUID();
        final SeedingHearing seedingHearing = SeedingHearing.seedingHearing()
                .withSeedingHearingId(seedingHearingId)
                .withSittingDay(hearingDay.toString())
                .build();

        final ListNextHearingsV3 listNextHearings = getListNextHearings(hearingId, singletonList(prosecutionCase), seedingHearing);
        progressionService.updateHearingListingStatusToSentForListing(jsonEnvelope, listNextHearings);
        verify(sender).send(envelopeCaptor.capture());
        assertThat(envelopeCaptor.getValue().metadata().name(), is(PROGRESSION_UPDATE_DEFENDANT_LISTING_STATUS_COMMAND_V3));
        JsonObject jsonObject = (JsonObject) envelopeCaptor.getValue().payload();
        assertThat(jsonObject.getJsonObject("hearing").getString("id"), is(hearingId.toString()));
    }

    @Test
    public void shouldCreateNextHearingWhenHearingIsWithApplicationOnly() {

        final JsonEnvelope jsonEnvelope = getEnvelope(PROGRESSION_EVENT_NEXT_HEARINGS_REQUESTED);
        final UUID hearingId = randomUUID();
        final UUID applicationId = randomUUID();
        final LocalDate hearingDay = LocalDate.now();
        final UUID seedingHearingId = randomUUID();
        final SeedingHearing seedingHearing = SeedingHearing.seedingHearing()
                .withSeedingHearingId(seedingHearingId)
                .withSittingDay(hearingDay.toString())
                .build();
        final CourtApplication courtApplication =  CourtApplication.courtApplication().withId(applicationId).build();

        final ListNextHearingsV3 listNextHearings = getListNextHearingsWithApplicationsOnly(hearingId, asList(courtApplication), seedingHearing);
        progressionService.updateHearingListingStatusToSentForListing(jsonEnvelope, listNextHearings);
        verify(sender).send(envelopeCaptor.capture());
        assertThat(envelopeCaptor.getValue().metadata().name(), is(PROGRESSION_CREATE_HEARING_FOR_APPLICATION_COMMAND));
        JsonObject jsonObject = (JsonObject) envelopeCaptor.getValue().payload();
        assertThat(jsonObject.getJsonObject("hearing").getString("id"), is(hearingId.toString()));
        assertThat(jsonObject.getJsonObject("hearing").getJsonArray("courtApplications").getJsonObject(0).getString("id"), is(applicationId.toString()));
        assertThat(jsonObject.getString(HEARING_LISTING_STATUS), is(SENT_FOR_LISTING));
    }

    @Test
    public void shouldThrowDataValidationExceptionWhenHearingRequestedWithoutNextHearings() {
        final JsonEnvelope jsonEnvelope = getEnvelope(PROGRESSION_EVENT_NEXT_HEARINGS_REQUESTED);
        final UUID hearingId = randomUUID();
        final UUID caseId = randomUUID();
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withId(caseId)
                .build();

        final ListNextHearingsV3 listNextHearings = getListNextHearings(hearingId, singletonList(prosecutionCase), null);

        final Exception exception = assertThrows(DataValidationException.class, () -> progressionService.updateHearingListingStatusToSentForListing(jsonEnvelope, listNextHearings));
        assertThat(exception.getMessage(), is("Next hearing without hearing not possible"));
    }

    private ListNextHearingsV3 getListNextHearings(final UUID hearingId, final List<ProsecutionCase> prosecutionCases, final SeedingHearing seedingHearing ) {
        final UUID bookingReferenceId = randomUUID();
        final List<UUID> courtScheduleIds = Arrays.asList(randomUUID(), randomUUID());

        final List<BookingReferenceCourtScheduleIds> bookingReferenceCourtScheduleIds = Arrays.asList(BookingReferenceCourtScheduleIds.bookingReferenceCourtScheduleIds()
                .withBookingId(bookingReferenceId)
                .withCourtScheduleIds(courtScheduleIds)
                .build());

        final Map<UUID, Set<UUID>> alreadyExistingAndNewBookingReferencesWithCourtScheduleIds = new HashMap<>();
        alreadyExistingAndNewBookingReferencesWithCourtScheduleIds.put(bookingReferenceId, new HashSet<>(courtScheduleIds));

        final NextHearingsRequested hearingResulted = NextHearingsRequested.nextHearingsRequested()
                .withSeedingHearing(seedingHearing)
                .withPreviousBookingReferencesWithCourtScheduleIds(bookingReferenceCourtScheduleIds)
                .withHearing(Hearing.hearing()
                        .withId(hearingId)
                        .build())
                .build();


        final List<HearingListingNeeds> nextHearingsList = Arrays.asList(HearingListingNeeds.hearingListingNeeds()
                .withId(hearingId)
                        .withProsecutionCases(prosecutionCases)
                .build());
        UUID seedingHearingId = null;
        if(null != seedingHearing){
            seedingHearingId = seedingHearing.getSeedingHearingId();
        }
        final ListNextHearingsV3 listNextHearings = ListNextHearingsV3.listNextHearingsV3()
                .withSeedingHearing(seedingHearing)
                .withHearingId(seedingHearingId)
                .withAdjournedFromDate(LocalDate.now())
                .withHearings(nextHearingsList)
                .withShadowListedOffences(hearingResulted.getShadowListedOffences())
                .build();
        return listNextHearings;
    }

    public static JsonObject getPayload(final String path) {
        String response = null;
        try {
            response = Resources.toString(Resources.getResource(path), Charset.defaultCharset());
        } catch (final Exception e) {
            e.printStackTrace();
        }

        return new StringToJsonObjectConverter().convert(response);
    }

    private ProsecutionCase buildProsecutionCasesWithTwoDefendantsOffences(UUID caseId, UUID defendant1, UUID defendant2, UUID defendant1sOffence1, UUID defendant1sOffence2, UUID defendant2sOffence1, UUID defendant2sOffence2, Boolean youth) {
        return ProsecutionCase.prosecutionCase().withId(caseId).withCpsOrganisation("A01").withDefendants(asList(Defendant.defendant().withId(defendant1).withIsYouth(youth).withOffences(asList(Offence.offence().withId(defendant1sOffence1).build(), Offence.offence().withId(defendant1sOffence2).build())).build(), Defendant.defendant().withId(defendant2).withIsYouth(youth).withOffences(asList(Offence.offence().withId(defendant2sOffence1).build(), Offence.offence().withId(defendant2sOffence2).build())).build())).build();
    }

    private ProsecutionCase buildProsecutionCaseWithDefendantWithOffenceWithPlea(UUID caseId, UUID defendant, UUID offence, JurisdictionType jurisdictionType) {
        return buildProsecutionCaseWithDefendantWithOffenceWithPlea(caseId, defendant, offence, jurisdictionType, null);
    }

    private ProsecutionCase buildProsecutionCaseWithDefendantWithOffenceWithPlea(UUID caseId, UUID defendant, UUID offence, JurisdictionType jurisdictionType, final Integer listingNumber) {
        return ProsecutionCase.prosecutionCase().withId(caseId).withCpsOrganisation("A01").withTrialReceiptType("Voluntary bill").withDefendants(asList(Defendant.defendant().withId(defendant).withIsYouth(Boolean.FALSE).withOffences(asList(Offence.offence().withId(offence).withListingNumber(listingNumber).withPlea(Plea.plea().withPleaValue("NOT_GUILTY").build()).withJudicialResults(asList(judicialResult().withNextHearing(NextHearing.nextHearing().withJurisdictionType(JurisdictionType.MAGISTRATES).build()).build(), judicialResult().build(), judicialResult().withNextHearing(NextHearing.nextHearing().withJurisdictionType(jurisdictionType).build()).build())).build())).build())).build();
    }

    private ProsecutionCase buildProsecutionCaseWithDefendantWithOffenceWithPleaWithVerdict(UUID caseId, UUID defendant, UUID offence, JurisdictionType jurisdictionType) {
        return ProsecutionCase.prosecutionCase().withId(caseId).withCpsOrganisation("A01").withDefendants(asList(Defendant.defendant().withId(defendant).withIsYouth(Boolean.FALSE).withOffences(asList(Offence.offence().withId(offence).withPlea(Plea.plea().withPleaValue("NOT_GUILTY").build()).withVerdict(Verdict.verdict().build()).withJudicialResults(asList(judicialResult().withNextHearing(NextHearing.nextHearing().withJurisdictionType(JurisdictionType.MAGISTRATES).build()).build(), judicialResult().build(), judicialResult().withNextHearing(NextHearing.nextHearing().withJurisdictionType(jurisdictionType).build()).build())).build())).build())).build();
    }

    private PublicListingNewDefendantAddedForCourtProceedings getDefendantAddedPayload() {
        return publicListingNewDefendantAddedForCourtProceedings().withHearingId(randomUUID()).withCaseId(randomUUID()).withDefendantId(randomUUID()).withCourtCentre(CourtCentre.courtCentre().withId(randomUUID()).withRoomId(randomUUID()).build()).withHearingDateTime(ZonedDateTime.now()).build();
    }

    /**
     * Returns the JsonObject for the given json resource @param resourceName
     * @param resourceName The json file resource name
     * @return The Json Object
     */
    private JsonObject getJsonObjectResponseFromJsonResource(String resourceName) {
        String response = null;
        try {
            response = Resources.toString(getResource(resourceName), defaultCharset());
        } catch (final Exception ignored) {
        }
        return new StringToJsonObjectConverter().convert(response);
    }

    private ListNextHearingsV3 getListNextHearingsWithApplicationsOnly(final UUID hearingId, final List<CourtApplication> courtApplications, final SeedingHearing seedingHearing ) {
        final UUID bookingReferenceId = randomUUID();
        final List<UUID> courtScheduleIds = Arrays.asList(randomUUID(), randomUUID());

        final List<BookingReferenceCourtScheduleIds> bookingReferenceCourtScheduleIds = Arrays.asList(BookingReferenceCourtScheduleIds.bookingReferenceCourtScheduleIds()
                .withBookingId(bookingReferenceId)
                .withCourtScheduleIds(courtScheduleIds)
                .build());

        final Map<UUID, Set<UUID>> alreadyExistingAndNewBookingReferencesWithCourtScheduleIds = new HashMap<>();
        alreadyExistingAndNewBookingReferencesWithCourtScheduleIds.put(bookingReferenceId, new HashSet<>(courtScheduleIds));

        final NextHearingsRequested hearingResulted = NextHearingsRequested.nextHearingsRequested()
                .withSeedingHearing(seedingHearing)
                .withPreviousBookingReferencesWithCourtScheduleIds(bookingReferenceCourtScheduleIds)
                .withHearing(Hearing.hearing()
                        .withId(hearingId)
                        .build())
                .build();


        final List<HearingListingNeeds> nextHearingsList = Arrays.asList(HearingListingNeeds.hearingListingNeeds()
                .withId(hearingId)
                .withCourtApplications(courtApplications)
                .build());
        UUID seedingHearingId = null;
        if(null != seedingHearing){
            seedingHearingId = seedingHearing.getSeedingHearingId();
        }
        final ListNextHearingsV3 listNextHearings = ListNextHearingsV3.listNextHearingsV3()
                .withSeedingHearing(seedingHearing)
                .withHearingId(seedingHearingId)
                .withAdjournedFromDate(LocalDate.now())
                .withHearings(nextHearingsList)
                .withShadowListedOffences(hearingResulted.getShadowListedOffences())
                .build();
        return listNextHearings;
    }

    @Test
    public void shouldGetPublicListNotices() {
        final JsonEnvelope jsonEnvelop = getJsonEnvelop(PROGRESSION_GENERATE_PUBLIC_LIST_OPA_NOTICE);
        when(requester.request(any())).thenReturn(jsonEnvelop);

        progressionService.getPublicListNotices(jsonEnvelop);

        verify(requester).request(envelopeCaptor.capture());
        assertThat(envelopeCaptor.getValue().metadata().name(), is(PROGRESSION_QUERY_PUBLIC_LIST_OPA_NOTICES));
    }

    @Test
    public void shouldGetPressListNotices() {
        final JsonEnvelope jsonEnvelop = getJsonEnvelop(PROGRESSION_GENERATE_PRESS_LIST_OPA_NOTICE);
        when(requester.request(any())).thenReturn(jsonEnvelop);

        progressionService.getPressListNotices(jsonEnvelop);

        verify(requester).request(envelopeCaptor.capture());
        assertThat(envelopeCaptor.getValue().metadata().name(), is(PROGRESSION_QUERY_PRESS_LIST_OPA_NOTICES));
    }

    @Test
    public void shouldGetResultListNotices() {
        final JsonEnvelope jsonEnvelop = getJsonEnvelop(PROGRESSION_GENERATE_RESULT_LIST_OPA_NOTICE);

        when(requester.request(any())).thenReturn(jsonEnvelop);

        progressionService.getResultListNotices(jsonEnvelop);

        verify(requester).request(envelopeCaptor.capture());
        assertThat(envelopeCaptor.getValue().metadata().name(), is(PROGRESSION_QUERY_RESULT_LIST_OPA_NOTICES));
    }

    private JsonEnvelope getJsonEnvelop(final String commandName) {
        return envelopeFrom(
                metadataBuilder()
                        .createdAt(ZonedDateTime.now())
                        .withName(commandName)
                        .withId(randomUUID())
                        .build(),
                Json.createObjectBuilder().build());
    }

    @Test
    public void shouldGetActiveApplicationsOnCase(){
        final UUID caseId = randomUUID();
        final JsonEnvelope inputEnvelop = envelopeFrom(metadataBuilder()
                .withName("progression.event.prosecution-case-defendant-updated")
                .withId(randomUUID())
                .build(),Json.createObjectBuilder().build());
        final JsonEnvelope outputEnvelop = envelopeFrom(metadataBuilder()
                .withName("progression.query.active-applications-on-case")
                .withId(randomUUID())
                .build(),Json.createObjectBuilder().add("linkedApplications",
                Json.createArrayBuilder().add(Json.createObjectBuilder().add("applicationId", randomUUID().toString()).build())
                        .add(Json.createObjectBuilder().add("applicationId", randomUUID().toString()).build()).build()).build());
        when(requester.request(any())).thenReturn(outputEnvelop);

        final Optional<JsonObject> activeApplicationsOnCase = progressionService.getActiveApplicationsOnCase(inputEnvelop, caseId.toString());

        assertThat(activeApplicationsOnCase, is(notNullValue()));
        assertThat(activeApplicationsOnCase.get().getJsonArray("linkedApplications"), is(notNullValue()));
        assertThat(activeApplicationsOnCase.get().getJsonArray("linkedApplications").size(), is(2));
    }

    @Test
    public void shouldReturnEmptyWhenNoActiveApplicationsOnCase(){
        final UUID caseId = randomUUID();
        final JsonEnvelope inputEnvelop = envelopeFrom(metadataBuilder()
                .withName("progression.event.prosecution-case-defendant-updated")
                .withId(randomUUID())
                .build(),Json.createObjectBuilder().build());
        final JsonEnvelope outputEnvelop = envelopeFrom(metadataBuilder()
                .withName("progression.query.active-applications-on-case")
                .withId(randomUUID())
                .build(),Json.createObjectBuilder().build());
        when(requester.request(any())).thenReturn(outputEnvelop);

        final Optional<JsonObject> activeApplicationsOnCase = progressionService.getActiveApplicationsOnCase(inputEnvelop, caseId.toString());

        assertThat(activeApplicationsOnCase, is(Optional.empty()));
    }
}
