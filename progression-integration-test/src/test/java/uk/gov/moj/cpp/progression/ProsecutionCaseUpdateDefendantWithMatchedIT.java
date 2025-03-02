package uk.gov.moj.cpp.progression;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.util.ProsecutionCaseUpdateDefendantWithMatchedHelper;

import javax.json.JsonObject;

import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.not;
import org.hamcrest.Matcher;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.progression.helper.CaseHearingsQueryHelper.pollForHearing;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.matchDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollCaseAndGetHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.buildMetadata;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.sendPublicEvent;
import static uk.gov.moj.cpp.progression.stub.ListingStub.stubListingSearchHearingsQuery;
import static uk.gov.moj.cpp.progression.stub.ProbationCaseworkerStub.verifyProbationHearingCommandInvoked;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ProsecutionCaseUpdateDefendantWithMatchedHelper.getUpdatedDefendantMatchers;
import static uk.gov.moj.cpp.progression.util.ProsecutionCaseUpdateDefendantWithMatchedHelper.initiateCourtProceedingsForMatchedDefendants;

@SuppressWarnings("squid:S1607")
public class ProsecutionCaseUpdateDefendantWithMatchedIT extends AbstractIT {

    private final JmsMessageProducerClient messageProducerClientPublic = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();
    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";

    private String prosecutionCaseId_1;
    private String defendantId_1;
    private String masterDefendantId_1;
    private String prosecutionCaseId_2;
    private String defendantId_2;
    private String hearingId;
    private String courtCentreId;

    @BeforeEach
    public void setUp() {
        prosecutionCaseId_1 = randomUUID().toString();
        defendantId_1 = randomUUID().toString();
        masterDefendantId_1 = randomUUID().toString();
        prosecutionCaseId_2 = randomUUID().toString();
        defendantId_2 = randomUUID().toString();
        hearingId = randomUUID().toString();
        courtCentreId = randomUUID().toString();
    }

