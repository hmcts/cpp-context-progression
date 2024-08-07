package uk.gov.moj.cpp.progression.query.api;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import uk.gov.justice.api.resource.service.CourtDocumentProvider;
import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.progression.providers.ProgressionProvider;
import uk.gov.moj.cpp.accesscontrol.refdata.providers.RbacProvider;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

public class ProgressionQueryApiAccessControlTest extends BaseDroolsAccessControlTest {

    private static final List<String> ALLOWED_USER_GROUPS = Arrays.asList(
            "Listing Officer",
            "Defence"
    );

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    @Captor
    private ArgumentCaptor<String[]> arrayCaptor;

    @Captor
    private ArgumentCaptor<List<String>> listCaptor;

    @Mock
    private ProgressionProvider progressionProvider;

    @Mock
    private CourtDocumentProvider caseDocumentProvider;

    @Mock
    private RbacProvider rbacProvider;

    @Test
    public void shouldAllowUserInAuthorisedGroupToGetProsecutioncase() {
        assertSuccessfulOutcomeOnActionForTheSuppliedGroups("progression.query.prosecutioncase", "System Users","Court Clerks", "Crown Court Admin", "Listing Officers", "Court Administrators", "Legal Advisers", "Probation Admin", "Judiciary", "Court Associate", "Deputies", "DJMC", "Judge", "Youth Offending Service Admin", "Magistrates",  "District Judge", "Second Line Support","Police Admin", "NCES", "Victims & Witness Care Admin","Recorders", "Eject Case Group", "CPS", "Defence Lawyers", "Advocates","Non Police Prosecutors", "Non CPS Prosecutors");
    }

    @Test
    public void shouldNotAllowUserInAuthorisedGroupToGetProsecutioncase() {
        assertFailureOutcomeOnActionForTheSuppliedGroups("progression.query.prosecutioncase", "System Users","Court Clerks", "Crown Court Admin", "Listing Officers", "Court Administrators", "Legal Advisers", "Probation Admin", "Judiciary", "Court Associate", "Deputies", "DJMC", "Judge", "Youth Offending Service Admin", "Magistrates",  "District Judge", "Second Line Support","Police Admin", "NCES", "Victims & Witness Care Admin","Recorders", "Eject Case Group", "CPS", "Defence Lawyers", "Advocates","Non Police Prosecutors", "Non CPS Prosecutors");
    }

    @Test
    public void shouldAllowUserInAuthorisedGroupToGetMaterialContent() {
        assertSuccessfulOutcomeOnActionForTheSuppliedGroups("progression.query.material-content", "System Users" ,"Court Clerks",  "Crown Court Admin", "Listing Officers", "Judiciary", "Case Officer", "Court Clerks", "Legal Advisers", "District Judge", "Court Associate", "Defence Users", "Probation Admin", "Youth Offending Service Admin", "Magistrates","Court Administrators", "Probation Admin", "Second Line Support", "Deputies", "DJMC", "NCES", "Police Admin", "Victims & Witness Care Admin", "Judge", "Recorders", "Defence Lawyers", "Advocates","Non Police Prosecutors");

    }

    @Test
    public void shouldNotAllowUserInAuthorisedGroupToGetMaterialContent() {
        assertFailureOutcomeOnActionForTheSuppliedGroups("progression.query.material-content", "System Users", "Court Clerks", "Crown Court Admin", "Listing Officers", "Judiciary", "Case Officer", "Court Clerks", "Legal Advisers", "Probation Admin","District Judge", "Court Associate", "Defence Users", "Probation Admin", "Youth Offending Service Admin", "Magistrates","Court Administrators", "Second Line Support", "Deputies", "DJMC", "NCES", "Police Admin", "Victims & Witness Care Admin", "Judge", "Recorders", "Defence Lawyers", "Advocates","Non Police Prosecutors");
    }

    @Test
    public void shouldAllowUserInAuthorisedGroupToSearchCases() {
        assertSuccessfulOutcomeOnActionForTheSuppliedGroups("progression.query.search-cases", "Court Clerks", "Crown Court Admin", "Listing Officers", "Court Administrators", "Legal Advisers", "System Users", "Probation Admin","Court Associate", "Magistrates", "Online Plea System Users");
    }

