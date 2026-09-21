package uk.gov.moj.cpp.progression.command;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.fromString;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.core.courts.LinkType.STANDALONE;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.moj.cpp.progression.command.api.UserDetailsLoader.getAllowedHearingTypes;
import static uk.gov.moj.cpp.progression.command.api.UserDetailsLoader.isUserHasPermissionForApplicationTypeCode;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.common.exception.ForbiddenRequestException;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(COMMAND_API)
public class InitiateCourtApplicationProceedingsCommandApi {

    private static final Pattern URN_PATTERN = Pattern.compile("^[A-Z0-9]{11}$");
    private static final String LINK_TYPE = "linkType";
    public static final String COURT_APPLICATION = "courtApplication";

    @Inject
    private Sender sender;

    @Inject
    private Requester requester;

    @Handles("progression.initiate-court-proceedings-for-application")
    public void initiateCourtApplicationProceedings(final JsonEnvelope command) {

        if (isUserNotAuthorised(command)) {
            throw new ForbiddenRequestException("User is not authorised to use this application type!");
        }

        validateInputsForApplication(command);

        this.sender.send(Envelope.envelopeFrom(metadataFrom(command.metadata()).withName("progression.command.initiate-court-proceedings-for-application").build(),
                command.payloadAsJsonObject()));
    }

    private void validateDefaultHearingType(final JsonEnvelope command) {
        final JsonObject payload = command.payloadAsJsonObject();
        final JsonObject courtApplication = payload.getJsonObject(COURT_APPLICATION);
        if (!standaloneApplication(courtApplication)) {
            return;
        }

        final String applicationTypeId = courtApplication.getJsonObject("type").getString("id");
        final List<UUID> allowedHearingTypes = getAllowedHearingTypes(command.metadata(), requester, applicationTypeId);
        if (isNotEmpty(allowedHearingTypes)) {
            final Optional<UUID> submittedHearingTypeId = submittedHearingTypeId(payload);
            if (!submittedHearingTypeId.isPresent() || !allowedHearingTypes.contains(submittedHearingTypeId.get())) {
                throw new BadRequestException("Hearing type must be one of the allowed hearing types for this application type!");
            }
        }
    }

    private Optional<UUID> submittedHearingTypeId(final JsonObject payload) {
        if (!payload.containsKey("courtHearing")) {
            return empty();
        }
        final JsonObject courtHearing = payload.getJsonObject("courtHearing");
        if (!courtHearing.containsKey("hearingType")) {
            return empty();
        }
        return of(fromString(courtHearing.getJsonObject("hearingType").getString("id")));
    }

    private void validateInputsForApplication(final JsonEnvelope command) {
        final JsonObject courtApplication = command.payloadAsJsonObject().getJsonObject(COURT_APPLICATION);
        if (standaloneApplication(courtApplication) && courtApplication.containsKey("applicationReference") && isNotValidUrn(courtApplication.getString("applicationReference"))) {
            throw new BadRequestException("Entered URN is not valid!");
        }
        validateDefaultHearingType(command);
    }

    private boolean standaloneApplication(final JsonObject courtApplication) {
        return STANDALONE.toString().equals(courtApplication.getJsonObject("type").getString(LINK_TYPE));
    }

    private boolean isNotValidUrn(final String applicationReference) {
        return !URN_PATTERN.matcher(applicationReference).matches();
    }

    private boolean isUserNotAuthorised(final JsonEnvelope command) {
        final String applicationTypeCode = command.payloadAsJsonObject().getJsonObject(COURT_APPLICATION).getJsonObject("type").getString("code");
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
