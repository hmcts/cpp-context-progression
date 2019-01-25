package uk.gov.moj.cpp.progression.command;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloperWithEvents;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.progression.test.matchers.BeanMatcher.isBean;
import static uk.gov.moj.cpp.progression.test.matchers.ElementAtListMatcher.first;

import uk.gov.justice.core.courts.CreateNowsRequest;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.aggregate.NowsAggregate;
import uk.gov.moj.cpp.progression.events.NowsRequested;
import uk.gov.moj.cpp.progression.handler.GenerateNowsHandler;
import uk.gov.moj.cpp.progression.test.TestTemplates;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.JsonObject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)

public class GenerateNowsCommandHandlerTest {

    @Spy
    private final Enveloper enveloper = createEnveloperWithEvents(
            NowsRequested.class
    );


    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;
    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @InjectMocks
    private GenerateNowsHandler generateNowsHandler;

    private NowsAggregate aggregate;

    @Before
    public void setup() {
        setField(this.jsonObjectToObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        aggregate = new NowsAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, NowsAggregate.class)).thenReturn(aggregate);
    }

    @Test
    public void generateNowsTest() throws Throwable {

        final NowsRequested nowsRequestedCommand = NowsRequested.nowsRequested()
                .withCreateNowsRequest(TestTemplates.generateNowsRequestTemplate(UUID.randomUUID()))
                .build();
        final CreateNowsRequest request = nowsRequestedCommand.getCreateNowsRequest();

        final JsonEnvelope command = envelopeFrom(metadataWithRandomUUID("progession.command.generate-nows"), objectToJsonObjectConverter.convert(nowsRequestedCommand));

        this.generateNowsHandler.generateNows(command);

        final Stream<JsonEnvelope> eventEnvelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        final List<Envelope> envelopes = eventEnvelopeStream.collect(Collectors.toList());

        final Envelope jsonEnvelope = envelopes.get(0);

        assertThat(jsonEnvelope.metadata().name(), is("progression.event.nows-requested"));

        NowsRequested nowsRequested = jsonObjectToObjectConverter.convert((JsonObject) jsonEnvelope.payload(), NowsRequested.class);

        assertThat(nowsRequested, isBean(NowsRequested.class)
                .with(NowsRequested::getCreateNowsRequest, isBean(CreateNowsRequest.class)
                        .with(CreateNowsRequest::getHearing, isBean(Hearing.class)
                                .with(Hearing::getId, is(request.getHearing().getId()))
                                .with(Hearing::getProsecutionCases, first(isBean(ProsecutionCase.class)
                                        .with(ProsecutionCase::getId, is(request.getHearing().getProsecutionCases().get(0).getId()))
                                ))
                        )
                )
        );
    }

    /*

//TODO GPE-6752
        @Test
        public void nowsGeneratedTest() throws Throwable {

            final NowsMaterialStatusUpdated nowsMaterialStatusUpdated = new NowsMaterialStatusUpdated(UUID.randomUUID(), UUID.randomUUID(), "generated");

            setupMockedEventStream(nowsMaterialStatusUpdated.getHearingId(), this.hearingEventStream, new HearingAggregate());

            final JsonEnvelope jsonEnvelope = envelopeFrom(metadataWithRandomUUID("hearing.command.update-nows-material-status"), objectToJsonObjectConverter.convert(nowsMaterialStatusUpdated));

            this.generateNowsCommandHandler.nowsGenerated(jsonEnvelope);

            assertThat(verifyAppendAndGetArgumentFrom(this.hearingEventStream), streamContaining(
                    jsonEnvelope(
                            withMetadataEnvelopedFrom(jsonEnvelope)
                                    .withName("hearing.events.nows-material-status-updated"),
                            payloadIsJson(allOf(
                                    withJsonPath("$.hearingId", equalTo(nowsMaterialStatusUpdated.getHearingId().toString())),
                                    withJsonPath("$.materialId", equalTo(nowsMaterialStatusUpdated.getMaterialId().toString())),
                                    withJsonPath("$.status", equalTo(nowsMaterialStatusUpdated.getStatus()))
                            )))
            ));
        }
    */
    private <T extends Aggregate> void setupMockedEventStream(UUID id, EventStream eventStream, T aggregate) {
        when(this.eventSource.getStreamById(id)).thenReturn(eventStream);
        Class<T> clz = (Class<T>) aggregate.getClass();
        when(this.aggregateService.get(eventStream, clz)).thenReturn(aggregate);
    }

}