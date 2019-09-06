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
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher;
import uk.gov.moj.cpp.progression.aggregate.DefenceAssociationAggregate;
import uk.gov.moj.cpp.progression.events.DefenceOrganisationAssociated;
import uk.gov.moj.cpp.progression.handler.AssociateDefenceOrganisationHandler;

import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
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
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @InjectMocks
    private AssociateDefenceOrganisationHandler associateDefenceOrganisationHandler;

    private DefenceAssociationAggregate aggregate;

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
                        .thatHandles("progression.command.handler.associate-defence-organisation")
                ));
    }

    @Test
    public void shouldProcessCommand() throws Exception {
        //Given
        final AssociateDefenceOrganisation associateDefenceOrganisation = generateAssociateDefenceOrganisationCommandPrivateFunded();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.handler.associate-defence-organisation")
                .withId(randomUUID())
                .build();

        final Envelope<AssociateDefenceOrganisation> envelope = envelopeFrom(metadata, associateDefenceOrganisation);

        //When
        associateDefenceOrganisationHandler.handle(envelope);

        //Then
        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.defence-organisation-associated"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.defenceOrganisationId", notNullValue()))
                        ))

                )
        );
    }

    private AssociateDefenceOrganisation generateAssociateDefenceOrganisationCommandPrivateFunded() {
        return AssociateDefenceOrganisation.associateDefenceOrganisation()
                .withDefendantId(randomUUID())
                .withRepresentationType(RepresentationType.PRIVATE_FUNDED)
                .withRequesterDefenceOrganisationId(randomUUID())
                .withRequesterUserId(randomUUID())
                .build();
    }

}