package uk.gov.moj.cpp.progression.command.rules;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.Arrays;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

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

        AddCaseToCrownCourtTest("progression.command.add-case-to-crown-court", "Crown Court Admin", "Listing Officers", "Court Clerks", "Court Administrators", "Legal Advisers"),
        DocumentUploadRuleTest("progression.command.defendant-document", "Crown Court Admin", "Listing Officers", "Court Clerks", "Court Administrators", "Legal Advisers"),
        RequestPSRForDefendantsTest("progression.command.request-psr-for-defendants", "Crown Court Admin", "Listing Officers", "Court Clerks", "Court Administrators", "Legal Advisers"),
        SendingCommittalHearingInfoTest("progression.command.sending-committal-hearing-information", "Crown Court Admin", "Listing Officers", "Court Clerks", "Court Administrators", "Legal Advisers"),
        SentenceHearingDateTest("progression.command.sentence-hearing-date", "Crown Court Admin", "Listing Officers", "Court Clerks", "Court Administrators", "Legal Advisers"),
        SendingSheetCompleteRuleTest("progression.command.complete-sending-sheet", "Crown Court Admin", "Listing Officers", "Court Clerks", "Court Administrators", "Legal Advisers"),
        ReferCaseToCourtTest("progression.refer-cases-to-court", "Legal Advisers"),
        UpdateDefendentDetails("progression.update-defendant-for-prosecution-case", "Court Clerks", "Crown Court Admin", "Listing Officers", "Court Administrators", "Legal Advisers"),
        UpdateOffences("progression.update-offences-for-prosecution-case", "Court Clerks", "Crown Court Admin", "Listing Officers", "Court Administrators", "Legal Advisers"),
        UploadCourtDocument("progression.upload-court-document", "Legal Advisers", "Listing Officers", "Court Clerks", "Crown Court Admin", "Court Administrators"),
        AddCourtDocument("progression.add-court-document", "Legal Advisers", "Listing Officers", "Court Clerks", "Crown Court Admin", "Court Administrators"),
        CreateCourtApplication("progression.create-court-application", "Court Clerks", "Crown Court Admin", "Listing Officers", "Court Administrators", "Legal Advisers"),
        InitiateCourtProceedings("progression.initiate-court-proceedings", "Court Clerks", "Crown Court Admin", "Listing Officers", "Court Administrators", "Legal Advisers", "System Users"),
        AddDefendantsToCourtProceedings("progression.add-defendants-to-court-proceedings", "Court Clerks", "Crown Court Admin", "Listing Officers", "Court Administrators", "Legal Advisers", "System Users"),
        EjectCaseOrApplication("progression.eject-case-or-application", "Eject Case Group"),
        AssociateDefenceOrganisation("progression.associate-defence-organisation", "Defence Users"),
        DisassociateDefenceOrganisation("progression.disassociate-defence-organisation", "Defence Users", "Court Clerks", "Court Administrators", "Crown Court Admin", "Listing Officers", "Legal Advisers");

        private final String actionName;
        private final String[] allowedUserGroups;

        ProgressionRules(final String actionName, final String... allowedUserGroups) {
            this.actionName = actionName;
            this.allowedUserGroups = allowedUserGroups;
        }
    }
}
