package uk.gov.moj.cpp.progression.command.rules;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.ImmutableMap;

import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

@RunWith(MockitoJUnitRunner.class)
public class ProgressionCommandRuleExecutorTest extends BaseDroolsAccessControlTest {


    protected Action action;
    @Mock
    protected UserAndGroupProvider userAndGroupProvider;

    @Override
    protected Map<Class, Object> getProviderMocks() {
        return ImmutableMap.<Class, Object>builder()
                .put(UserAndGroupProvider.class, userAndGroupProvider).build();
    }

    @Test
    public void whenUserIsAMemberOfAllowedUserGroups_thenSuccessfullyAllowUpload() throws Exception {
        Arrays.stream(ProgressionRules.values()).forEach(ruleTest -> {
            action = createActionFor(ruleTest.actionName);
            when(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, ruleTest.allowedUserGroups)).thenReturn(true);
            final ExecutionResults executionResults = executeRulesWith(action);
            assertSuccessfulOutcome(executionResults);
            verify(userAndGroupProvider).isMemberOfAnyOfTheSuppliedGroups(action, ruleTest.allowedUserGroups);
            verifyNoMoreInteractions(userAndGroupProvider);
        });
    }

    @Test
    public void whenUserIsNotAMemberOfAllowedUserGroups_thenFailUpload() throws Exception {
        Arrays.stream(ProgressionRules.values()).forEach(ruleTest -> {
            action = createActionFor(ruleTest.actionName);
            when(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, ruleTest.allowedUserGroups)).thenReturn(false);
            final ExecutionResults executionResults = executeRulesWith(action);
            assertFailureOutcome(executionResults);
            verify(userAndGroupProvider).isMemberOfAnyOfTheSuppliedGroups(action, ruleTest.allowedUserGroups);
            verifyNoMoreInteractions(userAndGroupProvider);
        });
    }

    public enum ProgressionRules {

        AddAditionalDefendantInfoTest("progression.command.add-defendant-additional-information", "Judiciary", "Case Officer"),
        AddCaseToCrownCourtTest("progression.command.add-case-to-crown-court", "Crown Court Admin", "Listing Officers"),
        CaseToBeAssignedTest("progression.command.case-to-be-assigned", "Listing Officers", "Crown Court Admin"),
        DocumentUploadRuleTest("progression.command.defendant-document", "Crown Court Admin", "Listing Officers"),
        NoMoreDefendantInfoRequiredTest("progression.command.no-more-information-required", "Judiciary", "Case Officer"),
        RequestPSRForDefendantsTest("progression.command.request-psr-for-defendants", "Crown Court Admin", "Listing Officers"),
        SendingCommittalHearingInfoTest("progression.command.sending-committal-hearing-information", "Crown Court Admin", "Listing Officers"),
        SentenceHearingDateTest("progression.command.sentence-hearing-date", "Crown Court Admin","Listing Officers"),
        SendingSheetCompleteRuleTest("progression.command.complete-sending-sheet", "Crown Court Admin", "Listing Officers");

        private final String actionName;
        private final String[] allowedUserGroups;

        ProgressionRules(final String actionName, final String... allowedUserGroups) {
            this.actionName = actionName;
            this.allowedUserGroups = allowedUserGroups;
        }
    }
}