    @Test
    public void shouldNotAllowUserInAuthorisedGroupToSearchCases() {
        assertFailureOutcomeOnActionForTheSuppliedGroups("progression.query.search-cases", "Court Clerks", "Crown Court Admin", "Listing Officers", "Court Administrators", "Legal Advisers", "System Users", "Probation Admin","Court Associate", "Magistrates", "Online Plea System Users");
    }

    @Test
    public void shouldAllowUserInAuthorisedGroupToProgressionCaseDetail() {
        assertSuccessfulOutcomeOnActionForTheSuppliedGroups("progression.query.caseprogressiondetail", "Legal Advisers", "System Users", "Court Clerks", "Crown Court Admin", "Listing Officers", "Judiciary", "Case Officer", "Court Clerks", "Probation Admin","Court Associate");
    }

    @Test
    public void shouldNotAllowUserInAuthorisedGroupToProgressionCaseDetail() {
        assertFailureOutcomeOnActionForTheSuppliedGroups("progression.query.caseprogressiondetail", "Legal Advisers", "System Users", "Court Clerks", "Crown Court Admin", "Listing Officers", "Judiciary", "Case Officer", "Court Clerks", "Probation Admin","Court Associate");
    }
    @Test
    public void shouldAllowUserInAuthorisedGroupToGetDefendantDocument() {
        assertSuccessfulOutcomeOnActionForTheSuppliedGroups("progression.query.defendant.document", "Crown Court Admin", "Listing Officers", "Judiciary", "Case Officer", "Court Clerks","Court Associate");
    }

    @Test
    public void shouldNotAllowUserInAuthorisedGroupToGetDefendantDocument() {
        assertFailureOutcomeOnActionForTheSuppliedGroups("progression.query.defendant.document", "Crown Court Admin", "Listing Officers", "Judiciary", "Case Officer", "Court Clerks","Court Associate");
    }

    @Test
    public void shouldAllowUserInAuthorisedGroupToGetMagistrateCourts() {
        assertSuccessfulOutcomeOnActionForTheSuppliedGroups("progression.query.crown-court.magistrate-courts", "Crown Court Admin", "Listing Officers", "Judiciary", "Case Officer", "Court Clerks","Court Associate");
    }

    @Test
    public void shouldNotAllowUserInAuthorisedGroupToGetMagistrateCourts() {
        assertFailureOutcomeOnActionForTheSuppliedGroups("progression.query.crown-court.magistrate-courts", "Crown Court Admin", "Listing Officers", "Judiciary", "Case Officer", "Court Clerks","Court Associate");
    }

    @Test
    public void shouldAllowUserInAuthorisedGroupToCasesSearchByMaterialId() {
        assertSuccessfulOutcomeOnActionForTheSuppliedGroups("progression.query.cases-search-by-material-id", "System Users", "Crown Court Admin", "Listing Officers", "Judiciary", "Case Officer");
    }

    @Test
    public void shouldNotAllowUserInAuthorisedGroupToCasesSearchByMaterialId() {
        assertFailureOutcomeOnActionForTheSuppliedGroups("progression.query.cases-search-by-material-id", "System Users", "Crown Court Admin", "Listing Officers", "Judiciary", "Case Officer");
    }

    @Test
    public void shouldAllowUserInAuthorisedGroupToUserGroupsByMaterialId() {
        assertSuccessfulOutcomeOnActionForTheSuppliedGroups("progression.query.usergroups-by-material-id", "System Users");
    }

    @Test
    public void shouldNotAllowUserInAuthorisedGroupToUserGroupsByMaterialId() {
        assertFailureOutcomeOnActionForTheSuppliedGroups("progression.query.usergroups-by-material-id", "System Users");
    }

    @Test
    public void shouldAllowUserInAuthorisedGroupToGetMaterialMetadata() {
        assertSuccessfulOutcomeOnActionForTheSuppliedGroupsList("progression.query.material-metadata");
    }

    @Test
    public void shouldNotAllowUserInAuthorisedGroupToGetMaterialMetadata() {
        assertFailureOutcomeOnActionForTheSuppliedGroupsList("progression.query.material-metadata");
    }

    @Test
    public void shouldAllowUserInAuthorisedGroupToGetQueryMaterialContentNows() {
        assertSuccessfulOutcomeOnActionForTheSuppliedGroups("progression.query.material-nows-content", "System Users", "Listing Officers", "Court Clerks", "Legal Advisers", "Prison Admin", "Probation Admin", "Police Admin", "Victims & Witness Care Admin", "Youth Offending Service Admin", "Legal Aid Agency Admin", "Probation Admin", "Deputies", "DJMC","Court Associate");
    }

