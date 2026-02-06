package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPrivateJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.generateUrn;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getProsecutionCaseDefendantUpdatedEvent;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.recordLAAReference;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.receiveRepresentationOrder;
import static uk.gov.moj.cpp.progression.stub.AuthorisationServiceStub.stubEnableAllCapabilities;
import static uk.gov.moj.cpp.progression.stub.DefenceStub.stubForAssociatedOrganisation;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubLegalStatus;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetGroupsForLoggedInQuery;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetOrganisationDetailForLAAContractNumber;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetOrganisationDetails;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetOrganisationQuery;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetUsersAndGroupsQueryForSystemUsers;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.it.framework.ContextNameProvider;

import javax.ws.rs.core.Response;

import org.apache.http.HttpStatus;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IsYouthPreservedAfterLaaAndRepOrderIT extends AbstractIT {

    private static final String PROSECUTION_CASE_DEFENDANT_UPDATED_EVENT = "progression.event.prosecution-case-defendant-updated";
    private static final String DEFENDANT_UPDATED_EVENT_RESOURCE = "ingestion/progression.event.prosecution-case-defendant-updated.json";
    private static final String OFFENCE_ID = "3789ab16-0bb7-4ef1-87ef-c936bf0364f1";
    private static final String STATUS_CODE = "G2";
    private static final String LAA_CONTRACT_NUMBER = "LAA3456";

    private final JmsMessageProducerClient messageProducer = newPrivateJmsMessageProducerClientProvider(ContextNameProvider.CONTEXT_NAME)
            .getMessageProducerClient();

    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    private String userId;
    private String caseId;
    private String defendantId;
    private String caseUrn;
    private String organisationId;
    private String organisationName;

    @BeforeEach
    void setUp() {
        userId = randomUUID().toString();
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        caseUrn = generateUrn();
        organisationId = randomUUID().toString();
        organisationName = RandomStringUtils.randomAlphabetic(10);

        stubLegalStatus("/restResource/ref-data-legal-statuses.json", STATUS_CODE);
        stubGetOrganisationDetailForLAAContractNumber(LAA_CONTRACT_NUMBER, organisationId, organisationName);
        stubGetOrganisationQuery(userId, organisationId, organisationName);
        stubGetOrganisationDetails(organisationId, organisationName);
        stubGetUsersAndGroupsQueryForSystemUsers(userId);
        stubGetGroupsForLoggedInQuery(userId);
        stubEnableAllCapabilities();
    }

    @Test
    void shouldPreserveIsYouthAfterLaaReferenceAndRepresentationOrder() throws Exception {
        stubForAssociatedOrganisation("stub-data/defence.get-associated-organisation.json", defendantId);

        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));

        sendProsecutionCaseDefendantUpdatedEvent();

        pollProsecutionCasesProgressionFor(caseId,
                withJsonPath("$.prosecutionCase.defendants[0].isYouth", is(true)));

        recordLAAReference(caseId, defendantId, OFFENCE_ID, STATUS_CODE);

        try (Response responseForRepOrder = receiveRepresentationOrder(caseId, defendantId, OFFENCE_ID, STATUS_CODE, LAA_CONTRACT_NUMBER, userId)) {
            assertThat(responseForRepOrder.getStatus(), equalTo(HttpStatus.SC_ACCEPTED));
        }

        pollProsecutionCasesProgressionFor(caseId,
                withJsonPath("$.prosecutionCase.defendants[0].isYouth", is(true)));
    }

    private void sendProsecutionCaseDefendantUpdatedEvent() {
        final String eventPayload = getProsecutionCaseDefendantUpdatedEvent(caseId, defendantId, caseUrn, DEFENDANT_UPDATED_EVENT_RESOURCE);
        final JsonEnvelope eventEnvelope = envelopeFrom(createMetadata(PROSECUTION_CASE_DEFENDANT_UPDATED_EVENT),
                stringToJsonObjectConverter.convert(eventPayload));
        messageProducer.sendMessage(PROSECUTION_CASE_DEFENDANT_UPDATED_EVENT, eventEnvelope);
    }

    private Metadata createMetadata(final String eventName) {
        return metadataBuilder()
                .withId(randomUUID())
                .withStreamId(randomUUID())
                .withPosition(1)
                .withPreviousEventNumber(123)
                .withEventNumber(randomUUID().getMostSignificantBits())
                .withSource("integration-test")
                .withName(eventName)
                .withUserId(userId)
                .build();
    }
}
