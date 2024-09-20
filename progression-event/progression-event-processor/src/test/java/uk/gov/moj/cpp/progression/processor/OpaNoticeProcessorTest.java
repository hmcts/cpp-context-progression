package uk.gov.moj.cpp.progression.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.progression.helper.TestHelper.getPayload;

import uk.gov.justice.progression.event.OpaPressListNoticeGenerated;
import uk.gov.justice.progression.event.OpaPublicListNoticeGenerated;
import uk.gov.justice.progression.event.OpaResultListNoticeGenerated;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher;
import uk.gov.moj.cpp.progression.plea.json.schemas.OpaNoticeDocument;
import uk.gov.moj.cpp.progression.service.OpaNoticeService;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonObject;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class OpaNoticeProcessorTest {

    private static final String PUBLIC_PROGRESSION_OPA_NOTICE = "public.progression.opa-notice.json";
    private static final String PUBLIC_PROGRESSION_PUBLIC_LIST_OPA_NOTICE_SENT = "public.progression.opa-public-list-notice-sent";
    private static final String PUBLIC_PROGRESSION_PRESS_LIST_OPA_NOTICE_SENT = "public.progression.opa-press-list-notice-sent";
    private static final String PUBLIC_PROGRESSION_RESULT_LIST_OPA_NOTICE_SENT = "public.progression.opa-result-list-notice-sent";
    private static final String PROGRESSION_COMMAND_OPA_PUBLIC_LIST_NOTICE_SENT = "progression.command.opa-public-list-notice-sent";
    private static final String PROGRESSION_COMMAND_OPA_PRESS_LIST_NOTICE_SENT = "progression.command.opa-press-list-notice-sent";
    private static final String PROGRESSION_COMMAND_OPA_RESULT_LIST_NOTICE_SENT = "progression.command.opa-result-list-notice-sent";
    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeArgumentCaptor;
    @Captor
    private ArgumentCaptor<Envelope<OpaPressListNoticeGenerated>> pressListEnvelopeCaptor;
    @Captor
    private ArgumentCaptor<Envelope<OpaPublicListNoticeGenerated>> publicListEnvelopeCaptor;
    @Captor
    private ArgumentCaptor<Envelope<OpaResultListNoticeGenerated>> resultListEnvelopeCaptor;

    @Mock
    private OpaNoticeService opaNoticeService;
    @InjectMocks
    private OpaNoticeProcessor processor;

    @Test
    public void shouldProcessOpaPublicListNoticeRequested() {
        final JsonEnvelope jsonEnvelop = getJsonEnvelop("progression.event.opa-public-list-notice-requested");

        processor.processOpaPublicListNoticeRequested(jsonEnvelop);

        verify(opaNoticeService).generateOpaPublicListNotice(jsonEnvelop);
    }

    @Test
    public void shouldProcessOpaPressListNoticeRequested() {
        final JsonEnvelope jsonEnvelop = getJsonEnvelop("progression.event.opa-press-list-notice-requested");

        processor.processOpaPressListNoticeRequested(jsonEnvelop);

        verify(opaNoticeService).generateOpaPressListNotice(jsonEnvelop);
    }

    @Test
    public void shouldProcessOpaResultListNoticeRequested() {
        final JsonEnvelope jsonEnvelop = getJsonEnvelop("progression.event.opa-result-list-notice-requested");

        processor.processOpaResultListNoticeRequested(jsonEnvelop);

        verify(opaNoticeService).generateOpaResultListNotice(jsonEnvelop);
    }

    @Test
    public void shouldProcessPressListOpaNoticeGeneratedAndRaisePublicEvent() {
        final OpaPressListNoticeGenerated pressListOpaNoticeGenerated = OpaPressListNoticeGenerated.opaPressListNoticeGenerated()
                .withOpaNotice(OpaNoticeDocument.opaNoticeDocument()
                        .withCaseUrn("urn1")
                        .build())
                .withNotificationId(randomUUID())
                .withHearingId(randomUUID())
                .withDefendantId(randomUUID())
                .withTriggerDate(LocalDate.now())
                .build();

        final Envelope<OpaPressListNoticeGenerated> requestMessage = Envelope.envelopeFrom(
                metadataWithRandomUUID("progression.event.press-list-opa-notice-generated"),
                pressListOpaNoticeGenerated);

        processor.processPressListOpaNoticeGenerated(requestMessage);

        verify(sender).send(pressListEnvelopeCaptor.capture());

        final Envelope<OpaPressListNoticeGenerated> publicEvent = pressListEnvelopeCaptor.getValue();
        assertThat(publicEvent.metadata(), is(withMetadataEnvelopedFrom(requestMessage).withName("public.progression.press-list-opa-notice-generated")));
        assertThat(publicEvent.payload(), is(pressListOpaNoticeGenerated));
    }

    @Test
    public void shouldProcessPublicListOpaNoticeGeneratedAndRaisePublicEvent() {
        final OpaPublicListNoticeGenerated publicListOpaNoticeGenerated = OpaPublicListNoticeGenerated.opaPublicListNoticeGenerated()
                .withOpaNotice(OpaNoticeDocument.opaNoticeDocument()
                        .withCaseUrn("urn1")
                        .build())
                .withNotificationId(randomUUID())
                .withHearingId(randomUUID())
                .withDefendantId(randomUUID())
                .withTriggerDate(LocalDate.now())
                .build();

        final Envelope<OpaPublicListNoticeGenerated> requestMessage = Envelope.envelopeFrom(
                metadataWithRandomUUID("progression.event.public-list-opa-notice-generated"),
                publicListOpaNoticeGenerated);

        processor.processPublicListOpaNoticeGenerated(requestMessage);

        verify(sender).send(publicListEnvelopeCaptor.capture());

        final Envelope<OpaPublicListNoticeGenerated> publicEvent = publicListEnvelopeCaptor.getValue();
        assertThat(publicEvent.metadata(), is(withMetadataEnvelopedFrom(requestMessage).withName("public.progression.public-list-opa-notice-generated")));
        assertThat(publicEvent.payload(), is(publicListOpaNoticeGenerated));
    }

    @Test
    public void shouldProcessResultListOpaNoticeGeneratedAndRaisePublicEvent() {
        final OpaResultListNoticeGenerated resultListOpaNoticeGenerated = OpaResultListNoticeGenerated.opaResultListNoticeGenerated()
                .withOpaNotice(OpaNoticeDocument.opaNoticeDocument()
                        .withCaseUrn("urn1")
                        .build())
                .withNotificationId(randomUUID())
                .withHearingId(randomUUID())
                .withDefendantId(randomUUID())
                .withTriggerDate(LocalDate.now())
                .build();

        final Envelope<OpaResultListNoticeGenerated> requestMessage = Envelope.envelopeFrom(
                metadataWithRandomUUID("progression.event.result-list-opa-notice-generated"),
                resultListOpaNoticeGenerated);

        processor.processResultListOpaNoticeGenerated(requestMessage);

        verify(sender).send(resultListEnvelopeCaptor.capture());

        final Envelope<OpaResultListNoticeGenerated> publicEvent = resultListEnvelopeCaptor.getValue();
        assertThat(publicEvent.metadata(), is(withMetadataEnvelopedFrom(requestMessage).withName("public.progression.result-list-opa-notice-generated")));
        assertThat(publicEvent.payload(), is(resultListOpaNoticeGenerated));
    }

    @Test
    public void testProcessOpaPublicListNoticeSent() {
        final JsonObject opaNoticeSent = getPayload(PUBLIC_PROGRESSION_OPA_NOTICE);
        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID(PUBLIC_PROGRESSION_PUBLIC_LIST_OPA_NOTICE_SENT),
                opaNoticeSent);

        processor.processOpaPublicListNoticeSent(event);

        verify(sender).send(envelopeArgumentCaptor.capture());
        final Envelope<JsonObject> envelope = envelopeArgumentCaptor.getValue();

        verifyOpaNoticeSentContents(envelope, opaNoticeSent, PROGRESSION_COMMAND_OPA_PUBLIC_LIST_NOTICE_SENT);
    }

    @Test
    public void testProcessOpaPressListNoticeSent() {
        final JsonObject opaNoticeSent = getPayload(PUBLIC_PROGRESSION_OPA_NOTICE);
        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID(PUBLIC_PROGRESSION_PRESS_LIST_OPA_NOTICE_SENT),
                opaNoticeSent);

        processor.processOpaPressListNoticeSent(event);

        verify(sender).send(envelopeArgumentCaptor.capture());
        final Envelope<JsonObject> envelope = envelopeArgumentCaptor.getValue();

        verifyOpaNoticeSentContents(envelope, opaNoticeSent, PROGRESSION_COMMAND_OPA_PRESS_LIST_NOTICE_SENT);
    }

    @Test
    public void testProcessOpaResultListNoticeSent() {
        final JsonObject opaNoticeSent = getPayload(PUBLIC_PROGRESSION_OPA_NOTICE);
        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID(PUBLIC_PROGRESSION_RESULT_LIST_OPA_NOTICE_SENT),
                opaNoticeSent);

        processor.processOpaResultListNoticeSent(event);

        verify(sender).send(envelopeArgumentCaptor.capture());
        final Envelope<JsonObject> envelope = envelopeArgumentCaptor.getValue();

        verifyOpaNoticeSentContents(envelope, opaNoticeSent, PROGRESSION_COMMAND_OPA_RESULT_LIST_NOTICE_SENT);
    }

    private JsonEnvelope getJsonEnvelop(final String commandName) {
        return envelopeFrom(
                metadataBuilder()
                        .createdAt(ZonedDateTime.now())
                        .withName(commandName)
                        .withId(randomUUID())
                        .build(),
                Json.createObjectBuilder().build());
    }

    private void verifyOpaNoticeSentContents(final Envelope<JsonObject> envelope, final JsonObject opaNoticeSent, final String event) {
        final JsonEnvelope jsonEnvelope = envelopeFrom(
                metadataFrom(envelope.metadata()),
                envelope.payload());
        final Stream<JsonEnvelope> envelopeStream = Stream.of(jsonEnvelope);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName(event),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.hearingId", CoreMatchers.is(opaNoticeSent.getString("hearingId"))),
                                withJsonPath("$.defendantId", CoreMatchers.is(opaNoticeSent.getString("defendantId"))),
                                withJsonPath("$.notificationId", CoreMatchers.is(opaNoticeSent.getString("notificationId"))),
                                withJsonPath("$.triggerDate", CoreMatchers.is(opaNoticeSent.getString("triggerDate"))))))));
    }
}
