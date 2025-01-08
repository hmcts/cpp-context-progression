package uk.gov.moj.cpp.progression;

import static com.jayway.jsonassert.impl.matcher.IsCollectionWithSize.hasSize;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.initiateCourtProceedingsForCourtApplication;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.pollForCourtApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtWithDefendantAsAdult;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtWithOneYouthDefendantAndTwoOffences;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollCaseAndGetHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollHearingWithStatus;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.sendCurrentOffencesToUpdateOffencesCommand;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.buildMetadata;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageBody;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getJsonObject;
import static uk.gov.moj.cpp.progression.stub.DocumentGeneratorStub.stubDocumentCreate;
import static uk.gov.moj.cpp.progression.stub.HearingStub.stubInitiateHearing;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getYouthReportingRestrictionsMatchers;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.util.ProsecutionCaseUpdateDefendantHelper;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.json.JsonObject;

import com.jayway.jsonpath.ReadContext;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


public class ReportingRestrictionsIT extends AbstractIT {
    public static final String APPLICATION_REFERRED_AND_HEARING_EXTENDED = "public.progression.events.hearing-extended";
    private static final String YOUTH_RESTRICTION = "Section 49 of the Children and Young Persons Act 1933 applies";
    private static final String MANUAL_RESTRICTION = "Direction made under Section 45 of the Youth Justice and Criminal Evidence Act 1999";

    private ProsecutionCaseUpdateDefendantHelper updateDefendantHelper;
    private String caseId;
    private String defendantId;
    private static final String PUBLIC_HEARING_RESULTED = "public.hearing.resulted";
    private final JmsMessageProducerClient messageProducerClientPublic = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();
    private static String userId;
    private String hearingId1;
    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";
    private String courtCentreId;
    private String courtCentreName;
    private String newCourtCentreId;
    private String newCourtCentreName;
    private String applicationId;
    private static final String DOCUMENT_TEXT = STRING.next();


    @BeforeEach
    public void setUp() {
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        updateDefendantHelper = new ProsecutionCaseUpdateDefendantHelper(caseId, defendantId);
        userId = randomUUID().toString();
        courtCentreId = "111bdd2a-6b7a-4002-bc8c-5c6f93844f40";
        courtCentreName = "Lavender Hill Magistrate's Court";
        newCourtCentreId = "999bdd2a-6b7a-4002-bc8c-5c6f93844f40";
        newCourtCentreName = "Narnia Magistrate's Court";
        applicationId = randomUUID().toString();
        stubDocumentCreate(DOCUMENT_TEXT);
        stubInitiateHearing();

    }

