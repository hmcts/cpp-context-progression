package uk.gov.moj.cpp.progression.command.api.accesscontrol;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataOf;

import com.google.common.collect.ImmutableMap;
import uk.gov.justice.api.resource.CourtDocumentCommandProvider;
import uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder;
import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.refdata.providers.RbacProvider;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class PermissionConstantsTest extends BaseDroolsAccessControlTest {

    private Action action;

    @Mock
    private UserAndGroupProvider userAndGroupProvider;
    @Mock
    private RbacProvider rbacProvider;
    @Mock
    private CourtDocumentCommandProvider courtDocumentCommandProvider;

    public PermissionConstantsTest() {
        super("COMMAND_API_SESSION");
    }

    @Test
    public void shouldAllowAuthorisedUserToCourtProceedingsForApplication() throws JsonProcessingException {
        final Map<String, String> metadata = new HashMap();
        metadata.putIfAbsent("id", UUID.randomUUID().toString());
        metadata.putIfAbsent("name", "progression.initiate-court-proceedings-for-application");
        action = createActionFor(metadata);
        given(this.userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, PermissionConstants.getUsersForCourtProceedingsForApplication())).willReturn(true);
        given(userAndGroupProvider.hasPermission(action, PermissionConstants.getCaseCreatePermission())).willReturn(true);
        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
        verify(userAndGroupProvider, times(1)).isMemberOfAnyOfTheSuppliedGroups(action, PermissionConstants.getUsersForCourtProceedingsForApplication());
        verify(userAndGroupProvider, times(1)).hasPermission(action, PermissionConstants.getCaseCreatePermission());

    }

    @Test
    public void shouldAllowAuthorisedUserWithPermissionToCourtProceedingsForApplication() throws JsonProcessingException {
        final Map<String, String> metadata = new HashMap();
        metadata.putIfAbsent("id", UUID.randomUUID().toString());
        metadata.putIfAbsent("name", "progression.initiate-court-proceedings-for-application");
        action = createActionFor(metadata);
        given(this.userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, PermissionConstants.getUsersForCourtProceedingsForApplication())).willReturn(false);
        given(userAndGroupProvider.hasPermission(action, PermissionConstants.getCaseCreatePermission())).willReturn(true);
        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
        verify(userAndGroupProvider, times(1)).isMemberOfAnyOfTheSuppliedGroups(action, PermissionConstants.getUsersForCourtProceedingsForApplication());
        verify(userAndGroupProvider, times(1)).hasPermission(action, PermissionConstants.getCaseCreatePermission());

    }

    @Test
    public void shouldNotAllowUnauthorisedUserToCourtProceedingsForApplication() throws JsonProcessingException {
        final Map<String, String> metadata = new HashMap();
        metadata.putIfAbsent("id", UUID.randomUUID().toString());
        metadata.putIfAbsent("name", "progression.initiate-court-proceedings-for-application");
        action = createActionFor(metadata);
        given(this.userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, PermissionConstants.getUsersForCourtProceedingsForApplication())).willReturn(false);
        given(userAndGroupProvider.hasPermission(action, PermissionConstants.getCaseCreatePermission())).willReturn(false);

        final ExecutionResults results = executeRulesWith(action);

        assertFailureOutcome(results);
        verify(userAndGroupProvider, times(1)).isMemberOfAnyOfTheSuppliedGroups(action, PermissionConstants.getUsersForCourtProceedingsForApplication());
        verify(userAndGroupProvider, times(1)).hasPermission(action, PermissionConstants.getCaseCreatePermission());
    }

    @Test
    public void shouldAllowAutomationUserToDeleteCourtDocument() throws JsonProcessingException {
        final Action action = createActionFor("progression.remove-court-document");
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, "Court Administrators", "NCES")).willReturn(true);

        final ExecutionResults results = executeRulesWith(action);

        assertSuccessfulOutcome(results);
        verify(userAndGroupProvider, times(1)).isMemberOfAnyOfTheSuppliedGroups(action, "Court Administrators", "NCES");
    }

    @Test
    public void shouldNotAllowNonAutomationUserToDeleteCourtDocument() throws JsonProcessingException {
        final Action action = createActionFor("progression.remove-court-document");
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, "Court Administrators", "NCES")).willReturn(false);

        final ExecutionResults results = executeRulesWith(action);

        assertFailureOutcome(results);
        verify(userAndGroupProvider, times(1)).isMemberOfAnyOfTheSuppliedGroups(action, "Court Administrators", "NCES");
    }

    @Test
    public void shouldAllowAuthorisedUserToDeleteCourtDocument() throws JsonProcessingException {
        final Action action = createActionFor("progression.remove-court-document");
        given(userAndGroupProvider.hasPermission(action, PermissionConstants.getDeleteCourtDocumentPermission())).willReturn(true);
        given(courtDocumentCommandProvider.getDocumentTypeId(action)).willReturn(action);
        given(rbacProvider.isLoggedInUserAllowedToUploadDocument(action)).willReturn(true);

        final ExecutionResults results = executeRulesWith(action);

        assertSuccessfulOutcome(results);
        verify(userAndGroupProvider, times(1)).hasPermission(action, PermissionConstants.getDeleteCourtDocumentPermission());
    }

    @Test
    public void shouldNotAllowUserToDeleteCourtDocument_whenTheyLackPermission() throws JsonProcessingException {
        final Action action = createActionFor("progression.remove-court-document");
        given(userAndGroupProvider.hasPermission(action, PermissionConstants.getDeleteCourtDocumentPermission())).willReturn(false);

        final ExecutionResults results = executeRulesWith(action);

        assertFailureOutcome(results);
        verify(userAndGroupProvider, times(1)).hasPermission(action, PermissionConstants.getDeleteCourtDocumentPermission());
    }

    @Test
    public void shouldNotAllowUserToDeleteCourtDocument_whenTheyCannotUploadToSection() throws JsonProcessingException {
        final Action action = createActionFor("progression.remove-court-document");
        given(userAndGroupProvider.hasPermission(action, PermissionConstants.getDeleteCourtDocumentPermission())).willReturn(true);
        given(courtDocumentCommandProvider.getDocumentTypeId(action)).willReturn(action);
        given(rbacProvider.isLoggedInUserAllowedToUploadDocument(action)).willReturn(false);

        final ExecutionResults results = executeRulesWith(action);

        assertFailureOutcome(results);
        verify(userAndGroupProvider, times(1)).hasPermission(action, PermissionConstants.getDeleteCourtDocumentPermission());
    }

    @Override
    protected Map<Class<?>, Object> getProviderMocks() {
        return ImmutableMap.<Class<?>, Object>builder()
                .put(UserAndGroupProvider.class, userAndGroupProvider)
                .put(RbacProvider.class, rbacProvider)
                .put(CourtDocumentCommandProvider.class, courtDocumentCommandProvider)
                .build();
    }

    @Override
    protected Action createActionFor(final Map<String, String> metadata) {
        JsonEnvelopeBuilder jsonEnvelopeBuilder = JsonEnvelopeBuilder.envelope().withPayloadOf(UUID.randomUUID().toString(), "caseId");
        return new Action(jsonEnvelopeBuilder.with(metadataOf(UUID.randomUUID().toString(), metadata.get("name"))).build());
    }
}