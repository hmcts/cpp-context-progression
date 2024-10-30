package uk.gov.moj.cpp.progression.handler;


import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;

import uk.gov.justice.core.courts.CasesAddedForUpdatedRelatedHearing;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.SeedingHearing;
import uk.gov.justice.progression.courts.AddCasesForUpdatedRelatedHearing;
import uk.gov.justice.progression.courts.RelatedHearingUpdated;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.progression.aggregate.HearingAggregate;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AddCasesForUpdatedRelatedHearingHandlerTest {

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            CasesAddedForUpdatedRelatedHearing.class);

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @InjectMocks
    private AddCasesForUpdatedRelatedHearingHandler addCasesForUpdatedRelatedHearingHandler;

    private HearingAggregate hearingAggregate;

    private ObjectMapper mapper = new ObjectMapperProducer().objectMapper();

    private final JsonObjectToObjectConverter jsonToObjectConverter = new JsonObjectToObjectConverter(mapper);


    @BeforeEach
    public void setup() {
        hearingAggregate = new HearingAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(hearingAggregate);
    }

    @Test
    public void shouldHandleAddCasesForUpdateRelatedHearing() throws EventStreamException {
        final UUID hearingId = randomUUID();
        final UUID seedingHearingId = randomUUID();
        final RelatedHearingUpdated relatedHearingUpdated = RelatedHearingUpdated.relatedHearingUpdated()
                .withHearingRequest(HearingListingNeeds.hearingListingNeeds()
                        .withId(hearingId)
                        .build())
                .withSeedingHearing(SeedingHearing.seedingHearing()
                        .withSeedingHearingId(seedingHearingId)
                        .build())
                .build();
        hearingAggregate.apply(relatedHearingUpdated);

        final AddCasesForUpdatedRelatedHearing addCasesForUpdatedRelatedHearing = AddCasesForUpdatedRelatedHearing.addCasesForUpdatedRelatedHearing()
                .withHearingId(hearingId)
                .withSeedingHearingId(seedingHearingId)
                .build();
        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.add-cases-for-updated-related-hearing")
                .withId(randomUUID())
                .build();
        final Envelope<AddCasesForUpdatedRelatedHearing> envelope = envelopeFrom(metadata, addCasesForUpdatedRelatedHearing);
        addCasesForUpdatedRelatedHearingHandler.handleAddCasesForUpdateRelatedHearing(envelope);

        final List<JsonEnvelope> events = verifyAppendAndGetArgumentFrom(eventStream).collect(Collectors.toList());

        assertThat(events.size(), is(1));
        assertThat(events.get(0).metadata().name(), is("progression.event.cases-added-for-updated-related-hearing"));
        final CasesAddedForUpdatedRelatedHearing casesAddedForUpdatedRelatedHearing = jsonToObjectConverter.convert(events.get(0).payloadAsJsonObject(), CasesAddedForUpdatedRelatedHearing.class);
        assertThat(casesAddedForUpdatedRelatedHearing.getHearingRequest().getId(), is(hearingId));
        assertThat(casesAddedForUpdatedRelatedHearing.getSeedingHearing().getSeedingHearingId(), is(seedingHearingId));

    }

}
