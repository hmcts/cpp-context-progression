package uk.gov.moj.cpp.progression.command.rules;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.progression.command.accesscontrol.PermissionRuleConstants.adhocHearingCreatePermission;
import static uk.gov.moj.cpp.progression.command.accesscontrol.PermissionRuleConstants.getBCMCreatePermission;
import static uk.gov.moj.cpp.progression.command.accesscontrol.PermissionRuleConstants.getBCMEditPermission;
import static uk.gov.moj.cpp.progression.command.accesscontrol.PermissionRuleConstants.getBCMFinalisePermission;
import static uk.gov.moj.cpp.progression.command.accesscontrol.PermissionRuleConstants.getPTPHCreatePermission;
import static uk.gov.moj.cpp.progression.command.accesscontrol.PermissionRuleConstants.getPTPHEditPermission;
import static uk.gov.moj.cpp.progression.command.accesscontrol.PermissionRuleConstants.getPTPHFinalisePermission;
import static uk.gov.moj.cpp.progression.command.accesscontrol.PermissionRuleConstants.petFormFinaliseAccessPermission;
import static uk.gov.moj.cpp.progression.command.accesscontrol.PermissionRuleConstants.petFormGrantAccessPermission;
import static uk.gov.moj.cpp.progression.command.api.accesscontrol.PermissionConstants.addFurtherInfoDefenceCotrPermissions;
import static uk.gov.moj.cpp.progression.command.api.accesscontrol.PermissionConstants.archivePermissions;
import static uk.gov.moj.cpp.progression.command.api.accesscontrol.PermissionConstants.changeDefendantsCotrPermissions;
import static uk.gov.moj.cpp.progression.command.api.accesscontrol.PermissionConstants.createCotrPermissions;
import static uk.gov.moj.cpp.progression.command.api.accesscontrol.PermissionConstants.getCaseCreatePermission;
import static uk.gov.moj.cpp.progression.command.api.accesscontrol.PermissionConstants.serveDefendantCotrPermissions;
import static uk.gov.moj.cpp.progression.command.api.accesscontrol.PermissionConstants.updateReviewNotesPermissions;

import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.refdata.providers.RbacProvider;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.Arrays;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

@ExtendWith(MockitoExtension.class)
public class ProgressionCommandRuleExecutorTest extends BaseDroolsAccessControlTest {

    protected Action action;
    @Mock
    protected UserAndGroupProvider userAndGroupProvider;
    @Mock
    protected RbacProvider rbacProvider;

    @Mock
    private static Logger LOGGER;

    public ProgressionCommandRuleExecutorTest() {
        super("COMMAND_API_SESSION");
    }


    @Override
    protected Map<Class<?>, Object> getProviderMocks() {
        return ImmutableMap.<Class<?>, Object>builder()
                .put(RbacProvider.class, rbacProvider)
                .put(UserAndGroupProvider.class, userAndGroupProvider).build();
    }

