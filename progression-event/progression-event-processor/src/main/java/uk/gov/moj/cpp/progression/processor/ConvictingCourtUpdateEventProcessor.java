package uk.gov.moj.cpp.progression.processor;

import static java.util.UUID.fromString;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;

import uk.gov.justice.core.courts.AddConvictingCourt;
import uk.gov.justice.core.courts.AddConvictingInformation;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.LjaDetails;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.service.RefDataService;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class ConvictingCourtUpdateEventProcessor {

    private static final String CASE_ID = "caseId";
    private static final String CONVICTING_COURT = "convictingCourt";
    private static final String OFFENCE_ID = "offenceId";
    private static final String CONVICTION_DATE = "convictionDate";

    @Inject
    private ProgressionService progressionService;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ConvictingCourtUpdateEventProcessor.class.getName());

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Sender sender;

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Requester requester;

    @Inject
    private RefDataService referenceDataService;

    @Handles("public.sjp.events.conviction-court-resolved")
    public void handleConvictingCourt(final JsonEnvelope envelope) {
        LOGGER.info("Handling public.sjp.events.conviction-court-resolved {}", envelope.payload());
        final JsonObject payload = envelope.payloadAsJsonObject();
        final UUID caseId = fromString(payload.getString(CASE_ID));
        final JsonArray resolvedConvictingInformation = payload.getJsonArray("resolvedConvictingInformation");
        LOGGER.debug("resolvedConvictingInformation {}", resolvedConvictingInformation);
        final List<AddConvictingInformation> addConvictingInfo =
                resolvedConvictingInformation.stream().map(obj ->
                {
                    final UUID offenceId = fromString(((JsonObject) obj).getString(OFFENCE_ID));
                    final ZonedDateTime convictionDate = ZonedDateTime.parse(((JsonObject) obj).getString(CONVICTION_DATE));
                    JsonObject convictingCourt = ((JsonObject) obj).getJsonObject(CONVICTING_COURT);
                    String ouCode = convictingCourt.getString("courtHouseCode");
                    String ljaCode = convictingCourt.getString("ljaCode");
                    CourtCentre courtCentre =  referenceDataService.getCourtByCourtHouseOUCode(ouCode, envelope,requester);
                    LjaDetails ljaDetails = referenceDataService.getLjaDetails(envelope, ljaCode, requester);
                    return new AddConvictingInformation(CourtCentre.courtCentre().withValuesFrom(courtCentre).withLja(ljaDetails).build(),
                            convictionDate, offenceId);
                }).collect(Collectors.toList());
        LOGGER.info("addConvictingInfo {}", addConvictingInfo);
        final Optional<JsonObject> prosecutionCaseDetail = progressionService.getProsecutionCaseDetailById(envelope, payload.getString(CASE_ID));
        final AddConvictingCourt addConvictionCourt = AddConvictingCourt.addConvictingCourt().withCaseId(caseId).withAddConvictingInformation(addConvictingInfo).build();
        LOGGER.info("prosecutionCaseDetail {}", prosecutionCaseDetail);
        if(prosecutionCaseDetail.isPresent()){
            initiateAddConvictingCourt(envelope, addConvictionCourt);
        } else {
            LOGGER.info("There is no prosecutionCaseDetail found in View Store for the case id: {}", CASE_ID);
        }
    }

    private void initiateAddConvictingCourt(JsonEnvelope event, final AddConvictingCourt addConvictionCourt) {
        final JsonObject payload = this.objectToJsonObjectConverter.convert(addConvictionCourt);
        sender.send(envelopeFrom(metadataFrom(event.metadata()).withName("progression.command.add-convicting-court").build(), payload));
    }
}