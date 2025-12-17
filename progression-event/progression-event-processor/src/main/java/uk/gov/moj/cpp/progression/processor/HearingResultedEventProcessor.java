package uk.gov.moj.cpp.progression.processor;

import static java.util.Objects.nonNull;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;

import uk.gov.justice.core.courts.LegalEntityDefendant;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.model.HearingEventLog;
import uk.gov.moj.cpp.progression.model.HearingEventReport;
import uk.gov.moj.cpp.progression.service.HearingService;
import uk.gov.moj.cpp.progression.service.MessageService;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.service.hearingeventlog.HearingEventLogGenerationService;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class HearingResultedEventProcessor {

    public static final String APPLICANT_DETAILS = "applicantDetails";
    public static final String RESPONDENT_DETAILS = "respondentDetails";
    public static final String NAME = "name";
    private static final String PUBLIC_PROGRESSION_HEARING_RESULTED = "public.progression.hearing-resulted";
    private static final String PUBLIC_PROGRESSION_CASE_ARCHIVED = "public.events.progression.case-retention-length-calculated";
    private static final String PUBLIC_HEARING_EVENT_LOGS_DOCUMENT_REJECTED = "public.progression.hearing-event-logs-document-failed";
    private static final String ERROR_MESSAGE = "NO_EVENT_LOGS";
    private static final String CASE_ID = "caseId";
    private static final String APPLICATION_ID = "applicationId";
    private static final String HEARINGS = "hearings";
    private static final Logger LOGGER = LoggerFactory.getLogger(HearingResultedEventProcessor.class.getName());
    private static final String PUBLIC_HEARING_EVENT_LOGS_DOCUMENT_SUCCESS = "public.progression.hearing-event-logs-document-success";
    private static final String REASON = "reason";
    private static final String SUCCESS_STATUS = "success";
    private static final String FAILURE_STATUS = "failed";
    private static final String STATUS = "status";
    private static final String INACTIVE = "INACTIVE";
    private static final String NOCASESTATUS = "NOCASESTATUS";
    private static final String ACTIVE = "ACTIVE";
    private static final String CASE_STATUS = "caseStatus";
    private static final String TARGET = "\"";
    private static final String REPLACEMENT = "";
    @Inject
    private Sender sender;
    @Inject
    private Requester requester;
    @Inject
    private MessageService messageService;
    @Inject
    private HearingEventLogGenerationService hearingEventLogGenerationService;
    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;
    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;
    @Inject
    private HearingService hearingService;
    @Inject
    private ProgressionService progressionService;

    //Need to decommission it
    @Handles("progression.event.hearing-resulted")
    public void processEvent(final JsonEnvelope event) {
        LOGGER.info("progression.event.hearing-resulted event received with metadata {} and payload {}", event.metadata(), event.toObfuscatedDebugString());
        final Metadata metadata = metadataFrom(event.metadata()).withName(PUBLIC_PROGRESSION_HEARING_RESULTED).build();

        final JsonObject outboundPayload = createObjectBuilder()
                .add("hearing", event.payloadAsJsonObject().getJsonObject("hearing"))
                .add("sharedTime", event.payloadAsJsonObject().getJsonString("sharedTime"))
                .build();

        sender.send(envelopeFrom(metadata, outboundPayload));
    }

    @Handles("progression.events.case-retention-length-calculated")
    public void processRetentionCalculated(final JsonEnvelope event) {

        LOGGER.info("progression.events.case-retention-length-calculated event received with metadata {} and payload {}", event.metadata(), event.toObfuscatedDebugString());

        sender.send(JsonEnvelope.envelopeFrom(metadataFrom(event.metadata())
                        .withName(PUBLIC_PROGRESSION_CASE_ARCHIVED),
                event.payload()));
    }

    @Handles("progression.event.hearing-event-logs-document-created")
    public void processHearingLogDocument(final JsonEnvelope event) {
        JsonObject hearingEventLogs = null;
        Optional<JsonObject> caseStatusObject = null;
        LOGGER.info("progression.event.hearing-event-logs-document-created event received with metadata {} ", event.metadata());

        final JsonObject payload = event.payloadAsJsonObject();
        final UUID caseId = UUID.fromString(payload.getString(CASE_ID));
        final Optional<String> applicationId = Optional.ofNullable(payload.containsKey(APPLICATION_ID) ? payload.getString(APPLICATION_ID) : StringUtils.EMPTY);
        if (applicationId.isPresent() && !applicationId.get().isEmpty()) {
            caseStatusObject = progressionService.getCaseStatusForApplicationId(event, applicationId.get(), String.valueOf(caseId));
            if (caseStatusObject.isPresent() && (caseStatusObject.get().size() > 0)) {
                generateAAAGHearingEventLog(event, caseStatusObject, caseId, applicationId);
            } else {
                sendNoHearingLogEvent(event, caseId, applicationId);
            }
        } else {
            LOGGER.info(" If only CaseID is present then Case Hearing Event Log View - CAAG");
            hearingEventLogs = hearingService.getHearingEventLogs(event, caseId, Optional.empty());
            if (nonNull(hearingEventLogs) && hearingEventLogs.size() > 0 && hearingEventLogs.get(HEARINGS) != JsonValue.NULL) {
                generateHearingEventLog(event, hearingEventLogs, caseId, Optional.empty(), Optional.empty());
            } else {
                sendNoHearingLogEvent(event, caseId, applicationId);
            }
        }
    }


    private void sendNoHearingLogEvent(final JsonEnvelope event, final UUID caseId, final Optional<String> applicationId) {
        sender.send(
                envelop(getPayload(caseId, FAILURE_STATUS, ERROR_MESSAGE, applicationId))
                        .withName(PUBLIC_HEARING_EVENT_LOGS_DOCUMENT_REJECTED)
                        .withMetadataFrom(event));
    }

    private void generateAAAGHearingEventLog(final JsonEnvelope event, final Optional<JsonObject> caseStatusObject, final UUID caseId, final Optional<String> applicationId) {
        JsonObject hearingEventLogs;
        if (caseStatusObject.isPresent() && caseStatusObject.get().containsKey(CASE_STATUS) && nonNull(caseStatusObject.get().getString(CASE_STATUS))) {
            hearingEventLogs = hearingService.getHearingEventLogs(event, caseId, applicationId);
            generateAAGHearingEventLogIfCaseStatusPresent(event, caseStatusObject.get(), caseId, applicationId, hearingEventLogs);
        } else {
            LOGGER.info(" If ApplicationId is present with No Case status then Application Hearing Event Log View - AAAG");
            hearingEventLogs = hearingService.getHearingEventLogs(event, caseId, applicationId);
            if (nonNull(hearingEventLogs) && hearingEventLogs.size() > 0 && hearingEventLogs.get(HEARINGS) != JsonValue.NULL) {
                populateApplicantRespondentInEventLog(event, hearingEventLogs, caseId, applicationId, Optional.of(NOCASESTATUS));
            } else {
                sendNoHearingLogEvent(event, caseId, applicationId);
            }
        }
    }

    private void generateAAGHearingEventLogIfCaseStatusPresent(final JsonEnvelope event, final JsonObject caseStatusObject, final UUID caseId, final Optional<String> applicationId, final JsonObject hearingEventLogs) {
        if (nonNull(hearingEventLogs) && hearingEventLogs.size() > 0 && hearingEventLogs.get(HEARINGS) != JsonValue.NULL) {
            if (INACTIVE.equals(caseStatusObject.getString(CASE_STATUS))) {
                LOGGER.info(" If ApplicationId is present and Case is INACTIVE then Application Hearing Event Log View - AAAG");
                populateApplicantRespondentInEventLog(event, hearingEventLogs, caseId, applicationId, Optional.of(INACTIVE));
            } else {
                LOGGER.info(" If ApplicationId is present and Case is ACTIVE then Case Hearing Event Log View - AAAG");
                final JsonObject updatedHearingEventLogs = addCaseHeader(hearingEventLogs, caseStatusObject);
                generateHearingEventLog(event, updatedHearingEventLogs, caseId, applicationId, Optional.of(ACTIVE));
            }
        } else {
            sendNoHearingLogEvent(event, caseId, applicationId);
        }
    }

    private JsonObject removeCaseHeader(final JsonObject hearingEventLogs) {
        final HearingEventLog hearingEventLog = jsonObjectToObjectConverter.convert(hearingEventLogs, HearingEventLog.class);
        hearingEventLog.getHearings().forEach(hearing -> {
            if (nonNull(hearing.getCaseUrns())) {
                hearing.getCaseUrns().clear();
            }
            if (nonNull(hearing.getCaseIds())) {
                hearing.getCaseIds().clear();
            }
        });
        return objectToJsonObjectConverter.convert(hearingEventLog);
    }


    private JsonObject removeApplicationHeader(final JsonObject hearingEventLogs) {
        final HearingEventLog hearingEventLog = jsonObjectToObjectConverter.convert(hearingEventLogs, HearingEventLog.class);
        hearingEventLog.getHearings().forEach(hearing -> {
            if (nonNull(hearing.getApplicationIds())) {
                hearing.getApplicationIds().clear();
            }
            if (nonNull(hearing.getApplicationReferences())) {
                hearing.getApplicationReferences().clear();
            }
        });
        return objectToJsonObjectConverter.convert(hearingEventLog);
    }

    private void populateApplicantRespondentInEventLog(final JsonEnvelope event, final JsonObject hearingEventLogs, final UUID caseId, final Optional<String> applicationId, final Optional<String> caseStatus) {
        JsonObject udpatedHearingEventLogs = null;
        if (caseStatus.isPresent() && (INACTIVE.equals(caseStatus.get()) || NOCASESTATUS.equals(caseStatus.get()))) {
            udpatedHearingEventLogs = removeCaseHeader(hearingEventLogs);
        }
        final HearingEventLog finalHearingEventLog = jsonObjectToObjectConverter.convert(udpatedHearingEventLogs, HearingEventLog.class);
        populateHearingEventLog(event, applicationId, finalHearingEventLog);
        generateHearingEventLog(event, objectToJsonObjectConverter.convert(finalHearingEventLog), caseId, applicationId, caseStatus);
    }

    private void populateHearingEventLog(final JsonEnvelope event, final Optional<String> applicationId, final HearingEventLog hearingEventLog) {
        if (nonNull(hearingEventLog) && nonNull(hearingEventLog.getHearings())) {
            hearingEventLog.getHearings().forEach(hearingEventReport -> {
                        final JsonObject courtApplicationPayload = progressionService.retrieveApplication(event, UUID.fromString(applicationId.get()));
                        if (nonNull(courtApplicationPayload)) {
                            if (nonNull(courtApplicationPayload.getJsonObject(APPLICANT_DETAILS))) {
                                polpulateApplicants(hearingEventReport, courtApplicationPayload);
                            }
                            if (nonNull(courtApplicationPayload.getJsonArray(RESPONDENT_DETAILS))) {
                                polpulateRespondents(hearingEventReport, courtApplicationPayload);
                            }
                        }
                    }
            );
        }
    }

    private void generateHearingEventLog(final JsonEnvelope event, final JsonObject hearingEventLogs, final UUID caseId, final Optional<String> applicationId, final Optional<String> caseStatus) {
        try {
            JsonObject finalHearingEventLogs = null;
            if (applicationId.isPresent() && caseStatus.isPresent() && ACTIVE.equals(caseStatus.get())) {
                finalHearingEventLogs = removeApplicationHeader(hearingEventLogs);
                hearingEventLogGenerationService.generateHearingLogEvent(event, caseId, finalHearingEventLogs, applicationId);
            } else {
                hearingEventLogGenerationService.generateHearingLogEvent(event, caseId, hearingEventLogs, applicationId);
            }
            sender.send(envelop(getPayload(caseId, SUCCESS_STATUS, null, applicationId)).withName(PUBLIC_HEARING_EVENT_LOGS_DOCUMENT_SUCCESS)
                    .withMetadataFrom(event));
        } catch (IOException e) {
            LOGGER.error("Hearing Event Log Pdf document failed ", e);
        }
    }

    private void polpulateApplicants(final HearingEventReport hearingEventReport, final JsonObject application) {
        hearingEventReport.getApplicants().add(application.getJsonObject(APPLICANT_DETAILS).getString(NAME));
    }

    private void polpulateRespondents(final HearingEventReport hearingEventReport, final JsonObject application) {

        for (final JsonObject respondent : application.getJsonArray(RESPONDENT_DETAILS).getValuesAs(JsonObject.class)) {
            if (nonNull(respondent)) {
                hearingEventReport.getRespondents().add(respondent.get(NAME).toString().replace(TARGET, REPLACEMENT));
            }
        }
    }


    private JsonObject getPayload(final UUID caseId, final String status, final String reason, final Optional<String> applicationId) {
        final JsonObjectBuilder payloadBuilder = createObjectBuilder();
        if (!applicationId.isPresent()) {
            payloadBuilder.add(CASE_ID, caseId.toString())
                    .add(STATUS, status);
        } else {
            payloadBuilder.add(APPLICATION_ID, applicationId.get())
                    .add(STATUS, status);
        }
        if (nonNull(reason)) {
            payloadBuilder.add(REASON, reason);
        }
        return payloadBuilder.build();
    }

    private JsonObject addCaseHeader(final JsonObject hearingEventLogs, final JsonObject caseStatusObject) {
        final ProsecutionCase prosecutionCase = jsonObjectToObjectConverter.convert(caseStatusObject, ProsecutionCase.class);
        final HearingEventLog hearingEventLog = jsonObjectToObjectConverter.convert(hearingEventLogs, HearingEventLog.class);
        for (final HearingEventReport hearingEventReport : hearingEventLog.getHearings()) {
            hearingEventReport.getCaseUrns().add(getCaseUrn(prosecutionCase));
            getDefendants(prosecutionCase, hearingEventReport);
        }
        return objectToJsonObjectConverter.convert(hearingEventLog);
    }

    private void getDefendants(final ProsecutionCase prosecutionCase, final HearingEventReport hearingEventReport) {
        prosecutionCase.getDefendants().forEach(defendant ->  {
            final PersonDefendant personDefendant = defendant.getPersonDefendant();
            if (nonNull(personDefendant) && nonNull(personDefendant.getPersonDetails())) {
                hearingEventReport.getDefendants().add(personDefendant.getPersonDetails().getFirstName() + " " + personDefendant.getPersonDetails().getLastName());
            } else {
                final LegalEntityDefendant legalEntityDefendant = defendant.getLegalEntityDefendant();
                if (nonNull(legalEntityDefendant)) {
                    hearingEventReport.getDefendants().add(legalEntityDefendant.getOrganisation().getName());
                }
            }
        });
    }

    private String getCaseUrn(final ProsecutionCase prosecutionCase) {
        return nonNull(prosecutionCase.getProsecutionCaseIdentifier().getCaseURN()) ?
                prosecutionCase.getProsecutionCaseIdentifier().getCaseURN() :
                prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityReference();
    }
}

