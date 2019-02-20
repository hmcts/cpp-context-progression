package uk.gov.moj.cpp.progression.command.api;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;

import javax.inject.Inject;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
/**
 * 
 * @deprecated This is deprecated for Release 2.4
 *
 */
@Deprecated
@ServiceComponent(COMMAND_API)
public class UpdateOffencesForDefendantApi {

    @Inject
    private Sender sender;

    @Handles("progression.command.update-offences-for-defendant")
    public void updateOffencesForDefendant(final JsonEnvelope envelope) {
        sender.send(envelope);
    }
}
