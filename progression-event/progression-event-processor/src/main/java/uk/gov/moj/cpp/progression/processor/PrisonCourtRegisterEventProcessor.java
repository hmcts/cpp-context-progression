package uk.gov.moj.cpp.progression.processor;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;

import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.api.FileStorer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.ApplicationParameters;
import uk.gov.moj.cpp.progression.service.NotificationNotifyService;
import uk.gov.moj.cpp.system.documentgenerator.client.DocumentGeneratorClientProducer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import com.google.common.base.Strings;


@ServiceComponent(EVENT_PROCESSOR)
public class PrisonCourtRegisterEventProcessor {

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
    private static final String HEARING_VENUE = "hearingVenue";
    private static final String HEARING_ID = "hearingId";
    private static final String HEARING_DATE = "hearingDate";

    private static final String PROGRESSION_COMMAND_RECORD_PRISON_COURT_REGISTER_GENERATED = "progression.command.record-prison-court-register-generated";

    @Inject
    private FileStorer fileStorer;

    @Inject
    private DocumentGeneratorClientProducer documentGeneratorClientProducer;

    @Inject
    private PrisonCourtRegisterPdfPayloadGenerator prisonCourtRegisterPdfPayloadGenerator;

    @Inject
    private SystemUserProvider systemUserProvider;

    @Inject
    private Sender sender;

    @Inject
    private ApplicationParameters applicationParameters;

    @Inject
    private NotificationNotifyService notificationNotifyService;

    @Inject
    private UtcClock utcClock;

    @SuppressWarnings("squid:S1160")
    @Handles("progression.event.prison-court-register-recorded")
    public void generatePrisonCourtRegister(final JsonEnvelope envelope) throws IOException, FileServiceException {
        final JsonObject payload = envelope.payloadAsJsonObject();
        final byte[] pdfDocumentInBytes = this.generateDocument(PRISON_COURT_REGISTER_TEMPLATE, prisonCourtRegisterPdfPayloadGenerator.mapPayload(payload));
        final String filename = String.format("PrisonCourtRegister_%s.pdf", utcClock.now().format(TIMESTAMP_FORMATTER));
        final JsonObject metadata = createObjectBuilder().add("fileName", filename).build();
        final UUID fileId = fileStorer.store(metadata, new ByteArrayInputStream(pdfDocumentInBytes));

        final JsonObject prisonCourtRegister = payload.getJsonObject("prisonCourtRegister");

        final JsonObjectBuilder payloadBuilder = createObjectBuilder()
                .add("courtCentreId", prisonCourtRegister.getString("courtCentreId"))
                .add(DEFENDANT, prisonCourtRegister.getJsonObject(DEFENDANT))
                .add(HEARING_VENUE, prisonCourtRegister.getJsonObject(HEARING_VENUE))
                .add(HEARING_ID, prisonCourtRegister.getString(HEARING_ID))
                .add(HEARING_DATE, prisonCourtRegister.getString(HEARING_DATE))
                .add(FILE_ID, fileId.toString())
                .add(FIELD_RECIPIENTS, prisonCourtRegister.getJsonArray(FIELD_RECIPIENTS));

        if(payload.containsKey(ID)){
            payloadBuilder.add(ID, payload.getString(ID));
        }

        sender.send(envelop(payloadBuilder.build())
                .withName(PROGRESSION_COMMAND_RECORD_PRISON_COURT_REGISTER_GENERATED)
                .withMetadataFrom(envelope));
    }

    @SuppressWarnings("squid:S1160")
    @Handles("progression.event.prison-court-register-generated")
    public void sendPrisonCourtRegister(final JsonEnvelope envelope) {
        final JsonObject payload = envelope.payloadAsJsonObject();

        final JsonObject defendant = payload.getJsonObject(DEFENDANT);
        final JsonObject hearingVenue = payload.getJsonObject(HEARING_VENUE);
        final String fileId = payload.getString(FILE_ID);
        final JsonArray recipients = payload.getJsonArray(FIELD_RECIPIENTS);

        final String emailSubject = String.format("RESULTS SUMMARY: %s; %s; %s; %s",
                hearingVenue.getString("courtHouse"),
                defendant.getString("name"), defendant.getString("dateOfBirth"), generateURN(defendant));

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
                    notifyObjectBuilder.add(FILE_ID, fileId);
                    notifyObjectBuilder.add(PERSONALISATION, createObjectBuilder()
                            .add("subject", emailSubject)
                            .add("name_of_defendant", defendant.getString("name"))
                            .build());

                    this.notificationNotifyService.sendEmailNotification(envelope, notifyObjectBuilder.build());
                });
    }

    private String generateURN(JsonObject defendant) {
        final Optional<JsonObject> op = defendant.getJsonArray("prosecutionCasesOrApplications").stream()
                .map(JsonObject.class::cast).findFirst();
        return op.isPresent() ? op.get().getString("caseOrApplicationReference") : "";
    }

    private byte[] generateDocument(final String template, final JsonObject payload) throws IOException {
        return this.documentGeneratorClientProducer.documentGeneratorClient()
                .generatePdfDocument(payload, template, getSystemUser());
    }

    private UUID getSystemUser() {
        return systemUserProvider.getContextSystemUserId()
                .orElseThrow(() -> new RuntimeException("systemUserProvider.getContextSystemUserId() not available"));
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