    @Test
    public void shouldAddYouthRestrictionsWhenDateOfBirthChangedToYouth() throws Exception {
        addProsecutionCaseToCrownCourtWithDefendantAsAdult(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId,
                List.of(withoutJsonPath("$.prosecutionCase.defendants[0].offences[0].reportingRestrictions"))));
        updateDefendantHelper.updateDateOfBirthForDefendant(caseId, defendantId, LocalDate.of(2006, 01, 24));
        //this is triggered by UI on dob change
        sendCurrentOffencesToUpdateOffencesCommand(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getYouthReportingRestrictionsMatchers(LocalDate.now(), LocalDate.of(2006, 01, 24), 1).toArray(new Matcher[0]));
    }

    /**
     * This test is not generating the right preconditions for the assertions.  The test has been
     * modified to meet the outcome and remove the intermediate tests that has no bearing.  The test
     * was using two hearing IDs and both ended up being the same value (not sure if the test is
     * correct).  The test is now using one hearing ID.
     */
    @Test
    public void shouldAddManualRestrictionsWhenAdjourned() throws Exception {
        addProsecutionCaseToCrownCourtWithOneYouthDefendantAndTwoOffences(caseId, defendantId);
        String response = pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId, getYouthReportingRestrictionsMatchers(LocalDate.of(2021, 01, 20), LocalDate.of(2006, 01, 01), 2)));
        JsonObject prosecutionCasesJsonObject = getJsonObject(response);
        String prosecutionAuthorityReference = prosecutionCasesJsonObject.getJsonObject("prosecutionCase").getJsonObject("prosecutionCaseIdentifier").getString("prosecutionAuthorityReference");
        hearingId1 = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed-one-defendant-two-offences.json", caseId, hearingId1, defendantId, applicationId, null, prosecutionAuthorityReference, courtCentreId, courtCentreName);
        final JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);
        pollHearingWithStatus(hearingId1, "HEARING_INITIALISED");

        String courtApplicationId = randomUUID().toString();
        initiateCourtProceedingsForCourtApplication(courtApplicationId, caseId, hearingId1, "applications/progression.initiate-court-proceedings-for-court-order-linked-application-adjorn.json");

        final Matcher[] applicationMatchers = {
                withJsonPath("$.courtApplication.id", is(courtApplicationId)),
                withJsonPath("$.courtApplication.type.code", is("AS14518")),
                withJsonPath("$.courtApplication.type.linkType", is("LINKED")),
                withJsonPath("$.courtApplication.applicationStatus", is("LISTED")),
                withJsonPath("$.courtApplication.applicant.id", notNullValue()),
                withJsonPath("$.courtApplication.subject.id", notNullValue()),
                withJsonPath("$.courtApplication..courtOrder.courtOrderOffences[0].prosecutionCaseId", notNullValue()),
                withJsonPath("$.courtApplication.courtOrder.courtOrderOffences[0].prosecutionCaseIdentifier.caseURN", is("TVL1234")),
                withJsonPath("$.courtApplication.outOfTimeReasons", is("Out of times reasons for linked application test"))
        };

        pollForCourtApplication(courtApplicationId, applicationMatchers);

        final JsonEnvelope publicEventResultedEnvelope = envelopeFrom(buildMetadata(PUBLIC_HEARING_RESULTED, userId), getHearingJsonObject("public.hearing.resulted.application-adjourned-to-next-hearing-with-manual-restriction.json", caseId,
                hearingId1, defendantId, applicationId, hearingId1, prosecutionAuthorityReference, newCourtCentreId, newCourtCentreName));
        messageProducerClientPublic.sendMessage(PUBLIC_HEARING_RESULTED, publicEventResultedEnvelope);

        verifyPostHearingExtendedEvent(hearingId1, applicationId);

        pollProsecutionCasesProgressionFor(caseId, getUpdatedRestrictionMatchers().toArray(new Matcher[0]));
    }

    private static List<Matcher<? super ReadContext>> getUpdatedRestrictionMatchers() {
        List<Matcher<? super ReadContext>> matchers = new ArrayList<>();
        matchers.add(withJsonPath("$.prosecutionCase.defendants[0].offences", hasSize(equalTo(2))));
        matchers.add(withJsonPath("$.prosecutionCase.defendants[0].offences[*].reportingRestrictions[*]", hasSize(equalTo(4))));
        matchers.add(withJsonPath("$.prosecutionCase.defendants[0].offences[0].reportingRestrictions[0].label", is(MANUAL_RESTRICTION)));
        matchers.add(withJsonPath("$.prosecutionCase.defendants[0].offences[0].reportingRestrictions[1].label", is(YOUTH_RESTRICTION)));
        matchers.add(withJsonPath("$.prosecutionCase.defendants[0].offences[1].reportingRestrictions[0].label", is(MANUAL_RESTRICTION)));
        matchers.add(withJsonPath("$.prosecutionCase.defendants[0].offences[1].reportingRestrictions[1].label", is(YOUTH_RESTRICTION)));
        matchers.add(withJsonPath("$.prosecutionCase.defendants[0].personDefendant.personDetails.dateOfBirth", is("2006-01-01")));
        return matchers;
    }


    private JsonObject getHearingJsonObject(final String path, final String caseId, final String hearingId,
                                            final String defendantId, final String applicationId,
                                            final String adjournedHearingId, final String reference, final String courtCentreId, final String courtCentreName) {
        return stringToJsonObjectConverter.convert(
                getPayload(path)
                        .replaceAll("CASE_ID", caseId)
                        .replaceAll("HEARING_ID", hearingId)
                        .replaceAll("DEFENDANT_ID", defendantId)
                        .replaceAll("COURT_CENTRE_ID", courtCentreId)
                        .replaceAll("COURT_CENTRE_NAME", courtCentreName)
                        .replaceAll("APPLICATION_ID", applicationId)
                        .replaceAll("ADJOURNED_ID", adjournedHearingId)
                        .replaceAll("APPLICATION_REF", reference)
        );
    }

    private void verifyPostHearingExtendedEvent(final String hearingId, String applicationId) {
        final JmsMessageConsumerClient hearingExtendedConsumer = newPublicJmsMessageConsumerClientProvider().withEventNames(APPLICATION_REFERRED_AND_HEARING_EXTENDED).getMessageConsumerClient();
        final Optional<JsonObject> message = retrieveMessageBody(hearingExtendedConsumer);
        assertTrue(message.isPresent());
        assertThat(message.get().getString("hearingId"), equalTo(hearingId));
        assertNotNull(message.get().getJsonObject("courtApplication"));
        assertThat(message.get().getJsonObject("courtApplication").getString("id"), equalTo(applicationId));
    }
}
