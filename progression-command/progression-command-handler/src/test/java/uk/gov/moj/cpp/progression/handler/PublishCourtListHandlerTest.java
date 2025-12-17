package uk.gov.moj.cpp.progression.handler;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.listing.courts.CourtListPublished;
import uk.gov.justice.listing.courts.PublishCourtList;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher;
import uk.gov.moj.cpp.progression.aggregate.CourtCentreAggregate;
import uk.gov.moj.cpp.progression.command.helper.FileResourceObjectMapper;

import java.util.stream.Stream;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;



@ExtendWith(MockitoExtension.class)
public class PublishCourtListHandlerTest {

    @Mock
    private AggregateService aggregateService;

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(CourtListPublished.class);


    private CourtCentreAggregate courtCentreAggregate;

    @InjectMocks
    private PublishCourtListHandler publishCourtListHandler;



    private final FileResourceObjectMapper handlerTestHelper = new FileResourceObjectMapper();

    @BeforeEach
    public void setup() {

        courtCentreAggregate = new CourtCentreAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CourtCentreAggregate.class)).thenReturn(courtCentreAggregate);
    }

    @Test
    public  void testPublishCourtListWithProsecutorInHearings () throws Exception {

        final PublishCourtList publishCourtList = handlerTestHelper.convertFromFile("json/publish-court-list-with-prosecutor-in-hearings.json", PublishCourtList.class);
        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.publish-court-list")
                .withId(randomUUID())
                .build();
        final Envelope<PublishCourtList> envelope = envelopeFrom(metadata, publishCourtList);

        publishCourtListHandler.handlePublishCourtList(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.court-list-published"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.courtCentreId", is("4d3332e0-d6de-37a9-b638-b14e8a944cd6")),
                                withJsonPath("$.courtListId", is("5dec5287-0299-3a4a-ab26-c72ff193c6b8")))
                        ))
                )
        );


    }

}
