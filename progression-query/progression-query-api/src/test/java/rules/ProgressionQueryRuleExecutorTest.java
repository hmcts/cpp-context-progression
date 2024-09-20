package rules;

import static java.util.Collections.singletonMap;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataOf;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;
import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProgressionQueryRuleExecutorTest extends BaseDroolsAccessControlTest {

    protected Action action;
    @Mock
    protected UserAndGroupProvider userAndGroupProvider;


    public ProgressionQueryRuleExecutorTest() {
        super("QUERY_API_SESSION");
    }

    @Override
    protected Map<Class<?>, Object> getProviderMocks() {
        return singletonMap(UserAndGroupProvider.class, userAndGroupProvider);
    }

    @Test
    public void whenUserIsAMemberOfAllowedUserGroups() {
        Arrays.stream(ProgressionRules.values()).forEach(ruleTest -> {
            action = createActionFor(ruleTest.actionName);
            when(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, ruleTest.allowedUserGroups)).thenReturn(true);
            final ExecutionResults executionResults = executeRulesWith(action);
            assertSuccessfulOutcome(executionResults);
            verify(userAndGroupProvider).isMemberOfAnyOfTheSuppliedGroups(action, ruleTest.allowedUserGroups);
            verifyNoMoreInteractions(userAndGroupProvider);
        });
    }


    @Test
    public void whenUserIsNotAMemberOfAllowedUserGroups_thenFailUpload() throws Exception {
        Arrays.stream(ProgressionRules.values()).forEach(ruleTest -> {
            Map<String, String> metadata = new HashMap();
            metadata.putIfAbsent("id", UUID.randomUUID().toString());
            metadata.putIfAbsent("name", ruleTest.actionName);
            final JsonEnvelope envelope = JsonEnvelopeBuilder.envelope().with(MetadataBuilderFactory.metadataOf(UUID.randomUUID().toString(), (String)metadata.get("name"))).withPayloadOf(UUID.randomUUID().toString(),"defenceClientId").build();
            action = new Action(envelope);
            when(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, ruleTest.allowedUserGroups)).thenReturn(false);
            final ExecutionResults executionResults = executeRulesWith(action);
            assertFailureOutcome(executionResults);
            verify(userAndGroupProvider).isMemberOfAnyOfTheSuppliedGroups(action, ruleTest.allowedUserGroups);
        });
    }

    public enum ProgressionRules {
        GetProgressionCaseQuery("progression.query.case", new String[]{"Defence Lawyers", "CPS", "Court Associates",  "System Users","Court Clerks", "Crown Court Admin", "Listing Officers", "Court Administrators", "Legal Advisers", "Probation Admin", "Judiciary", "Court Associate", "Deputies", "DJMC", "Judge",
                "Youth Offending Service Admin", "Magistrates", "Advocates", "District Judge", "Second Line Support", "Police Admin", "NCES", "Victims & Witness Care Admin", "Recorders", "Eject Case Group", "Online Plea System Users", "Non CPS Prosecutors"});
        private final String actionName;
        private final String[] allowedUserGroups;
        ProgressionRules(final String actionName, final String... allowedUserGroups) {
            this.actionName = actionName;
            this.allowedUserGroups = allowedUserGroups;
        }
    }

    @Override
    protected Action createActionFor(final Map<String, String> metadata) {
        JsonEnvelopeBuilder jsonEnvelopeBuilder = JsonEnvelopeBuilder.envelope().withPayloadOf( UUID.randomUUID().toString(),"caseId");;
        return new Action(jsonEnvelopeBuilder.with(metadataOf(UUID.randomUUID().toString(), metadata.get("name"))).build());
    }
}
