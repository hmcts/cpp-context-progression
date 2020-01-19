package uk.gov.moj.cpp.progression.command.api.accesscontrol;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.progression.providers.ProgressionProvider;
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

public class ProgressionCommandApiAccessControlTest extends BaseDroolsAccessControlTest  {

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    @Captor
    private ArgumentCaptor<String[]> arrayCaptor;

    @Mock
    private ProgressionProvider progressionProvider;

    private static final List<String> ALLOWED_USER_GROUPS = Arrays.asList(
            "Court Clerks",
            "Crown Court Admin",
            "Listing Officers",
            "Court Administrators",
            "Legal Advisers",
            "Judiciary",
            "Court Associate",
            "Deputies",
            "DJMC",
            "Judge"
    );

    @Override
    protected Map<Class, Object> getProviderMocks() {
        return ImmutableMap.<Class, Object>builder().put(UserAndGroupProvider.class, this.userAndGroupProvider).put(ProgressionProvider.class, this.progressionProvider).build();
    }

    @Test
    public void shouldAllowUserInAuthorisedGroupToExtendHearing() {
        assertSuccessfulOutcomeOnActionForTheSuppliedGroups("progression.extend-hearing", "Court Clerks", "Crown Court Admin", "Listing Officers", "Court Administrators", "Legal Advisers", "Judiciary", "Court Associate", "Deputies", "DJMC", "Judge");
    }

    @Test
    public void shouldNotAllowUserInAuthorisedGroupToExtendHearing() {
        assertFailureOutcomeOnActionForTheSuppliedGroups("progression.extend-hearing", "Court Clerks", "Crown Court Admin", "Listing Officers", "Court Administrators", "Legal Advisers", "Judiciary", "Court Associate", "Deputies", "DJMC", "Judge");
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
}
