package uk.gov.moj.cpp.progression.command;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.moj.cpp.progression.command.api.UserDetailsLoader.isUserHasPermissionForApplicationTypeCode;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.common.exception.ForbiddenRequestException;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(COMMAND_API)
public class InitiateCourtApplicationProceedingsCommandApi {

    private static final Pattern URN_PATTERN = Pattern.compile("^[A-Z0-9]{10}$");

    @Inject
    private Sender sender;

    @Inject
    private Requester requester;

    @Handles("progression.initiate-court-proceedings-for-application")
    public void initiateCourtApplicationProceedings(final JsonEnvelope command) {

        if(isUserNotAuthorised(command)){
            throw new ForbiddenRequestException("User is not authorised to use this application type!");
        }

        validateInputsForApplication(command.payloadAsJsonObject());

        this.sender.send(Envelope.envelopeFrom(metadataFrom(command.metadata()).withName("progression.command.initiate-court-proceedings-for-application").build(),
                command.payloadAsJsonObject()));
    }

    private void validateInputsForApplication(final JsonObject jsonObject) {
        final JsonObject courtApplication = jsonObject.getJsonObject("courtApplication");
        if(courtApplication.containsKey("applicationReference") && isNotValidUrn(courtApplication.getString("applicationReference"))) {
            throw new BadRequestException("Entered URN is not valid!");
        }
    }

    private boolean isNotValidUrn(final String applicationReference) {
        return !URN_PATTERN.matcher(applicationReference).matches();
    }

    private boolean isUserNotAuthorised(final JsonEnvelope command) {
        final String applicationTypeCode = command.payloadAsJsonObject().getJsonObject("courtApplication").getJsonObject("type").getString("code");
        return !isUserHasPermissionForApplicationTypeCode(command.metadata(), requester, applicationTypeCode);
    }

    @Handles("progression.edit-court-proceedings-for-application")
    public void editCourtApplicationProceedings(final JsonEnvelope command) {
        this.sender.send(Envelope.envelopeFrom(metadataFrom(command.metadata()).withName("progression.command.edit-court-proceedings-for-application").build(),
                command.payloadAsJsonObject()));
    }

    @Handles("progression.add-breach-application")
    public void addBreachApplication(final JsonEnvelope command) {
        this.sender.send(Envelope.envelopeFrom(metadataFrom(command.metadata()).withName("progression.command.add-breach-application").build(),
                command.payloadAsJsonObject()));
    }

}
