package uk.gov.moj.cpp.progression.command.api.accesscontrol;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.Mock;
import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.Arrays;
import java.util.Map;

import static org.mockito.BDDMockito.given;
import static uk.gov.moj.cpp.progression.command.api.accesscontrol.RuleConstants.getUpdateInterpreterorDefendantActionGroups;

public class UpdateDefendantInterpreterTest extends BaseDroolsAccessControlTest {

    private static final String PROGRESSION_COMMAND_UPDATE_INTERPRETER_FOR_DEFENDANT = "progression.command.update-interpreter-for-defendant";
    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    @Test
    public void shouldAllowUserInAuthorisedGroupToUpdateDefendantInterpreter() {
        final Action action = createActionFor(PROGRESSION_COMMAND_UPDATE_INTERPRETER_FOR_DEFENDANT);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, getUpdateInterpreterorDefendantActionGroups()))
                .willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldNotAllowUserNotInAuthorisedGroupToUpdateDefendantInterpreter() {
        final Action action = createActionFor(PROGRESSION_COMMAND_UPDATE_INTERPRETER_FOR_DEFENDANT);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, Arrays.asList("random group")))
                .willReturn(false);

        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
    }

    @Override
    protected Map<Class, Object> getProviderMocks() {
        return ImmutableMap.<Class, Object>builder().put(UserAndGroupProvider.class, userAndGroupProvider).build();
    }
}
