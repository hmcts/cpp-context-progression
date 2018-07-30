package uk.gov.moj.cpp.progression.command.api;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;

@ServiceComponent(COMMAND_API)
public class UpdateDefendantApi {

    @Inject
    private Sender sender;

    @Handles("progression.command.update-defendant")
    public void updateDefendant(final JsonEnvelope envelope) {
        sender.send(envelope);
    }
}