    @Test
    public void shouldAllowUserInAuthorisedGroupToGetQueryHearing() {
        assertSuccessfulOutcomeOnActionForTheSuppliedGroups("progression.query.hearing", "System Users", "Court Clerks", "Crown Court Admin", "Listing Officers", "Court Administrators", "Legal Advisers", "Judiciary", "Court Associate");
    }

    @Test
    public void shouldNotAllowUserInAuthorisedGroupToGetQueryMaterialContentNows() {
        assertFailureOutcomeOnActionForTheSuppliedGroups("progression.query.material-nows-content", "System Users", "Listing Officers", "Court Clerks", "Legal Advisers", "Prison Admin", "Probation Admin", "Police Admin", "Victims & Witness Care Admin",
                "Youth Offending Service Admin", "Legal Aid Agency Admin", "Probation Admin", "Deputies", "DJMC","Court Associate");
    }

    @Test
    public void shouldAllowUserInQueryAssociatedUser() {
        assertSuccessfulOutcomeOnActionForTheSuppliedGroups("progression.query.associated-organisation", "System Users", "Defence Users", "Court Clerks", "Court Administrators", "Crown Court Admin", "Listing Officers", "Legal Advisers", "Judiciary", "Court Associate", "Deputies", "DJMC", "Judge");
    }

    @Test
    public void shouldAllowUserInAuthorisedGroupToGetQueryCaseAtAGlance() {
        assertSuccessfulOutcomeOnActionForTheSuppliedGroups("progression.query.prosecutioncase.caag", "Judiciary", "Listing Officers", "Legal Advisers", "Court Associate", "Court Clerks", "NCES", "CPS",
                "Probation Admin", "Youth Offending Service Admin", "Court Administrators", "Crown Court Admin", "Judge", "Police Admin", "Recorders", "DJMC", "Deputies", "Victims & Witness Care Admin", "System Users", "Advocates", "Defence Lawyers");
    }

    @Test
    public void shouldNotAllowUserInUnAuthorisedGroupToGetQueryCaseAtAGlance() {
        assertFailureOutcomeOnActionForTheSuppliedGroups("progression.query.prosecutioncase.caag", "Youth Offending Service Admin", "Probation Admin", "Judiciary", "Listing Officers", "Legal Advisers",
                "Court Associate", "Court Clerks", "NCES", "CPS", "Court Administrators", "Crown Court Admin", "Judge", "Police Admin", "Recorders", "DJMC", "Deputies", "Victims & Witness Care Admin", "System Users", "Advocates", "Defence Lawyers");
    }


    @Test
    public void shouldAllowUserInAuthorisedGroupToGetQueryApplicationAtAGlance() {
        assertSuccessfulOutcomeOnActionForTheSuppliedGroups("progression.query.application.aaag", "Judiciary", "Listing Officers", "Legal Advisers", "Court Associate", "Court Clerks", "NCES", "CPS",
                "Probation Admin", "Youth Offending Service Admin", "Court Administrators", "Crown Court Admin", "Judge", "Victims & Witness Care Admin","Police Admin","Recorders","DJMC","Deputies");
    }

    @Test
    public void shouldNotAllowUserInUnAuthorisedGroupToGetQueryApplicationAtAGlance() {
        assertFailureOutcomeOnActionForTheSuppliedGroups("progression.query.application.aaag", "Youth Offending Service Admin", "Probation Admin", "Judiciary", "Listing Officers", "Legal Advisers", "Court Associate",
                "Court Clerks", "NCES", "CPS", "Court Administrators", "Crown Court Admin", "Judge", "Victims & Witness Care Admin","Police Admin","Recorders","DJMC","Deputies");
    }

    @Test
    public void shouldAllowUserInAuthorisedGroupToGetQueryApplicationAtAGlanceForDefence() {
        assertSuccessfulOutcomeOnActionForTheSuppliedGroups("progression.query.application.aaag-for-defence", "Chambers Clerk", "Chambers Admin", "Defence Lawyers", "Advocates", "Non CPS Prosecutors");
    }

    @Test
    public void shouldNotAllowUserInUnAuthorisedGroupToGetQueryApplicationAtAGlanceForDefence() {
        assertFailureOutcomeOnActionForTheSuppliedGroups("progression.query.application.aaag-for-defence", "Chambers Clerk", "Chambers Admin", "Defence Lawyers", "Advocates", "Non CPS Prosecutors");
    }

