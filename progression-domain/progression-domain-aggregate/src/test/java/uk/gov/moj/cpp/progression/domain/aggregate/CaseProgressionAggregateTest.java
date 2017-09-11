package uk.gov.moj.cpp.progression.domain.aggregate;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.moj.cpp.progression.aggregate.CaseProgressionAggregate;
import uk.gov.moj.cpp.progression.command.defendant.DefendantCommand;
import uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum;
import uk.gov.moj.cpp.progression.domain.event.CaseAddedToCrownCourt;
import uk.gov.moj.cpp.progression.domain.event.CaseAssignedForReviewUpdated;
import uk.gov.moj.cpp.progression.domain.event.CasePendingForSentenceHearing;
import uk.gov.moj.cpp.progression.domain.event.CaseReadyForSentenceHearing;
import uk.gov.moj.cpp.progression.domain.event.CaseToBeAssignedUpdated;
import uk.gov.moj.cpp.progression.domain.event.Defendant;
import uk.gov.moj.cpp.progression.domain.event.NewCaseDocumentReceivedEvent;
import uk.gov.moj.cpp.progression.domain.event.PreSentenceReportForDefendantsRequested;
import uk.gov.moj.cpp.progression.domain.event.SendingCommittalHearingInformationAdded;
import uk.gov.moj.cpp.progression.domain.event.SentenceHearingAdded;
import uk.gov.moj.cpp.progression.domain.event.SentenceHearingDateAdded;
import uk.gov.moj.cpp.progression.domain.event.SentenceHearingDateUpdated;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantAdditionalInformationAdded;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantPSR;
import uk.gov.moj.cpp.progression.domain.event.defendant.NoMoreInformationRequiredEvent;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Stream;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum.INCOMPLETE;

@RunWith(MockitoJUnitRunner.class)
public class CaseProgressionAggregateTest {

    @InjectMocks
    private CaseProgressionAggregate caseProgressionAggregate;

    @Test
    public void shouldReturnEmptyStringForNonExistingDefendant() {
        // given
        DefendantCommand defendantCommand = DefendantBuilder.defaultDefendant();

        // when
        final Stream<Object> objectStream = caseProgressionAggregate
                        .addAdditionalInformationForDefendant(defendantCommand);

        // then
        assertThat(objectStream.count(), is(0L));
    }

    @Test
    public void shouldAddAdditionalInformationForDefendant() {
        // given
        final UUID defendantId = randomUUID();
        // and
        createDefendant(defendantId);
        // and
        DefendantCommand defendantCommand = DefendantBuilder.defaultDefendantWith(defendantId);

        // when
        final Stream<Object> objectStream = caseProgressionAggregate
                        .addAdditionalInformationForDefendant(defendantCommand);

        // then
        assertAdditionalInformationEvent(defendantId, objectStream);
    }

    @Test
    public void shouldNotAddAdditionalInformationForDefendant() {
        // given
        final UUID defendantId = randomUUID();
        // and
        createDefendant(defendantId);
        // and
        DefendantCommand defendantCommand = DefendantBuilder.defaultDefendantWithoutAdditionalInfo(defendantId);
        // when
        final Stream<Object> objectStream = caseProgressionAggregate
                        .addAdditionalInformationForDefendant(defendantCommand);

        // then
        assertAdditionalInformationEvent(defendantId, objectStream);
    }

    @Test
    public void shouldHandleNoMoreInformationRequired() {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID caseProgressionId = randomUUID();
        createDefendant(defendantId);
        
        final NoMoreInformationRequiredEvent noMoreInformationRequiredEvent =
                        new NoMoreInformationRequiredEvent(caseId, defendantId, caseProgressionId);
        
        Object response = caseProgressionAggregate.apply(noMoreInformationRequiredEvent);
        
        assertNoMoreInformationRequiredEvent(defendantId, response);
    }
    
    @Test
    public void shouldHandleNoMoreInformationForDefendant() {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID caseProgressionId = randomUUID();
        createDefendant(defendantId);
        final Stream<Object> response = caseProgressionAggregate.noMoreInformationForDefendant(defendantId,caseId, caseProgressionId);
        final Object[] events = response.toArray();
        assertNoMoreInformationRequiredEvent(defendantId, events[events.length-1]);
    }
    
    @Test
    public void shouldHandleNoMoreInformationForDefendantWrongId() {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID caseProgressionId = randomUUID();
        createDefendant(defendantId);
        final UUID wrongDefendantId = randomUUID();
        final Stream<Object> response = caseProgressionAggregate.noMoreInformationForDefendant(wrongDefendantId,caseId, caseProgressionId);
        
        assertThat(response.count(), is(0L));
    }


