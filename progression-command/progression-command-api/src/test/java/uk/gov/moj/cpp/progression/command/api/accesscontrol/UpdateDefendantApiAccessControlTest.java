package uk.gov.moj.cpp.progression.command.api.accesscontrol;

import static org.mockito.BDDMockito.given;

import java.util.Arrays;
import java.util.Map;

import org.junit.Test;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.Mock;

import com.google.common.collect.ImmutableMap;

import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;
@Deprecated
public class UpdateDefendantApiAccessControlTest extends BaseDroolsAccessControlTest {

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    @Test
    public void shouldAllowUserInAuthorisedGroupToUpdateOffencesForDefendant() {
        final Action action = createActionFor("progression.command.update-defendant");
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, TestRuleConstants.getUpdateDefendantActionGroups()))
                .willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldNotAllowUserNotInAuthorisedGroupToUpdateOffencesForDefendant() {
        final Action action = createActionFor("progression.command.update-defendant");
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, Arrays.asList("test")))
                .willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
    }


    @Override
    protected Map<Class, Object> getProviderMocks() {
        return ImmutableMap.<Class, Object>builder().put(UserAndGroupProvider.class, userAndGroupProvider).build();
    }
}