    @Test
    public void shouldAllowUserInQueryDefendantsByLAAContractNumber() {
        assertSuccessfulOutcomeOnActionForTheSuppliedGroups("progression.query.defendants-by-laacontractnumber", "System Users", "Defence Users", "Court Clerks", "Court Administrators", "Crown Court Admin", "Listing Officers", "Legal Advisers","Court Associate");
    }

    @Override
    protected Map<Class, Object> getProviderMocks() {
        return ImmutableMap.<Class, Object>builder().put(UserAndGroupProvider.class, this.userAndGroupProvider).put(ProgressionProvider.class, this.progressionProvider).put(CourtDocumentProvider.class, this.caseDocumentProvider).put(RbacProvider.class, this.rbacProvider).build();
    }

    @Test
    public void shouldAllowUserInAuthorisedGroupToGetAllCaseNotes() {
        assertSuccessfulOutcomeOnActionForTheSuppliedGroups("progression.query.case-notes", "Court Clerks", "Crown Court Admin", "Listing Officers", "Court Administrators", "Legal Advisers", "Judiciary", "DJMC", "Court Associate", "Judge", "Recorders", "Deputies");
    }

    @Test
    public void shouldNotAllowUserInAuthorisedGroupToGetAllCaseNotes() {
        assertFailureOutcomeOnActionForTheSuppliedGroups("progression.query.case-notes", "Court Clerks", "Crown Court Admin", "Listing Officers", "Court Administrators", "Legal Advisers", "Judiciary", "DJMC", "Court Associate","Judge","Deputies","Recorders");
    }

    @Test
    public void shouldAllowUserInAuthorisedGroupToGetAllCourtDocuments() {
        assertSuccessfulOutcomeOnActionForTheSuppliedGroups("progression.query.courtdocuments", "System Users","Court Clerks", "Crown Court Admin", "Listing Officers", "Court Administrators", "Legal Advisers",
                "Prison Admin", "Court Admin", "Probation Admin", "Police Admin", "Victims & Witness Care Admin", "Youth Offending Service Admin", "Magistrates", "Court Associate", "District Judge", "Probation Admin", "Judiciary", "Court Associate", "Deputies", "DJMC", "Judge", "Second Line Support", "NCES", "Recorders", "Defence Lawyers", "Advocates","Non Police Prosecutors");
    }

    @Test
    public void shouldNotAllowUserInAuthorisedGroupToGetAllCourtDocuments() {
        assertFailureOutcomeOnActionForTheSuppliedGroups("progression.query.courtdocuments", "System Users","Court Clerks", "Crown Court Admin", "Listing Officers", "Court Administrators", "Legal Advisers",
                "Prison Admin", "Court Admin", "Probation Admin", "Police Admin", "Victims & Witness Care Admin", "Youth Offending Service Admin", "Magistrates", "Court Associate", "District Judge", "Probation Admin", "Judiciary", "Court Associate", "Deputies", "DJMC", "Judge", "Second Line Support", "NCES", "Recorders", "Defence Lawyers", "Advocates","Non Police Prosecutors");
    }

    @Test
    public void shouldAllowUserInAuthorisedGroupToGetCaseHearings() {
        assertSuccessfulOutcomeOnActionForTheSuppliedGroups("progression.query.casehearings", "System Users","Judiciary", "Listing Officers", "Legal Advisers", "Court Associate", "Court Clerks", "NCES", "CPS", "Probation Admin", "Youth Offending Service Admin", "Court Administrators", "Crown Court Admin", "Judge", "Police Admin","Recorders","DJMC","Deputies","Victims & Witness Care Admin", "Magistrates","Second Line Support","Defence Lawyers", "Eject Case Group", "Recorders", "Advocates","Non Police Prosecutors");
    }

    @Test
    public void shouldNotAllowUserInAuthorisedGroupToGetCaseHearings() {
        assertFailureOutcomeOnActionForTheSuppliedGroups("progression.query.casehearings", "System Users", "Judiciary", "Listing Officers", "Legal Advisers", "Court Associate", "Court Clerks", "NCES", "CPS", "Probation Admin", "Youth Offending Service Admin", "Court Administrators", "Crown Court Admin", "Judge", "Police Admin","Recorders","DJMC","Deputies","Victims & Witness Care Admin", "Magistrates","Second Line Support","Defence Lawyers", "Eject Case Group", "Recorders", "Advocates","Non Police Prosecutors");
    }

