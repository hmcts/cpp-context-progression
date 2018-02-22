package uk.gov.moj.cpp.progression.domain.aggregate;

import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.mockito.internal.util.reflection.Whitebox;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.aggregate.CaseProgressionAggregate;
import uk.gov.moj.cpp.progression.command.defendant.AddDefendant;
import uk.gov.moj.cpp.progression.command.defendant.DefendantCommand;
import uk.gov.moj.cpp.progression.domain.aggregate.utils.DefendantBuilder;
import uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum;
import uk.gov.moj.cpp.progression.domain.event.CaseAddedToCrownCourt;
import uk.gov.moj.cpp.progression.domain.event.CasePendingForSentenceHearing;
import uk.gov.moj.cpp.progression.domain.event.CaseReadyForSentenceHearing;
import uk.gov.moj.cpp.progression.domain.event.CaseToBeAssignedUpdated;
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
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantAdditionalInformationAdded;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantPSR;
import uk.gov.moj.cpp.progression.domain.event.defendant.NoMoreInformationRequiredEvent;
import uk.gov.moj.cpp.progression.domain.event.defendant.Offence;
import uk.gov.moj.cpp.progression.domain.event.defendant.OffenceForDefendant;
import uk.gov.moj.cpp.progression.domain.event.defendant.OffencesForDefendantUpdated;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonObject;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CaseProgressionAggregateTest {

    private static final String PROGRESSION_ID = randomUUID().toString();
    private static final String CASE_ID = randomUUID().toString();
    private static final String DEFENDANT_ID = randomUUID().toString();
    private static final String COURT_CENTRE_NAME = "Warwick Justice Centre";
    private static final String COURT_CENTRE_ID = "1234";
    private static final String HEARING_TYPE = "PTP";
    private static final String SENDING_COMMITAL_DATE = "01-01-1990";
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


    @Mock
    JsonEnvelope envelope;
    @Mock
    JsonObject jsonObj;
    @InjectMocks
    private CaseProgressionAggregate caseProgressionAggregate;
    private static final AddDefendant addDefendant = DefendantBuilder.defaultAddDefendant();

    @Before
    public void setUp(){
        this.caseProgressionAggregate =new CaseProgressionAggregate();
    }
    @Test
    public void shouldReturnEmptyStringForNonExistingDefendant() {
        // given
        final DefendantCommand defendantCommand = DefendantBuilder.defaultDefendant();

        // when
        final Stream<Object> objectStream = this.caseProgressionAggregate
                        .addAdditionalInformationForDefendant(defendantCommand);

        // then
        assertThat(objectStream.count(), is(0L));
    }



    @Test
    public void shouldHandleNoMoreInformationRequired() {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        createDefendant(defendantId);
        
        final NoMoreInformationRequiredEvent noMoreInformationRequiredEvent =
                        new NoMoreInformationRequiredEvent(caseId, defendantId);
        
        final Object response = this.caseProgressionAggregate.apply(noMoreInformationRequiredEvent);
        
        assertNoMoreInformationRequiredEvent(defendantId, response);
    }
    
    @Test
    public void shouldHandleNoMoreInformationForDefendant() {
        createDefendantWithOffence(addDefendant);
        final Stream<Object> response = this.caseProgressionAggregate.noMoreInformationForDefendant(addDefendant.getDefendantId(),addDefendant.getCaseId());
        final Object[] events = response.toArray();
        assertNoMoreInformationRequiredEvent(addDefendant.getDefendantId(), events[events.length-1]);
    }
    
    @Test
    public void shouldHandleNoMoreInformationForDefendantWrongId() {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        createDefendant(defendantId);
        final UUID wrongDefendantId = randomUUID();
        final Stream<Object> response = this.caseProgressionAggregate.noMoreInformationForDefendant(wrongDefendantId,caseId);
        
        assertThat(response.count(), is(0L));
    }


    @Test
    public void shouldHandleSentenceHearingDateAdded() {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final LocalDate sentenceHearingDate = LocalDate.now();
        createDefendant(defendantId);

        final SentenceHearingDateAdded sentenceHearingDateAdded =
                new SentenceHearingDateAdded(sentenceHearingDate, caseId);

        final Object response = this.caseProgressionAggregate.apply(sentenceHearingDateAdded);

        assertThat(((SentenceHearingDateAdded)response).getCaseId(), is(caseId));
        assertThat(((SentenceHearingDateAdded)response).getSentenceHearingDate(), is(sentenceHearingDate));
    }

    @Test
    public void shouldAddCaseToCrownCourt() {
        final UUID caseId = randomUUID();
        final UUID courtCentreId = randomUUID();


        final CaseAddedToCrownCourt caseAddedToCrownCourt =
                new CaseAddedToCrownCourt(caseId ,courtCentreId.toString());

        final Object response = this.caseProgressionAggregate.apply(caseAddedToCrownCourt);

        assertThat(((CaseAddedToCrownCourt)response).getCaseId(), is(caseId));
        assertThat(((CaseAddedToCrownCourt)response).getCourtCentreId(), is(courtCentreId.toString()));
    }

    @Test
    public void shouldAddNewCaseDocumentReceivedEvent() {
        final UUID caseId = randomUUID();
        final NewCaseDocumentReceivedEvent newCaseDocumentReceivedEvent =
                new NewCaseDocumentReceivedEvent(caseId, "fileId", "fileMimeType", "fileName");

        final Object response = this.caseProgressionAggregate.apply(newCaseDocumentReceivedEvent);

        assertThat(((NewCaseDocumentReceivedEvent)response).getCppCaseId(), is(caseId));

    }

    @Test
    public void shouldMarkCaseReadyForSentenceHearing() {
        final ZonedDateTime dateTime = ZonedDateTime.now(ZoneOffset.UTC);
        final UUID caseId = randomUUID();
        final CaseReadyForSentenceHearing caseReadyForSentenceHearing =
                new CaseReadyForSentenceHearing(caseId, CaseStatusEnum.READY_FOR_SENTENCING_HEARING ,dateTime);

        final Object response = this.caseProgressionAggregate.apply(caseReadyForSentenceHearing);

        assertThat(((CaseReadyForSentenceHearing)response).getCaseId(), is(caseId));
        assertThat(((CaseReadyForSentenceHearing)response).getCaseStatusUpdatedDateTime(), is(dateTime));
        assertThat(((CaseReadyForSentenceHearing)response).getStatus(), is( CaseStatusEnum.READY_FOR_SENTENCING_HEARING));
    }

    @Test
    public void shouldMarkCasePendingForSentenceHearing() {
        final ZonedDateTime dateTime = ZonedDateTime.now(ZoneOffset.UTC);
        final UUID caseId = randomUUID();
        final CasePendingForSentenceHearing casePendingForSentenceHearing =
                new CasePendingForSentenceHearing(caseId, CaseStatusEnum.PENDING_FOR_SENTENCING_HEARING ,dateTime);

        final Object response = this.caseProgressionAggregate.apply(casePendingForSentenceHearing);

        assertThat(((CasePendingForSentenceHearing)response).getCaseId(), is(caseId));
        assertThat(((CasePendingForSentenceHearing)response).getCaseStatusUpdatedDateTime(), is(dateTime));
        assertThat(((CasePendingForSentenceHearing)response).getStatus(), is( CaseStatusEnum.PENDING_FOR_SENTENCING_HEARING));
    }

    @Test
    public void shouldApplyPreSentenceReportForDefendantsRequested() {
        final UUID defendantId = randomUUID();
        final UUID caseId = randomUUID();
        final PreSentenceReportForDefendantsRequested preSentenceReportForDefendantsRequested =
                new PreSentenceReportForDefendantsRequested(caseId, Arrays.asList(new DefendantPSR(defendantId,Boolean.TRUE)));

        final Object response = this.caseProgressionAggregate.apply(preSentenceReportForDefendantsRequested);

        assertThat(((PreSentenceReportForDefendantsRequested)response).getCaseId(), is(caseId));
        assertThat(((PreSentenceReportForDefendantsRequested)response).getDefendants().get(0).getDefendantId(), is(defendantId));
    }

    @Test
    public void shouldApplySendingCommittalHearingInformationAdded() {
        final LocalDate localDate = LocalDate.now();
        final String courtCentre = "birmingham";
        final UUID caseId = randomUUID();
        final SendingCommittalHearingInformationAdded sendingCommittalHearingInformationAdded =
                new SendingCommittalHearingInformationAdded(caseId,courtCentre, localDate);

        final Object response = this.caseProgressionAggregate.apply(sendingCommittalHearingInformationAdded);

        assertThat(((SendingCommittalHearingInformationAdded)response).getCaseId(), is(caseId));
        assertThat(((SendingCommittalHearingInformationAdded)response).getSendingCommittalDate(), is(localDate));
    }



    @Test
    public void shouldApplyCaseToBeAssignedUpdated() {

        final UUID caseId = randomUUID();
        final CaseToBeAssignedUpdated caseToBeAssignedUpdated =
                new CaseToBeAssignedUpdated(caseId,CaseStatusEnum.READY_FOR_REVIEW);

        final Object response = this.caseProgressionAggregate.apply(caseToBeAssignedUpdated);

        assertThat(((CaseToBeAssignedUpdated)response).getCaseId(), is(caseId));
        assertThat(((CaseToBeAssignedUpdated)response).getStatus(), is(CaseStatusEnum.READY_FOR_REVIEW));
    }

    @Test
    public void shouldApplyCompleteSendingSheet() {
        final List<Object> objects = applySendingSheet(a->{});
        assertThat(objects.size(), is(1));
        final Object obj = objects.get(0);
        assertThat(obj, instanceOf(SendingSheetCompleted.class));
        assertSendingSheetCompletedValues((SendingSheetCompleted) obj);
    }

    @Test
    public void shouldInvalidateSendingSheetWrongCourtCentre() {
        final List<Object> objects = applySendingSheet(a->{
            Whitebox.setInternalState(this.caseProgressionAggregate, "courtCentreId", null);
        });
        assertThat(objects.size(), is(1));
        final Object obj = objects.get(0);
        assertThat(obj, instanceOf(SendingSheetInvalidated.class));
        SendingSheetInvalidated sendingSheetInvalidated = (SendingSheetInvalidated) obj;
        Assert.assertTrue(sendingSheetInvalidated.getDescription().contains(CC_COURT_CENTRE_ID));
    }

    @Test
    public void shouldInvalidateSendingSheetNoDefendants() {
        final List<Object> objects = applySendingSheet(a->{
            Whitebox.setInternalState(this.caseProgressionAggregate, "defendants", new HashSet<>());
        });
        assertThat(objects.size(), is(1));
        final Object obj = objects.get(0);
        assertThat(obj, instanceOf(SendingSheetInvalidated.class));
        SendingSheetInvalidated sendingSheetInvalidated = (SendingSheetInvalidated) obj;
        Assert.assertTrue(sendingSheetInvalidated.getCaseId().equals(UUID.fromString(CASE_ID)));
    }

    @Test
    public void shouldInvalidateSendingSheetWrongDefendants() {
        final List<Object> objects = applySendingSheet(a->{
            Defendant defendant = new Defendant();
            defendant.setId(UUID.randomUUID());
            Whitebox.setInternalState(this.caseProgressionAggregate, "defendants", new HashSet<>(Arrays.asList(defendant)));
        });
        assertThat(objects.size(), is(1));
        final Object obj = objects.get(0);
        assertThat(obj, instanceOf(SendingSheetInvalidated.class));
        SendingSheetInvalidated sendingSheetInvalidated = (SendingSheetInvalidated) obj;
        Assert.assertTrue(sendingSheetInvalidated.getCaseId().equals(UUID.fromString(CASE_ID)));
    }

    @Test
    public void shouldInvalidateSendingSheetWrongOffences() {
        final List<Object> objects = applySendingSheet(a->{
            Map<UUID, Set<UUID>> offenceIdsByDefendantId = new HashMap<>();
            Whitebox.setInternalState(this.caseProgressionAggregate, "offenceIdsByDefendantId", offenceIdsByDefendantId);
        });
        assertThat(objects.size(), is(1));
        final Object obj = objects.get(0);
        assertThat(obj, instanceOf(SendingSheetInvalidated.class));
        SendingSheetInvalidated sendingSheetInvalidated = (SendingSheetInvalidated) obj;
        Assert.assertTrue(sendingSheetInvalidated.getCaseId().equals(UUID.fromString(CASE_ID)));
        Assert.assertTrue(sendingSheetInvalidated.getDescription().contains(OFFENCE_ID));

    }


    private List<Object> applySendingSheet(Consumer<CaseProgressionAggregate> adjustInternals) {
        createCompleteSendingSheetEnvelope();
        final SendingSheetCompleted sendingSheetCompleted = new SendingSheetCompleted();
        final Hearing hearing = new Hearing();
        hearing.setCaseId(UUID.fromString("4daefec6-5f77-4109-82d9-1e60544a6c05"));
        sendingSheetCompleted.setHearing(hearing);
        Set<Defendant> defendants = new HashSet<>();
        Defendant defendant = new Defendant();
        defendants.add(defendant);
        defendant.setId(UUID.fromString(DEFENDANT_ID));
        Map<UUID, Set<UUID>> offenceIdsByDefendantId = new HashMap<>();
        offenceIdsByDefendantId.put(UUID.fromString(DEFENDANT_ID), new HashSet(Arrays.asList(UUID.fromString(OFFENCE_ID))));
        //green path internals
        Whitebox.setInternalState(this.caseProgressionAggregate, "courtCentreId", CC_COURT_CENTRE_ID);
        Whitebox.setInternalState(this.caseProgressionAggregate, "defendants", defendants);
        Whitebox.setInternalState(this.caseProgressionAggregate, "offenceIdsByDefendantId", offenceIdsByDefendantId);
        adjustInternals.accept(this.caseProgressionAggregate);

        final Stream<Object> stream = this.caseProgressionAggregate.completeSendingSheet(this.envelope);
        return stream.collect(Collectors.toList());

    }

    @Test
    public void shouldApplyCompleteSendingSheetPreviouslyCompleted() {
        final List<Object> objects = applySendingSheet(a->{
            Set<UUID> caseIdsWithCompletedSendingSheet = new HashSet<>(Arrays.asList(UUID.fromString(CASE_ID)));
            Whitebox.setInternalState(this.caseProgressionAggregate, "caseIdsWithCompletedSendingSheet", caseIdsWithCompletedSendingSheet);
        });
        assertThat(objects.size(), is(1));
        final Object obj = objects.get(0);
        assertThat(obj, instanceOf(SendingSheetPreviouslyCompleted.class));
        assertThat(CASE_ID, equalTo(((SendingSheetPreviouslyCompleted) obj).getCaseId().toString()));
    }


    /*  @Test
    public void shouldApplyCompleteSendingSheetPreviouslyCompleted() {
        createCompleteSendingSheetEnvelope();
        final SendingSheetCompleted sendingSheetCompleted = new SendingSheetCompleted();
        final Hearing hearing = new Hearing();
        hearing.setCaseId(UUID.fromString(CASE_ID));
        sendingSheetCompleted.setHearing(hearing);

        this.caseProgressionAggregate.completeSendingSheet(this.envelope);
        // send the same case again for completion
        final Stream<Object> stream = this.caseProgressionAggregate.completeSendingSheet(this.envelope);
        final List<Object> objects = stream.collect(Collectors.toList());
        assertThat(objects.size(), is(1));
        final Object obj = objects.get(0);
        assertThat(obj, instanceOf(SendingSheetPreviouslyCompleted.class));
        assertThat(CASE_ID, equalTo(((SendingSheetPreviouslyCompleted) obj).getCaseId().toString()));
    }
*/
    private void assertNoMoreInformationRequiredEvent(final UUID defendantId, final Object response) {
        final NoMoreInformationRequiredEvent o =   (NoMoreInformationRequiredEvent) response;
        assertThat(o.getDefendantId(), is(defendantId));
    }
    
    private void assertAdditionalInformationEvent(final UUID defendantId, final Stream<Object> objectStream) {
        final DefendantAdditionalInformationAdded o =
                        (DefendantAdditionalInformationAdded) objectStream
                                        .filter(obj -> obj instanceof DefendantAdditionalInformationAdded)
                                        .findFirst().get();
        assertThat(o.getDefendantId(), is(defendantId));
    }

    
    private void addedCaseToCrownCourt(final UUID defendantId) {

        final UUID caseId = randomUUID();
        final String courtCentreId = RandomStringUtils.random(3);
        final CaseAddedToCrownCourt caseAddedToCrownCourt = new CaseAddedToCrownCourt(
                caseId, courtCentreId);
        this.caseProgressionAggregate.apply(caseAddedToCrownCourt);
    }

    private void createDefendant(final UUID defendantId) {
        final UUID caseId = randomUUID();
        final Defendant defendant = new Defendant(defendantId);
        final Offence offence=new Offence(randomUUID(),
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
        final DefendantAdded defendantAdded =new DefendantAdded(caseId,defendantId,randomUUID(),"",Arrays.asList(offence),"CaseUrn");
        this.caseProgressionAggregate.apply(defendantAdded);
    }

    private void createDefendantWithOffence(final AddDefendant addDefendant ){
        //Adding defendant
        this.caseProgressionAggregate =new CaseProgressionAggregate();
        this.caseProgressionAggregate.addDefendant(addDefendant);

        //Adding Offences for defendant
        final List<OffenceForDefendant> offenceForDefendants = Arrays.asList(new OffenceForDefendant(randomUUID(), "offenceCode"
                ,  "section",
                " wording", LocalDate.now(), LocalDate.now(), 0, 0, null, null, null));


        final OffencesForDefendantUpdated offencesForDefendantUpdated =
                new OffencesForDefendantUpdated(addDefendant.getCaseId(), addDefendant.getDefendantId(), offenceForDefendants);

        this.caseProgressionAggregate.updateOffencesForDefendant(offencesForDefendantUpdated ).collect(toList());
    }

    private Map<UUID, Set<UUID>>  defendantId2OffenceIds = new HashMap<>();


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

        UUID defendantId = randomUUID();
        when(this.jsonObj.getJsonArray(Mockito.eq("defendants"))).thenReturn(Json.createArrayBuilder()
                .add(Json.createObjectBuilder().add("id", defendantId.toString()).build())
                .build());


        when(this.jsonObj.getJsonObject("hearing")).thenReturn(Json.createObjectBuilder()
                .add("courtCentreName", COURT_CENTRE_NAME)
                .add("courtCentreId", COURT_CENTRE_ID).add("type", HEARING_TYPE)
                .add("sendingCommittalDate", SENDING_COMMITAL_DATE).add("caseId", CASE_ID)
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
                                .add("postCode", DEFENDANT_POSTCODE).build())
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
        assertThat(SENDING_COMMITAL_DATE, equalTo(hearing.getSendingCommittalDate()));
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

}
