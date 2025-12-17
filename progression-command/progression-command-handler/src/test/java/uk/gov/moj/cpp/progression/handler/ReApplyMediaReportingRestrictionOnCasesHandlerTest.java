package uk.gov.moj.cpp.progression.handler;


import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseCreated;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.core.courts.ReapplyMiReportingRestrictions;
import uk.gov.justice.progression.courts.ReapplyMediaReportingRestrictions;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;

import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)

public class ReApplyMediaReportingRestrictionOnCasesHandlerTest {

    @InjectMocks
    private ReApplyMediaReportingRestrictionOnCasesHandler reApplyMediaReportingRestrictionOnCasesHandler;

    @Mock
    private EventSource eventSource;

    @Mock
    private AggregateService aggregateService;

    @Mock
    private EventStream eventStream;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            ReapplyMiReportingRestrictions.class);


    @Test
    public void testHandle() throws EventStreamException {
        final UUID caseId1 = randomUUID();
        ReapplyMediaReportingRestrictions reapplyMediaReportingRestrictions = ReapplyMediaReportingRestrictions
                .reapplyMediaReportingRestrictions().withCaseId(caseId1).build();

        final MetadataBuilder metadataBuilder = metadataFrom(metadataWithRandomUUID("progression.command.handler.reapply-media-reporting-restrictions")
                .withUserId(randomUUID().toString()).build());
        Envelope<ReapplyMediaReportingRestrictions> reapplyMediaReportingRestrictionsEnvelope = envelopeFrom(metadataBuilder, reapplyMediaReportingRestrictions);

        when(eventSource.getStreamById(caseId1)).thenReturn(eventStream);
        final CaseAggregate caseAggregate = getCaseAggregate(caseId1);

        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);

        reApplyMediaReportingRestrictionOnCasesHandler.handle(reapplyMediaReportingRestrictionsEnvelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.reapply.mi.reporting.restrictions"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                        withJsonPath("$.caseId", is(caseId1.toString()))

                                )
                        )
                ))
        );
    }

    private CaseAggregate getCaseAggregate(final UUID caseId) {
        return new CaseAggregate() {{
            apply(ProsecutionCaseCreated.prosecutionCaseCreated()
                    .withProsecutionCase(ProsecutionCase.prosecutionCase()
                            .withId(caseId)
                            .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().build())
                            .withDefendants(singletonList(Defendant.defendant()
                                    .withId(randomUUID())
                                    .withOffences(singletonList(Offence.offence()
                                            .withId(randomUUID())
                                            .withOrderIndex(1)
                                            .build()))
                                    .build()))
                            .build())
                    .build());
        }};
    }
}

