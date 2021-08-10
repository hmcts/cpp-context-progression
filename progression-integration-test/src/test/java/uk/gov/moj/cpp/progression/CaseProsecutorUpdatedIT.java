package uk.gov.moj.cpp.progression;

import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertTrue;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.privateEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageAsJsonObject;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.sendMessage;
import static uk.gov.moj.cpp.progression.stub.DocumentGeneratorStub.stubDocumentCreate;
import static uk.gov.moj.cpp.progression.stub.HearingStub.stubInitiateHearing;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.platform.test.feature.toggle.FeatureStubber;
import uk.gov.moj.cpp.progression.util.CaseProsecutorUpdateHelper;

import java.util.List;
import java.util.Optional;

import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.json.JsonObject;

import com.google.common.collect.ImmutableMap;
import com.jayway.jsonpath.ReadContext;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;

public class CaseProsecutorUpdatedIT extends AbstractIT {
    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";
    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();
    private static final MessageProducer messageProducerClientPublic = publicEvents.createProducer();
    private static final String DOCUMENT_TEXT = STRING.next();
    private String caseId;
    private String defendantId;

    private CaseProsecutorUpdateHelper caseProsecutorUpdateHelper;

    @Before
    public void setUp() {
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        caseProsecutorUpdateHelper = new CaseProsecutorUpdateHelper(caseId);

        stubDocumentCreate(DOCUMENT_TEXT);
        stubInitiateHearing();
    }

    @Test
    public void shouldUpdateHearingResultedCaseUpdated() throws Exception {
        final ImmutableMap<String, Boolean> features = ImmutableMap.of("amendReshare", false);
        FeatureStubber.stubFeaturesFor(PROGRESSION_CONTEXT, features);

        final String hearingId;

        try (final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
                .createConsumer("progression.event.prosecutionCase-defendant-listing-status-changed")) {

            addProsecutionCaseToCrownCourt(caseId, defendantId);
            pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));
            hearingId = doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        }

        final Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withName(PUBLIC_LISTING_HEARING_CONFIRMED)
                .withUserId(randomUUID().toString())
                .build();
        final String updatedCourtCentreId = randomUUID().toString();
        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, updatedCourtCentreId );
        sendMessage(messageProducerClientPublic, PUBLIC_LISTING_HEARING_CONFIRMED, hearingConfirmedJson, metadata);

        verifyInMessagingQueueForCasesReferredToCourts();

        caseProsecutorUpdateHelper.updateCaseProsecutor();

        caseProsecutorUpdateHelper.verifyInActiveMQ();

        caseProsecutorUpdateHelper.verifyInMessagingQueueForProsecutorUpdated(1);
        final List<Matcher<? super ReadContext>> customMatchers = newArrayList(
                withJsonPath("$.prosecutionCase.isCpsOrgVerifyError", is(false)),
                //verify prosecutionCaseIdentifier is not updated
                withJsonPath("$.prosecutionCase.prosecutionCaseIdentifier.prosecutionAuthorityName", is("Transport for London")),
                withJsonPath("$.prosecutionCase.prosecutionCaseIdentifier.prosecutionAuthorityOUCode", is("GB10056")),
                //verify prosecutor is updated
                withJsonPath("$.prosecutionCase.prosecutor.prosecutorCode", is("TFL-CM")),
                withJsonPath("$.prosecutionCase.prosecutor.prosecutorName", is("BL001"))
        );
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId, customMatchers));
    }

    @Test
    public void shouldUpdateHearingResultedCaseUpdatedWhenHearingsNotExist() throws Exception {
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));
        caseProsecutorUpdateHelper.updateCaseProsecutor();

        caseProsecutorUpdateHelper.verifyInActiveMQ();

        caseProsecutorUpdateHelper.verifyInMessagingQueueForProsecutorUpdated(0);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));
    }

    private String doVerifyProsecutionCaseDefendantListingStatusChanged(final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged) {
        final Optional<JsonObject> message = retrieveMessageAsJsonObject(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        final JsonObject prosecutionCaseDefendantListingStatusChanged = message.get();
        return prosecutionCaseDefendantListingStatusChanged.getJsonObject("hearing").getString("id");
    }

    private JsonObject getHearingJsonObject(final String path, final String caseId, final String hearingId,
                                            final String defendantId, final String courtCentreId) {
        final String strPayload = getPayload(path)
                .replaceAll("CASE_ID", caseId)
                .replaceAll("HEARING_ID", hearingId)
                .replaceAll("DEFENDANT_ID", defendantId)
                .replaceAll("COURT_CENTRE_ID", courtCentreId);
        return stringToJsonObjectConverter.convert(strPayload);
    }

    public static void verifyInMessagingQueueForCasesReferredToCourts() {
        final String referredToCourt = "public.progression.prosecution-cases-referred-to-court";
        final MessageConsumer messageConsumerClientPublicForReferToCourtOnHearingInitiated = publicEvents.createConsumer(referredToCourt);

        final Optional<JsonObject> message = retrieveMessageAsJsonObject(messageConsumerClientPublicForReferToCourtOnHearingInitiated);
        assertTrue(message.isPresent());
    }
}

