package uk.gov.moj.cpp.progression.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.CasesReferredToCourt;
import uk.gov.justice.core.courts.ReferCasesToCourt;
import uk.gov.justice.core.courts.ReferredDefendant;
import uk.gov.justice.core.courts.ReferredOffence;
import uk.gov.justice.core.courts.ReferredPerson;
import uk.gov.justice.core.courts.ReferredPersonDefendant;
import uk.gov.justice.core.courts.ReferredProsecutionCase;
import uk.gov.justice.core.courts.ReportingRestriction;
import uk.gov.justice.core.courts.SjpCourtReferral;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher;
import uk.gov.moj.cpp.progression.aggregate.CasesReferredToCourtAggregate;
import uk.gov.moj.cpp.progression.service.MatchedDefendantLoadService;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ReferCasesToCourtHandlerTest {

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Mock
    MatchedDefendantLoadService matchedDefendantLoadService;

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            CasesReferredToCourt.class);

    @InjectMocks
    private ReferCasesToCourtHandler referCasesToCourtHandler;


    private static final UUID CASE_ID = UUID.fromString("5002d600-af66-11e8-b568-0800200c9a66");
    private static final UUID RR_ID = randomUUID();


    @Test
    public void shouldHandleCommand() {
        assertThat(new ReferCasesToCourtHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handle")
                        .thatHandles("progression.command.refer-cases-to-court")
                ));
    }

    @Test
    public void shouldProcessCommand() throws Exception {
        final ReferCasesToCourt referCasesToCourt = generateReferCasesToCourt();

        final CasesReferredToCourtAggregate aggregate = new CasesReferredToCourtAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CasesReferredToCourtAggregate.class)).thenReturn(aggregate);

        aggregate.referCasesToCourt(referCasesToCourt.getCourtReferral());


        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.refer-cases-to-court")
                .withId(randomUUID())
                .build();

        final Envelope<ReferCasesToCourt> envelope = envelopeFrom(metadata, referCasesToCourt);

        referCasesToCourtHandler.handle(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.cases-referred-to-court"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.courtReferral.prosecutionCases[0].defendants[0].offences[0].reportingRestrictions[0].id", is(RR_ID.toString())),
                                withJsonPath("$.courtReferral.prosecutionCases[0].defendants[0].offences[0].reportingRestrictions[0].label", is ("label")))
                        ))

                )
        );

        verify(matchedDefendantLoadService).aggregateDefendantsSearchResultForAProsecutionCase(any(),any());
    }

    private static ReferCasesToCourt generateReferCasesToCourt() {
        return ReferCasesToCourt.referCasesToCourt()
                .withCourtReferral(generateCourtReferral())
                .build();
    }

    private static SjpCourtReferral generateCourtReferral() {
         return SjpCourtReferral.sjpCourtReferral()
                .withProsecutionCases(generateProsecutionCases())
                .build();
    }

    private static List<ReferredProsecutionCase> generateProsecutionCases() {
        return singletonList(
                ReferredProsecutionCase.referredProsecutionCase()
                        .withId(CASE_ID)
                        .withDefendants(singletonList(
                                ReferredDefendant.referredDefendant()
                                        .withId(randomUUID())
                                        .withPersonDefendant(ReferredPersonDefendant.referredPersonDefendant()
                                                .withPersonDetails(ReferredPerson.referredPerson()
                                                        .withAddress(Address.address().build())
                                                        .build())
                                                .build())
                                        .withOffences(singletonList(ReferredOffence.referredOffence()
                                                .withId(randomUUID())
                                                .withOffenceDefinitionId(randomUUID())
                                                .withWording("wording")
                                                .withReportingRestrictions(singletonList(ReportingRestriction.reportingRestriction()
                                                        .withId(RR_ID)
                                                        .withLabel("label")
                                                        .build()))
                                                .build()))
                                        .build()
                        ))
                        .build()
        );
    }


}
