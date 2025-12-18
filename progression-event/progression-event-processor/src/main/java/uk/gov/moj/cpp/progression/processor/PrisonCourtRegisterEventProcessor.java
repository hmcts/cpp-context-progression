package uk.gov.moj.cpp.progression.processor;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.justice.core.courts.PrisonCourtRegisterGenerated;
import uk.gov.justice.core.courts.PrisonCourtRegisterRecorded;
import uk.gov.justice.core.courts.prisonCourtRegisterDocument.PrisonCourtRegisterCaseOrApplication;
import uk.gov.justice.core.courts.prisonCourtRegisterDocument.PrisonCourtRegisterDefendant;
import uk.gov.justice.core.courts.prisonCourtRegisterDocument.PrisonCourtRegisterHearingVenue;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.ApplicationParameters;
import uk.gov.moj.cpp.progression.service.ConversionFormat;
import uk.gov.moj.cpp.progression.service.DocumentGenerationRequest;
import uk.gov.moj.cpp.progression.service.FileService;
import uk.gov.moj.cpp.progression.service.NotificationNotifyService;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.service.SystemDocGeneratorService;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;


@ServiceComponent(EVENT_PROCESSOR)
public class PrisonCourtRegisterEventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrisonCourtRegisterEventProcessor.class);
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String PRISON_COURT_REGISTER_TEMPLATE = "OEE_Layout5";
    private static final String FIELD_RECIPIENTS = "recipients";
    private static final String FIELD_NOTIFICATION_ID = "notificationId";
    private static final String FIELD_TEMPLATE_ID = "templateId";
    private static final String SEND_TO_ADDRESS = "sendToAddress";
    private static final String FILE_ID = "fileId";
    private static final String ID = "id";
    private static final String PERSONALISATION = "personalisation";
    private static final String DEFENDANT = "defendant";
    private static final String COURT_CENTRE_ID = "courtCentreId";
    private static final String HEARING_VENUE = "hearingVenue";
    private static final String PRISON_COURT_REGISTER = "PRISON_COURT_REGISTER";
    private static final String HEARING_ID = "hearingId";
    private static final String HEARING_DATE = "hearingDate";
    private static final String PROGRESSION_COMMAND_RECORD_PRISON_COURT_REGISTER_DOCUMENT_SENT = "progression.command.record-prison-court-register-document-sent";
    public static final String PAYLOAD_FILE_ID = "payloadFileId";
    public static final String PIPE_CHARACTER = "|";
    public static final String PRISON_COURT_REGISTER_ID = "prisonCourtRegisterId";
    public static final String PRISON_COURT_DEFENDANT_NAME = "defendantName";
    public static final String PRISON_COURT_CASE_ID = "caseId";
    public static final String PRISON_COURT_REGISTER_STREAM_ID = "prisonCourtRegisterStreamId";
    private static final String CASE_ID = "caseId";
    @Inject
    private Sender sender;

    @Inject

    private ApplicationParameters applicationParameters;

    @Inject
    private NotificationNotifyService notificationNotifyService;

    @Inject
    private UtcClock utcClock;

    @Inject
    private SystemDocGeneratorService systemDocGeneratorService;

    @Inject
    private FileService fileService;

    @Inject
    private JsonObjectToObjectConverter converter;
    @Inject
    private ProgressionService progressionService;
    @Inject
    private PrisonCourtRegisterPdfPayloadGenerator prisonCourtRegisterPdfPayloadGenerator;

    @SuppressWarnings("squid:S1160")
    @Handles("progression.event.prison-court-register-recorded")
    public void generatePrisonCourtRegister(final JsonEnvelope envelope) {
        LOGGER.info("progression.event.prison-court-register-recorded {}", envelope.payload());
        final JsonObject payload = envelope.payloadAsJsonObject();

        final JsonObject prisonCourtRegister = payload.getJsonObject("prisonCourtRegister");

        final String prisonCourtRegisterStreamId = payload.getString(PRISON_COURT_REGISTER_STREAM_ID);

        final JsonObject mappedPayload = prisonCourtRegisterPdfPayloadGenerator.mapPayload(payload);

        final String fileName = String.format("PrisonCourtRegister_%s.pdf", utcClock.now().format(TIMESTAMP_FORMATTER));

        final UUID fileId = fileService.storePayload(mappedPayload, fileName, PRISON_COURT_REGISTER_TEMPLATE);

        final String prisonCourtRegisterId = payload.getString(ID);

        String defendantName = "";
        if (prisonCourtRegister.containsKey(DEFENDANT) &&
                prisonCourtRegister.getJsonObject(DEFENDANT).containsKey("name")) {
            defendantName = prisonCourtRegister.getJsonObject(DEFENDANT).getString("name");
        }
        final String caseId = getCaseUUID(envelope, payload);
        sendRequestToGenerateDocumentAsync(envelope, prisonCourtRegisterStreamId, fileId, prisonCourtRegisterId,
                defendantName, caseId);

        final JsonObjectBuilder payloadBuilder = createObjectBuilder()
                .add(COURT_CENTRE_ID, prisonCourtRegister.getString(COURT_CENTRE_ID))
                .add(DEFENDANT, prisonCourtRegister.getJsonObject(DEFENDANT))
                .add(HEARING_VENUE, prisonCourtRegister.getJsonObject(HEARING_VENUE))
                .add(HEARING_ID, prisonCourtRegister.getString(HEARING_ID))
                .add(HEARING_DATE, prisonCourtRegister.getString(HEARING_DATE))
                .add(PAYLOAD_FILE_ID, fileId.toString())
                .add(PRISON_COURT_REGISTER_STREAM_ID, prisonCourtRegisterStreamId)
                .add(FIELD_RECIPIENTS, prisonCourtRegister.getJsonArray(FIELD_RECIPIENTS));

        if (payload.containsKey(ID)) {
            payloadBuilder.add(ID, payload.getString(ID));
        }

        sender.send(envelop(payloadBuilder.build())
                .withName(PROGRESSION_COMMAND_RECORD_PRISON_COURT_REGISTER_DOCUMENT_SENT)
                .withMetadataFrom(envelope));
    }

    private String getCaseUUID(JsonEnvelope envelope, JsonObject mappedPayload) {
        List<UUID> caseUUIDList = new ArrayList<>();
        final PrisonCourtRegisterRecorded prisonCourtRegisterRecorded = converter.convert(mappedPayload, PrisonCourtRegisterRecorded.class);

        List<PrisonCourtRegisterCaseOrApplication> pcrCaseOrApplicationList = Optional.ofNullable(prisonCourtRegisterRecorded)
                .map(pcr -> pcr.getPrisonCourtRegister())
                .map(defendant -> defendant.getDefendant())
                .map(pcrCaseOrApplication -> pcrCaseOrApplication.getProsecutionCasesOrApplications())
                .stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());

        List<String> urnList = pcrCaseOrApplicationList.stream()
                .map(PrisonCourtRegisterCaseOrApplication::getCaseOrApplicationReference)
                .distinct()
                .collect(Collectors.toList());

        urnList.stream().forEach(urn -> {
            Optional<JsonObject> caseIdJsonObject = progressionService.caseExistsByCaseUrn(envelope, urn);
            caseIdJsonObject.ifPresent(jsonObject -> {
                if (jsonObject.containsKey(CASE_ID)) {
                    caseUUIDList.add(fromString(jsonObject.getString(CASE_ID)));
                }
            });
        });

        return caseUUIDList.isEmpty() ? "" : caseUUIDList.get(0).toString();
    }

    @SuppressWarnings("squid:S1160")
    @Handles("progression.event.prison-court-register-generated")
    public void sendPrisonCourtRegister(final JsonEnvelope envelope) {
        LOGGER.info("progression.event.prison-court-register-generated {}", envelope.payload());
        final JsonObject payload = envelope.payloadAsJsonObject();
        final PrisonCourtRegisterGenerated prisonCourtRegisterGenerated = converter.convert(payload, PrisonCourtRegisterGenerated.class);
        final PrisonCourtRegisterDefendant defendant = prisonCourtRegisterGenerated.getDefendant();
        final PrisonCourtRegisterHearingVenue hearingVenue = prisonCourtRegisterGenerated.getHearingVenue();
        final UUID fileId = prisonCourtRegisterGenerated.getFileId();


        final JsonArray recipients = payload.getJsonArray(FIELD_RECIPIENTS);

        final String emailSubject = String.format("RESULTS SUMMARY: %s; %s; %s; %s",
                getCourtHouse(hearingVenue), defendant.getName(), getDefendantDateOfBirth(defendant), generateURN(defendant));

        recipients.stream().map(JsonObject.class::cast)
                .filter(recipient -> recipient.containsKey("emailAddress1"))
                .flatMap(recipient -> Stream.of(
                        new EmailPair(recipient.getString("emailAddress1"), recipient.getString("emailTemplateName")))
                )
                .filter(pair -> !Strings.isNullOrEmpty(pair.getEmail()))
                .forEach(pair -> {
                    final JsonObjectBuilder notifyObjectBuilder = createObjectBuilder();
                    notifyObjectBuilder.add(FIELD_NOTIFICATION_ID, UUID.randomUUID().toString());
                    notifyObjectBuilder.add(FIELD_TEMPLATE_ID, applicationParameters.getEmailTemplateId(pair.getTemplate()));
                    notifyObjectBuilder.add(SEND_TO_ADDRESS, pair.getEmail());
                    notifyObjectBuilder.add(FILE_ID, fileId.toString());
                    notifyObjectBuilder.add(PERSONALISATION, createObjectBuilder()
                            .add("subject", emailSubject)
                            .add("name_of_defendant", defendant.getName())
                            .build());

                    this.notificationNotifyService.sendEmailNotification(envelope, notifyObjectBuilder.build());
                });
    }

    private String getDefendantDateOfBirth(final PrisonCourtRegisterDefendant defendant) {
        if(nonNull(defendant.getDateOfBirth())) {
            return defendant.getDateOfBirth();
        }
        return EMPTY;
    }

    private String getCourtHouse(final PrisonCourtRegisterHearingVenue hearingVenue) {
        if(nonNull(hearingVenue) && nonNull(hearingVenue.getCourtHouse())){
            return hearingVenue.getCourtHouse();
        }
        return EMPTY;
    }

    private void sendRequestToGenerateDocumentAsync(
            final JsonEnvelope envelope, final String prisonCourtRegisterStreamId, final UUID fileId, final String prisonCourtRegisterId,
            final String defendantName, final String caseId) {

        Map<String, String> additionalInformation = ImmutableMap.of(PRISON_COURT_REGISTER_ID, prisonCourtRegisterId,
                PRISON_COURT_DEFENDANT_NAME, defendantName,
                PRISON_COURT_CASE_ID, caseId);

        final DocumentGenerationRequest documentGenerationRequest = new DocumentGenerationRequest(
                PRISON_COURT_REGISTER,
                PRISON_COURT_REGISTER_TEMPLATE,
                ConversionFormat.PDF,
                prisonCourtRegisterStreamId,
                fileId,
                additionalInformation);

        systemDocGeneratorService.generateDocument(documentGenerationRequest, envelope);
    }

    private String generateURN(PrisonCourtRegisterDefendant defendant) {
        if(nonNull(defendant.getProsecutionCasesOrApplications()) && !defendant.getProsecutionCasesOrApplications().isEmpty()) {
            return defendant.getProsecutionCasesOrApplications().get(0).getCaseOrApplicationReference();
        }
        return EMPTY;
    }

    class EmailPair {
        private String email;
        private String template;

        public EmailPair(final String email, final String template) {
            this.email = email;
            this.template = template;
        }

        public String getEmail() {
            return email;
        }

        public String getTemplate() {
            return template;
        }
    }
}