package uk.gov.moj.cpp.progression;

import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static org.junit.Assert.assertTrue;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.initiateCourtProceedings;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.initiateCourtProceedingsWithoutCourtDocument;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageAsJsonObject;
import static uk.gov.moj.cpp.progression.stub.ListingStub.verifyPostListCourtHearing;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.justice.services.common.converter.ZonedDateTimes;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Optional;

import javax.jms.MessageConsumer;
import javax.json.JsonObject;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;


public class InitiateCourtProceedingsIT extends AbstractIT {

    protected MessageConsumer publicEventConsumer = publicEvents
            .createConsumer("public.progression.prosecution-case-created");
    private String caseId;
    private String materialIdActive;
    private String materialIdDeleted;
    private String defendantId;
    private String referralReasonId;
    private String listedStartDateTime;
    private String earliestStartDateTime;
    private String defendantDOB;

    @Before
    public void setUp() {
        caseId = randomUUID().toString();
        materialIdActive = randomUUID().toString();
        materialIdDeleted = randomUUID().toString();
        defendantId = randomUUID().toString();
        referralReasonId = randomUUID().toString();
        listedStartDateTime = ZonedDateTimes.fromString("2019-06-30T18:32:04.238Z").toString();
        earliestStartDateTime = ZonedDateTimes.fromString("2019-05-30T18:32:04.238Z").toString();
        defendantDOB = LocalDate.now().minusYears(15).toString();
    }

    @Test
    public void shouldInitiateCourtProceedings() throws IOException {
        //given
        initiateCourtProceedings(caseId, defendantId, materialIdActive, materialIdDeleted, referralReasonId, listedStartDateTime, earliestStartDateTime, defendantDOB);
        //when
        verifyInMessagingQueueForProsecutionCaseCreated();

        final Matcher[] prosecutionCaseMatchers = getProsecutionCaseMatchers(caseId, defendantId, emptyList());

        pollProsecutionCasesProgressionFor(caseId, prosecutionCaseMatchers);
    }

    @Test
    public void shouldInitiateCourtProceedingsWithDefendantIsYouth() throws IOException {
        //given
        initiateCourtProceedings(caseId, defendantId, materialIdActive, materialIdDeleted, referralReasonId, listedStartDateTime, earliestStartDateTime, defendantDOB);
        verifyPostListCourtHearing(caseId, defendantId, true);
    }

    @Test
    public void shouldInitiateCourtProceedingsWithDefendantIsNotYouth() throws IOException {
        defendantDOB = LocalDate.now().minusYears(25).toString();
        //given
        initiateCourtProceedings(caseId, defendantId, materialIdActive, materialIdDeleted, referralReasonId, listedStartDateTime, earliestStartDateTime, defendantDOB);
        verifyPostListCourtHearing(caseId, defendantId, false);
    }

    @Test
    public void shouldInitiateCourtProceedingsNoCourtDocuments() throws IOException {
        //given
        initiateCourtProceedingsWithoutCourtDocument(caseId, defendantId, listedStartDateTime, earliestStartDateTime, defendantDOB);
        //when
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));
    }

    private void verifyInMessagingQueueForProsecutionCaseCreated() {
        final Optional<JsonObject> message = retrieveMessageAsJsonObject(publicEventConsumer);
        assertTrue(message.isPresent());
    }


}