    @Test
    public void shouldHandleSentenceHearingDateAdded() {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final LocalDate sentenceHearingDate = LocalDate.now();
        final UUID caseProgressionId = randomUUID();
        createDefendant(defendantId);

        final SentenceHearingDateAdded sentenceHearingDateAdded =
                new SentenceHearingDateAdded(caseProgressionId, sentenceHearingDate, caseId);

        Object response = caseProgressionAggregate.apply(sentenceHearingDateAdded);

        assertThat(((SentenceHearingDateAdded)response).getCaseId(), is(caseId));
        assertThat(((SentenceHearingDateAdded)response).getCaseProgressionId(), is(caseProgressionId));
        assertThat(((SentenceHearingDateAdded)response).getSentenceHearingDate(), is(sentenceHearingDate));
    }

    @Test
    public void shouldHandleSentenceHearingDateUpdated() {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final LocalDate sentenceHearingDate = LocalDate.now();
        final UUID caseProgressionId = randomUUID();
        createDefendant(defendantId);

        final SentenceHearingDateUpdated sentenceHearingDateUpdated =
                new SentenceHearingDateUpdated(caseProgressionId, sentenceHearingDate, caseId);

        Object response = caseProgressionAggregate.apply(sentenceHearingDateUpdated);

        assertThat(((SentenceHearingDateUpdated)response).getCaseId(), is(caseId));
        assertThat(((SentenceHearingDateUpdated)response).getCaseProgressionId(), is(caseProgressionId));
        assertThat(((SentenceHearingDateUpdated)response).getSentenceHearingDate(), is(sentenceHearingDate));
    }

    @Test
    public void shouldHandleSentenceHearingAdded() {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID sentenceHearingID = randomUUID();
        final UUID caseProgressionId = randomUUID();
        createDefendant(defendantId);

        final SentenceHearingAdded sentenceHearingAdded =
                new SentenceHearingAdded(caseProgressionId, sentenceHearingID, caseId);

        Object response = caseProgressionAggregate.apply(sentenceHearingAdded);

        assertThat(((SentenceHearingAdded)response).getCaseId(), is(caseId));
        assertThat(((SentenceHearingAdded)response).getCaseProgressionId(), is(caseProgressionId));
        assertThat(((SentenceHearingAdded)response).getSentenceHearingId(), is(sentenceHearingID));
    }

    @Test
    public void shouldAddCaseToCrownCourt() {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID courtCentreId = randomUUID();
        final UUID caseProgressionId = randomUUID();


        final CaseAddedToCrownCourt caseAddedToCrownCourt =
                new CaseAddedToCrownCourt(caseProgressionId,  caseId ,courtCentreId.toString(), Arrays.asList(new Defendant(defendantId)),INCOMPLETE);

        Object response = caseProgressionAggregate.apply(caseAddedToCrownCourt);

        assertThat(((CaseAddedToCrownCourt)response).getCaseId(), is(caseId));
        assertThat(((CaseAddedToCrownCourt)response).getCaseProgressionId(), is(caseProgressionId));
        assertThat(((CaseAddedToCrownCourt)response).getDefendants().get(0).getId(), is(defendantId));
    }

    @Test
    public void shouldAddNewCaseDocumentReceivedEvent() {
        final UUID caseId = randomUUID();
        final NewCaseDocumentReceivedEvent newCaseDocumentReceivedEvent =
                new NewCaseDocumentReceivedEvent(caseId, "fileId", "fileMimeType", "fileName");

        Object response = caseProgressionAggregate.apply(newCaseDocumentReceivedEvent);

        assertThat(((NewCaseDocumentReceivedEvent)response).getCppCaseId(), is(caseId));

    }

    @Test
    public void shouldMarkCaseReadyForSentenceHearing() {
        final ZonedDateTime dateTime = ZonedDateTime.now(ZoneOffset.UTC);
        final UUID caseProgressionId = randomUUID();
        final CaseReadyForSentenceHearing caseReadyForSentenceHearing =
                new CaseReadyForSentenceHearing(caseProgressionId, CaseStatusEnum.READY_FOR_SENTENCING_HEARING ,dateTime);

        Object response = caseProgressionAggregate.apply(caseReadyForSentenceHearing);

        assertThat(((CaseReadyForSentenceHearing)response).getCaseProgressionId(), is(caseProgressionId));
        assertThat(((CaseReadyForSentenceHearing)response).getCaseStatusUpdatedDateTime(), is(dateTime));
        assertThat(((CaseReadyForSentenceHearing)response).getStatus(), is( CaseStatusEnum.READY_FOR_SENTENCING_HEARING));
    }