    @Test
    public void shouldAllowUserInAuthorisedGroupToGetApplication() {
        assertSuccessfulOutcomeOnActionForTheSuppliedGroups("progression.query.application", "System Users","Court Clerks", "Crown Court Admin", "Listing Officers", "Court Administrators", "Legal Advisers", "Probation Admin", "Second Line Support", "Court Associate", "Eject Case Group");
    }

    @Test
    public void shouldNotAllowUserInAuthorisedGroupToGetApplication() {
        assertFailureOutcomeOnActionForTheSuppliedGroups("progression.query.application", "System Users","Court Clerks", "Crown Court Admin", "Listing Officers", "Court Administrators", "Legal Advisers", "Probation Admin", "Second Line Support", "Court Associate", "Eject Case Group");
    }

    @Test
    public void shouldAllowUserInAuthorisedGroupToGetApplicationOnly() {
        assertSuccessfulOutcomeOnActionForTheSuppliedGroups("progression.query.application-only", "System Users","Court Clerks", "Crown Court Admin", "Listing Officers", "Court Administrators", "Legal Advisers", "Probation Admin", "Second Line Support", "Court Associate", "Eject Case Group");
    }

    @Test
    public void shouldNotAllowUserInAuthorisedGroupToGetApplicationOnly() {
        assertFailureOutcomeOnActionForTheSuppliedGroups("progression.query.application-only", "System Users","Court Clerks", "Crown Court Admin", "Listing Officers", "Court Administrators", "Legal Advisers", "Probation Admin", "Second Line Support", "Court Associate", "Eject Case Group");
    }

    @Test
    public void shouldAllowUserInAuthorisedGroupToGetCourtProceedingsForApplication() {
        assertSuccessfulOutcomeOnActionForTheSuppliedGroups("progression.query.court-proceedings-for-application", "System Users", "Listing Officers", "Court Clerks", "Legal Advisers", "Prison Admin", "Probation Admin", "Police Admin", "Victims & Witness Care Admin",
                "Youth Offending Service Admin", "Legal Aid Agency Admin", "Probation Admin", "Court Associate", "Eject Case Group", "Deputies" , "Recorders", "Crown Court Admin", "Court Administrators","DJMC","Judge","Non Police Prosecutors");
    }

    @Test
    public void shouldNotAllowUserInAuthorisedGroupToGetCourtProceedingsForApplication() {
        assertFailureOutcomeOnActionForTheSuppliedGroups("progression.query.court-proceedings-for-application", "System Users", "Listing Officers", "Court Clerks", "Legal Advisers", "Prison Admin", "Probation Admin", "Police Admin", "Victims & Witness Care Admin",
                "Youth Offending Service Admin", "Legal Aid Agency Admin", "Probation Admin", "Court Associate", "Eject Case Group", "Deputies" , "Recorders", "Crown Court Admin", "Court Administrators","DJMC","Judge","Non Police Prosecutors");
    }

    @Test
    public void shouldAllowUserInAuthorisedGroupToPartialMatchDefendants() {
        assertSuccessfulOutcomeOnActionForTheSuppliedGroups("progression.query.partial-match-defendants", "System Users", "Court Admin", "Court Associate", "Court Clerk", "Listing Officers", "Legal Advisers", "Court Administrators");
    }

    @Test
    public void shouldNotAllowUserInAuthorisedGroupToPartialMatchDefendants() {
        assertFailureOutcomeOnActionForTheSuppliedGroups("progression.query.partial-match-defendants", "System Users", "Court Admin", "Court Associate", "Court Clerk", "Listing Officers", "Legal Advisers", "Court Administrators");
    }

    @Test
    public void shouldAllowUserInAuthorisedGroupToGetAllCourtDocumentsForProsecution() {
        assertSuccessfulOutcomeOnActionForTheSuppliedGroups("progression.query.courtdocuments.for.prosecution", "Chambers Clerk","Chambers Admin","Defence Lawyers", "Advocates",  "Non CPS Prosecutors");
    }

