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
        assertSuccessfulOutcomeOnActionForTheSuppliedGroups("progression.query.prosecutioncase", "System Users","Court Clerks", "Crown Court Admin", "Listing Officers", "Court Administrators", "Legal Advisers", "Defence Users", "Probation Admin", "Judiciary", "Court Associate", "Deputies", "DJMC", "Judge", "Defence Advocate", "Youth Offending Service Admin", "Magistrates",  "District Judge", "Second Line Support");
    }

    @Test
    public void shouldNotAllowUserInAuthorisedGroupToGetProsecutioncase() {
        assertFailureOutcomeOnActionForTheSuppliedGroups("progression.query.prosecutioncase", "System Users", "Court Clerks", "Crown Court Admin", "Listing Officers", "Court Administrators", "Legal Advisers", "Defence Users","Probation Admin", "Judiciary","Court Associate", "Deputies", "DJMC", "Judge", "Youth Offending Service Admin", "Magistrates", "Defence Advocate", "District Judge", "Second Line Support");
    }

    @Test
    public void shouldAllowUserInAuthorisedGroupToGetMaterialContent() {
        assertSuccessfulOutcomeOnActionForTheSuppliedGroups("progression.query.material-content", "System Users" ,"Court Clerks",  "Crown Court Admin", "Listing Officers", "Judiciary", "Case Officer", "Court Clerks", "Legal Advisers", "District Judge", "Court Associate", "Defence Users", "Probation Admin", "Youth Offending Service Admin", "Magistrates","Court Administrators", "Probation Admin", "Second Line Support");

    }

    @Test
    public void shouldNotAllowUserInAuthorisedGroupToGetMaterialContent() {
        assertFailureOutcomeOnActionForTheSuppliedGroups("progression.query.material-content", "System Users", "Court Clerks", "Crown Court Admin", "Listing Officers", "Judiciary", "Case Officer", "Court Clerks", "Legal Advisers", "Probation Admin","District Judge", "Court Associate", "Defence Users", "Probation Admin", "Youth Offending Service Admin", "Magistrates","Court Administrators", "Second Line Support");
    }

    @Test
    public void shouldAllowUserInAuthorisedGroupToGetProgressionCases() {
        assertSuccessfulOutcomeOnActionForTheSuppliedGroups("progression.query.cases", "System Users", "Crown Court Admin", "Listing Officers", "Judiciary", "Case Officer", "Court Clerks", "Probation Admin");
    }

    @Test
    public void shouldNotAllowUserInAuthorisedGroupToGetProgressionCases() {
        assertFailureOutcomeOnActionForTheSuppliedGroups("progression.query.cases", "System Users", "Crown Court Admin", "Listing Officers", "Judiciary", "Case Officer", "Court Clerks", "Probation Admin");
    }

    @Test
    public void shouldAllowUserInAuthorisedGroupToSearchCases() {
        assertSuccessfulOutcomeOnActionForTheSuppliedGroups("progression.query.search-cases", "Court Clerks", "Crown Court Admin", "Listing Officers", "Court Administrators", "Legal Advisers", "System Users", "Probation Admin");
    }

    @Test
    public void shouldNotAllowUserInAuthorisedGroupToSearchCases() {
        assertFailureOutcomeOnActionForTheSuppliedGroups("progression.query.search-cases", "Court Clerks", "Crown Court Admin", "Listing Officers", "Court Administrators", "Legal Advisers", "System Users", "Probation Admin");
    }

    @Test
    public void shouldAllowUserInAuthorisedGroupToProgressionCaseDetail() {
        assertSuccessfulOutcomeOnActionForTheSuppliedGroups("progression.query.caseprogressiondetail", "Legal Advisers", "System Users", "Court Clerks", "Crown Court Admin", "Listing Officers", "Judiciary", "Case Officer", "Court Clerks", "Probation Admin");
    }

    @Test
    public void shouldNotAllowUserInAuthorisedGroupToProgressionCaseDetail() {
        assertFailureOutcomeOnActionForTheSuppliedGroups("progression.query.caseprogressiondetail", "Legal Advisers", "System Users", "Court Clerks", "Crown Court Admin", "Listing Officers", "Judiciary", "Case Officer", "Court Clerks", "Probation Admin");
    }

    @Test
    public void shouldAllowUserInAuthorisedGroupToGetCaseByUrn() {
        assertSuccessfulOutcomeOnActionForTheSuppliedGroups("progression.query.case-by-urn", "System Users", "Court Clerks", "Legal Advisers", "System Users", "CMS", "Crown Court Admin", "Listing Officers", "Judiciary", "Case Officer", "Probation Admin");
    }

    @Test
    public void shouldNotAllowUserInAuthorisedGroupToGetCaseByUrn() {
        assertFailureOutcomeOnActionForTheSuppliedGroups("progression.query.case-by-urn", "System Users", "Court Clerks", "Legal Advisers", "System Users", "CMS", "Crown Court Admin", "Listing Officers", "Judiciary", "Case Officer", "Probation Admin");
    }

    @Test
    public void shouldAllowUserInAuthorisedGroupToGetDefendant() {
        assertSuccessfulOutcomeOnActionForTheSuppliedGroups("progression.query.defendant", "System Users", "Court Clerks", "Crown Court Admin", "Listing Officers", "Judiciary", "Case Officer", "Court Clerks");
    }

    @Test
    public void shouldNotAllowUserInAuthorisedGroupToGetDefendant() {
        assertFailureOutcomeOnActionForTheSuppliedGroups("progression.query.defendant", "System Users", "Court Clerks", "Crown Court Admin", "Listing Officers", "Judiciary", "Case Officer", "Court Clerks");
    }

    @Test
    public void shouldAllowUserInAuthorisedGroupToGetDefendants() {
        assertSuccessfulOutcomeOnActionForTheSuppliedGroups("progression.query.defendants", "System Users", "Court Clerks", "Crown Court Admin", "Listing Officers", "Judiciary", "Case Officer", "Court Clerks");
    }

    @Test
    public void shouldNotAllowUserInAuthorisedGroupToGetDefendants() {
        assertFailureOutcomeOnActionForTheSuppliedGroups("progression.query.defendants", "System Users", "Court Clerks", "Crown Court Admin", "Listing Officers", "Judiciary", "Case Officer", "Court Clerks");
    }

    @Test
    public void shouldAllowUserInAuthorisedGroupToGetDefendantDocument() {
        assertSuccessfulOutcomeOnActionForTheSuppliedGroups("progression.query.defendant.document", "Crown Court Admin", "Listing Officers", "Judiciary", "Case Officer", "Court Clerks");
    }

    @Test
    public void shouldNotAllowUserInAuthorisedGroupToGetDefendantDocument() {
        assertFailureOutcomeOnActionForTheSuppliedGroups("progression.query.defendant.document", "Crown Court Admin", "Listing Officers", "Judiciary", "Case Officer", "Court Clerks");
    }

    @Test
    public void shouldAllowUserInAuthorisedGroupToGetMagistrateCourts() {
        assertSuccessfulOutcomeOnActionForTheSuppliedGroups("progression.query.crown-court.magistrate-courts", "Crown Court Admin", "Listing Officers", "Judiciary", "Case Officer", "Court Clerks");
    }

    @Test
    public void shouldNotAllowUserInAuthorisedGroupToGetMagistrateCourts() {
        assertFailureOutcomeOnActionForTheSuppliedGroups("progression.query.crown-court.magistrate-courts", "Crown Court Admin", "Listing Officers", "Judiciary", "Case Officer", "Court Clerks");
    }

    @Test
    public void shouldAllowUserInAuthorisedGroupToGetDefendantOffences() {
        assertSuccessfulOutcomeOnActionForTheSuppliedGroups("progression.query.defendant-offences", "Court Clerks", "Crown Court Admin", "Listing Officers", "Judiciary", "Case Officer", "Court Clerks");
    }

    @Test
    public void shouldNotAllowUserInAuthorisedGroupToGetDefendantOffences() {
        assertFailureOutcomeOnActionForTheSuppliedGroups("progression.query.defendant-offences", "Court Clerks", "Crown Court Admin", "Listing Officers", "Judiciary", "Case Officer", "Court Clerks");
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
        assertSuccessfulOutcomeOnActionForTheSuppliedGroups("progression.query.material-nows-content", "System Users", "Listing Officers", "Court Clerks", "Legal Advisers", "Prison Admin", "Probation Admin", "Police Admin", "Victims & Witness Care Admin", "Youth Offending Service Admin", "Legal Aid Agency Admin", "Probation Admin");
    }

    @Test
    public void shouldAllowUserInAuthorisedGroupToGetQueryHearing() {
        assertSuccessfulOutcomeOnActionForTheSuppliedGroups("progression.query.hearing", "System Users", "Court Clerks", "Crown Court Admin", "Listing Officers", "Court Administrators", "Legal Advisers", "Judiciary","Court Associate");
    }

    @Test
    public void shouldNotAllowUserInAuthorisedGroupToGetQueryMaterialContentNows() {
        assertFailureOutcomeOnActionForTheSuppliedGroups("progression.query.material-nows-content", "System Users", "Listing Officers", "Court Clerks", "Legal Advisers", "Prison Admin", "Probation Admin", "Police Admin", "Victims & Witness Care Admin",
                "Youth Offending Service Admin", "Legal Aid Agency Admin", "Probation Admin");
    }

    @Test
    public void shouldAllowUserInQueryAssociatedUser() {
        assertSuccessfulOutcomeOnActionForTheSuppliedGroups("progression.query.associated-organisation", "System Users", "Defence Users", "Court Clerks", "Court Administrators", "Crown Court Admin", "Listing Officers", "Legal Advisers", "Judiciary","Court Associate", "Deputies", "DJMC", "Judge");
    }

    @Test
    public void shouldAllowUserInQueryDefendantsByLAAContractNumber() {
        assertSuccessfulOutcomeOnActionForTheSuppliedGroups("progression.query.defendants-by-laacontractnumber", "System Users", "Defence Users", "Court Clerks", "Court Administrators", "Crown Court Admin", "Listing Officers", "Legal Advisers");
    }

    @Override
    protected Map<Class, Object> getProviderMocks() {
        return ImmutableMap.<Class, Object>builder().put(UserAndGroupProvider.class, this.userAndGroupProvider).put(ProgressionProvider.class, this.progressionProvider).put(CourtDocumentProvider.class, this.caseDocumentProvider).put(RbacProvider.class, this.rbacProvider).build();
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
