package uk.gov.moj.cpp.progression.processor;

import static com.google.common.io.Resources.getResource;
import static java.nio.charset.Charset.defaultCharset;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.moj.cpp.progression.service.payloads.PublishCourtListPayload.publishCourtListPayloadBuilder;

import uk.gov.justice.listing.courts.PublishCourtListType;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.service.PublishCourtListNotificationService;
import uk.gov.moj.cpp.progression.service.PublishCourtListPayloadBuilderService;
import uk.gov.moj.cpp.progression.service.payloads.PublishCourtListPayload;

import java.io.IOException;
import java.util.Map;

import javax.json.JsonObject;

import com.google.common.io.Resources;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PublishCourtListEventProcessorTest {

    private final static String PRIVATE_EVENT_COURT_LIST_PUBLISHED = "progression.event.court-list-published";

    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeArgumentCaptor;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(new ObjectMapperProducer().objectMapper());

    @Mock
    private PublishCourtListPayloadBuilderService publishCourtListPayloadBuilderService;

    @Mock
    private PublishCourtListNotificationService publishCourtListNotificationService;

    @InjectMocks
    private PublishCourtListEventProcessor underTest;

    @Test
    public void shouldRaisePublishCourtListCommandWhenListingPublicEventIsHandled() {
        final JsonEnvelope publicEvent = prepareEnvelope();

        underTest.processListingCourtListPublished(publicEvent);

        verify(sender).send(envelopeArgumentCaptor.capture());
        assertThat(envelopeArgumentCaptor.getValue().payload(), is(publicEvent.payloadAsJsonObject()));
        assertThat(envelopeArgumentCaptor.getValue().metadata().name(), is("progression.command.publish-court-list"));
    }

    @Test
    public void shouldSendNotification_ProcessCourtListPublishedHandled() throws IOException {
        final JsonEnvelope privateEvent = getPayloadEnvelope(PRIVATE_EVENT_COURT_LIST_PUBLISHED, "publish-court-list/progression.event.court-list-published.json");
        doAnswer(this::buildAnswer).when(publishCourtListPayloadBuilderService).buildPayloadForInterestedParties(any(), any(), anyMap(), anyMap(), anyMap());

        underTest.processCourtListPublished(privateEvent);

        verify(publishCourtListPayloadBuilderService).buildPayloadForInterestedParties(any(), any(), anyMap(), anyMap(), anyMap());
        verify(publishCourtListNotificationService, times(2)).sendNotification(any(), any(), any());
    }

    @Test
    public void shouldNotSendNotification_WhenNoFlagPresent_ProcessCourtListPublishedHandled() throws IOException {
        final JsonEnvelope privateEvent = getPayloadEnvelope(PRIVATE_EVENT_COURT_LIST_PUBLISHED, "publish-court-list/progression.event.court-list-published-no-flag.json");

        underTest.processCourtListPublished(privateEvent);

        verify(publishCourtListPayloadBuilderService, never()).buildPayloadForInterestedParties(any(), any(), anyMap(), anyMap(), anyMap());
        verify(publishCourtListNotificationService, never()).sendNotification(any(), any(), any());
    }


    @Test
    public void shouldNotSendNotification_WhenFlagIsFalse_ProcessCourtListPublishedHandled() throws IOException {
        final JsonEnvelope privateEvent = getPayloadEnvelope(PRIVATE_EVENT_COURT_LIST_PUBLISHED, "publish-court-list/progression.event.court-list-published-flag-false.json");

        underTest.processCourtListPublished(privateEvent);

        verify(publishCourtListPayloadBuilderService, never()).buildPayloadForInterestedParties(any(), any(), anyMap(), anyMap(), anyMap());
        verify(publishCourtListNotificationService, never()).sendNotification(any(), any(), any());
    }

    private Object buildAnswer(final InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        PublishCourtListPayload.PublishCourtListPayloadBuilder payloadBuilder = publishCourtListPayloadBuilder().withPublishCourtListType(PublishCourtListType.DRAFT);
        ((Map) args[2]).put(randomUUID().toString(), payloadBuilder);// that's the argument I want to modify
        ((Map) args[3]).put(randomUUID().toString(), payloadBuilder);// that's the argument I want to modify
        return null;
    }

    private JsonEnvelope prepareEnvelope() {
        return envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("public.listing.court-list-published").build(),
                createObjectBuilder().build());
    }

    private String getStringFromResource(final String path) throws IOException {
        return Resources.toString(getResource(path), defaultCharset());
    }

    private JsonEnvelope getPayloadEnvelope(final String eventName, final String filePath) throws IOException {
        final String hearingCasePleaAddOrUpdate = getStringFromResource(filePath);

        final Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withName(eventName)
                .build();
        return JsonEnvelope.envelopeFrom(metadata, new StringToJsonObjectConverter().convert(hearingCasePleaAddOrUpdate));
    }

}