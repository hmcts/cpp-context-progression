package uk.gov.moj.cpp.progression.command.handler;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;

import uk.gov.justice.core.courts.CivilFees;
import uk.gov.justice.core.courts.CivilFeesAdded;
import uk.gov.justice.core.courts.CourtProceedingsInitiated;
import uk.gov.justice.core.courts.CourtReferral;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.FeeStatus;
import uk.gov.justice.core.courts.FeeType;
import uk.gov.justice.core.courts.InitiateCourtProceedingsForGroupCases;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseCreated;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import uk.gov.moj.cpp.progression.aggregate.FeeAggregate;
import uk.gov.moj.cpp.progression.aggregate.GroupCaseAggregate;
import uk.gov.moj.cpp.progression.events.CivilCaseExists;
import uk.gov.moj.cpp.progression.handler.InitiateCourtProceedingsForGroupCasesHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class InitiateCourtProceedingsForGroupCasesHandlerTest {

    private static final String INIT_GROUP_CASE = "progression.command.initiate-court-proceedings-for-group-cases";

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(objectMapper);

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(CourtProceedingsInitiated.class, ProsecutionCaseCreated.class, CivilCaseExists.class,
            CivilFeesAdded.class);

    @InjectMocks
    private InitiateCourtProceedingsForGroupCasesHandler initiateCourtProceedingsForGroupCasesHandler;

    private GroupCaseAggregate aggregate;

    private CaseAggregate caseAggregate;
    private FeeAggregate feeAggregate;

    private static final UUID CASE_ID = UUID.fromString("5002d600-af66-11e8-b568-0800200c9a66");

    private static final UUID GROUP_ID = UUID.fromString("bd9f04f6-7ef5-48c7-a103-dd8b92b3f195");

    private final InitiateCourtProceedingsForGroupCases initiateCourtProceedingsForGroupCases = generateInitiateCourtProceedingsForGroup();

    private final Metadata metadata = Envelope
            .metadataBuilder()
            .withName(INIT_GROUP_CASE)
            .withId(randomUUID())
            .build();

    private final Envelope<InitiateCourtProceedingsForGroupCases> envelope = envelopeFrom(metadata, initiateCourtProceedingsForGroupCases);

    @BeforeEach
    public void setup() {
        aggregate = new GroupCaseAggregate();
        caseAggregate = new CaseAggregate();
        feeAggregate = new FeeAggregate();
    }

    @Test
    public void shouldHandleCommand() {
        assertThat(new InitiateCourtProceedingsForGroupCasesHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handle")
                        .thatHandles(INIT_GROUP_CASE)
                ));
    }

    @Test
    public void shouldProcessCommand() throws Exception {
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        when(aggregateService.get(eventStream, GroupCaseAggregate.class)).thenReturn(aggregate);
        when(aggregateService.get(eventStream, FeeAggregate.class)).thenReturn(feeAggregate);
        initiateCourtProceedingsForGroupCasesHandler.handle(envelope);

        final ArgumentCaptor<Stream> argumentCaptor = ArgumentCaptor.forClass(Stream.class);
        Mockito.verify(eventStream, times(3)).append(argumentCaptor.capture());

        final JsonEnvelope civilFeesAddedEnvelope = (JsonEnvelope) argumentCaptor.getAllValues().get(0).findFirst().orElse(null);
        CivilFeesAdded civilFeesAdded = jsonObjectToObjectConverter.convert(civilFeesAddedEnvelope.payloadAsJsonObject(), CivilFeesAdded.class);
        assertThat("progression.event.civil-fees-added", is(civilFeesAddedEnvelope.metadata().name()));
        assertThat(civilFeesAdded.getFeeId(), notNullValue());

        final JsonEnvelope prosecutionCaseCreatedEnvelope = (JsonEnvelope) argumentCaptor.getAllValues().get(1).findFirst().orElse(null);
        ProsecutionCaseCreated prosecutionCaseCreated = jsonObjectToObjectConverter.convert(prosecutionCaseCreatedEnvelope.payloadAsJsonObject(), ProsecutionCaseCreated.class);
        assertThat("progression.event.prosecution-case-created", is(prosecutionCaseCreatedEnvelope.metadata().name()));
        assertThat(prosecutionCaseCreated.getProsecutionCase(), notNullValue());

        final JsonEnvelope courtProceedingsInitiatedEnvelope = (JsonEnvelope) argumentCaptor.getAllValues().get(2).findFirst().orElse(null);
        CourtProceedingsInitiated courtProceedingsInitiated = jsonObjectToObjectConverter.convert(courtProceedingsInitiatedEnvelope.payloadAsJsonObject(), CourtProceedingsInitiated.class);
        assertThat("progression.event.court-proceedings-initiated", is(courtProceedingsInitiatedEnvelope.metadata().name()));
        assertThat(courtProceedingsInitiated.getCourtReferral(), notNullValue());
    }

    @Test
    public void shouldInitiateCourtProceedingsForGroupCasesWithExistingCase() throws Exception {
        final Envelope<InitiateCourtProceedingsForGroupCases> envelope = envelopeFrom(metadata, initiateCourtProceedingsForGroupCases);
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(eventSource.getStreamById(initiateCourtProceedingsForGroupCases.getCourtReferral().getProsecutionCases().get(0).getId()).size()).thenReturn(1L);
        when(aggregateService.get(eventStream, GroupCaseAggregate.class)).thenReturn(aggregate);
        initiateCourtProceedingsForGroupCasesHandler.handle(envelope);

        final ArgumentCaptor<Stream> argumentCaptor = ArgumentCaptor.forClass(Stream.class);
        Mockito.verify(eventStream, times(1)).append(argumentCaptor.capture());
        final JsonEnvelope courtProceedingsInitiatedEnvelope = (JsonEnvelope) argumentCaptor.getAllValues().get(0).findFirst().orElse(null);
        final CivilCaseExists civilCaseExists = jsonObjectToObjectConverter.convert(courtProceedingsInitiatedEnvelope.payloadAsJsonObject(), CivilCaseExists.class);
        assertThat("progression.event.civil-case-exists", is(courtProceedingsInitiatedEnvelope.metadata().name()));
        assertThat(civilCaseExists.getGroupId(), is(initiateCourtProceedingsForGroupCases.getGroupId()));
        assertThat(civilCaseExists.getProsecutionCaseId(), is(initiateCourtProceedingsForGroupCases.getCourtReferral().getProsecutionCases().get(0).getId()));
        assertThat(civilCaseExists.getCaseUrn(), is(initiateCourtProceedingsForGroupCases.getCourtReferral().getProsecutionCases().get(0).getProsecutionCaseIdentifier().getCaseURN()));
    }

    private static InitiateCourtProceedingsForGroupCases generateInitiateCourtProceedingsForGroup() {
        return InitiateCourtProceedingsForGroupCases.
                initiateCourtProceedingsForGroupCases().
                withCourtReferral(generateCourtReferral()).
                withGroupId(GROUP_ID).build();
    }

    private static CourtReferral generateCourtReferral() {
        return CourtReferral.courtReferral().withProsecutionCases(generateProsecutionCases()).build();
    }

    private static List<ProsecutionCase> generateProsecutionCases() {
        return Collections.singletonList(
                ProsecutionCase.prosecutionCase()
                        .withId(CASE_ID)
                        .withCivilFees(List.of(CivilFees.civilFees()
                                .withFeeId(UUID.randomUUID())
                                .withFeeStatus(FeeStatus.OUTSTANDING)
                                .withFeeType(FeeType.INITIAL)
                                .withPaymentReference("PaymentRef01")
                                .build()))
                        .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                                .withCaseURN("TFL43434")
                                .build())
                        .withDefendants(generateDefendants())
                        .build()
        );
    }

    private static List<Defendant> generateDefendants() {
        return Collections.singletonList(
                Defendant.defendant()
                        .withId(randomUUID())
                        .withOffences(new ArrayList<>())
                        .build()
        );
    }
}
