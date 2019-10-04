package uk.gov.moj.cpp.progression.command;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;

import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher;
import uk.gov.moj.cpp.progression.aggregate.DefenceAssociationAggregate;
import uk.gov.moj.cpp.progression.command.handler.service.UsersGroupService;
import uk.gov.moj.cpp.progression.command.handler.service.payloads.OrganisationDetails;
import uk.gov.moj.cpp.progression.command.handler.service.payloads.UserGroupDetails;
import uk.gov.moj.cpp.progression.events.DefenceOrganisationDisassociated;
import uk.gov.moj.cpp.progression.handler.DisassociateDefenceOrganisationHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DisassociateDefenceOrganisationHandlerTest {

    private static final String COMMAND_HANDLER_DEFENCE_DISASSOCIATION_NAME = "progression.command.handler.disassociate-defence-organisation";
    private static final String ORGANISATION_NAME = "CompanyZ";
    private static final String LEGAL_ORGANISATION = "LEGAL_ORGANISATION";
    private static final String HMCTS_ORG = "HMCTS";
    private static final String MAGISTRATES_ORG = "MAGISTRATES";
    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            DefenceOrganisationDisassociated.class);
    @Mock
    private UsersGroupService usersGroupService;
    @Mock
    private EventSource eventSource;
    @Mock
    private EventStream eventStream;
    @Mock
    private AggregateService aggregateService;
    @InjectMocks
    private DisassociateDefenceOrganisationHandler disassociateDefenceOrganisationHandler;
    private DefenceAssociationAggregate aggregate;

    @Before
    public void setup() {
        aggregate = new DefenceAssociationAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, DefenceAssociationAggregate.class)).thenReturn(aggregate);
    }

    @Test
    public void shouldHandleCommand() {
        assertThat(new DisassociateDefenceOrganisationHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handle")
                        .thatHandles(COMMAND_HANDLER_DEFENCE_DISASSOCIATION_NAME)
                ));
    }


    @Test
    public void shouldProcessCommandSucessfullyForTheMatchingOrgIds() throws Exception {

        //Given
        final Envelope<DisassociateDefenceOrganisation> envelope = prepareDisassociateCommandAndEnvelopeForTest();
        //When
        disassociateDefenceOrganisationHandler.handle(envelope);
        //Then
        assertHandlerResults();
    }

    @Test
    public void shouldProcessCommandSuccessiveleyForTheOrgTypeHMCTS() throws Exception {
        //Given
        final Envelope<DisassociateDefenceOrganisation> envelope = prepareDisassociateCommandAndEnvelopeForTestWithDifferingOrgIdsOrgTypeHMCTS();
        //When
        disassociateDefenceOrganisationHandler.handle(envelope);
        //Then
        assertHandlerResults();
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldProcessCommandNegativelyForTheOrgTypeNonHMCTS() throws Exception {
        //Given
        final Envelope<DisassociateDefenceOrganisation> envelope = prepareDisassociateCommandAndEnvelopeForTestForDifferingOrgIdsOrgTypeNonHMCTS();
        //When
        disassociateDefenceOrganisationHandler.handle(envelope);
    }

    private Envelope<DisassociateDefenceOrganisation> prepareDisassociateCommandAndEnvelopeForTest() {

        final UUID userId = UUID.randomUUID();
        final OrganisationDetails organisationDetails = createOrganisation(UUID.randomUUID(), LEGAL_ORGANISATION);
        final DisassociateDefenceOrganisation disassociateDefenceOrganisation
                = generateDisassociateDefenceOrganisationCommand(organisationDetails.getId());
        final Envelope<DisassociateDefenceOrganisation> envelope
                = createDefenceDisassociationEnvelope(userId, disassociateDefenceOrganisation);
        final List<UserGroupDetails> defenceUserGroupDetails = this.createDefenceUserGroupDetails();
        when(usersGroupService.getUserOrgDetails(any())).thenReturn(organisationDetails);
        when(usersGroupService.getUserGroupsForUser(any())).thenReturn(defenceUserGroupDetails);
        return envelope;
    }

    private Envelope<DisassociateDefenceOrganisation> prepareDisassociateCommandAndEnvelopeForTestWithDifferingOrgIdsOrgTypeHMCTS() {

        final UUID userId = UUID.randomUUID();
        final OrganisationDetails organisationDetails = createOrganisation(UUID.randomUUID(), HMCTS_ORG);
        final DisassociateDefenceOrganisation disassociateDefenceOrganisation
                = generateDisassociateDefenceOrganisationCommand(UUID.randomUUID());
        final Envelope<DisassociateDefenceOrganisation> envelope
                = createDefenceDisassociationEnvelope(userId, disassociateDefenceOrganisation);
        final List<UserGroupDetails> hmctsUserGroupDetails = this.createHMCTSUserGroupDetails();
        when(usersGroupService.getUserOrgDetails(any())).thenReturn(organisationDetails);
        when(usersGroupService.getUserGroupsForUser(any())).thenReturn(hmctsUserGroupDetails);
        return envelope;
    }

    private Envelope<DisassociateDefenceOrganisation> prepareDisassociateCommandAndEnvelopeForTestForDifferingOrgIdsOrgTypeNonHMCTS() {

        final UUID userId = UUID.randomUUID();
        final OrganisationDetails organisationDetails = createOrganisation(UUID.randomUUID(), MAGISTRATES_ORG);
        final DisassociateDefenceOrganisation disassociateDefenceOrganisation
                = generateDisassociateDefenceOrganisationCommand(UUID.randomUUID());
        final Envelope<DisassociateDefenceOrganisation> envelope
                = createDefenceDisassociationEnvelope(userId, disassociateDefenceOrganisation);
        final List<UserGroupDetails> systemUserGroupDetails = this.createSystemUserGroupDetails();
        when(usersGroupService.getUserOrgDetails(any())).thenReturn(organisationDetails);
        when(usersGroupService.getUserGroupsForUser(any())).thenReturn(systemUserGroupDetails);
        return envelope;
    }

    private Envelope<DisassociateDefenceOrganisation> createDefenceDisassociationEnvelope(final UUID userId,
                                                                                          final DisassociateDefenceOrganisation disassociateDefenceOrganisation) {
        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName(COMMAND_HANDLER_DEFENCE_DISASSOCIATION_NAME)
                .withId(randomUUID())
                .withUserId(userId.toString())
                .build();

        return envelopeFrom(metadata, disassociateDefenceOrganisation);
    }

    private DisassociateDefenceOrganisation generateDisassociateDefenceOrganisationCommand(final UUID orgId) {
        return DisassociateDefenceOrganisation.disassociateDefenceOrganisation()
                .withDefendantId(randomUUID())
                .withOrganisationId(orgId)
                .build();
    }

    private OrganisationDetails createOrganisation(final UUID orgId, final String organisationType) {
        return OrganisationDetails.newBuilder()
                .withId(orgId)
                .withName(ORGANISATION_NAME)
                .withType(organisationType)
                .build();
    }

    private void assertHandlerResults() throws EventStreamException {
        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.defence-organisation-disassociated"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.organisationId", notNullValue()))
                        ))

                )
        );
    }

    private List<UserGroupDetails> createHMCTSUserGroupDetails() {

        UserGroupDetails userGroupListingOfficers = new UserGroupDetails(UUID.fromString("7e2f143e-d619-40b3-8611-8015f3a18957"), "Listing Officers");
        UserGroupDetails userGroupListingCourtClerks = new UserGroupDetails(UUID.fromString("8c5327b6-354e-4574-9558-b13fce8c055a"), "Court Clerks");
        List<UserGroupDetails> destinedUserGroups = new ArrayList<>();
        destinedUserGroups.add(userGroupListingOfficers);

        return destinedUserGroups;
    }

    private List<UserGroupDetails> createDefenceUserGroupDetails() {
        UserGroupDetails userGroupDefenceUsers = new UserGroupDetails(UUID.fromString("7e2f143e-d619-40b3-8611-8015f3a18957"), "Defence Users");
        List<UserGroupDetails> destinedUserGroups = new ArrayList<>();
        destinedUserGroups.add(userGroupDefenceUsers);
        return destinedUserGroups;
    }

    private List<UserGroupDetails> createSystemUserGroupDetails() {
        UserGroupDetails userSystemUsers = new UserGroupDetails(UUID.fromString("7e2f143e-d619-40b3-8611-8015f3a18957"), "System Users");
        List<UserGroupDetails> destinedUserGroups = new ArrayList<>();
        destinedUserGroups.add(userSystemUsers);
        return destinedUserGroups;
    }
}