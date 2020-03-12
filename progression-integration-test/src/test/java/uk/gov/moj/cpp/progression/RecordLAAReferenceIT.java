package uk.gov.moj.cpp.progression;

import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.recordLAAReference;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getJsonObject;
import static uk.gov.moj.cpp.progression.stub.ListingStub.verifyPostListCourtHearing;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.moj.cpp.progression.helper.QueueUtil;
import uk.gov.moj.cpp.progression.stub.ReferenceDataStub;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.json.JsonObject;

import org.hamcrest.Matcher;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

public class RecordLAAReferenceIT {
    static final String PUBLIC_PROGRESSION_DEFENDANT_OFFENCES_UPDATED = "public.progression.defendant-offences-changed";
    static final String PUBLIC_PROGRESSION_DEFENDANT_LEGALAID_STATUS_UPDATED = "public.progression.defendant-legalaid-status-updated";
    private static final MessageProducer messageProducerClientPublic = publicEvents.createProducer();
    private static final MessageConsumer messageConsumerClientPublicForRecordLAAReference = publicEvents
            .createConsumer(PUBLIC_PROGRESSION_DEFENDANT_OFFENCES_UPDATED);
    private static final MessageConsumer messageConsumerClientPublicForDefendantLegalAidStatusUpdated = publicEvents
            .createConsumer(PUBLIC_PROGRESSION_DEFENDANT_LEGALAID_STATUS_UPDATED);
    private String caseId;
    private String defendantId;
    private final String offenceId = "3789ab16-0bb7-4ef1-87ef-c936bf0364f1";
    private String statusCode;

    @AfterClass
    public static void tearDown() throws JMSException {
        messageProducerClientPublic.close();
        messageConsumerClientPublicForRecordLAAReference.close();
        messageConsumerClientPublicForDefendantLegalAidStatusUpdated.close();
    }

    private static void verifyInMessagingQueueForDefendantOffenceUpdated() {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(messageConsumerClientPublicForRecordLAAReference);
        assertTrue(message.isPresent());
        assertThat(message.get().getJsonArray("updatedOffences").size(), is(1));
        assertFalse(message.get().containsKey("addedOffences"));
        assertFalse(message.get().containsKey("deletedOffences"));
    }

    private static void verifyInMessagingQueueForDefendantLegalAidStatusUpdated() {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(messageConsumerClientPublicForDefendantLegalAidStatusUpdated);
        assertTrue(message.isPresent());
    }

    @Before
    public void setUp() {
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        statusCode = "G2";
        ReferenceDataStub.stubLegalStatus("/restResource/ref-data-legal-statuses.json", statusCode);
    }

    @Test
    public void recordLAAReferenceForOffence() throws IOException {
        //Create prosecution case
        //Given
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        verifyPostListCourtHearing(caseId, defendantId);
        final String response = pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));
        final JsonObject prosecutionCasesJsonObject = getJsonObject(response);

        //Record LAA reference
        //When
        recordLAAReference(caseId, defendantId, offenceId, statusCode);

        //Then
        verifyInMessagingQueueForDefendantOffenceUpdated();
        verifyInMessagingQueueForDefendantLegalAidStatusUpdated();


        final String hearingId = prosecutionCasesJsonObject.getJsonObject("hearingsAtAGlance").getJsonArray("defendantHearings")
                .getJsonObject(0).getJsonArray("hearingIds").getString(0);
        getHearingForDefendant(hearingId, getHearingMatchers());


    }

    private Matcher[] getHearingMatchers() {
        final List<Matcher> matchers = newArrayList(withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].laaApplnReference.applicationReference", is("AB746921")),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].laaApplnReference.statusCode", is("G2")),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].laaApplnReference.statusDescription", is("Application Pending")),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].laaApplnReference.statusDate", is("1980-07-15")),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].legalAidStatus", is("Granted"))
        );
        return matchers.toArray(new Matcher[0]);
    }

}