    @Test
    public void shouldNotAllowUserInAuthorisedGroupToGetAllCourtDocumentsForProsecution() {
        assertFailureOutcomeOnActionForTheSuppliedGroups("progression.query.courtdocuments.for.prosecution", "Chambers Clerk","Chambers Admin","Defence Lawyers", "Advocates",  "Non CPS Prosecutors");
    }
    @Test
    public void shouldAllowUserInAuthorisedGroupToGetMaterialForProsecution() {
        assertSuccessfulOutcomeOnActionForTheSuppliedGroups("progression.query.material-content-for-prosecution", "Chambers Clerk","Chambers Admin","Defence Lawyers", "Advocates", "Non CPS Prosecutors");
    }

    @Test
    public void shouldNotAllowUserInAuthorisedGroupToGetMaterialForProsecution() {
        assertFailureOutcomeOnActionForTheSuppliedGroups("progression.query.material-content-for-prosecution", "Chambers Clerk","Chambers Admin","Defence Lawyers", "Advocates", "Non CPS Prosecutors");
    }

    private void assertFailureOutcomeOnActionForTheSuppliedGroups(final String actionName, final String... groupNames) {
        final Action action = createActionFor(actionName);
        when(progressionProvider.getAllowedUserGroups(action)).thenReturn(ALLOWED_USER_GROUPS);
        given(this.userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, groupNames))
                .willReturn(false);

        assertFailureOutcome(executeRulesWith(action));

        verify(this.userAndGroupProvider).isMemberOfAnyOfTheSuppliedGroups(eq(action), this.arrayCaptor.capture());
        assertThat(this.arrayCaptor.getAllValues(), containsInAnyOrder(groupNames));
        verifyNoMoreInteractions(this.userAndGroupProvider);
    }

    private void assertSuccessfulOutcomeOnActionForTheSuppliedGroups(final String actionName, final String... groupNames) {
        final Action action = createActionFor(actionName);
        when(progressionProvider.getAllowedUserGroups(action)).thenReturn(ALLOWED_USER_GROUPS);
        given(this.userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, groupNames))
                .willReturn(true);

        assertSuccessfulOutcome(executeRulesWith(action));

        verify(this.userAndGroupProvider).isMemberOfAnyOfTheSuppliedGroups(eq(action), this.arrayCaptor.capture());
        assertThat(this.arrayCaptor.getAllValues(), containsInAnyOrder(groupNames));
        verifyNoMoreInteractions(this.userAndGroupProvider);
    }


    private void assertSuccessfulOutcomeOnActionForTheSuppliedGroupsList(final String actionName) {
        final Action action = createActionFor(actionName);
        when(progressionProvider.getAllowedUserGroups(action)).thenReturn(ALLOWED_USER_GROUPS);
        when(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(any(Action.class), anyList())).thenReturn(true);

        assertSuccessfulOutcome(executeRulesWith(action));
        verify(userAndGroupProvider).isMemberOfAnyOfTheSuppliedGroups(eq(action), listCaptor.capture());
        assertThat(listCaptor.getValue(), Matchers.containsInAnyOrder(ALLOWED_USER_GROUPS.toArray()));
    }

    private void assertFailureOutcomeOnActionForTheSuppliedGroupsList(final String actionName) {
        final Action action = createActionFor(actionName);
        when(progressionProvider.getAllowedUserGroups(action)).thenReturn(ALLOWED_USER_GROUPS);
        when(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(any(Action.class), anyList())).thenReturn(false);

        assertFailureOutcome(executeRulesWith(action));
        verify(userAndGroupProvider).isMemberOfAnyOfTheSuppliedGroups(eq(action), listCaptor.capture());
        assertThat(listCaptor.getValue(), Matchers.containsInAnyOrder(ALLOWED_USER_GROUPS.toArray()));
    }


    private void assertSuccessfulOutcomeOnActionForTheSuppliedGroupsOnAddDocument(final String actionName, final String... groupNames) {
        final Action action = createActionFor(actionName);
        when(progressionProvider.getAllowedUserGroups(action)).thenReturn(ALLOWED_USER_GROUPS);
        when(caseDocumentProvider.getDocumentTypeId(action)).thenReturn(action);
        when(rbacProvider.isLoggedInUserAllowedToReadDocument(action)).thenReturn(true);

        given(this.userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, groupNames))
                .willReturn(true);

        assertSuccessfulOutcome(executeRulesWith(action));

        verify(this.userAndGroupProvider).isMemberOfAnyOfTheSuppliedGroups(eq(action), this.arrayCaptor.capture());
        assertThat(this.arrayCaptor.getAllValues(), containsInAnyOrder(groupNames));
        verifyNoMoreInteractions(this.userAndGroupProvider);
    }

}
