package uk.gov.moj.cpp.progression.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.moj.cpp.progression.events.RemoveDefendantCustodialEstablishmentFromCase.removeDefendantCustodialEstablishmentFromCase;

import uk.gov.justice.core.courts.CivilFees;
import uk.gov.justice.core.courts.CivilFeesAdded;
import uk.gov.justice.core.courts.CreateProsecutionCase;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.FeeStatus;
import uk.gov.justice.core.courts.FeeType;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseCreated;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import uk.gov.moj.cpp.progression.aggregate.FeeAggregate;
import uk.gov.moj.cpp.progression.events.RemoveDefendantCustodialEstablishmentFromCase;
import uk.gov.moj.cpp.progression.service.ProsecutionCaseQueryService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CreateProsecutionCaseHandlerTest {

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Mock
    private ProsecutionCaseQueryService prosecutionCaseQueryService;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            ProsecutionCaseCreated.class, CivilFeesAdded.class);

    @InjectMocks
    private CreateProsecutionCaseHandler createProsecutionCaseHandler;

    private CaseAggregate aggregate;


    @Captor
    ArgumentCaptor<UUID> prosecutionCaseIdArgumentCapture;


    @Test
    public void shouldHandleCommand() {
        assertThat(new CreateProsecutionCaseHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handle")
                        .thatHandles("progression.command.create-prosecution-case")
                ));
    }

    @Test
    public void shouldProcessCommand() throws Exception {

        final Defendant defendant = Defendant.defendant().withId(randomUUID()).withPersonDefendant(PersonDefendant.personDefendant().build())
                .withOffences(singletonList(Offence.offence().build()))
                .build();
        final List<Defendant> defendants = new ArrayList<Defendant>() {{
            add(defendant);
        }};
        final CreateProsecutionCase createProsecutionCase = CreateProsecutionCase.createProsecutionCase()
                .withProsecutionCase(ProsecutionCase.prosecutionCase()
                        .withDefendants(defendants)
                        .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                                .withProsecutionAuthorityId(randomUUID())
                                .withProsecutionAuthorityCode("code")
                                .withProsecutionAuthorityReference("reference")
                                .build())
                        .withClassOfCase("Class 1")
                        .withCivilFees(List.of(CivilFees.civilFees()
                                        .withFeeId(UUID.randomUUID())
                                        .withFeeType(FeeType.INITIAL)
                                        .withFeeStatus(FeeStatus.OUTSTANDING)
                                        .withPaymentReference("payref01")
                                .build()))
                        .build())
                .build();

        final CaseAggregate caseAggregate = new CaseAggregate();
        final FeeAggregate feeAggregate = new FeeAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        when(aggregateService.get(eventStream, FeeAggregate.class)).thenReturn(feeAggregate);

        caseAggregate.apply(createProsecutionCase.getProsecutionCase());


        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.create-prosecution-case")
                .withId(randomUUID())
                .build();

        final Envelope<CreateProsecutionCase> envelope = envelopeFrom(metadata, createProsecutionCase);

        createProsecutionCaseHandler.handle(envelope);

        ArgumentCaptor<Stream> argumentCaptor = ArgumentCaptor.forClass(Stream.class);
        ((EventStream) Mockito.verify(eventStream, times(2))).append((Stream)argumentCaptor.capture());

        final Stream<JsonEnvelope> prosecutionCaseEnvelopeStream = (Stream)argumentCaptor.getAllValues().get(0);

      assertThat(prosecutionCaseEnvelopeStream, streamContaining(
                        jsonEnvelope(
                                metadata()
                                        .withName("progression.event.prosecution-case-created"),
                                JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                        withJsonPath("$.prosecutionCase", notNullValue()),
                                        withJsonPath("$.prosecutionCase.classOfCase", is("Class 1")))
                                ))
           )
        );

        final Stream<JsonEnvelope> feeEnvelopeStream = (Stream)argumentCaptor.getAllValues().get(1);
        assertThat(feeEnvelopeStream, streamContaining(
                        jsonEnvelope(
                                metadata()
                                        .withName("progression.event.civil-fees-added"),
                                JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                        withJsonPath("$.feeId", notNullValue()),
                                        withJsonPath("$.feeStatus", is(FeeStatus.OUTSTANDING.name())),
                                        withJsonPath("$.feeType", is(FeeType.INITIAL.name())),
                                        withJsonPath("$.paymentReference", is("payref01")))
                                ))
                )
        );
    }


    @Test
    public void shouldHandleRemoveDefendantCustodialEstablishment() throws Exception {
        final UUID defendantId = randomUUID();
        final UUID masterDefendantId = randomUUID();
        final UUID prosecutionCaseId = randomUUID();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.remove-defendant-custodial-establishment-from-case")
                .withId(randomUUID())
                .build();

        final Envelope<RemoveDefendantCustodialEstablishmentFromCase> envelope = envelopeFrom(metadata, removeDefendantCustodialEstablishmentFromCase()
                .withDefendantId(defendantId)
                .withMasterDefendantId(masterDefendantId)
                .withProsecutionCaseId(prosecutionCaseId)
                .build());

        final CaseAggregate caseAggregate = new CaseAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);

        createProsecutionCaseHandler.handleRemoveDefendantCustodialEstablishmentFromCase(envelope);

        verify(eventSource).getStreamById(prosecutionCaseIdArgumentCapture.capture());
        assertEquals(prosecutionCaseIdArgumentCapture.getValue(), prosecutionCaseId);
    }



}
