package uk.gov.moj.cpp.progression.command;

import static java.util.UUID.fromString;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.command.service.UserGroupQueryService;

import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("squid:S3655")
@ServiceComponent(COMMAND_API)
public class HearingEventLogApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingEventLogApi.class);
    @Inject
    private Sender sender;
    @Inject
    private UserGroupQueryService userGroupQueryService;

    @Handles("progression.create-hearing-event-log-document")
    public void handleHearingEventLog(final JsonEnvelope command) {

        final UUID userId = command.metadata().userId().isPresent() ? fromString(command.metadata().userId().get()) : null;

        final boolean flag = userGroupQueryService.doesUserBelongsToHmctsOrganisation(command, userId);

        if (flag) {
            sender.send(Envelope.envelopeFrom(metadataFrom(command.metadata()).withName("progression.command.create-hearing-event-log-document").build(),
                    command.payloadAsJsonObject()));
        } else {
            LOGGER.error("Hearing Event Log failed due to Organisation ID mismatch");
        }
    }

    @Handles("progression.create-aaag-hearing-event-log-document")
    public void handleAaagHearingEventLog(final JsonEnvelope command) {

        final UUID userId = command.metadata().userId().isPresent() ? fromString(command.metadata().userId().get()) : null;

        final boolean flag = userGroupQueryService.doesUserBelongsToHmctsOrganisation(command, userId);

        if (flag) {
            sender.send(Envelope.envelopeFrom(metadataFrom(command.metadata()).withName("progression.command.create-aaag-hearing-event-log-document").build(),
                    command.payloadAsJsonObject()));
        } else {
            LOGGER.error("AAAG Hearing Event Log failed due to Organisation ID mismatch");
        }
    }
}

