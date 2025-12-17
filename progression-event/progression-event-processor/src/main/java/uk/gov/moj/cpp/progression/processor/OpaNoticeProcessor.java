package uk.gov.moj.cpp.progression.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.progression.event.OpaPressListNoticeGenerated;
import uk.gov.justice.progression.event.OpaPublicListNoticeGenerated;
import uk.gov.justice.progression.event.OpaResultListNoticeGenerated;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.OpaNoticeService;

import java.time.LocalDate;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class OpaNoticeProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpaNoticeProcessor.class);
    private static final String LOG_OUTPUT_FORMAT = "raised public event {} correlationId: {}, notificationId:{}";
    private static final String PROGRESSION_COMMAND_OPA_PUBLIC_LIST_NOTICE_SENT = "progression.command.opa-public-list-notice-sent";
    private static final String PROGRESSION_COMMAND_OPA_PRESS_LIST_NOTICE_SENT = "progression.command.opa-press-list-notice-sent";
    private static final String PROGRESSION_COMMAND_OPA_RESULT_LIST_NOTICE_SENT = "progression.command.opa-result-list-notice-sent";

    @Inject
    private Sender sender;

    @Inject
    private OpaNoticeService opaNoticeService;

    @Handles("progression.event.opa-public-list-notice-requested")
    public void processOpaPublicListNoticeRequested(final JsonEnvelope envelope) {
        LOGGER.info("progression.event.opa-public-list-notice-requested received at {}", LocalDate.now());

        opaNoticeService.generateOpaPublicListNotice(envelope);
    }

    @Handles("progression.event.opa-press-list-notice-requested")
    public void processOpaPressListNoticeRequested(final JsonEnvelope envelope) {
        LOGGER.info("progression.event.opa-press-list-notice-requested received at {}", LocalDate.now());
        opaNoticeService.generateOpaPressListNotice(envelope);
    }

    @Handles("progression.event.opa-result-list-notice-requested")
    public void processOpaResultListNoticeRequested(final JsonEnvelope envelope) {
        LOGGER.info("progression.event.opa-result-list-notice-requested received at {}", LocalDate.now());
        opaNoticeService.generateOpaResultListNotice(envelope);
    }

    @Handles("progression.event.opa-press-list-notice-generated")
    public void processPressListOpaNoticeGenerated(final Envelope<OpaPressListNoticeGenerated> event) {
        final Envelope<OpaPressListNoticeGenerated> envelope = envelop(event.payload())
                .withName("public.progression.press-list-opa-notice-generated")
                .withMetadataFrom(event);

        sender.send(envelope);

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(LOG_OUTPUT_FORMAT, envelope.metadata().name(), envelope.metadata().clientCorrelationId().orElse(null), envelope.payload().getNotificationId());
        }
    }

    @Handles("progression.event.opa-public-list-notice-generated")
    public void processPublicListOpaNoticeGenerated(final Envelope<OpaPublicListNoticeGenerated> event) {
        final Envelope<OpaPublicListNoticeGenerated> envelope = envelop(event.payload())
                .withName("public.progression.public-list-opa-notice-generated")
                .withMetadataFrom(event);

        sender.send(envelope);

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(LOG_OUTPUT_FORMAT, envelope.metadata().name(), envelope.metadata().clientCorrelationId().orElse(null), envelope.payload().getNotificationId());
        }
    }

    @Handles("progression.event.opa-result-list-notice-generated")
    public void processResultListOpaNoticeGenerated(final Envelope<OpaResultListNoticeGenerated> event) {
        final Envelope<OpaResultListNoticeGenerated> envelope = envelop(event.payload())
                .withName("public.progression.result-list-opa-notice-generated")
                .withMetadataFrom(event);

        sender.send(envelope);

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(LOG_OUTPUT_FORMAT, envelope.metadata().name(), envelope.metadata().clientCorrelationId().orElse(null), envelope.payload().getNotificationId());
        }
    }

    @Handles("public.stagingpubhub.opa-public-list-notice-sent")
    public void processOpaPublicListNoticeSent(final JsonEnvelope envelope) {
        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(PROGRESSION_COMMAND_OPA_PUBLIC_LIST_NOTICE_SENT),
                envelope.payloadAsJsonObject()));
    }
    @Handles("public.stagingpubhub.opa-press-list-notice-sent")
    public void processOpaPressListNoticeSent(final JsonEnvelope envelope) {
        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(PROGRESSION_COMMAND_OPA_PRESS_LIST_NOTICE_SENT),
                envelope.payloadAsJsonObject()));
    }
    @Handles("public.stagingpubhub.opa-result-list-notice-sent")
    public void processOpaResultListNoticeSent(final JsonEnvelope envelope) {
        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(PROGRESSION_COMMAND_OPA_RESULT_LIST_NOTICE_SENT),
                envelope.payloadAsJsonObject()));
    }
}
