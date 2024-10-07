package uk.gov.moj.cpp.progression.command.api.accesscontrol;

import static org.mockito.Mockito.lenient;
import static uk.gov.moj.cpp.progression.command.api.accesscontrol.TestRuleConstants.getAddDefendantActionGroups;

import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.Arrays;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.Mock;

@Deprecated
public class AddDefendantTest extends BaseDroolsAccessControlTest {

    private static final String PROGRESSION_COMMAND_ADD_DEFENDANT = "progression.command.add-defendant";

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    public AddDefendantTest() {
        super("COMMAND_API_SESSION");
    }

    @Test
    public void shouldAllowUserInAuthorisedGroupToAddDefendant() {
        final Action action = createActionFor(PROGRESSION_COMMAND_ADD_DEFENDANT);
        lenient().when(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, getAddDefendantActionGroups())).thenReturn(true);
        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldNotAllowUserNotInAuthorisedGroupToAddDefendant() {
        final Action action = createActionFor(PROGRESSION_COMMAND_ADD_DEFENDANT);
        lenient().when(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, Arrays.asList("test"))).thenReturn(true);
        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
    }


    @Override
    protected Map<Class<?>, Object> getProviderMocks() {
        return ImmutableMap.<Class<?>, Object>builder().put(UserAndGroupProvider.class, userAndGroupProvider).build();
    }
}
