package uk.gov.moj.cpp.progression.command;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.core.courts.NowDocumentRequested;
import uk.gov.justice.core.courts.nowdocument.NowDocumentContent;
import uk.gov.justice.core.courts.nowdocument.NowDocumentRequest;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher;
import uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil;
import uk.gov.justice.services.test.utils.framework.api.JsonObjectConvertersFactory;
import uk.gov.moj.cpp.progression.aggregate.MaterialAggregate;
import uk.gov.moj.cpp.progression.handler.NowDocumentRequestHandler;

import java.util.UUID;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class NowDocumentRequestHandlerTest {
    private static final String ADD_NOW_DOCUMENT_REQUEST_COMMAND_NAME = "progression.command.add-now-document-request";
    private static final UUID MATERIAL_ID = randomUUID();

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @InjectMocks
    private NowDocumentRequestHandler nowDocumentRequestHandler;

    @Spy
    private final JsonObjectToObjectConverter jsonToObjectConverter = new JsonObjectConvertersFactory().jsonObjectToObjectConverter();

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(NowDocumentRequested.class);

    @Test
    public void shouldHandleCommand() {
        assertThat(new NowDocumentRequestHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handleAddNowDocumentRequest")
                        .thatHandles("progression.command.add-now-document-request")
                ));
    }

    @Test
    public void shouldProcessCommand() throws Exception {

        final MaterialAggregate materialAggregate = new MaterialAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, MaterialAggregate.class)).thenReturn(materialAggregate);

        nowDocumentRequestHandler.handleAddNowDocumentRequest(buildEnvelope());

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.now-document-requested"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.materialId", is(MATERIAL_ID.toString())),
                                withJsonPath("$.nowDocumentRequest", notNullValue())
                                )
                        ))
                )
        );
    }

    private Envelope<NowDocumentRequest> buildEnvelope() {

        final NowDocumentRequest nowDocumentRequest = NowDocumentRequest.nowDocumentRequest()
                .withMaterialId(MATERIAL_ID)
                .withNowContent(NowDocumentContent.nowDocumentContent().build())
                .build();

        return envelope(ADD_NOW_DOCUMENT_REQUEST_COMMAND_NAME, nowDocumentRequest);
    }

    private <T> Envelope<T> envelope(final String name, final T t) {
        final MetadataBuilder metadataBuilder = metadataFrom(metadataWithRandomUUID(name).withUserId(randomUUID().toString()).build());
        return envelopeFrom(metadataBuilder, t);
    }
}