    @Test
    public void shouldUpdateProsecutionCaseDefendantWithMatched() throws Exception {

        // initiation of first case
        initiateCourtProceedingsForMatchedDefendants(prosecutionCaseId_1, defendantId_1, masterDefendantId_1);
        hearingId = pollCaseAndGetHearingForDefendant(prosecutionCaseId_1, defendantId_1);
        stubListingSearchHearingsQuery("stub-data/listing.search.hearings.json", hearingId);
        sendPublicEvent(
                PUBLIC_LISTING_HEARING_CONFIRMED, getHearingJsonObject("public.listing.hearing-confirmed.json",
                        prosecutionCaseId_1, hearingId, defendantId_1, courtCentreId), JsonEnvelope.metadataBuilder()
                        .withId(randomUUID())
                        .withName(PUBLIC_LISTING_HEARING_CONFIRMED)
                        .withUserId(randomUUID().toString())
                        .build());

        // initiation of second case
        initiateCourtProceedingsForMatchedDefendants(prosecutionCaseId_2, defendantId_2, defendantId_2);
        String hearingId2 = pollCaseAndGetHearingForDefendant(prosecutionCaseId_2, defendantId_2);
        stubListingSearchHearingsQuery("stub-data/listing.search.hearings.json", hearingId2);
        sendPublicEvent(
                PUBLIC_LISTING_HEARING_CONFIRMED, getHearingJsonObject("public.listing.hearing-confirmed.json",
                        prosecutionCaseId_2, hearingId2, defendantId_2, courtCentreId), JsonEnvelope.metadataBuilder()
                        .withId(randomUUID())
                        .withName(PUBLIC_LISTING_HEARING_CONFIRMED)
                        .withUserId(randomUUID().toString())
                        .build());


        // match defendant2 associated to case 2
        matchDefendant(prosecutionCaseId_2, defendantId_2, prosecutionCaseId_1, defendantId_1, masterDefendantId_1);
        pollProsecutionCasesProgressionFor(prosecutionCaseId_2,
                withJsonPath("$.prosecutionCase.defendants[0].masterDefendantId", is(masterDefendantId_1))
        );

        // confirm Hearing with 2 Prosecution Case
        final JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, randomUUID()), getHearingJsonObject("public.listing.hearing-confirmed-multiple-case.json",
                hearingId, prosecutionCaseId_1, defendantId_1, prosecutionCaseId_2, defendantId_2));
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);


        Matcher[] hearingMatchers = {
                withJsonPath("$", notNullValue()),
                withJsonPath("$.hearing.id", is(hearingId)),
                withJsonPath("$.hearing.prosecutionCases.[*].id", hasItems(prosecutionCaseId_1, prosecutionCaseId_2)),
                withJsonPath("$.hearing.prosecutionCases.[*].defendants.[*].id", hasItems(defendantId_1, defendantId_2))
        };

        pollForHearing(hearingId, hearingMatchers);

        // Update Multiple Defendant on Same Hearing
        ProsecutionCaseUpdateDefendantWithMatchedHelper prosecutionCaseUpdateDefendantWithMatchedHelper = new ProsecutionCaseUpdateDefendantWithMatchedHelper();
        prosecutionCaseUpdateDefendantWithMatchedHelper.updateDefendant(defendantId_1, prosecutionCaseId_1, hearingId);

        // Verify Original Defendant
        Matcher[] prosecutionCaseMatchers = getUpdatedDefendantMatchers("$.prosecutionCase", prosecutionCaseId_1, defendantId_1, emptyList());
        pollProsecutionCasesProgressionFor(prosecutionCaseId_1, prosecutionCaseMatchers);

        // Verify Matched Defendant
        prosecutionCaseMatchers = getUpdatedDefendantMatchers("$.prosecutionCase", prosecutionCaseId_2, defendantId_2, emptyList());
        pollProsecutionCasesProgressionFor(prosecutionCaseId_2, prosecutionCaseMatchers);

        Matcher[] hearingDefendantMatchers = getUpdatedDefendantMatchers("$.hearing.prosecutionCases[0]", prosecutionCaseId_1, defendantId_1, emptyList());
        pollForHearing(hearingId, hearingDefendantMatchers);

        verifyProbationHearingCommandInvoked(newArrayList(hearingId));
    }

    @Test
    public void shouldAddOffenceToProsecutionCaseDefendantWithMatched() throws Exception {

        // initiation of first case
        initiateCourtProceedingsForMatchedDefendants(prosecutionCaseId_1, defendantId_1, masterDefendantId_1);
        hearingId = pollCaseAndGetHearingForDefendant(prosecutionCaseId_1, defendantId_1);

        // initiation of second case
        initiateCourtProceedingsForMatchedDefendants(prosecutionCaseId_2, defendantId_2, defendantId_2);
        pollProsecutionCasesProgressionFor(prosecutionCaseId_2,
                withJsonPath("$.prosecutionCase.defendants[0].masterDefendantId", not(masterDefendantId_1))
        );

        // match defendant2 associated to case 2
        matchDefendant(prosecutionCaseId_2, defendantId_2, prosecutionCaseId_1, defendantId_1, masterDefendantId_1);
        pollProsecutionCasesProgressionFor(prosecutionCaseId_2,
                withJsonPath("$.prosecutionCase.defendants[0].masterDefendantId", is(masterDefendantId_1))
        );

        // confirm Hearing with 2 Prosecution Case
        sendPublicEvent(
                PUBLIC_LISTING_HEARING_CONFIRMED, getHearingJsonObject("public.listing.hearing-confirmed-multiple-case.json",
                        hearingId, prosecutionCaseId_1, defendantId_1, prosecutionCaseId_2, defendantId_2), JsonEnvelope.metadataBuilder()
                        .withId(randomUUID())
                        .withName(PUBLIC_LISTING_HEARING_CONFIRMED)
                        .withUserId(randomUUID().toString())
                        .build());


        Matcher[] hearingMatchers = {
                withJsonPath("$", notNullValue()),
                withJsonPath("$.hearing.id", is(hearingId)),
                withJsonPath("$.hearing.prosecutionCases.[*].id", hasItems(prosecutionCaseId_1, prosecutionCaseId_2)),
                withJsonPath("$.hearing.prosecutionCases.[*].defendants.[*].id", hasItems(defendantId_1, defendantId_2))
        };

        pollForHearing(hearingId, hearingMatchers);

        // Update Multiple Defendant on Same Hearing
        ProsecutionCaseUpdateDefendantWithMatchedHelper prosecutionCaseUpdateDefendantWithMatchedHelper = new ProsecutionCaseUpdateDefendantWithMatchedHelper();

        final String newOffenceId = randomUUID().toString();
        prosecutionCaseUpdateDefendantWithMatchedHelper.addOffenceToDefendant(defendantId_1, prosecutionCaseId_1, newOffenceId, "TFL123");

        Matcher[] lastMatchers = {withJsonPath("$.hearing.id", is(hearingId)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].id", is(defendantId_1)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].prosecutionCaseId", is(prosecutionCaseId_1)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences.length()", is(2)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].id", is("3789ab16-0bb7-4ef1-87ef-c936bf0364f1")),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[1].id", is(newOffenceId)),
                withJsonPath("$.hearing.prosecutionCases[1].defendants[0].id", is(defendantId_2)),
                withJsonPath("$.hearing.prosecutionCases[1].defendants[0].prosecutionCaseId", is(prosecutionCaseId_2)),
                withJsonPath("$.hearing.prosecutionCases[1].defendants[0].offences.length()", is(1)),
                withJsonPath("$.hearing.prosecutionCases[1].defendants[0].offences[0].id", is("3789ab16-0bb7-4ef1-87ef-c936bf0364f1"))
        };

        pollForHearing(hearingId, lastMatchers);

        verifyProbationHearingCommandInvoked(newArrayList(hearingId));
    }

    private JsonObject getHearingJsonObject(final String path, final String hearingId,
                                            final String caseId_1, final String defendantId_1,
                                            final String caseId_2, final String defendantId_2) {
        final String strPayload = getPayload(path)
                .replaceAll("HEARING_ID", hearingId)
                .replaceAll("CASE_ID_1", caseId_1)
                .replaceAll("CASE_ID_2", caseId_2)
                .replaceAll("DEFENDANT_ID_1", defendantId_1)
                .replaceAll("DEFENDANT_ID_2", defendantId_2);
        return new StringToJsonObjectConverter().convert(strPayload);
    }

    private JsonObject getHearingJsonObject(final String path, final String caseId, final String hearingId,
                                            final String defendantId, final String courtCentreId) {
        final String strPayload = getPayload(path)
                .replaceAll("CASE_ID", caseId)
                .replaceAll("HEARING_ID", hearingId)
                .replaceAll("DEFENDANT_ID", defendantId)
                .replaceAll("COURT_CENTRE_ID", courtCentreId);
        return new StringToJsonObjectConverter().convert(strPayload);
    }
}

