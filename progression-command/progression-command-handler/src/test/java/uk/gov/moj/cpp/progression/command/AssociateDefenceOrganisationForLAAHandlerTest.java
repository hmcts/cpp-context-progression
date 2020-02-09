package uk.gov.moj.cpp.progression.command;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
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
import uk.gov.moj.cpp.progression.handler.AssociateDefenceOrganisationForLAAHandler;

import javax.json.Json;
import javax.json.JsonObject;
import java.util.UUID;
import java.util.stream.Stream;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;

@RunWith(MockitoJUnitRunner.class)
public class AssociateDefenceOrganisationForLAAHandlerTest {
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
    private AssociateDefenceOrganisationForLAAHandler associateDefenceOrganisationForLAAHandler;

    private DefenceAssociationAggregate aggregate;

    private static final String COMMAND_HANDLER_DEFENCE_ASSOCIATION_NAME_FOR_LAA = "progression.command.handler.associate-defence-organisation-for-laa";

    private static final String ORGANISATION_NAME = "CompanyZ";

    private static final String LAA_CONTRACT_NUMEBR = "LAA1234";


    @Before
    public void setup() {
        aggregate = new DefenceAssociationAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, DefenceAssociationAggregate.class)).thenReturn(aggregate);
    }

    @Test
    public void shouldHandleCommand() {
        assertThat(new AssociateDefenceOrganisationForLAAHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handle")
                        .thatHandles(COMMAND_HANDLER_DEFENCE_ASSOCIATION_NAME_FOR_LAA)
                ));
    }

    @Test
    public void shouldProcessCommandSucessfully() throws Exception {

        //Given
        final UUID userId = UUID.randomUUID();
        final String defendantId = UUID.randomUUID().toString();
        final String organisationId = UUID.randomUUID().toString();
        final JsonEnvelope envelope = createDefenceAssociationEnvelope(userId, defendantId, organisationId, ORGANISATION_NAME);

        //When
        associateDefenceOrganisationForLAAHandler.handle(envelope);

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

    private JsonEnvelope createDefenceAssociationEnvelope(final UUID userId, final String defendantId, final String orgId, final String orgName) {
        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName(COMMAND_HANDLER_DEFENCE_ASSOCIATION_NAME_FOR_LAA)
                .withId(randomUUID())
                .withUserId(userId.toString())
                .build();
        final JsonObject payload = Json.createObjectBuilder()
                 .add("defendantId", defendantId)
                 .add("organisationId", orgId)
                 .add("organisationName", orgName)
                 .add("representationType", RepresentationType.REPRESENTATION_ORDER.toString())
                 .add("laaContractNumber", LAA_CONTRACT_NUMEBR)
                 .build();

        return JsonEnvelope.envelopeFrom(metadata, payload);
    }
}
