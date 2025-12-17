package uk.gov.moj.cpp.progression;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.moj.cpp.progression.util.CaseProsecutorUpdateHelper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CaseProsecutorUpdatedIT extends AbstractIT {
    private final JmsMessageConsumerClient publicEventsCaseProsecutorUpdated = newPublicJmsMessageConsumerClientProvider().withEventNames("public.progression.events.cps-prosecutor-updated").getMessageConsumerClient();
    private String caseId;
    private String defendantId;

    private CaseProsecutorUpdateHelper caseProsecutorUpdateHelper;

    @BeforeEach
    public void setUp() {
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        caseProsecutorUpdateHelper = new CaseProsecutorUpdateHelper(caseId);
    }

    @Test
    public void shouldUpdateHearingResultedCaseUpdatedWhenHearingsNotExist() throws Exception {
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));
        caseProsecutorUpdateHelper.updateCaseProsecutor();
        caseProsecutorUpdateHelper.verifyInMessagingQueueForProsecutorUpdated(1, publicEventsCaseProsecutorUpdated);
    }

}