    @Test
    public void whenUserIsAMemberOfAllowedUserGroups() {
        Arrays.stream(ProgressionRules.values()).forEach(ruleTest -> {
            action = createActionFor(ruleTest.actionName);
            when(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, ruleTest.allowedUserGroups)).thenReturn(true);
            lenient().when(rbacProvider.isLoggedInUserAllowedToUploadDocument(action)).thenReturn(true);
            final ExecutionResults executionResults = executeRulesWith(action);
            assertSuccessfulOutcome(executionResults);
            verify(userAndGroupProvider).isMemberOfAnyOfTheSuppliedGroups(action, ruleTest.allowedUserGroups);
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
        });
    }

    @Test
    public void whenUserHasPermissionThenItShouldVerify() {

        Arrays.stream(ProgressionPermissions.values()).forEach(ruleTest -> {
            action = createActionFor(ruleTest.actionName);
            lenient().when(userAndGroupProvider.hasPermission(action, ruleTest.allowedPermissions)).thenReturn(true);
            final ExecutionResults executionResults = executeRulesWith(action);
            assertSuccessfulOutcome(executionResults);
            verify(userAndGroupProvider).hasPermission(action, ruleTest.allowedPermissions);
        });
        verify(userAndGroupProvider, atLeast(10)).hasPermission(any(),any());
    }


    public enum ProgressionRules {

        DocumentUploadRuleTest("progression.command.defendant-document", "Crown Court Admin", "Listing Officers", "Court Clerks", "Court Administrators", "Legal Advisers", "Court Associate"),
        ReferCaseToCourtTest("progression.refer-cases-to-court", "Legal Advisers", "Magistrates"),
        UpdateDefendentDetails("progression.update-defendant-for-prosecution-case", "Court Clerks", "Crown Court Admin", "Listing Officers", "Court Administrators", "Legal Advisers", "Probation Admin", "Court Associate"),
        UpdateOffences("progression.update-offences-for-prosecution-case", "Court Clerks", "Crown Court Admin", "Listing Officers", "Court Administrators", "Legal Advisers", "Probation Admin", "Court Associate"),
        UploadCourtDocument("progression.upload-court-document", "Legal Advisers","Listing Officers" ,"Court Clerks", "Crown Court Admin", "Court Administrators", "District Judge", "Court Associate", "Probation Admin", "Second Line Support", "Deputies", "DJMC", "Youth Offending Service Admin", "Judge", "Recorders", "Defence Lawyers", "Advocates", "Police Admin","Non Police Prosecutors", "Non CPS Prosecutors", "NCES"),
        UploadCourtDocumentDefence("progression.upload-court-document-for-defence", "Chambers Clerk","Chambers Admin","Defence Lawyers", "Advocates", "Court Administrators", "NCES"),
        UpdateCourtDocument("progression.update-court-document", "Legal Advisers","Listing Officers" ,"Court Clerks", "Crown Court Admin", "Court Associate", "Court Administrators", "System Users", "Second Line Support", "Deputies", "DJMC"),
        AddCourtDocument("progression.add-court-document", "Legal Advisers", "Listing Officers", "Court Clerks", "Crown Court Admin", "Court Administrators", "System Users", "Defence Users", "District Judge", "Court Associate", "Probation Admin", "Second Line Support", "Deputies", "DJMC", "Youth Offending Service Admin", "Judge", "Recorders", "Defence Lawyers", "Advocates","Police Admin",
                "Non Police Prosecutors", "Non CPS Prosecutors", "NCES"),
        AddCourtDocumentDefence("progression.add-court-document-for-defence", "Chambers Clerk","Chambers Admin","Defence Lawyers", "Advocates", "Court Administrators", "NCES"),
        InitiateCourtProceedings("progression.initiate-court-proceedings", "Court Clerks", "Crown Court Admin", "Listing Officers", "Court Administrators", "Legal Advisers", "System Users", "Probation Admin", "Court Associate"),
        InitiateCourtProceedingsForApplication("progression.initiate-court-proceedings-for-application", "Court Clerks", "Crown Court Admin", "Listing Officers", "Court Administrators", "Legal Advisers", "System Users", "Probation Admin", "Court Associate", "NCES"),
        AddDefendantsToCourtProceedings("progression.add-defendants-to-court-proceedings", "Court Clerks", "Crown Court Admin", "Listing Officers", "Court Administrators", "Legal Advisers", "System Users", "Probation Admin", "Court Associate"),
        AddCaseNote("progression.add-case-note", "Court Clerks", "Crown Court Admin", "Listing Officers", "Court Administrators", "Legal Advisers", "Judiciary", "Court Associate"),
        AddApplicationNote("progression.command.add-application-note", "Court Clerks", "Crown Court Admin", "Listing Officers", "Court Administrators", "Legal Advisers", "Judiciary", "Court Associate"),
        UpdateCaseMarkersApiTest("progression.update-case-markers", "Court Clerks", "Crown Court Admin", "Listing Officers", "Court Administrators", "Legal Advisers", "Court Associate"),
        RecordLAAReference("progression.command.record-laareference-for-offence", "System Users"),
        EjectCaseOrApplication("progression.eject-case-or-application", "Eject Case Group"),
        ReceiveRepresentationOrder("progression.command.receive-representationorder-for-defendant", "System Users"),
        UpdateDefendantListingStatus("progression.update-defendant-listing-status", "System Users"),
        EditCaseNote("progression.edit-case-note", "Court Clerks", "Crown Court Admin", "Listing Officers", "Court Administrators", "Legal Advisers", "Judiciary", "Court Associate"),
        EditApplicationNote("progression.command.edit-application-note", "Court Clerks", "Crown Court Admin", "Listing Officers", "Court Administrators", "Legal Advisers", "Judiciary", "Court Associate"),
        UpdateCpsProsecutor("progression.update-cps-prosecutor-details", "Court Clerks", "Crown Court Admin", "Listing Officers", "Court Administrators", "Legal Advisers", "Probation Admin", "Court Associate"),
        PleadOnline("progression.plead-online", "Online Plea System Users");

        private final String actionName;
        private final String[] allowedUserGroups;

        ProgressionRules(final String actionName, final String... allowedUserGroups) {
            this.actionName = actionName;
            this.allowedUserGroups = allowedUserGroups;
        }
    }

    public enum ProgressionPermissions {

        CreateForm("progression.create-form", getBCMCreatePermission()),
        CreatePTPHForm("progression.create-form", getPTPHCreatePermission()),
        UpdateForm("progression.update-form", getBCMEditPermission()),
        UpdatePTPHForm("progression.update-form", getPTPHEditPermission()),
        FinaliseForm("progression.finalise-form", getBCMFinalisePermission()),
        FinalisePTPHForm("progression.finalise-form", getPTPHFinalisePermission()),
        UpdateBcmDefendants("progression.update-form-defendants", getBCMEditPermission()),
        UpdatePTPHDefendants("progression.update-form-defendants", getPTPHEditPermission()),
        RequestEditBCMForm("progression.request-edit-form", getBCMEditPermission()),
        RequestEditPTPHForm("progression.request-edit-form", getPTPHEditPermission()),
        CourtProceedingsForApplication("progression.initiate-court-proceedings-for-application", getCaseCreatePermission()),
        ListNewHearing("progression.list-new-hearing", adhocHearingCreatePermission()),
        UpdatePetForm("progression.update-pet-form-for-defendant", petFormGrantAccessPermission()),
        FinalisePetForm("progression.finalise-pet-form",petFormFinaliseAccessPermission()),
        UpdateReviewNotes("progression.update-review-notes", updateReviewNotesPermissions()),
        AddFurtherInfoDefenceCotr("progression.add-further-info-defence-cotr", addFurtherInfoDefenceCotrPermissions()),
        ChangeDefendantsCotr("progression.change-defendants-cotr", changeDefendantsCotrPermissions()),
        ServeDefendantCotr("progression.serve-defendant-cotr", serveDefendantCotrPermissions()),
        ArchiveCotr("progression.archive-cotr", archivePermissions()),
        CreateCotr("progression.create-cotr", createCotrPermissions());

        private final String actionName;
        private final String[] allowedPermissions;

        ProgressionPermissions(final String actionName, final String... allowedPermissions) {
            this.actionName = actionName;
            this.allowedPermissions = allowedPermissions;
        }
    }
}
