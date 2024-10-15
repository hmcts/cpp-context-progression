package uk.gov.moj.cpp.progression.command.api.accesscontrol;

import static org.mockito.BDDMockito.given;

import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.Mock;
@Deprecated
public class UpdateOffencesForDefendantApiAccessControlTest extends BaseDroolsAccessControlTest {

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    public UpdateOffencesForDefendantApiAccessControlTest() {
        super("COMMAND_API_SESSION");
    }

    @Test
    public void shouldAllowUserInAuthorisedGroupToUpdateOffencesForDefendant() {
        final Action action = createActionFor("progression.command.update-offences-for-defendant");
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, TestRuleConstants.getUpdateOffencesForDefendantActionGroups()))
                .willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldNotAllowUserNotInAuthorisedGroupToUpdateOffencesForDefendant() {
        final Action action = createActionFor("progression.command.update-offences-for-defendant");

        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
    }


    @Override
    protected Map<Class<?>, Object> getProviderMocks() {
        return ImmutableMap.<Class<?>, Object>builder().put(UserAndGroupProvider.class, userAndGroupProvider).build();
    }
}
