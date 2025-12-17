package uk.gov.moj.cpp.progression.handler;


import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
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

import uk.gov.justice.core.courts.CaseHearingDetailsUpdatedInUnifiedSearch;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingInitiateEnriched;
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
import uk.gov.moj.cpp.progression.aggregate.HearingAggregate;
import uk.gov.moj.cpp.progression.command.handler.courts.UpdateHearingDetailsInUnifiedSearch;

import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)

public class UpdateLaaHearingDetailsInUnifiedSearchHandlerTest {

    @InjectMocks
    private UpdateLaaHearingDetailsInUnifiedSearchHandler updateLaaHearingDetailsInUnifiedSearchHandler;

    @Mock
    private EventSource eventSource;

    @Mock
    private AggregateService aggregateService;

    @Mock
    private EventStream eventStream;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            CaseHearingDetailsUpdatedInUnifiedSearch.class);


    @Test
    public void testHandle() throws EventStreamException {


        final UUID hearingId = randomUUID();


        UpdateHearingDetailsInUnifiedSearch updateHearingDetailsInUnifiedSearch = UpdateHearingDetailsInUnifiedSearch.updateHearingDetailsInUnifiedSearch().withHearingId(hearingId).build();
        final MetadataBuilder metadataBuilder = metadataFrom(metadataWithRandomUUID("progression.command.handler.update-hearing-details-in-unified-search").withUserId(randomUUID().toString()).build());
        Envelope<UpdateHearingDetailsInUnifiedSearch> resendLaaOutcomeConcludedEnvelope = envelopeFrom(metadataBuilder, updateHearingDetailsInUnifiedSearch);

        when(eventSource.getStreamById(hearingId)).thenReturn(eventStream);
        final HearingAggregate hearingAggregate = new HearingAggregate();
        hearingAggregate.apply(HearingInitiateEnriched.hearingInitiateEnriched()
                .withHearing(Hearing.hearing()
                        .withId(hearingId)
                        .build())
                .build());
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(hearingAggregate);

        updateLaaHearingDetailsInUnifiedSearchHandler.handle(resendLaaOutcomeConcludedEnvelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.case-hearing-details-updated-in-unified-search"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                        withJsonPath("$.hearing.id", is(hearingId.toString()))

                                )
                        )
                ))
        );

    }


}