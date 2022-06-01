package uk.gov.moj.cpp.progression.command.rules;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.refdata.providers.RbacProvider;
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
    @Mock
    protected RbacProvider rbacProvider;

    @Override
    protected Map<Class, Object> getProviderMocks() {
        return ImmutableMap.<Class, Object>builder()
                .put(RbacProvider.class, rbacProvider)
                .put(UserAndGroupProvider.class, userAndGroupProvider)
                .build();
    }


    @Test
    public void whenUserIsAMemberOfAllowedUserGroups() {
        Arrays.stream(ProgressionRules.values()).forEach(ruleTest -> {
            action = createActionFor(ruleTest.actionName);
            when(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, ruleTest.allowedUserGroups)).thenReturn(true);
            when(rbacProvider.isLoggedInUserAllowedToUploadDocument(action)).thenReturn(true);
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

        AddCaseToCrownCourtTest("progression.command.add-case-to-crown-court", "Crown Court Admin", "Listing Officers" , "Court Clerks", "Court Administrators", "Legal Advisers", "Court Associate"),
        DocumentUploadRuleTest("progression.command.defendant-document", "Crown Court Admin", "Listing Officers", "Court Clerks", "Court Administrators", "Legal Advisers", "Court Associate"),
        RequestPSRForDefendantsTest("progression.command.request-psr-for-defendants", "Crown Court Admin", "Listing Officers", "Court Clerks", "Court Administrators", "Legal Advisers", "Court Associate"),
        SendingCommittalHearingInfoTest("progression.command.sending-committal-hearing-information", "Crown Court Admin", "Listing Officers", "Court Clerks", "Court Administrators", "Legal Advisers", "Court Associate"),
        SentenceHearingDateTest("progression.command.sentence-hearing-date", "Crown Court Admin", "Listing Officers", "Court Clerks", "Court Administrators", "Legal Advisers", "Court Associate"),
        SendingSheetCompleteRuleTest("progression.command.complete-sending-sheet", "Crown Court Admin", "Listing Officers", "Court Clerks", "Court Administrators", "Legal Advisers", "Court Associate"),
        ReferCaseToCourtTest("progression.refer-cases-to-court", "Legal Advisers", "Magistrates"),
        UpdateDefendentDetails("progression.update-defendant-for-prosecution-case", "Court Clerks", "Crown Court Admin", "Listing Officers", "Court Administrators", "Legal Advisers", "Probation Admin", "Court Associate"),
        UpdateOffences("progression.update-offences-for-prosecution-case", "Court Clerks", "Crown Court Admin", "Listing Officers", "Court Administrators", "Legal Advisers", "Probation Admin", "Court Associate"),
        UploadCourtDocument("progression.upload-court-document", "Legal Advisers","Listing Officers" ,"Court Clerks", "Crown Court Admin", "Court Administrators", "District Judge", "Court Associate", "Probation Admin", "Second Line Support", "Deputies", "DJMC", "Youth Offending Service Admin", "Judge", "Recorders", "Defence Lawyers", "Advocates"),
        UploadCourtDocumentDefence("progression.upload-court-document-for-defence", "Chambers Clerk","Chambers Admin","Defence Lawyers", "Advocates"),
        UpdateCourtDocument("progression.update-court-document", "Legal Advisers","Listing Officers" ,"Court Clerks", "Crown Court Admin", "Court Associate", "Court Administrators", "System Users", "Second Line Support", "Deputies", "DJMC"),
        AddCourtDocument("progression.add-court-document", "Legal Advisers", "Listing Officers", "Court Clerks", "Crown Court Admin", "Court Administrators", "System Users", "Defence Users", "District Judge", "Court Associate", "Probation Admin", "Second Line Support", "Deputies", "DJMC", "Youth Offending Service Admin", "Judge", "Recorders", "Defence Lawyers", "Advocates"),
        AddCourtDocumentDefence("progression.add-court-document-for-defence", "Chambers Clerk","Chambers Admin","Defence Lawyers", "Advocates"),
        InitiateCourtProceedings("progression.initiate-court-proceedings", "Court Clerks", "Crown Court Admin", "Listing Officers", "Court Administrators", "Legal Advisers", "System Users", "Probation Admin", "Court Associate"),
        InitiateCourtProceedingsForApplication("progression.initiate-court-proceedings-for-application", "Court Clerks", "Crown Court Admin", "Listing Officers", "Court Administrators", "Legal Advisers", "System Users", "Probation Admin", "Court Associate"),
        AddDefendantsToCourtProceedings("progression.add-defendants-to-court-proceedings", "Court Clerks", "Crown Court Admin", "Listing Officers", "Court Administrators", "Legal Advisers", "System Users", "Probation Admin", "Court Associate"),
        AddCaseNote("progression.add-case-note", "Court Clerks", "Crown Court Admin", "Listing Officers", "Court Administrators", "Legal Advisers", "Judiciary", "Court Associate"),
        UpdateCaseMarkersApiTest("progression.update-case-markers", "Court Clerks", "Crown Court Admin", "Listing Officers", "Court Administrators", "Legal Advisers", "Court Associate"),
        RecordLAAReference("progression.command.record-laareference-for-offence","System Users"),
        EjectCaseOrApplication("progression.eject-case-or-application", "Eject Case Group"),
        ReceiveRepresentationOrder("progression.command.receive-representationorder-for-defendant","System Users"),
        UpdateDefendantListingStatus("progression.update-defendant-listing-status", "System Users"),
        EditCaseNote("progression.edit-case-note", "Court Clerks", "Crown Court Admin", "Listing Officers", "Court Administrators", "Legal Advisers", "Judiciary", "Court Associate"),
        UpdateCpsProsecutor("progression.update-cps-prosecutor-details", "Court Clerks", "Crown Court Admin", "Listing Officers", "Court Administrators", "Legal Advisers", "Probation Admin", "Court Associate");

        private final String actionName;
        private final String[] allowedUserGroups;

        ProgressionRules(final String actionName, final String... allowedUserGroups) {
            this.actionName = actionName;
            this.allowedUserGroups = allowedUserGroups;
        }
    }
}
