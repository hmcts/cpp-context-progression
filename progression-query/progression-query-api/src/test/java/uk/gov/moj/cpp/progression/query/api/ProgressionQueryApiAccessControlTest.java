package uk.gov.moj.cpp.progression.query.api;

import com.google.common.collect.ImmutableMap;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.progression.providers.ProgressionProvider;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

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

    @Test
    public void shouldAllowUserInAuthorisedGroupToGetProsecutioncase() {
        assertSuccessfulOutcomeOnActionForTheSuppliedGroups("progression.query.prosecutioncase", "System Users", "Court Clerks", "Crown Court Admin", "Listing Officers", "Court Administrators", "Legal Advisers");
    }

    @Test
    public void shouldNotAllowUserInAuthorisedGroupToGetProsecutioncase() {
        assertFailureOutcomeOnActionForTheSuppliedGroups("progression.query.prosecutioncase", "System Users", "Court Clerks", "Crown Court Admin", "Listing Officers", "Court Administrators", "Legal Advisers");
    }

    @Test
    public void shouldAllowUserInAuthorisedGroupToGetMaterialContent() {
        assertSuccessfulOutcomeOnActionForTheSuppliedGroups("progression.query.material-content", "System Users" ,"Court Clerks",  "Crown Court Admin", "Listing Officers", "Judiciary", "Case Officer", "Court Clerks", "Legal Advisers");
    }

    @Test
    public void shouldNotAllowUserInAuthorisedGroupToGetMaterialContent() {
        assertFailureOutcomeOnActionForTheSuppliedGroups("progression.query.material-content", "System Users" ,"Court Clerks",  "Crown Court Admin", "Listing Officers", "Judiciary", "Case Officer", "Court Clerks", "Legal Advisers");
    }

    @Test
    public void shouldAllowUserInAuthorisedGroupToGetProgressionCases() {
        assertSuccessfulOutcomeOnActionForTheSuppliedGroups("progression.query.cases", "System Users", "Crown Court Admin", "Listing Officers", "Judiciary", "Case Officer", "Court Clerks");
    }

    @Test
    public void shouldNotAllowUserInAuthorisedGroupToGetProgressionCases() {
        assertFailureOutcomeOnActionForTheSuppliedGroups("progression.query.cases", "System Users", "Crown Court Admin", "Listing Officers", "Judiciary", "Case Officer", "Court Clerks");
    }

    @Test
    public void shouldAllowUserInAuthorisedGroupToSearchCases() {
        assertSuccessfulOutcomeOnActionForTheSuppliedGroups("progression.query.search-cases", "Court Clerks", "Crown Court Admin", "Listing Officers", "Court Administrators", "Legal Advisers");
    }

    @Test
    public void shouldNotAllowUserInAuthorisedGroupToSearchCases() {
        assertFailureOutcomeOnActionForTheSuppliedGroups("progression.query.search-cases", "Court Clerks", "Crown Court Admin", "Listing Officers", "Court Administrators", "Legal Advisers");
    }

    @Test
    public void shouldAllowUserInAuthorisedGroupToProgressionCaseDetail() {
        assertSuccessfulOutcomeOnActionForTheSuppliedGroups("progression.query.caseprogressiondetail", "System Users", "Court Clerks", "Crown Court Admin", "Listing Officers", "Judiciary", "Case Officer", "Court Clerks");
    }

    @Test
    public void shouldNotAllowUserInAuthorisedGroupToProgressionCaseDetail() {
        assertFailureOutcomeOnActionForTheSuppliedGroups("progression.query.caseprogressiondetail", "System Users", "Court Clerks", "Crown Court Admin", "Listing Officers", "Judiciary", "Case Officer", "Court Clerks");
    }

    @Test
    public void shouldAllowUserInAuthorisedGroupToGetCaseByUrn() {
        assertSuccessfulOutcomeOnActionForTheSuppliedGroups("progression.query.case-by-urn", "System Users", "Court Clerks", "Court Clerks", "System Users", "CMS", "Crown Court Admin", "Listing Officers", "Judiciary", "Case Officer");
    }

    @Test
    public void shouldNotAllowUserInAuthorisedGroupToGetCaseByUrn() {
        assertFailureOutcomeOnActionForTheSuppliedGroups("progression.query.case-by-urn", "System Users", "Court Clerks", "Court Clerks", "System Users", "CMS", "Crown Court Admin", "Listing Officers", "Judiciary", "Case Officer");
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
        assertSuccessfulOutcomeOnActionForTheSuppliedGroups("progression.query.material-nows-content", "System Users", "Listing Officers", "Court Clerks", "Legal Advisers", "Prison Admin", "Probation Admin", "Police Admin", "Victims & Witness Care Admin", "Youth Offending Service Admin", "Legal Aid Agency Admin");
    }

    @Test
    public void shouldNotAllowUserInAuthorisedGroupToGetQueryMaterialContentNows() {
        assertFailureOutcomeOnActionForTheSuppliedGroups("progression.query.material-nows-content", "System Users", "Listing Officers", "Court Clerks", "Legal Advisers", "Prison Admin","Probation Admin", "Police Admin", "Victims & Witness Care Admin",
 "Youth Offending Service Admin", "Legal Aid Agency Admin");
    }

    @Override
    protected Map<Class, Object> getProviderMocks() {
        return ImmutableMap.<Class, Object>builder().put(UserAndGroupProvider.class, this.userAndGroupProvider).put(ProgressionProvider.class, this.progressionProvider).build();
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

}
