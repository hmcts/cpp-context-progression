package uk.gov.moj.cpp.progression.handler;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;

import uk.gov.justice.core.courts.CourtProceedingsInitiated;
import uk.gov.justice.core.courts.CourtReferral;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantPartialMatchCreated;
import uk.gov.justice.core.courts.InitiateCourtProceedings;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.framework.api.JsonObjectConvertersFactory;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import uk.gov.moj.cpp.progression.aggregate.CasesReferredToCourtAggregate;
import uk.gov.moj.cpp.progression.domain.PartialMatchDefendant;
import uk.gov.moj.cpp.progression.service.MatchedDefendantLoadService;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

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
public class InitiateCourtProceedingsHandlerTest {

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Mock
    private MatchedDefendantLoadService matchedDefendantLoadService;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectConvertersFactory().jsonObjectToObjectConverter();

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter = new JsonObjectConvertersFactory().objectToJsonObjectConverter();

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            CourtProceedingsInitiated.class, DefendantPartialMatchCreated.class);

    @InjectMocks
    private InitiateCourtProceedingsHandler initiateCourtProceedingsHandler;

    private static final UUID CASE_ID = UUID.fromString("5002d600-af66-11e8-b568-0800200c9a66");

    @Test
    public void shouldHandleCommand() {
        assertThat(new InitiateCourtProceedingsHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handle")
                        .thatHandles("progression.command.initiate-court-proceedings")
                ));
    }

    @Test
    public void shouldProcessCommand() throws Exception {
        final InitiateCourtProceedings initiateCourtProceedings = generateInitiateCourtProceedings();
        final PartialMatchDefendant partialMatchDefendant = PartialMatchDefendant.partialMatchDefendant()
                .withDefendantId(randomUUID())
                .withProsecutionCaseId(randomUUID())
                .withCaseReference("CASE123")
                .withDefendantName("Justin Adam Scot")
                .build();

        final CasesReferredToCourtAggregate casesReferredToCourtAggregate = new CasesReferredToCourtAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CasesReferredToCourtAggregate.class)).thenReturn(casesReferredToCourtAggregate);

        casesReferredToCourtAggregate.initiateCourtProceedings(initiateCourtProceedings.getInitiateCourtProceedings());

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.initiate-court-proceedings")
                .withId(randomUUID())
                .build();

        final Envelope<InitiateCourtProceedings> envelope = envelopeFrom(metadata, initiateCourtProceedings);

        initiateCourtProceedingsHandler.handle(envelope);

        ArgumentCaptor<Stream> argumentCaptor = ArgumentCaptor.forClass(Stream.class);
        Mockito.verify(eventStream, times(1)).append(argumentCaptor.capture());
        final JsonEnvelope courtProceedingsInitiatedEnvelope = (JsonEnvelope) argumentCaptor.getAllValues().get(0).findFirst().orElse(null);

        CourtProceedingsInitiated courtProceedingsInitiated = jsonObjectToObjectConverter.convert(courtProceedingsInitiatedEnvelope.payloadAsJsonObject(), CourtProceedingsInitiated.class);

        assertThat("progression.event.court-proceedings-initiated", is(courtProceedingsInitiatedEnvelope.metadata().name()));
        assertThat(courtProceedingsInitiated.getCourtReferral(), notNullValue());

        verify(matchedDefendantLoadService).aggregateDefendantsSearchResultForAProsecutionCase(any(),any());
    }

    private static InitiateCourtProceedings generateInitiateCourtProceedings() {
        return InitiateCourtProceedings.initiateCourtProceedings().withInitiateCourtProceedings(generateCourtReferral()).build();
    }

    private static CourtReferral generateCourtReferral() {
        return CourtReferral.courtReferral().withProsecutionCases(generateProsecutionCases()).build();
    }

    private static List<ProsecutionCase> generateProsecutionCases() {
        return Collections.singletonList(
                ProsecutionCase.prosecutionCase()
                        .withId(CASE_ID)
                        .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                                .withCaseURN("TFL43434")
                                .build())
                        .withDefendants(generateDefendants())
                        .build()
        );
    }

    private static List<Defendant> generateDefendants() {
        return Collections.singletonList(
                Defendant.defendant().withId(randomUUID())
                        .build()
        );
    }
}
