package uk.gov.moj.cpp.progression.command;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.cpp.progression.events.DefendantDefenceAssociationLockedForLaa;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.progression.aggregate.DefenceAssociationAggregate;
import uk.gov.moj.cpp.progression.handler.LockDefenceAssociationForLAAHandler;

import javax.json.Json;
import javax.json.JsonObject;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static java.util.UUID.randomUUID;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;

@RunWith(MockitoJUnitRunner.class)
public class LockDefenceAssociationForLAAHandlerTest {
    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            DefendantDefenceAssociationLockedForLaa.class);

    @InjectMocks
    private LockDefenceAssociationForLAAHandler lockDefenceAssociationForLAAHandler;


    private DefenceAssociationAggregate aggregate;


    private static final UUID defendantId = randomUUID();
    private static final UUID prosecutionCaseId = randomUUID();

    private static final String DEFENDANT_ID = "defendantId";

    private static final String LOCKED_BY_REP_ORDER = "lockedByRepOrder";
    private static final String PROSECUTION_CASE_ID = "prosecutionCaseId";
    @Before
    public void setup() {
        aggregate = new DefenceAssociationAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, DefenceAssociationAggregate.class)).thenReturn(aggregate);
    }

    @Test
    public void shouldHandleCommand() {
        assertThat(new LockDefenceAssociationForLAAHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handle")
                        .thatHandles("progression.command.handler.lock-defence-association-for-laa")
                ));
    }

    @Test
    public void shouldProcessCommand_whenOrganisationIsSetupAndNoAssoicatedOrgExist_expectAssociationEvent() throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withUserId(randomUUID().toString()).withId(randomUUID()).withName("progression.command.handler.lock-defence-association-for-laa").build(),
                createPayloadForLockDefendantAssociation());
        lockDefenceAssociationForLAAHandler.handle(jsonEnvelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        Optional<JsonEnvelope> associatedOrDisAssociatedEnvelope = envelopeStream.filter
                (a->a.metadata().name().equals("progression.event.defendant-defence-association-locked-for-laa"))
                .findAny();

        assertTrue(associatedOrDisAssociatedEnvelope.isPresent());
    }


    private static JsonObject createPayloadForLockDefendantAssociation() {
        return Json.createObjectBuilder()
                .add(DEFENDANT_ID, defendantId.toString())
                .add(PROSECUTION_CASE_ID, prosecutionCaseId.toString())
                .add(LOCKED_BY_REP_ORDER, true)
                .build();

    }

}
