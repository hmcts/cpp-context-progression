package uk.gov.moj.cpp.progression.command;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

@ServiceComponent(COMMAND_API)
public class CaseNoteApi {

    private final Sender sender;

    @Inject
    public CaseNoteApi(final Sender sender) {
        this.sender = sender;
    }

    @Handles("progression.add-case-note")
    public void addCaseNote(final JsonEnvelope envelope) {
        sender.send(envelop(envelope.payloadAsJsonObject())
                .withName("progression.command.add-case-note")
                .withMetadataFrom(envelope));
    }

}
