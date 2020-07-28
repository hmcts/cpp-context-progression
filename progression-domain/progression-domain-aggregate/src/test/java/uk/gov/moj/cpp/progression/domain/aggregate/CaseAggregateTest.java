package uk.gov.moj.cpp.progression.domain.aggregate;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.LaaReference.laaReference;
import static uk.gov.justice.core.courts.ProsecutionCase.prosecutionCase;
import static uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum.INACTIVE;
import static uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum.SJP_REFERRAL;
import static uk.gov.moj.cpp.progression.domain.constant.LegalAidStatusEnum.GRANTED;
import static uk.gov.moj.cpp.progression.domain.constant.LegalAidStatusEnum.NO_VALUE;
import static uk.gov.moj.cpp.progression.domain.constant.LegalAidStatusEnum.REFUSED;
import static uk.gov.moj.cpp.progression.domain.constant.LegalAidStatusEnum.WITHDRAWN;

import uk.gov.justice.core.courts.CaseEjected;
import uk.gov.justice.core.courts.CaseLinkedToHearing;
import uk.gov.justice.core.courts.CaseNoteAdded;
import uk.gov.justice.core.courts.Category;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.DefendantDefenceOrganisationChanged;
import uk.gov.justice.core.courts.DefendantsAddedToCourtProceedings;
import uk.gov.justice.core.courts.DefendantsNotAddedToCourtProceedings;
import uk.gov.justice.core.courts.HearingConfirmedCaseStatusUpdated;
import uk.gov.justice.core.courts.HearingResultedCaseUpdated;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.InitiationCode;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.LaaReference;
import uk.gov.justice.core.courts.ListDefendantRequest;
import uk.gov.justice.core.courts.ListHearingRequest;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseCreated;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.core.courts.ProsecutionCaseOffencesUpdated;
import uk.gov.justice.core.courts.ReferralReason;
import uk.gov.justice.cpp.progression.events.DefendantDefenceAssociationLocked;
import uk.gov.justice.progression.courts.DefendantLegalaidStatusUpdated;
import uk.gov.justice.progression.courts.OffencesForDefendantChanged;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import uk.gov.moj.cpp.progression.domain.constant.LegalAidStatusEnum;
import uk.gov.moj.cpp.progression.domain.event.CaseAddedToCrownCourt;
import uk.gov.moj.cpp.progression.domain.event.ConvictionDateAdded;
import uk.gov.moj.cpp.progression.domain.event.ConvictionDateRemoved;
import uk.gov.moj.cpp.progression.domain.event.Defendant;
import uk.gov.moj.cpp.progression.domain.event.NewCaseDocumentReceivedEvent;
import uk.gov.moj.cpp.progression.domain.event.PreSentenceReportForDefendantsRequested;
import uk.gov.moj.cpp.progression.domain.event.SendingCommittalHearingInformationAdded;
import uk.gov.moj.cpp.progression.domain.event.SentenceHearingDateAdded;
import uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.Hearing;
import uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.SendingSheetCompleted;
import uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.SendingSheetInvalidated;
import uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.SendingSheetPreviouslyCompleted;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantAdded;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantPSR;
import uk.gov.moj.cpp.progression.domain.event.defendant.Offence;
import uk.gov.moj.cpp.progression.domain.event.defendant.Person;
import uk.gov.moj.cpp.progression.events.DefendantDefenceOrganisationDisassociated;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CaseAggregateTest {

    private static final String CASE_ID = randomUUID().toString();
    private static final String DEFENDANT_ID = randomUUID().toString();
    private static final String COURT_CENTRE_NAME = "Warwick Justice Centre";
    private static final String COURT_CENTRE_ID = "1234";
    private static final String HEARING_TYPE = "PTP";
    private static final String SENDING_COMMITTAL_DATE = "01-01-1990";
    private static final String CASE_URN = "87GD9945217";
    private static final String DEFENDANT_PERSON_ID = randomUUID().toString();
    private static final String DEFENDANT_FIRST_NAME = "David";
    private static final String DEFENDANT_LAST_NAME = "Lloyd";
    private static final String DEFENDANT_NATIONALITY = "British";
    private static final String DEFENDANT_GENDER = "Male";
    private static final String DEFENDANT_ADDRESS_1 = "Lima Court";
    private static final String DEFENDANT_ADDRESS_2 = "Bath Road";
    private static final String DEFENDANT_ADDRESS_3 = "Norwich";
    private static final String DEFENDANT_ADDRESS_4 = "UK";
    private static final String DEFENDANT_POSTCODE = "NR11HF";
    private static final String DEFENDANT_DATE_OF_BIRTH = "23-10-1995";
    private static final String BAIL_STATUS = "bailed";
    private static final String CUSTODY_TIME_LIMIT_DATE = "2018-01-30";
    private static final String DEFENCE_ORGANISATION = "Solicitor Jacob M";
    private static final boolean INTERPRETER_NEEDED = false;
    private static final String INTERPRETER_LANGUAGE = "English";
    private static final String OFFENCE_ID = randomUUID().toString();
    private static final String OFFENCE_CODE = "OF61131";
    private static final String SECTION = "S 51";
    private static final String WORDING = "On 10 Oct ...";
    private static final String REASON = "Not stated";
    private static final String DESCRIPTION = "Not available";
    private static final String CATEGORY = "Civil";
    private static final String START_DATE = "10-10-2017";
    private static final String END_DATE = "11-11-2017";
    private static final String CC_HEARING_DATE = "15-10-2017";
    private static final String CC_COURT_CENTRE_NAME = "Liverpool crown court";
    private static final String CC_COURT_CENTRE_ID = randomUUID().toString();
    private static final String INDICATED_PLEA_ID = randomUUID().toString();
    private static final String INDICATED_PLEA_VALUE = "NO_INDICATION";
    private static final String ALLOCATION_DECISION = "COURT_DECLINED";
    private static final uk.gov.justice.core.courts.Defendant defendant = uk.gov.justice.core.courts.Defendant.defendant().withId(randomUUID())
            .withPersonDefendant(PersonDefendant.personDefendant().build()).build();
    static final List<uk.gov.justice.core.courts.Defendant> defendants = new ArrayList<uk.gov.justice.core.courts.Defendant>() {{
        add(defendant);
    }};
    private static final ProsecutionCase prosecutionCase = prosecutionCase()
            .withCaseStatus("caseStatus")
            .withId(randomUUID())
            .withOriginatingOrganisation("originatingOrganisation")
            .withDefendants(defendants)
            .withInitiationCode(InitiationCode.C)
            .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                    .withProsecutionAuthorityReference("reference")
                    .withProsecutionAuthorityCode("code")
                    .withProsecutionAuthorityId(randomUUID())
                    .build())
            .build();

    @Mock
    JsonEnvelope envelope;
    @Mock

    JsonObject jsonObj;
    @InjectMocks
    private CaseAggregate caseAggregate;

    private static LaaReference generateRecordLAAReferenceForOffence(final String statusCode, final String defendantLevelStatus) {
        return laaReference()
                .withApplicationReference("AB746921")
                .withStatusDate(LocalDate.now())
                .withStatusId(randomUUID())
                .withStatusCode(statusCode)
                .withStatusDescription("statusDescription")
                .withEffectiveStartDate(LocalDate.now())
                .withEffectiveEndDate(LocalDate.now())
                .withOffenceLevelStatus(defendantLevelStatus)
                .build();

    }

    @Before
    public void setUp() {
        this.caseAggregate = new CaseAggregate();
    }

    @Test
    public void shouldHandleSentenceHearingDateAdded() {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final LocalDate sentenceHearingDate = LocalDate.now();
        createDefendant(defendantId);

        final SentenceHearingDateAdded sentenceHearingDateAdded =
                new SentenceHearingDateAdded(sentenceHearingDate, caseId);

        final Object response = this.caseAggregate.apply(sentenceHearingDateAdded);

        assertThat(((SentenceHearingDateAdded) response).getCaseId(), is(caseId));
        assertThat(((SentenceHearingDateAdded) response).getSentenceHearingDate(), is(sentenceHearingDate));
    }

    @Test
    public void shouldAddCaseToCrownCourt() {
        final UUID caseId = randomUUID();
        final UUID courtCentreId = randomUUID();


        final CaseAddedToCrownCourt caseAddedToCrownCourt =
                new CaseAddedToCrownCourt(caseId, courtCentreId.toString());

        final Object response = this.caseAggregate.apply(caseAddedToCrownCourt);

        assertThat(((CaseAddedToCrownCourt) response).getCaseId(), is(caseId));
        assertThat(((CaseAddedToCrownCourt) response).getCourtCentreId(), is(courtCentreId.toString()));
    }

    @Test
    public void shouldDoHearingResultedUpdateCase() {
        final UUID caseId = randomUUID();

        final UUID defendantId1 = randomUUID();
        final UUID defendantId2 = randomUUID();
        final UUID defendantId3 = randomUUID();

        final List<uk.gov.justice.core.courts.Defendant> defendants = getDefendants(defendantId1, defendantId2, defendantId3);

        final ProsecutionCase prosecutionCase = prosecutionCase().withDefendants(defendants).withId(caseId).build();
        final HearingResultedCaseUpdated prosecutionCaseUpdated = HearingResultedCaseUpdated.hearingResultedCaseUpdated().withProsecutionCase(prosecutionCase).build();

        final Object response = this.caseAggregate.apply(prosecutionCaseUpdated);

        assertThat(((HearingResultedCaseUpdated) response).getProsecutionCase().getId(), is(caseId));
        assertThat(((HearingResultedCaseUpdated) response).getProsecutionCase().getDefendants().get(0).getId().toString(), is(defendantId1.toString()));
        assertThat(((HearingResultedCaseUpdated) response).getProsecutionCase().getDefendants().get(1).getId().toString(), is(defendantId2.toString()));
        assertThat(((HearingResultedCaseUpdated) response).getProsecutionCase().getDefendants().get(2).getId().toString(), is(defendantId3.toString()));

    }

    @Test
    public void shouldAddNewCaseDocumentReceivedEvent() {
        final UUID caseId = randomUUID();
        final NewCaseDocumentReceivedEvent newCaseDocumentReceivedEvent =
                new NewCaseDocumentReceivedEvent(caseId, "fileId", "fileMimeType", "fileName");

        final Object response = this.caseAggregate.apply(newCaseDocumentReceivedEvent);

        assertThat(((NewCaseDocumentReceivedEvent) response).getCppCaseId(), is(caseId));

    }

    @Test
    public void shouldApplyPreSentenceReportForDefendantsRequested() {
        final UUID defendantId = randomUUID();
        final UUID caseId = randomUUID();
        final PreSentenceReportForDefendantsRequested preSentenceReportForDefendantsRequested =
                new PreSentenceReportForDefendantsRequested(caseId, asList(new DefendantPSR(defendantId, Boolean.TRUE)));

        final Object response = this.caseAggregate.apply(preSentenceReportForDefendantsRequested);

        assertThat(((PreSentenceReportForDefendantsRequested) response).getCaseId(), is(caseId));
        assertThat(((PreSentenceReportForDefendantsRequested) response).getDefendants().get(0).getDefendantId(), is(defendantId));
    }

    @Test
    public void shouldApplySendingCommittalHearingInformationAdded() {
        final LocalDate localDate = LocalDate.now();
        final String courtCentre = "birmingham";
        final UUID caseId = randomUUID();
        final SendingCommittalHearingInformationAdded sendingCommittalHearingInformationAdded =
                new SendingCommittalHearingInformationAdded(caseId, courtCentre, localDate, randomUUID().toString());

        final Object response = this.caseAggregate.apply(sendingCommittalHearingInformationAdded);

        assertThat(((SendingCommittalHearingInformationAdded) response).getCaseId(), is(caseId));
        assertThat(((SendingCommittalHearingInformationAdded) response).getSendingCommittalDate(), is(localDate));
    }

    @Test
    public void shouldApplyCompleteSendingSheet() {
        final List<Object> objects = applySendingSheet(a -> {
        });
        assertThat(objects.size(), is(1));
        final Object obj = objects.get(0);
        assertThat(obj, instanceOf(SendingSheetCompleted.class));
        assertSendingSheetCompletedValues((SendingSheetCompleted) obj);
    }

    @Test
    public void shouldInvalidateSendingSheetWrongCourtCentre() {
        final List<Object> objects = applySendingSheet(a -> {
            Whitebox.setInternalState(this.caseAggregate, "courtCentreId", null);
        });
        assertThat(objects.size(), is(1));
        final Object obj = objects.get(0);
        assertThat(obj, instanceOf(SendingSheetInvalidated.class));
        final SendingSheetInvalidated sendingSheetInvalidated = (SendingSheetInvalidated) obj;
        assertTrue(sendingSheetInvalidated.getDescription().contains(CC_COURT_CENTRE_ID));
    }

    @Test
    public void shouldInvalidateSendingSheetNoDefendants() {
        final List<Object> objects = applySendingSheet(a -> {
            Whitebox.setInternalState(this.caseAggregate, "defendants", new HashSet<>());
        });
        assertThat(objects.size(), is(1));
        final Object obj = objects.get(0);
        assertThat(obj, instanceOf(SendingSheetInvalidated.class));
        final SendingSheetInvalidated sendingSheetInvalidated = (SendingSheetInvalidated) obj;
        assertEquals(sendingSheetInvalidated.getCaseId(), fromString(CASE_ID));
    }

    @Test
    public void shouldInvalidateSendingSheetWrongDefendants() {
        final List<Object> objects = applySendingSheet(a -> {
            final Defendant defendant = new Defendant();
            defendant.setId(UUID.randomUUID());
            Whitebox.setInternalState(this.caseAggregate, "defendants", new HashSet<>(asList(defendant)));
        });
        assertThat(objects.size(), is(1));
        final Object obj = objects.get(0);
        assertThat(obj, instanceOf(SendingSheetInvalidated.class));
        final SendingSheetInvalidated sendingSheetInvalidated = (SendingSheetInvalidated) obj;
        assertEquals(sendingSheetInvalidated.getCaseId(), fromString(CASE_ID));
    }

    @Test
    public void shouldInvalidateSendingSheetWrongOffences() {
        final List<Object> objects = applySendingSheet(a -> {
            final Map<UUID, Set<UUID>> offenceIdsByDefendantId = new HashMap<>();
            Whitebox.setInternalState(this.caseAggregate, "offenceIdsByDefendantId", offenceIdsByDefendantId);
        });
        assertThat(objects.size(), is(1));
        final Object obj = objects.get(0);
        assertThat(obj, instanceOf(SendingSheetInvalidated.class));
        final SendingSheetInvalidated sendingSheetInvalidated = (SendingSheetInvalidated) obj;
        assertEquals(sendingSheetInvalidated.getCaseId(), fromString(CASE_ID));
        assertTrue(sendingSheetInvalidated.getDescription().contains(OFFENCE_ID));

    }

    @Test
    public void shouldHandleConvictionDateAdded() {

        final UUID caseId = randomUUID();
        final UUID offenceId = randomUUID();
        final LocalDate convictionDate = LocalDate.now();

        final ConvictionDateAdded convictionDateAdded = ConvictionDateAdded.builder().withCaseId(caseId)
                .withOffenceId(offenceId).withConvictionDate(convictionDate).build();

        final Object response = this.caseAggregate.apply(convictionDateAdded);

        assertThat(((ConvictionDateAdded) response).getCaseId(), is(caseId));
        assertThat(((ConvictionDateAdded) response).getOffenceId(), is(offenceId));
        assertThat(((ConvictionDateAdded) response).getConvictionDate(), is(convictionDate));
    }

    @Test
    public void shouldHandleConvictionDateRemoved() {

        final UUID caseId = randomUUID();
        final UUID offenceId = randomUUID();

        final ConvictionDateRemoved convictionDateRemoved = ConvictionDateRemoved.builder().withCaseId(caseId)
                .withOffenceId(offenceId).build();

        final Object response = this.caseAggregate.apply(convictionDateRemoved);

        assertThat(((ConvictionDateRemoved) response).getCaseId(), is(caseId));
        assertThat(((ConvictionDateRemoved) response).getOffenceId(), is(offenceId));
    }

    private List<Object> applySendingSheet(final Consumer<CaseAggregate> adjustInternals) {
        createCompleteSendingSheetEnvelope();
        final SendingSheetCompleted sendingSheetCompleted = new SendingSheetCompleted();
        final Hearing hearing = new Hearing();
        hearing.setCaseId(UUID.fromString("4daefec6-5f77-4109-82d9-1e60544a6c05"));
        sendingSheetCompleted.setHearing(hearing);
        final Set<Defendant> defendants = new HashSet<>();
        final Defendant defendant = new Defendant();
        defendants.add(defendant);
        defendant.setId(UUID.fromString(DEFENDANT_ID));
        final Map<UUID, Set<UUID>> offenceIdsByDefendantId = new HashMap<>();
        offenceIdsByDefendantId.put(UUID.fromString(DEFENDANT_ID), new HashSet(asList(UUID.fromString(OFFENCE_ID))));
        //green path internals
        Whitebox.setInternalState(this.caseAggregate, "courtCentreId", CC_COURT_CENTRE_ID);
        Whitebox.setInternalState(this.caseAggregate, "defendants", defendants);
        Whitebox.setInternalState(this.caseAggregate, "offenceIdsByDefendantId", offenceIdsByDefendantId);
        adjustInternals.accept(this.caseAggregate);

        final Stream<Object> stream = this.caseAggregate.completeSendingSheet(this.envelope);
        return stream.collect(Collectors.toList());

    }

    @Test
    public void shouldApplyCompleteSendingSheetPreviouslyCompleted() {
        final List<Object> objects = applySendingSheet(a -> {
            final Set<UUID> caseIdsWithCompletedSendingSheet = new HashSet<>(asList(UUID.fromString(CASE_ID)));
            Whitebox.setInternalState(this.caseAggregate, "caseIdsWithCompletedSendingSheet", caseIdsWithCompletedSendingSheet);
        });
        assertThat(objects.size(), is(1));
        final Object obj = objects.get(0);
        assertThat(obj, instanceOf(SendingSheetPreviouslyCompleted.class));
        assertThat(CASE_ID, equalTo(((SendingSheetPreviouslyCompleted) obj).getCaseId().toString()));
    }

    private void createDefendant(final UUID defendantId) {
        final UUID caseId = randomUUID();
        final Defendant defendant = new Defendant(defendantId);
        final Offence offence = new Offence(randomUUID(),
                randomUUID().toString(),
                null,
                "",
                "",
                "",
                "",
                "",
                "",
                LocalDate.now(),
                LocalDate.now(),
                LocalDate.now(),
                LocalDate.now());
        final Person person = new Person(randomUUID(), "", "", "", LocalDate.now(), "", "", "", "", "", "", "", null);
        final DefendantAdded defendantAdded = new DefendantAdded(caseId, defendantId, person, "", asList(offence), "CaseUrn");
        this.caseAggregate.apply(defendantAdded);
    }

    private void createCompleteSendingSheetEnvelope() {
        when(this.envelope.payloadAsJsonObject()).thenReturn(this.jsonObj);
        when(this.jsonObj.getString(Mockito.eq("caseId"))).thenReturn(CASE_ID);
        when(this.jsonObj.getString(Mockito.eq("isKeyEvidence"))).thenReturn("true");
        when(this.jsonObj.getString(Mockito.eq("planDate"))).thenReturn(LocalDate.now().toString());
        when(this.jsonObj.getString(Mockito.eq("sendingCommittalDate")))
                .thenReturn(LocalDate.now().toString());
        when(this.jsonObj.getString(Mockito.eq("sentenceHearingDate")))
                .thenReturn(LocalDate.now().toString());
        when(this.jsonObj.getString(Mockito.eq("courtCentreId")))
                .thenReturn(COURT_CENTRE_ID);

        final UUID defendantId = randomUUID();
        when(this.jsonObj.getJsonArray(Mockito.eq("defendants"))).thenReturn(Json.createArrayBuilder()
                .add(Json.createObjectBuilder().add("id", defendantId.toString()).build())
                .build());


        when(this.jsonObj.getJsonObject("hearing")).thenReturn(Json.createObjectBuilder()
                .add("courtCentreName", COURT_CENTRE_NAME)
                .add("courtCentreId", COURT_CENTRE_ID).add("type", HEARING_TYPE)
                .add("sendingCommittalDate", SENDING_COMMITTAL_DATE).add("caseId", CASE_ID)
                .add("caseUrn", CASE_URN)
                .add("defendants", Json.createArrayBuilder().add(Json.createObjectBuilder()
                        .add("id", DEFENDANT_ID)
                        .add("personId", DEFENDANT_PERSON_ID)
                        .add("firstName", DEFENDANT_FIRST_NAME).add("lastName", DEFENDANT_LAST_NAME)
                        .add("nationality", DEFENDANT_NATIONALITY).add("gender", DEFENDANT_GENDER)
                        .add("address", Json.createObjectBuilder()
                                .add("address1", DEFENDANT_ADDRESS_1)
                                .add("address2", DEFENDANT_ADDRESS_2)
                                .add("address3", DEFENDANT_ADDRESS_3)
                                .add("address4", DEFENDANT_ADDRESS_4)
                                .add("postcode", DEFENDANT_POSTCODE).build())
                        .add("dateOfBirth", DEFENDANT_DATE_OF_BIRTH)
                        .add("bailStatus", BAIL_STATUS)
                        .add("custodyTimeLimitDate", CUSTODY_TIME_LIMIT_DATE)
                        .add("defenceOrganisation", DEFENCE_ORGANISATION)
                        .add("interpreter", Json.createObjectBuilder()
                                .add("needed", INTERPRETER_NEEDED)
                                .add("language", INTERPRETER_LANGUAGE).build())
                        .add("offences", Json.createArrayBuilder().add(Json
                                .createObjectBuilder()
                                .add("id", OFFENCE_ID)
                                .add("offenceCode", OFFENCE_CODE)
                                .add("indicatedPlea", Json.createObjectBuilder().add("id", INDICATED_PLEA_ID).add("value", INDICATED_PLEA_VALUE).add("allocationDecision", ALLOCATION_DECISION).build())
                                .add("section", SECTION)
                                .add("wording", WORDING)
                                .add("reason", REASON)
                                .add("description", DESCRIPTION)
                                .add("category", CATEGORY)
                                .add("startDate", START_DATE)
                                .add("endDate", END_DATE).build()))
                        .build()).build())
                .build());
        when(this.jsonObj.getJsonObject("crownCourtHearing"))
                .thenReturn(Json.createObjectBuilder().add("ccHearingDate", CC_HEARING_DATE)
                        .add("courtCentreName", CC_COURT_CENTRE_NAME).add("courtCentreId", CC_COURT_CENTRE_ID)
                        .build());
    }

    private void assertSendingSheetCompletedValues(final SendingSheetCompleted ssCompleted) {
        assertThat(CC_HEARING_DATE, equalTo(ssCompleted.getCrownCourtHearing().getCcHearingDate()));
        assertThat(CC_COURT_CENTRE_ID, equalTo(ssCompleted.getCrownCourtHearing().getCourtCentreId().toString()));
        assertThat(CC_COURT_CENTRE_NAME, equalTo(ssCompleted.getCrownCourtHearing().getCourtCentreName()));
        assertSendingSheetCompletedHearingValues(ssCompleted.getHearing());
    }

    private void assertSendingSheetCompletedHearingValues(final Hearing hearing) {
        assertThat(COURT_CENTRE_NAME, equalTo(hearing.getCourtCentreName()));
        assertThat(COURT_CENTRE_ID, equalTo(hearing.getCourtCentreId()));
        assertThat(HEARING_TYPE, equalTo(hearing.getType()));
        assertThat(SENDING_COMMITTAL_DATE, equalTo(hearing.getSendingCommittalDate()));
        assertThat(CASE_URN, equalTo(hearing.getCaseUrn()));
        assertThat(CASE_ID, equalTo(hearing.getCaseId().toString()));
        assertHearingDefendant(hearing.getDefendants().get(0));
    }

    private void assertHearingDefendant(final uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.Defendant defendant) {
        assertThat(DEFENDANT_ID, equalTo(defendant.getId().toString()));
        assertThat(DEFENDANT_PERSON_ID, equalTo(defendant.getPersonId().toString()));
        assertThat(DEFENDANT_FIRST_NAME, equalTo(defendant.getFirstName()));
        assertThat(DEFENDANT_LAST_NAME, equalTo(defendant.getLastName()));
        assertThat(DEFENDANT_ADDRESS_1, equalTo(defendant.getAddress().getAddress1()));
        assertThat(DEFENDANT_ADDRESS_2, equalTo(defendant.getAddress().getAddress2()));
        assertThat(DEFENDANT_ADDRESS_3, equalTo(defendant.getAddress().getAddress3()));
        assertThat(DEFENDANT_ADDRESS_4, equalTo(defendant.getAddress().getAddress4()));
        assertThat(DEFENDANT_POSTCODE, equalTo(defendant.getAddress().getPostcode()));
        assertThat(DEFENDANT_DATE_OF_BIRTH, equalTo(defendant.getDateOfBirth()));
        assertThat(BAIL_STATUS, equalTo(defendant.getBailStatus()));
        assertThat(CUSTODY_TIME_LIMIT_DATE, equalTo(defendant.getCustodyTimeLimitDate()));
        assertThat(DEFENCE_ORGANISATION, equalTo(defendant.getDefenceOrganisation()));
        assertThat(INTERPRETER_NEEDED, equalTo(defendant.getInterpreter().getNeeded()));
        assertThat(INTERPRETER_LANGUAGE, equalTo(defendant.getInterpreter().getLanguage()));
        assertDefendantOffence(defendant.getOffences().get(0));
    }

    private void assertDefendantOffence(final uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.Offence offence) {
        assertThat(OFFENCE_ID, equalTo(offence.getId().toString()));
        assertThat(OFFENCE_CODE, equalTo(offence.getOffenceCode()));
        assertThat(INDICATED_PLEA_ID, equalTo(offence.getIndicatedPlea().getId().toString()));
        assertThat(INDICATED_PLEA_VALUE, equalTo(offence.getIndicatedPlea().getValue()));
        assertThat(ALLOCATION_DECISION, equalTo(offence.getIndicatedPlea().getAllocationDecision()));
        assertThat(SECTION, equalTo(offence.getSection()));
        assertThat(WORDING, equalTo(offence.getWording()));
        assertThat(REASON, equalTo(offence.getReason()));
        assertThat(DESCRIPTION, equalTo(offence.getDescription()));
        assertThat(CATEGORY, equalTo(offence.getCategory()));
        assertThat(START_DATE, equalTo(offence.getStartDate()));
        assertThat(END_DATE, equalTo(offence.getEndDate()));
    }

    @Test
    public void shouldReturnProsecutionCaseCreated() {

        final List<Object> eventStream = caseAggregate.createProsecutionCase(prosecutionCase).collect(toList());

        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(equalTo(ProsecutionCaseCreated.class)));
    }

    @Test
    public void shouldDefendantsNotAddedToCourtProceedings() {

        final DefendantsNotAddedToCourtProceedings defendantsNotAddedToCourtProceedings = DefendantsNotAddedToCourtProceedings
                .defendantsNotAddedToCourtProceedings()
                .withDefendants(new ArrayList<>())
                .withListHearingRequests(new ArrayList<>())
                .build();

        final List<Object> eventStream = caseAggregate.defendantsAddedToCourtProceedings(defendantsNotAddedToCourtProceedings.getDefendants(),
                defendantsNotAddedToCourtProceedings.getListHearingRequests()).collect(toList());

        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(equalTo(DefendantsNotAddedToCourtProceedings.class)));

        //Assert total defendants are empty
        assertThat(defendantsNotAddedToCourtProceedings.getDefendants().isEmpty(), is(true));
        //Assert total listHearingRequests are empty
        assertThat(((DefendantsNotAddedToCourtProceedings) object).getListHearingRequests().isEmpty(), is(true));
    }

    @Test
    public void shouldDefendantsAddedToCourtProceedings() {

        final UUID caseId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();
        final UUID defendantId2 = UUID.randomUUID();
        final UUID offenceId = UUID.randomUUID();

        final DefendantsAddedToCourtProceedings defendantsAddedToCourtProceedings = buildDefendantsAddedToCourtProceedings(
                caseId, defendantId, defendantId2, offenceId);

        final CaseAggregate caseAggregate = new CaseAggregate();
        caseAggregate.apply(new ProsecutionCaseCreated(prosecutionCase, null));

        final List<Object> eventStream = caseAggregate.defendantsAddedToCourtProceedings(defendantsAddedToCourtProceedings.getDefendants(),
                defendantsAddedToCourtProceedings.getListHearingRequests()).collect(toList());

        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(equalTo(DefendantsAddedToCourtProceedings.class)));

        //Assert total defendants with count 3 including duplicates
        assertThat(defendantsAddedToCourtProceedings.getDefendants().size(), is(3));
        //Assert total defendants with count 2 excluded duplicates
        assertThat(((DefendantsAddedToCourtProceedings) object).getDefendants().size(), is(2));
    }

    @Test
    public void shouldReturnCaseEjected() {
        final List<Object> eventStream = caseAggregate.ejectCase(randomUUID(), "Legal").collect(toList());

        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(equalTo(CaseEjected.class)));
    }

    @Test
    public void shouldNotReturnCaseEjected() {
        Whitebox.setInternalState(this.caseAggregate, "caseStatus", "EJECTED");
        final List<Object> eventStream = caseAggregate.ejectCase(randomUUID(), "Legal").collect(toList());

        assertThat(eventStream.size(), is(0));
    }

    @Test
    public void shouldLAAReferenceUpdatedForOffence_whenOneOfTheOffenceIsGranted_expectGrantedInDefendantLevelLegalAidStatus() {
        final UUID caseId = fromString(CASE_ID);
        final UUID defendantId = fromString(DEFENDANT_ID);
        final UUID offenceId = fromString(OFFENCE_ID);
        final UUID defendantId2 = randomUUID();
        final DefendantsAddedToCourtProceedings defendantsAddedToCourtProceedings = buildDefendantsAddedToCourtProceedings(
                caseId, defendantId, defendantId2, offenceId);

        final CaseAggregate caseAggregate = new CaseAggregate();
        caseAggregate.apply(new ProsecutionCaseCreated(prosecutionCase, null));
        caseAggregate.defendantsAddedToCourtProceedings(defendantsAddedToCourtProceedings.getDefendants(),
                defendantsAddedToCourtProceedings.getListHearingRequests()).collect(toList());
        final LaaReference laaReference = generateRecordLAAReferenceForOffence("G2", GRANTED.getDescription());
        final List<Object> eventStream = caseAggregate.recordLAAReferenceForOffence(caseId, defendantId, offenceId, laaReference).collect(toList());
        assertThat(eventStream.size(), is(3));
        final Object object1 = eventStream.get(0);
        assertThat(object1.getClass(), is(equalTo(ProsecutionCaseOffencesUpdated.class)));
        assertThat(((ProsecutionCaseOffencesUpdated) object1).getDefendantCaseOffences().getOffences().size(), is(1));
        assertThat(((ProsecutionCaseOffencesUpdated) object1).getDefendantCaseOffences().getOffences().get(0).getLaaApplnReference().getApplicationReference(), is(laaReference.getApplicationReference()));
        assertThat(((ProsecutionCaseOffencesUpdated) object1).getDefendantCaseOffences().getOffences().get(0).getLaaApplnReference().getStatusId(), is(laaReference.getStatusId()));
        assertThat(((ProsecutionCaseOffencesUpdated) object1).getDefendantCaseOffences().getOffences().get(0).getLaaApplnReference().getStatusCode(), is(laaReference.getStatusCode()));
        assertThat(((ProsecutionCaseOffencesUpdated) object1).getDefendantCaseOffences().getOffences().get(0).getLaaApplnReference().getEffectiveStartDate(), is(laaReference.getEffectiveStartDate()));
        assertThat(((ProsecutionCaseOffencesUpdated) object1).getDefendantCaseOffences().getOffences().get(0).getLaaApplnReference().getEffectiveEndDate(), is(laaReference.getEffectiveEndDate()));
        assertThat(((ProsecutionCaseOffencesUpdated) object1).getDefendantCaseOffences().getOffences().get(0).getLaaApplnReference().getStatusDescription(), is(laaReference.getStatusDescription()));
        assertThat(((ProsecutionCaseOffencesUpdated) object1).getDefendantCaseOffences().getLegalAidStatus(),
                is(LegalAidStatusEnum.GRANTED.getDescription()));

        final Object object2 = eventStream.get(1);
        assertThat(object2.getClass(), is(equalTo(OffencesForDefendantChanged.class)));
        assertThat(((OffencesForDefendantChanged) object2).getAddedOffences(), is(nullValue()));
        assertThat(((OffencesForDefendantChanged) object2).getDeletedOffences(), is(nullValue()));
        assertThat(((OffencesForDefendantChanged) object2).getUpdatedOffences().size(), is(1));
        assertThat(((OffencesForDefendantChanged) object2).getUpdatedOffences().get(0).getOffences().get(0).getLaaApplnReference().getApplicationReference(), is(laaReference.getApplicationReference()));
        assertThat(((OffencesForDefendantChanged) object2).getUpdatedOffences().get(0).getOffences().get(0).getLaaApplnReference().getStatusId(), is(laaReference.getStatusId()));
        assertThat(((OffencesForDefendantChanged) object2).getUpdatedOffences().get(0).getOffences().get(0).getLaaApplnReference().getStatusCode(), is(laaReference.getStatusCode()));
        assertThat(((OffencesForDefendantChanged) object2).getUpdatedOffences().get(0).getOffences().get(0).getLaaApplnReference().getEffectiveStartDate(), is(laaReference.getEffectiveEndDate()));
        assertThat(((OffencesForDefendantChanged) object2).getUpdatedOffences().get(0).getOffences().get(0).getLaaApplnReference().getEffectiveEndDate(), is(laaReference.getEffectiveEndDate()));
        assertThat(((OffencesForDefendantChanged) object2).getUpdatedOffences().get(0).getOffences().get(0).getLaaApplnReference().getStatusDescription(), is(laaReference.getStatusDescription()));

    }

    @Test
    public void shouldLAAReferenceUpdatedForOffence_whenOneOfTheOffenceIsRefused_expectRefusedInDefendantLevelLegalAidStatus() {
        final UUID caseId = fromString(CASE_ID);
        final UUID defendantId = fromString(DEFENDANT_ID);
        final UUID offenceId = fromString(OFFENCE_ID);
        final UUID defendantId2 = randomUUID();
        final DefendantsAddedToCourtProceedings defendantsAddedToCourtProceedings = buildDefendantsAddedToCourtProceedings(
                caseId, defendantId, defendantId2, offenceId);

        final CaseAggregate caseAggregate = new CaseAggregate();
        caseAggregate.apply(new ProsecutionCaseCreated(prosecutionCase, null));
        caseAggregate.defendantsAddedToCourtProceedings(defendantsAddedToCourtProceedings.getDefendants(),
                defendantsAddedToCourtProceedings.getListHearingRequests()).collect(toList());
        final LaaReference laaReference = generateRecordLAAReferenceForOffence("FM", REFUSED.getDescription());
        final List<Object> eventStream = caseAggregate.recordLAAReferenceForOffence(caseId, defendantId, offenceId, laaReference).collect(toList());
        assertThat(eventStream.size(), is(3));
        final Object object1 = eventStream.get(0);
        assertThat(object1.getClass(), is(equalTo(ProsecutionCaseOffencesUpdated.class)));
        assertThat(((ProsecutionCaseOffencesUpdated) object1).getDefendantCaseOffences().getOffences().size(), is(1));
        assertThat(((ProsecutionCaseOffencesUpdated) object1).getDefendantCaseOffences().getOffences().get(0).getLaaApplnReference().getApplicationReference(), is(laaReference.getApplicationReference()));
        assertThat(((ProsecutionCaseOffencesUpdated) object1).getDefendantCaseOffences().getOffences().get(0).getLaaApplnReference().getStatusId(), is(laaReference.getStatusId()));
        assertThat(((ProsecutionCaseOffencesUpdated) object1).getDefendantCaseOffences().getOffences().get(0).getLaaApplnReference().getStatusCode(), is(laaReference.getStatusCode()));
        assertThat(((ProsecutionCaseOffencesUpdated) object1).getDefendantCaseOffences().getOffences().get(0).getLaaApplnReference().getEffectiveStartDate(), is(laaReference.getEffectiveStartDate()));
        assertThat(((ProsecutionCaseOffencesUpdated) object1).getDefendantCaseOffences().getOffences().get(0).getLaaApplnReference().getEffectiveEndDate(), is(laaReference.getEffectiveEndDate()));
        assertThat(((ProsecutionCaseOffencesUpdated) object1).getDefendantCaseOffences().getOffences().get(0).getLaaApplnReference().getStatusDescription(), is(laaReference.getStatusDescription()));
        assertThat(((ProsecutionCaseOffencesUpdated) object1).getDefendantCaseOffences().getLegalAidStatus(),
                is(LegalAidStatusEnum.REFUSED.getDescription()));

        final Object object2 = eventStream.get(1);
        assertThat(object2.getClass(), is(equalTo(OffencesForDefendantChanged.class)));
        assertThat(((OffencesForDefendantChanged) object2).getAddedOffences(), is(nullValue()));
        assertThat(((OffencesForDefendantChanged) object2).getDeletedOffences(), is(nullValue()));
        assertThat(((OffencesForDefendantChanged) object2).getUpdatedOffences().size(), is(1));
        assertThat(((OffencesForDefendantChanged) object2).getUpdatedOffences().get(0).getOffences().get(0).getLaaApplnReference().getApplicationReference(), is(laaReference.getApplicationReference()));
        assertThat(((OffencesForDefendantChanged) object2).getUpdatedOffences().get(0).getOffences().get(0).getLaaApplnReference().getStatusId(), is(laaReference.getStatusId()));
        assertThat(((OffencesForDefendantChanged) object2).getUpdatedOffences().get(0).getOffences().get(0).getLaaApplnReference().getStatusCode(), is(laaReference.getStatusCode()));
        assertThat(((OffencesForDefendantChanged) object2).getUpdatedOffences().get(0).getOffences().get(0).getLaaApplnReference().getEffectiveStartDate(), is(laaReference.getEffectiveEndDate()));
        assertThat(((OffencesForDefendantChanged) object2).getUpdatedOffences().get(0).getOffences().get(0).getLaaApplnReference().getEffectiveEndDate(), is(laaReference.getEffectiveEndDate()));
        assertThat(((OffencesForDefendantChanged) object2).getUpdatedOffences().get(0).getOffences().get(0).getLaaApplnReference().getStatusDescription(), is(laaReference.getStatusDescription()));

    }

    @Test
    public void shouldLAAReferenceUpdatedForOffence_whenOneOfTheOffenceIsWithDrawn_expectWithDrawnInDefendantLevelLegalAidStatus() {
        final UUID caseId = fromString(CASE_ID);
        final UUID defendantId = fromString(DEFENDANT_ID);
        final UUID offenceId = fromString(OFFENCE_ID);
        final UUID defendantId2 = randomUUID();
        final DefendantsAddedToCourtProceedings defendantsAddedToCourtProceedings = buildDefendantsAddedToCourtProceedings(
                caseId, defendantId, defendantId2, offenceId);

        final CaseAggregate caseAggregate = new CaseAggregate();
        caseAggregate.apply(new ProsecutionCaseCreated(prosecutionCase, null));
        caseAggregate.defendantsAddedToCourtProceedings(defendantsAddedToCourtProceedings.getDefendants(),
                defendantsAddedToCourtProceedings.getListHearingRequests()).collect(toList());
        final LaaReference laaReference = generateRecordLAAReferenceForOffence("WD", WITHDRAWN.getDescription());
        final List<Object> eventStream = caseAggregate.recordLAAReferenceForOffence(caseId, defendantId, offenceId, laaReference).collect(toList());
        assertThat(eventStream.size(), is(3));
        final Object object1 = eventStream.get(0);
        assertThat(object1.getClass(), is(equalTo(ProsecutionCaseOffencesUpdated.class)));
        assertThat(((ProsecutionCaseOffencesUpdated) object1).getDefendantCaseOffences().getOffences().size(), is(1));
        assertThat(((ProsecutionCaseOffencesUpdated) object1).getDefendantCaseOffences().getOffences().get(0).getLaaApplnReference().getApplicationReference(), is(laaReference.getApplicationReference()));
        assertThat(((ProsecutionCaseOffencesUpdated) object1).getDefendantCaseOffences().getOffences().get(0).getLaaApplnReference().getStatusId(), is(laaReference.getStatusId()));
        assertThat(((ProsecutionCaseOffencesUpdated) object1).getDefendantCaseOffences().getOffences().get(0).getLaaApplnReference().getStatusCode(), is(laaReference.getStatusCode()));
        assertThat(((ProsecutionCaseOffencesUpdated) object1).getDefendantCaseOffences().getOffences().get(0).getLaaApplnReference().getEffectiveStartDate(), is(laaReference.getEffectiveStartDate()));
        assertThat(((ProsecutionCaseOffencesUpdated) object1).getDefendantCaseOffences().getOffences().get(0).getLaaApplnReference().getEffectiveEndDate(), is(laaReference.getEffectiveEndDate()));
        assertThat(((ProsecutionCaseOffencesUpdated) object1).getDefendantCaseOffences().getOffences().get(0).getLaaApplnReference().getStatusDescription(), is(laaReference.getStatusDescription()));
        assertThat(((ProsecutionCaseOffencesUpdated) object1).getDefendantCaseOffences().getLegalAidStatus(),
                is(WITHDRAWN.getDescription()));

        final Object object2 = eventStream.get(1);
        assertThat(object2.getClass(), is(equalTo(OffencesForDefendantChanged.class)));
        assertThat(((OffencesForDefendantChanged) object2).getAddedOffences(), is(nullValue()));
        assertThat(((OffencesForDefendantChanged) object2).getDeletedOffences(), is(nullValue()));
        assertThat(((OffencesForDefendantChanged) object2).getUpdatedOffences().size(), is(1));
        assertThat(((OffencesForDefendantChanged) object2).getUpdatedOffences().get(0).getOffences().get(0).getLaaApplnReference().getApplicationReference(), is(laaReference.getApplicationReference()));
        assertThat(((OffencesForDefendantChanged) object2).getUpdatedOffences().get(0).getOffences().get(0).getLaaApplnReference().getStatusId(), is(laaReference.getStatusId()));
        assertThat(((OffencesForDefendantChanged) object2).getUpdatedOffences().get(0).getOffences().get(0).getLaaApplnReference().getStatusCode(), is(laaReference.getStatusCode()));
        assertThat(((OffencesForDefendantChanged) object2).getUpdatedOffences().get(0).getOffences().get(0).getLaaApplnReference().getEffectiveStartDate(), is(laaReference.getEffectiveEndDate()));
        assertThat(((OffencesForDefendantChanged) object2).getUpdatedOffences().get(0).getOffences().get(0).getLaaApplnReference().getEffectiveEndDate(), is(laaReference.getEffectiveEndDate()));
        assertThat(((OffencesForDefendantChanged) object2).getUpdatedOffences().get(0).getOffences().get(0).getLaaApplnReference().getStatusDescription(), is(laaReference.getStatusDescription()));

    }

    @Test
    public void shouldLAAReferenceUpdatedForOffence_whenOneOfTheOffenceIsRefusedAndOtherOneIsGranted_expectGrantedInDefendantLevelLegalAidStatus() {
        final UUID caseId = fromString(CASE_ID);
        final UUID defendantId = fromString(DEFENDANT_ID);
        final UUID offenceId1 = fromString(OFFENCE_ID);
        final UUID offenceId2 = randomUUID();

        final ProsecutionCase prosecutionCase = prosecutionCase()
                .withCaseStatus("caseStatus")
                .withId(randomUUID())
                .withOriginatingOrganisation("originatingOrganisation")
                .withDefendants(asList(uk.gov.justice.core.courts.Defendant.defendant().
                        withId(defendantId)
                        .withPersonDefendant(PersonDefendant.personDefendant().build())
                        .withOffences(asList(uk.gov.justice.core.courts.Offence.offence().withId(offenceId1).build()
                                , uk.gov.justice.core.courts.Offence.offence().withId(offenceId2).build()))
                        .build()))
                .withInitiationCode(InitiationCode.C)
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                        .withProsecutionAuthorityReference("reference")
                        .withProsecutionAuthorityCode("code")
                        .withProsecutionAuthorityId(randomUUID())
                        .build())
                .build();
        caseAggregate.createProsecutionCase(prosecutionCase);

        final LaaReference laaReference1 = generateRecordLAAReferenceForOffence("FM", REFUSED.getDescription());
        caseAggregate.recordLAAReferenceForOffence(caseId, defendantId, offenceId1, laaReference1);

        final LaaReference laaReference2 = generateRecordLAAReferenceForOffence("G2", GRANTED.getDescription());
        final List<Object> eventStream = caseAggregate.recordLAAReferenceForOffence(caseId, defendantId, offenceId2, laaReference2).collect(toList());
        assertThat(eventStream.size(), is(3));
        final Object object1 = eventStream.get(0);
        assertThat(object1.getClass(), is(equalTo(ProsecutionCaseOffencesUpdated.class)));
        assertThat(((ProsecutionCaseOffencesUpdated) object1).getDefendantCaseOffences().getLegalAidStatus(),
                is(LegalAidStatusEnum.GRANTED.getDescription()));
        final Object object2 = eventStream.get(1);
        assertThat(object2.getClass(), is(equalTo(OffencesForDefendantChanged.class)));
        assertThat(((OffencesForDefendantChanged) object2).getAddedOffences(), is(nullValue()));
        assertThat(((OffencesForDefendantChanged) object2).getDeletedOffences(), is(nullValue()));

    }

    @Test
    public void shouldDisassociateAssociatedDefenceOrganisation_whenLaaReferenceIsWithDrawnAndDefendantLevelStatusIsNotGranted() {
        final UUID caseId = fromString(CASE_ID);
        final UUID defendantId = fromString(DEFENDANT_ID);
        final UUID offenceId1 = fromString(OFFENCE_ID);
        final UUID offenceId2 = randomUUID();
        final UUID associateDefenceOrganisationId = randomUUID();

        final HashMap<UUID, UUID> map = new HashMap();
        map.put(defendantId, associateDefenceOrganisationId);

        ReflectionUtil.setField(caseAggregate, "defendantAssociatedDefenceOrganisation", map);

        final ProsecutionCase prosecutionCase = prosecutionCase()
                .withCaseStatus("caseStatus")
                .withId(randomUUID())
                .withOriginatingOrganisation("originatingOrganisation")
                .withDefendants(singletonList(uk.gov.justice.core.courts.Defendant.defendant().
                        withId(defendantId)
                        .withPersonDefendant(PersonDefendant.personDefendant().build())
                        .withAssociationLockedByRepOrder(true)
                        .withOffences(asList(uk.gov.justice.core.courts.Offence.offence().withId(offenceId1).build()
                                , uk.gov.justice.core.courts.Offence.offence().withId(offenceId2)
                                        .withLaaApplnReference(laaReference().withStatusCode("WD").withOffenceLevelStatus(WITHDRAWN.getDescription()).build())
                                        .build()))
                        .build()))
                .withInitiationCode(InitiationCode.C)
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                        .withProsecutionAuthorityReference("reference")
                        .withProsecutionAuthorityCode("code")
                        .withProsecutionAuthorityId(randomUUID())
                        .build())
                .build();
        caseAggregate.createProsecutionCase(prosecutionCase);

        final LaaReference laaReference1 = generateRecordLAAReferenceForOffence("WD", WITHDRAWN.getDescription());
        final List<Object> eventStream = caseAggregate.recordLAAReferenceForOffence(caseId, defendantId, offenceId1, laaReference1).collect(toList());
        assertThat(eventStream.size(), is(6));

        final Object object1 = eventStream.get(0);
        assertThat(object1.getClass(), is(equalTo(ProsecutionCaseOffencesUpdated.class)));
        assertThat(((ProsecutionCaseOffencesUpdated) object1).getDefendantCaseOffences().getLegalAidStatus(),
                is(WITHDRAWN.getDescription()));
        final Object object2 = eventStream.get(1);
        assertThat(object2.getClass(), is(equalTo(OffencesForDefendantChanged.class)));
        assertThat(((OffencesForDefendantChanged) object2).getAddedOffences(), is(nullValue()));
        assertThat(((OffencesForDefendantChanged) object2).getDeletedOffences(), is(nullValue()));

        final Object object3 = eventStream.get(2);
        assertThat(object3.getClass(), is(equalTo(DefendantLegalaidStatusUpdated.class)));

        final Object object4 = eventStream.get(3);
        assertThat(object4.getClass(), is(equalTo(DefendantDefenceOrganisationChanged.class)));

        final Object object5 = eventStream.get(4);
        assertThat(object5.getClass(), is(equalTo(DefendantDefenceOrganisationDisassociated.class)));

    }


    @Test
    public void shouldNotDisassociateAssociatedDefenceOrganisation_whenLaaReferenceIsWithDrawnAndDefendantLevelStatusIsNotGranted() {
        final UUID caseId = fromString(CASE_ID);
        final UUID defendantId = fromString(DEFENDANT_ID);
        final UUID offenceId1 = fromString(OFFENCE_ID);
        final UUID offenceId2 = randomUUID();
        final UUID associateDefenceOrganisationId = randomUUID();

        final HashMap<UUID, UUID> map = new HashMap();
        map.put(defendantId, associateDefenceOrganisationId);

        ReflectionUtil.setField(caseAggregate, "defendantAssociatedDefenceOrganisation", map);

        final ProsecutionCase prosecutionCase = prosecutionCase()
                .withCaseStatus("caseStatus")
                .withId(randomUUID())
                .withOriginatingOrganisation("originatingOrganisation")
                .withDefendants(singletonList(uk.gov.justice.core.courts.Defendant.defendant().
                        withId(defendantId)
                        .withPersonDefendant(PersonDefendant.personDefendant().build())
                        .withAssociationLockedByRepOrder(true)
                        .withOffences(asList(uk.gov.justice.core.courts.Offence.offence().withId(offenceId1)
                                        .withLaaApplnReference(laaReference().withStatusCode("WD").withOffenceLevelStatus(REFUSED.getDescription()).build())
                                        .build()
                                , uk.gov.justice.core.courts.Offence.offence().withId(offenceId2)
                                        .withLaaApplnReference(laaReference().withStatusCode("WD").withOffenceLevelStatus(REFUSED.getDescription()).build())
                                        .build()))
                        .build()))
                .withInitiationCode(InitiationCode.C)
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                        .withProsecutionAuthorityReference("reference")
                        .withProsecutionAuthorityCode("code")
                        .withProsecutionAuthorityId(randomUUID())
                        .build())
                .build();
        caseAggregate.createProsecutionCase(prosecutionCase);

        final LaaReference laaReference1 = generateRecordLAAReferenceForOffence("WD", WITHDRAWN.getDescription());
        final List<Object> eventStream = caseAggregate.recordLAAReferenceForOffence(caseId, defendantId, offenceId1, laaReference1).collect(toList());
        assertThat(eventStream.size(), is(6));

        final Object object1 = eventStream.get(0);
        assertThat(object1.getClass(), is(equalTo(ProsecutionCaseOffencesUpdated.class)));
        assertThat(((ProsecutionCaseOffencesUpdated) object1).getDefendantCaseOffences().getLegalAidStatus(),
                is(NO_VALUE.getDescription()));
        final Object object2 = eventStream.get(1);
        assertThat(object2.getClass(), is(equalTo(OffencesForDefendantChanged.class)));
        assertThat(((OffencesForDefendantChanged) object2).getAddedOffences(), is(nullValue()));
        assertThat(((OffencesForDefendantChanged) object2).getDeletedOffences(), is(nullValue()));

        final Object object3 = eventStream.get(2);
        assertThat(object3.getClass(), is(equalTo(DefendantLegalaidStatusUpdated.class)));
    }

    @Test
    public void shouldNotLAAReferenceUpdatedForOffence() {
        final UUID caseId = fromString(CASE_ID);
        final UUID defendantId = fromString(DEFENDANT_ID);
        final UUID offenceId = fromString(OFFENCE_ID);
        final LaaReference laaReference = generateRecordLAAReferenceForOffence("G2", GRANTED.getDescription());
        final List<Object> eventStream = caseAggregate.recordLAAReferenceForOffence(caseId, defendantId, offenceId, laaReference).collect(toList());
        assertThat(eventStream.size(), is(0));

    }

    private DefendantsAddedToCourtProceedings buildDefendantsAddedToCourtProceedings(
            final UUID caseId, final UUID defendantId, final UUID defendantId2, final UUID offenceId) {


        final uk.gov.justice.core.courts.Offence offence = uk.gov.justice.core.courts.Offence.offence()
                .withId(offenceId)
                .withOffenceDefinitionId(UUID.randomUUID())
                .withOffenceCode("TFL123")
                .withOffenceTitle("TFL Ticket Dodger")
                .withWording("TFL ticket dodged")
                .withStartDate(LocalDate.of(2019, 05, 01))
                .withCount(0)
                .build();
        final uk.gov.justice.core.courts.Defendant defendant = uk.gov.justice.core.courts.Defendant.defendant()
                .withId(defendantId)
                .withProsecutionCaseId(caseId)
                .withOffences(singletonList(offence))
                .build();

        //Add duplicate defendant
        final uk.gov.justice.core.courts.Defendant defendant1 = uk.gov.justice.core.courts.Defendant.defendant()
                .withId(defendantId)
                .withProsecutionCaseId(caseId)
                .withOffences(singletonList(offence))
                .build();

        final uk.gov.justice.core.courts.Defendant defendant2 = uk.gov.justice.core.courts.Defendant.defendant()
                .withId(defendantId2)
                .withProsecutionCaseId(caseId)
                .withOffences(singletonList(offence))
                .build();

        final ReferralReason referralReason = ReferralReason.referralReason()
                .withId(UUID.randomUUID())
                .withDefendantId(defendantId)
                .withDescription("Dodged TFL tickets with passion")
                .build();

        final ReferralReason referralReason2 = ReferralReason.referralReason()
                .withId(UUID.randomUUID())
                .withDefendantId(defendantId2)
                .withDescription("Dodged TFL tickets with passion")
                .build();

        final ListDefendantRequest listDefendantRequest = ListDefendantRequest.listDefendantRequest()
                .withProsecutionCaseId(caseId)
                .withDefendantOffences(singletonList(offenceId))
                .withReferralReason(referralReason)
                .build();
        final ListDefendantRequest listDefendantRequest2 = ListDefendantRequest.listDefendantRequest()
                .withProsecutionCaseId(caseId)
                .withDefendantOffences(singletonList(offenceId))
                .withReferralReason(referralReason2)
                .build();

        final HearingType hearingType = HearingType.hearingType().withId(UUID.randomUUID()).withDescription("TO_JAIL").build();
        final CourtCentre courtCentre = CourtCentre.courtCentre().withId(UUID.randomUUID()).build();

        final ListHearingRequest listHearingRequest = ListHearingRequest.listHearingRequest()
                .withCourtCentre(courtCentre).withHearingType(hearingType)
                .withJurisdictionType(JurisdictionType.MAGISTRATES)
                .withListDefendantRequests(asList(listDefendantRequest, listDefendantRequest2))
                .build();

        return DefendantsAddedToCourtProceedings
                .defendantsAddedToCourtProceedings()
                .withDefendants(asList(defendant, defendant1, defendant2))
                .withListHearingRequests(singletonList(listHearingRequest))
                .build();

    }

    @Test
    public void shouldLinkCaseToHearing() {
        final List<Object> eventStream = caseAggregate.linkProsecutionCaseToHearing(randomUUID(), randomUUID()).collect(toList());

        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(equalTo(CaseLinkedToHearing.class)));
    }

    @Test
    public void shouldAddCaseNote() {
        final List<Object> eventStream = caseAggregate.addNote(randomUUID(), "This is a Note", "Bob", "Marley").collect(toList());

        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(equalTo(CaseNoteAdded.class)));
    }

    @Test
    public void shouldHandleCaseNoteAdded() {
        final UUID caseId = randomUUID();
        final CaseNoteAdded caseNoteAdded = CaseNoteAdded.caseNoteAdded()
                .withCaseId(caseId)
                .withNote("Note")
                .withFirstName("Russell")
                .withLastName("Crow")
                .withCreatedDateTime(ZonedDateTime.now())
                .build();

        final Object response = this.caseAggregate.apply(caseNoteAdded);

        assertThat(response, is(caseNoteAdded));
    }

    private List<uk.gov.justice.core.courts.Defendant> getDefendants(final UUID defendantId1, final UUID defandantId2, final UUID defendnatId3) {

        final uk.gov.justice.core.courts.Defendant defendant1 = uk.gov.justice.core.courts.Defendant.defendant().withId(defendantId1).build();
        final uk.gov.justice.core.courts.Defendant defendant2 = uk.gov.justice.core.courts.Defendant.defendant().withId(defandantId2).build();
        final uk.gov.justice.core.courts.Defendant defendant3 = uk.gov.justice.core.courts.Defendant.defendant().withId(defendnatId3).build();

        final List<uk.gov.justice.core.courts.Defendant> defsList = new ArrayList<>();
        defsList.add(defendant1);
        defsList.add(defendant2);
        defsList.add(defendant3);
        return defsList;
    }

    @Test
    public void shouldUpdateCaseStatus() {
        final List<Object> eventStream = caseAggregate.updateCaseStatus(prosecutionCase().build(), SJP_REFERRAL.getDescription()).collect(toList());
        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(equalTo(HearingConfirmedCaseStatusUpdated.class)));
    }

    @Test
    public void shouldUpdateCaseStatusWhenAllApplicationResultsAreFinalized() {
        final UUID caseId = randomUUID();
        final CourtApplication courtApplication = CourtApplication.courtApplication()
                .withLinkedCaseId(caseId)
                .withJudicialResults(singletonList(JudicialResult.judicialResult().withCategory(Category.FINAL).build()))
                .build();
        final uk.gov.justice.core.courts.Defendant defendant = uk.gov.justice.core.courts.Defendant.defendant()
                .withId(randomUUID())
                .withProceedingsConcluded(true)
                .withOffences(
                        singletonList(uk.gov.justice.core.courts.Offence.offence()
                                .withProceedingsConcluded(true)
                                .withJudicialResults(
                                        singletonList(JudicialResult.judicialResult()
                                                .withCategory(Category.FINAL)
                                                .build())).build()))
                .build();
        final ProsecutionCase prosecutionCase = prosecutionCase()
                .withId(caseId)
                .withCaseStatus(SJP_REFERRAL.getDescription())
                .withDefendants(singletonList(defendant))
                .build();
        final List<Object> eventStream = caseAggregate.updateCase(prosecutionCase, singletonList(courtApplication)).collect(toList());

        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(equalTo(HearingResultedCaseUpdated.class)));
        final HearingResultedCaseUpdated hearingResultedCaseUpdated = (HearingResultedCaseUpdated) eventStream.get(0);
        assertThat(hearingResultedCaseUpdated.getProsecutionCase().getCaseStatus(), is(INACTIVE.getDescription()));

    }

    @Test
    public void shouldNotUpdateCaseStatusWhenAllApplicationResultsAreNotFinalized() {
        final UUID caseId = randomUUID();
        final CourtApplication courtApplication = CourtApplication.courtApplication()
                .withLinkedCaseId(caseId)
                .withJudicialResults(singletonList(JudicialResult.judicialResult().withCategory(Category.FINAL).build()))
                .build();
        final uk.gov.justice.core.courts.Defendant defendant = uk.gov.justice.core.courts.Defendant.defendant()
                .withId(randomUUID())
                .withProceedingsConcluded(true)
                .withOffences(
                        singletonList(uk.gov.justice.core.courts.Offence.offence()
                                .withProceedingsConcluded(true)
                                .withJudicialResults(
                                        singletonList(JudicialResult.judicialResult()
                                                .withCategory(Category.INTERMEDIARY)
                                                .build())).build()))
                .build();
        final ProsecutionCase prosecutionCase = prosecutionCase()
                .withId(caseId)
                .withCaseStatus(SJP_REFERRAL.getDescription())
                .withDefendants(singletonList(defendant))
                .build();
        final List<Object> eventStream = caseAggregate.updateCase(prosecutionCase, singletonList(courtApplication)).collect(toList());

        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(equalTo(HearingResultedCaseUpdated.class)));
        final HearingResultedCaseUpdated hearingResultedCaseUpdated = (HearingResultedCaseUpdated) eventStream.get(0);
        assertThat(hearingResultedCaseUpdated.getProsecutionCase().getCaseStatus(), is(SJP_REFERRAL.getDescription()));

    }
}


