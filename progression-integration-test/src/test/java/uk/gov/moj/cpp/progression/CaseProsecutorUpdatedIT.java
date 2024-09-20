package uk.gov.moj.cpp.progression;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.it.framework.ContextNameProvider.CONTEXT_NAME;
import static uk.gov.moj.cpp.progression.stub.DocumentGeneratorStub.stubDocumentCreate;
import static uk.gov.moj.cpp.progression.stub.HearingStub.stubInitiateHearing;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.moj.cpp.progression.util.CaseProsecutorUpdateHelper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CaseProsecutorUpdatedIT extends AbstractIT {
    private static final JmsMessageConsumerClient privateEventsConsumer = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.cps-prosecutor-updated").getMessageConsumerClient();
    private static final JmsMessageConsumerClient caseProsecutorUpdatedPrivateEventsConsumer = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.case-cps-prosecutor-updated").getMessageConsumerClient();
    private static final JmsMessageConsumerClient publicEventsCaseProsecutorUpdated = newPublicJmsMessageConsumerClientProvider().withEventNames("public.progression.events.cps-prosecutor-updated").getMessageConsumerClient();
    private static final String DOCUMENT_TEXT = STRING.next();
    private String caseId;
    private String defendantId;

    private CaseProsecutorUpdateHelper caseProsecutorUpdateHelper;

    @BeforeEach
    public void setUp() {
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        caseProsecutorUpdateHelper = new CaseProsecutorUpdateHelper(caseId);

        stubDocumentCreate(DOCUMENT_TEXT);
        stubInitiateHearing();
    }

    @Test
    public void shouldUpdateHearingResultedCaseUpdatedWhenHearingsNotExist() throws Exception {
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));
        caseProsecutorUpdateHelper.updateCaseProsecutor();

        caseProsecutorUpdateHelper.verifyInActiveMQ(privateEventsConsumer, caseProsecutorUpdatedPrivateEventsConsumer);

        caseProsecutorUpdateHelper.verifyInMessagingQueueForProsecutorUpdated(0, publicEventsCaseProsecutorUpdated);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));
    }

}

