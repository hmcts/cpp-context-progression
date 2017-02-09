package uk.gov.moj.cpp.progression.command.rules;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import com.google.common.collect.ImmutableMap;

import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

public class SentenceHearingDateTest extends BaseDroolsAccessControlTest {

    private static final List<String> ALLOWED_USER_GROUPS = Arrays.asList(
                    "Crown Court Admin", "Listing Officers");

    private static final String MEDIA_TYPE = "progression.command.sentence-hearing-date";
    private Action action;

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    @Captor
    private ArgumentCaptor<List<String>> listCaptor;

    @Override
    protected Map<Class, Object> getProviderMocks() {
        return ImmutableMap.<Class, Object>builder()
                        .put(UserAndGroupProvider.class, userAndGroupProvider).build();
    }

    @Before
    public void setUp() throws Exception {
        action = createActionFor(MEDIA_TYPE);
    }

    @After
    public void tearDown() throws Exception {
        verifyNoMoreInteractions(userAndGroupProvider);
    }



    @Test
    public void shouldPassAccessControl() throws Exception {
        when(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, ALLOWED_USER_GROUPS))
                        .thenReturn(true);

        final ExecutionResults executionResults = executeRulesWith(action);

        assertSuccessfulOutcome(executionResults);
        verifyListOfUserGroups();
    }

    @Test
    public void shouldNotPassAccessControl() throws Exception {
        when(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, ALLOWED_USER_GROUPS))
                        .thenReturn(false);

        final ExecutionResults executionResults = executeRulesWith(action);

        assertFailureOutcome(executionResults);
        verifyListOfUserGroups();
    }

    private void verifyListOfUserGroups() {
        verify(userAndGroupProvider).isMemberOfAnyOfTheSuppliedGroups(eq(action),
                        listCaptor.capture());
        assertThat(listCaptor.getValue(), containsInAnyOrder(ALLOWED_USER_GROUPS.toArray()));
    }
}
