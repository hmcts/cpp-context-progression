package uk.gov.moj.cpp.progression.processor;

import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.core.courts.BoxHearingRequest;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.progression.courts.SendStatdecAppointmentLetter;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.StatDecNotificationService;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(EVENT_PROCESSOR)
public class SendStatDecAppointmentLetterProcessor {

    private static final String STAT_DEC_VIRTUAL_HEARING = "NPE_StatutoryDeclarationVirtualHearing";
    private static final String STAT_DEC_COURT_HEARING = "NPE_StatutoryDeclarationHearing";
    private static final ZoneId ZONE_ID = ZoneId.of(ZoneOffset.UTC.getId());


    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private StatDecNotificationService statDecNotificationService;

    @Handles("progression.event.send-statdec-appointment-letter")
    public void process(final JsonEnvelope jsonEnvelope) {

        final JsonObject requestJson = jsonEnvelope.payloadAsJsonObject();

        final SendStatdecAppointmentLetter sendStatdecAppointmentLetter = jsonObjectToObjectConverter.convert(requestJson, SendStatdecAppointmentLetter.class);
        final BoxHearingRequest boxHearingRequest = sendStatdecAppointmentLetter.getBoxHearing();
        final ZonedDateTime hearingStartDateTime = nonNull(boxHearingRequest.getVirtualAppointmentTime()) ? boxHearingRequest.getVirtualAppointmentTime() :
                boxHearingRequest.getApplicationDueDate().atStartOfDay(ZONE_ID);
        final CourtApplication courtApplication = sendStatdecAppointmentLetter.getCourtApplication();

        final String documentTemplateName = nonNull(boxHearingRequest.getVirtualAppointmentTime()) ? STAT_DEC_VIRTUAL_HEARING : STAT_DEC_COURT_HEARING;

        statDecNotificationService.sendNotification(jsonEnvelope, randomUUID(), courtApplication, boxHearingRequest.getCourtCentre(), hearingStartDateTime, boxHearingRequest.getJurisdictionType(), documentTemplateName);


    }
}
