package uk.gov.moj.cpp.progression;

import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.generateUrn;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.initiateCourtProceedings;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollInactiveProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;

import java.time.LocalDate;
import java.util.List;

import com.jayway.jsonpath.ReadContext;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SearchInactiveMigratedCasesIT extends AbstractIT {

    private final JmsMessageConsumerClient publicEventConsumer = newPublicJmsMessageConsumerClientProvider().withEventNames("public.progression.prosecution-case-created").getMessageConsumerClient();
    private static final String INITIAL_COURT_PROCEEDINGS_MIGRATION_STATUS_INACTIVE = "progression.command.initiate-court-proceedings-migration-inactive.json";

    private String caseId;
    private String materialIdActive;
    private String materialIdDeleted;
    private String defendantId;
    private String referralReasonId;
    private String listedStartDateTime;
    private String earliestStartDateTime;
    private String defendantDOB;

    @BeforeEach
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
    void shouldInitiateCourtProceedingsForInactiveMigratedCase() {
        final String caseUrn = generateUrn();
        //given
        initiateCourtProceedings(INITIAL_COURT_PROCEEDINGS_MIGRATION_STATUS_INACTIVE, caseId, defendantId, materialIdActive, materialIdDeleted, referralReasonId, caseUrn, listedStartDateTime, earliestStartDateTime, defendantDOB);
        final String inactive = "INACTIVE";
        final String xhibit = "XHIBIT";
        final List<Matcher<? super ReadContext>> inactiveMigratedCaseMatchers = newArrayList(
                withJsonPath("$.prosecutionCase.caseStatus", is(inactive)),
                withJsonPath("$.prosecutionCase.migrationSourceSystem.migrationCaseStatus", is(inactive)),
                withJsonPath("$.prosecutionCase.migrationSourceSystem.migrationSourceSystemName", is(xhibit))
        );

        final List<Matcher<? super ReadContext>> inactiveMigratedCaseSearchMatchers = newArrayList(
                withJsonPath("$.inactiveMigratedCaseSummaries[0].inactiveCaseSummary.id", is(caseId)),

                withJsonPath("$.inactiveMigratedCaseSummaries[0].inactiveCaseSummary.migrationSourceSystem.migrationCaseStatus", is(inactive)),

                withJsonPath("$.inactiveMigratedCaseSummaries[0].inactiveCaseSummary.migrationSourceSystem.migrationSourceSystemName", is(xhibit)),

                withJsonPath("$.inactiveMigratedCaseSummaries[0].inactiveCaseSummary.defendants[0].masterDefendantId", is(defendantId))
        );

        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId, inactiveMigratedCaseMatchers));


        pollInactiveProsecutionCasesProgressionFor(caseId, inactiveMigratedCaseSearchMatchers.toArray(new Matcher[0]));

    }

}