    @Test
    public void shouldMarkCasePendingForSentenceHearing() {
        final ZonedDateTime dateTime = ZonedDateTime.now(ZoneOffset.UTC);
        final UUID caseProgressionId = randomUUID();
        final CasePendingForSentenceHearing casePendingForSentenceHearing =
                new CasePendingForSentenceHearing(caseProgressionId, CaseStatusEnum.PENDING_FOR_SENTENCING_HEARING ,dateTime);

        Object response = caseProgressionAggregate.apply(casePendingForSentenceHearing);

        assertThat(((CasePendingForSentenceHearing)response).getCaseProgressionId(), is(caseProgressionId));
        assertThat(((CasePendingForSentenceHearing)response).getCaseStatusUpdatedDateTime(), is(dateTime));
        assertThat(((CasePendingForSentenceHearing)response).getStatus(), is( CaseStatusEnum.PENDING_FOR_SENTENCING_HEARING));
    }

    @Test
    public void shouldApplyPreSentenceReportForDefendantsRequested() {
        final LocalDateTime localDateTime = LocalDateTime.now();
        final UUID defendantId = randomUUID();
        final UUID caseProgressionId = randomUUID();
        final PreSentenceReportForDefendantsRequested preSentenceReportForDefendantsRequested =
                new PreSentenceReportForDefendantsRequested(caseProgressionId, Arrays.asList(new DefendantPSR(defendantId,Boolean.TRUE)));

        Object response = caseProgressionAggregate.apply(preSentenceReportForDefendantsRequested);

        assertThat(((PreSentenceReportForDefendantsRequested)response).getCaseProgressionId(), is(caseProgressionId));
        assertThat(((PreSentenceReportForDefendantsRequested)response).getDefendants().get(0).getDefendantId(), is(defendantId));
    }

    @Test
    public void shouldApplySendingCommittalHearingInformationAdded() {
        final LocalDate localDate = LocalDate.now();
        final String courtCentre = "birmingham";
        final UUID caseProgressionId = randomUUID();
        final SendingCommittalHearingInformationAdded sendingCommittalHearingInformationAdded =
                new SendingCommittalHearingInformationAdded(caseProgressionId,courtCentre, localDate);

        Object response = caseProgressionAggregate.apply(sendingCommittalHearingInformationAdded);

        assertThat(((SendingCommittalHearingInformationAdded)response).getCaseProgressionId(), is(caseProgressionId));
        assertThat(((SendingCommittalHearingInformationAdded)response).getSendingCommittalDate(), is(localDate));
    }

    @Test
    public void shouldApplyCaseAssignedForReviewUpdated() {
        final UUID caseProgressionId = randomUUID();
        final CaseAssignedForReviewUpdated caseAssignedForReviewUpdated =
                new CaseAssignedForReviewUpdated(caseProgressionId, CaseStatusEnum.ASSIGNED_FOR_REVIEW);

        Object response = caseProgressionAggregate.apply(caseAssignedForReviewUpdated);

        assertThat(((CaseAssignedForReviewUpdated)response).getCaseProgressionId(), is(caseProgressionId));
        assertThat(((CaseAssignedForReviewUpdated)response).getStatus(), is( CaseStatusEnum.ASSIGNED_FOR_REVIEW));
    }


    @Test
    public void shouldApplyCaseToBeAssignedUpdated() {

        final UUID caseProgressionId = randomUUID();
        final CaseToBeAssignedUpdated caseToBeAssignedUpdated =
                new CaseToBeAssignedUpdated(caseProgressionId,CaseStatusEnum.READY_FOR_REVIEW);

        Object response = caseProgressionAggregate.apply(caseToBeAssignedUpdated);

        assertThat(((CaseToBeAssignedUpdated)response).getCaseProgressionId(), is(caseProgressionId));
        assertThat(((CaseToBeAssignedUpdated)response).getStatus(), is(CaseStatusEnum.READY_FOR_REVIEW));
    }


    private void assertNoMoreInformationRequiredEvent(UUID defendantId, Object response) {
        final NoMoreInformationRequiredEvent o =   (NoMoreInformationRequiredEvent) response;
        assertThat(o.getDefendantId(), is(defendantId));
    }
    
    private void assertAdditionalInformationEvent(UUID defendantId, Stream<Object> objectStream) {
        final DefendantAdditionalInformationAdded o =
                        (DefendantAdditionalInformationAdded) objectStream
                                        .filter(obj -> obj instanceof DefendantAdditionalInformationAdded)
                                        .findFirst().get();
        assertThat(o.getDefendantId(), is(defendantId));
    }

    
    private void createDefendant(UUID defendantId) {
        // and
        UUID caseProgressionId = randomUUID();
        // and
        UUID caseId = randomUUID();
        // and
        String courtCentreId = RandomStringUtils.random(3);
        // and
        Defendant defendant = new Defendant(defendantId);
        // and
        CaseAddedToCrownCourt caseAddedToCrownCourt = new CaseAddedToCrownCourt(caseProgressionId,
                        caseId, courtCentreId, Lists.newArrayList(defendant),INCOMPLETE);
        // and
        caseProgressionAggregate.apply(caseAddedToCrownCourt);
    }
}
