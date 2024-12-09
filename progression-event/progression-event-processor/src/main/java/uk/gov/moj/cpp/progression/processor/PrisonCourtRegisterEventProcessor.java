package uk.gov.moj.cpp.progression.processor;

import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;

import org.slf4j.LoggerFactory;
import uk.gov.justice.core.courts.CaseDocument;
import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.DocumentCategory;
import uk.gov.justice.core.courts.Material;
import uk.gov.justice.core.courts.PrisonCourtRegisterRecorded;
import uk.gov.justice.core.courts.prisonCourtRegisterDocument.PrisonCourtRegisterCaseOrApplication;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.api.FileStorer;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.ApplicationParameters;
import uk.gov.moj.cpp.progression.service.MaterialService;
import uk.gov.moj.cpp.progression.service.NotificationNotifyService;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.system.documentgenerator.client.DocumentGeneratorClientProducer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import com.google.common.base.Strings;


@ServiceComponent(EVENT_PROCESSOR)
public class PrisonCourtRegisterEventProcessor {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter TIMESTAMP_FORMATTER_FOR_FILE_UPLOAD = DateTimeFormatter.ofPattern("ddMMyy");
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(PrisonCourtRegisterEventProcessor.class);
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
    private MaterialService materialService;

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
    private JsonObjectToObjectConverter converter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;
    @Inject
    private UtcClock utcClock;
    @Inject
    ProgressionService progressionService;

    private static final String DOCUMENT_TYPE_DESCRIPTION = "Electronic Notifications" ;
    private static final UUID CASE_DOCUMENT_TYPE_ID = fromString("f471eb51-614c-4447-bd8d-28f9c2815c9e");
    private static final String APPLICATION_PDF = "application/pdf";
    private static final String MATERIAL_ID = "materialId" ;
    private static final String COURT_DOCUMENT = "courtDocument" ;
    private static final String PROGRESSION_COMMAND_ADD_COURT_DOCUMENT = "progression.command.add-court-document" ;
    private static final String CASE_ID = "caseId";

    @SuppressWarnings("squid:S1160")
    @Handles("progression.event.prison-court-register-recorded")
    public void generatePrisonCourtRegister(final JsonEnvelope envelope) throws IOException, FileServiceException {
        LOGGER.info(">>2047 generatePrisonCourtRegister payload {}", envelope.payloadAsJsonObject());
        final JsonObject payload = envelope.payloadAsJsonObject();
        JsonObject mappedPayload = prisonCourtRegisterPdfPayloadGenerator.mapPayload(payload);
        final byte[] pdfDocumentInBytes = this.generateDocument(PRISON_COURT_REGISTER_TEMPLATE, mappedPayload);
        final String filename = String.format("PrisonCourtRegister_%s.pdf", utcClock.now().format(TIMESTAMP_FORMATTER));
        final String[] filenameForCaseUpload = new String[1];

        final JsonObject metadata = createObjectBuilder().add("fileName", filename).build();
        final UUID fileId = fileStorer.store(metadata, new ByteArrayInputStream(pdfDocumentInBytes));

        final JsonObject prisonCourtRegister = payload.getJsonObject("prisonCourtRegister");
        if(prisonCourtRegister.containsKey("defendant") &&
                prisonCourtRegister.getJsonObject("defendant").containsKey("name")) {
            String defendantName = prisonCourtRegister.getJsonObject("defendant").getString("name");
            filenameForCaseUpload[0] = String.format("PCR_%s_dateOfHearing_%s.pdf", defendantName, utcClock.now().format(TIMESTAMP_FORMATTER_FOR_FILE_UPLOAD));
        }

        UUID materialId = randomUUID();
        materialService.uploadMaterial(fileId, materialId, envelope);
        List<Optional<UUID>> caseUUIDList = getCaseUUID(envelope, payload);
        caseUUIDList.stream().forEach(c -> {
            if (c.isPresent()){
                LOGGER.info(">>2047 adding court document for case id {}", c.get());
                addCourtDocument(envelope, c.get(), materialId, filenameForCaseUpload[0]);
            }
        });

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

    private List<Optional<UUID>> getCaseUUID(JsonEnvelope envelope, JsonObject mappedPayload) {
        List<Optional<UUID>> caseUUIDList = new ArrayList<>();
        final PrisonCourtRegisterRecorded prisonCourtRegisterRecorded = converter.convert(mappedPayload, PrisonCourtRegisterRecorded.class);
        List<PrisonCourtRegisterCaseOrApplication> pcoaList = Optional.ofNullable(prisonCourtRegisterRecorded).map(pcr -> pcr.getPrisonCourtRegister())
                .map(def -> def.getDefendant())
                .map(pcoa -> pcoa.getProsecutionCasesOrApplications()).stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());
        List<String> caseUrnList = pcoaList.stream().map(ar -> ar.getCaseOrApplicationReference()).distinct().collect(Collectors.toList());
        caseUrnList.stream().forEach(cu -> {
            Optional<JsonObject> caseIdJsonObject = progressionService.caseExistsByCaseUrn(envelope, cu);
            if(caseIdJsonObject.isPresent() && caseIdJsonObject.get().containsKey(CASE_ID)){
                caseUUIDList.add(Optional.of(fromString(caseIdJsonObject.get().getString(CASE_ID))));
            }

        });
        return caseUUIDList;
    }

    private void addCourtDocument(final JsonEnvelope envelope, UUID caseUUID, final UUID materialId, final String filename) {
        CourtDocument courtDocument = buildCourtDocument(caseUUID, materialId, filename);
        final JsonObject jsonObject = createObjectBuilder()
                .add(MATERIAL_ID, materialId.toString())
                .add(COURT_DOCUMENT, objectToJsonObjectConverter.convert(courtDocument))
                .build();
        final Envelope<JsonObject> data = envelopeFrom(JsonEnvelope.metadataFrom(envelope.metadata())
                .withName(PROGRESSION_COMMAND_ADD_COURT_DOCUMENT), jsonObject);
        sender.send(data);
    }

    private CourtDocument buildCourtDocument(UUID caseUUID, UUID materialId, String filename) {
        final DocumentCategory documentCategory = DocumentCategory.documentCategory()
                .withCaseDocument(CaseDocument.caseDocument()
                        .withProsecutionCaseId(caseUUID)
                        .build())
                .build();

        final Material material = Material.material().withId(materialId)
                .withReceivedDateTime(ZonedDateTime.now())
                .build();

        return CourtDocument.courtDocument()
                .withCourtDocumentId(randomUUID())
                .withDocumentCategory(documentCategory)
                .withDocumentTypeDescription(DOCUMENT_TYPE_DESCRIPTION)
                .withDocumentTypeId(CASE_DOCUMENT_TYPE_ID)
                .withMimeType(APPLICATION_PDF)
                .withName(filename)
                .withMaterials(Collections.singletonList(material))
                .withSendToCps(false)
                .withContainsFinancialMeans(false)
                .build();
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