package uk.gov.moj.cpp.progression.command;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;

import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher;
import uk.gov.moj.cpp.progression.aggregate.DefenceAssociationAggregate;
import uk.gov.moj.cpp.progression.command.handler.service.UsersGroupService;
import uk.gov.moj.cpp.progression.command.handler.service.payloads.OrganisationDetails;
import uk.gov.moj.cpp.progression.events.DefenceOrganisationAssociated;
import uk.gov.moj.cpp.progression.handler.AssociateDefenceOrganisationHandler;

import java.util.UUID;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AssociateDefenceOrganisationHandlerTest {

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            DefenceOrganisationAssociated.class);

    @Mock
    private UsersGroupService usersGroupService;

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private Requester requester;

    @Mock
    private AggregateService aggregateService;

    @InjectMocks
    private AssociateDefenceOrganisationHandler associateDefenceOrganisationHandler;

    private DefenceAssociationAggregate aggregate;

    private static final String COMMAND_HANDLER_DEFENCE_ASSOCIATION_NAME = "progression.command.handler.associate-defence-organisation";

    private static final String ORGANISATION_NAME = "CompanyZ";


    @Before
    public void setup() {
        aggregate = new DefenceAssociationAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, DefenceAssociationAggregate.class)).thenReturn(aggregate);
    }

    @Test
    public void shouldHandleCommand() {
        assertThat(new AssociateDefenceOrganisationHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handle")
                        .thatHandles(COMMAND_HANDLER_DEFENCE_ASSOCIATION_NAME)
                ));
    }

    @Test
    public void shouldProcessCommandSucessfully() throws Exception {

        //Given
        final UUID userId = UUID.randomUUID();
        final OrganisationDetails ORGANISATION_1 = createOrganisation(UUID.randomUUID());


        final AssociateDefenceOrganisation associateDefenceOrganisation
                = generateAssociateDefenceOrganisationCommand(ORGANISATION_1.getId());
        final Envelope<AssociateDefenceOrganisation> envelope = createDefenceAssociationEnvelope(userId, associateDefenceOrganisation);
        final JsonEnvelope queryResponse = getUserGroupsQueryResponse(userId, ORGANISATION_1.getId());
        when(usersGroupService.getUserOrgDetails(any())).thenReturn(ORGANISATION_1);


        //When
        associateDefenceOrganisationHandler.handle(envelope);

        //Then
        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.defence-organisation-associated"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.organisationId", notNullValue()))
                        ))

                )
        );
    }

    private OrganisationDetails createOrganisation(final UUID orgId) {
        return OrganisationDetails.newBuilder().withId(orgId).withName(ORGANISATION_NAME).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldProcessCommandNegatively() throws Exception {

        //Given - When a Non Expected Organisation Name is passed in....
        UUID userId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        final AssociateDefenceOrganisation associateDefenceOrganisation
                = generateAssociateDefenceOrganisationCommand(orgId);

        final Envelope<AssociateDefenceOrganisation> envelope = createDefenceAssociationEnvelope(userId, associateDefenceOrganisation);
        final JsonEnvelope queryResponse = getUserGroupsQueryResponse(userId, UUID.randomUUID());

        when(usersGroupService.getUserOrgDetails(any())).thenReturn(OrganisationDetails.newBuilder().withId(randomUUID()).build());

        //When
        associateDefenceOrganisationHandler.handle(envelope);

        //Then - The IllegalArgumentException is expected...
    }

    private JsonEnvelope getUserGroupsQueryResponse(final UUID userId, final UUID orgId) {
        return JsonEnvelope.envelopeFrom(
                metadataBuilder()
                        .withId(randomUUID())
                        .withName(COMMAND_HANDLER_DEFENCE_ASSOCIATION_NAME)
                        .withUserId(userId.toString())
                        .build(),
                createObjectBuilder()
                        .add("organisationId", orgId.toString())
                        .add("organisationName", ORGANISATION_NAME)
                        .build()
        );
    }

    private Envelope<AssociateDefenceOrganisation> createDefenceAssociationEnvelope(final UUID userId, final AssociateDefenceOrganisation associateDefenceOrganisation) {
        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName(COMMAND_HANDLER_DEFENCE_ASSOCIATION_NAME)
                .withId(randomUUID())
                .withUserId(userId.toString())
                .build();

        return envelopeFrom(metadata, associateDefenceOrganisation);
    }

    private AssociateDefenceOrganisation generateAssociateDefenceOrganisationCommand(UUID orgId) {
        return AssociateDefenceOrganisation.associateDefenceOrganisation()
                .withDefendantId(randomUUID())
                .withRepresentationType(RepresentationType.PRIVATE_FUNDED)
                .withOrganisationId(orgId)
                .build();
    }

}