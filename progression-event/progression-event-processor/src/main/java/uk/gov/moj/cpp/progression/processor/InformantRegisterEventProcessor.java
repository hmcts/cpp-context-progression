package uk.gov.moj.cpp.progression.processor;

import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;

import uk.gov.justice.core.courts.informantRegisterDocument.InformantRegisterCaseOrApplication;
import uk.gov.justice.core.courts.informantRegisterDocument.InformantRegisterDefendant;
import uk.gov.justice.core.courts.informantRegisterDocument.InformantRegisterDocumentRequest;
import uk.gov.justice.core.courts.informantRegisterDocument.InformantRegisterHearing;
import uk.gov.justice.core.courts.informantRegisterDocument.InformantRegisterOffence;
import uk.gov.justice.core.courts.informantRegisterDocument.InformantRegisterResult;
import uk.gov.justice.progression.courts.NotifyInformantRegister;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.FrameworkComponent;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.api.FileStorer;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.processor.model.InformantRegisterDocument;
import uk.gov.moj.cpp.progression.service.ApplicationParameters;
import uk.gov.moj.cpp.progression.service.NotificationNotifyService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;

import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.apache.commons.lang3.StringUtils;

@ServiceComponent(EVENT_PROCESSOR)
public class InformantRegisterEventProcessor {
    private static final String FIELD_PROSECUTION_AUTHORITY_ID = "prosecutionAuthorityId";
    private static final String FIELD_INFORMANT_REGISTER_DOCUMENT_REQUESTS = "informantRegisterDocumentRequests";
    private static final String FILE_NAME = "fileName";
    private static final String FIELD_RECIPIENTS = "recipients";
    private static final String FIELD_NOTIFICATION_ID = "notificationId";
    private static final String FIELD_TEMPLATE_ID = "templateId";
    private static final String SEND_TO_ADDRESS = "sendToAddress";
    private static final String FILE_ID = "fileId";
    private static final String PERSONALISATION = "personalisation";
    private static final String RECIPIENT = "Prosecutor_name";
    private static final String RECIPIENT_NAME = "recipientName";
    private static final String EMAIL_ADDRESS = "emailAddress1";
    private static final String EMAIL_TEMPLATE_NAME = "emailTemplateName";
    private static final String EMPTY_STRING = "";

    @Inject
    private NotificationNotifyService notificationNotifyService;

    @Inject
    private FileStorer fileStorer;

    @Inject
    @FrameworkComponent(EVENT_PROCESSOR)
    private Requester requester;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private ApplicationParameters applicationParameters;

    @Inject
    private Sender sender;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @SuppressWarnings({"squid:S1160"})
    @Handles("progression.event.informant-register-generated")
    public void generateInformantRegister(final JsonEnvelope envelope) throws IOException, FileServiceException {
        final JsonObject payload = envelope.payloadAsJsonObject();
        final List<JsonObject> informantRegisters = payload.getJsonArray(FIELD_INFORMANT_REGISTER_DOCUMENT_REQUESTS).getValuesAs(JsonObject.class);
        final JsonObject informantRegister = informantRegisters.get(0);
        final UUID prosecutionAuthorityId = fromString(informantRegister.getString(FIELD_PROSECUTION_AUTHORITY_ID));
        final String fileName = informantRegister.getString(FILE_NAME);

        final byte[] informantRegisterInBytes = generateCsvDocument(informantRegisters);

        final JsonObject metadata = createObjectBuilder()
                .add(FILE_NAME, fileName)
                .build();

        UUID fileId = null;

        final String templateId = informantRegister.containsKey(FIELD_RECIPIENTS) ? getTemplateId(informantRegister.getJsonArray(FIELD_RECIPIENTS)) : "";
        if (StringUtils.isNotBlank(templateId)) {
            fileId = fileStorer.store(metadata, new ByteArrayInputStream(informantRegisterInBytes));
        }
        processInformantRegisterNotificationRequest(envelope, prosecutionAuthorityId, fileId, templateId);
    }

    private void processInformantRegisterNotificationRequest(final JsonEnvelope event, final UUID prosecutionAuthorityId, final UUID fileId, final String templateId) {
        final NotifyInformantRegister notifyInformantRegister = NotifyInformantRegister.notifyInformantRegister()
                .withProsecutionAuthorityId(prosecutionAuthorityId)
                .withFileId(fileId)
                .withTemplateId(templateId)
                .build();
        this.sender.send(Envelope.envelopeFrom(metadataFrom(event.metadata()).withName("progression.command.notify-informant-register").build(),
                this.objectToJsonObjectConverter.convert(notifyInformantRegister)));
    }

    private String getTemplateId(final JsonArray recipients) {
        return recipients.getValuesAs(JsonObject.class).stream().findFirst().map(rp -> applicationParameters.getEmailTemplateId(rp.getString(EMAIL_TEMPLATE_NAME))).orElse("");
    }

    @Handles("progression.event.informant-register-notified")
    public void notifyProsecutionAuthority(final JsonEnvelope envelope) {
        final JsonObject payload = envelope.payloadAsJsonObject();
        final JsonString fileId = payload.getJsonString(FILE_ID);
        final JsonString templateId = payload.getJsonString(FIELD_TEMPLATE_ID);
        final List<JsonObject> recipients = payload.getJsonArray(FIELD_RECIPIENTS).getValuesAs(JsonObject.class);
        if(nonNull(recipients)) {
            recipients.forEach(rp -> {
                final JsonObjectBuilder notifyObjectBuilder = createObjectBuilder();
                notifyObjectBuilder.add(FIELD_NOTIFICATION_ID, randomUUID().toString());
                notifyObjectBuilder.add(FIELD_TEMPLATE_ID, templateId);
                notifyObjectBuilder.add(SEND_TO_ADDRESS, rp.getJsonString(EMAIL_ADDRESS));
                notifyObjectBuilder.add(FILE_ID, fileId);
                notifyObjectBuilder.add(PERSONALISATION, createObjectBuilder().add(RECIPIENT, rp.getString(RECIPIENT_NAME)).build());
                this.notificationNotifyService.sendEmailNotification(envelope, notifyObjectBuilder.build());
            });
        }
    }

    private byte[] generateCsvDocument(final List<JsonObject> informantRegistersByRegisterDate) throws IOException {
        final List<InformantRegisterDocument> informantRegisters = new ArrayList<>();
        informantRegistersByRegisterDate.forEach(
                informantRegister -> mapToInformantRegisterDocuments(informantRegisters, informantRegister));

        final CsvMapper csvMapper = new CsvMapper();
        final CsvSchema schema = csvMapper.schemaFor(InformantRegisterDocument.class).withHeader();
        final ObjectWriter writer = csvMapper.writer(schema);
        return writer.writeValueAsBytes(informantRegisters);
    }

    private void mapToInformantRegisterDocuments(final List<InformantRegisterDocument> informantRegisters, final JsonObject informantRegisterJson) {
        final InformantRegisterDocumentRequest documentRequest = jsonObjectToObjectConverter.convert(informantRegisterJson, InformantRegisterDocumentRequest.class);
        documentRequest.getHearingVenue().getCourtSessions().forEach(courtSession -> courtSession.getDefendants().forEach(defendant ->
        {
            if (isNotEmpty(defendant.getResults())) {
                defendant.getResults().forEach(result -> buildInformantRegister(informantRegisters, documentRequest, courtSession, defendant, null, result, null, null));
            }
            defendant.getProsecutionCasesOrApplications().forEach(caseOrApplication -> {

                if (isNotEmpty(caseOrApplication.getResults())) {
                    caseOrApplication.getResults().forEach(caseResult -> buildInformantRegister(informantRegisters, documentRequest, courtSession, defendant, caseOrApplication, caseResult, null, null));
                }

                caseOrApplication.getOffences().forEach(offence -> {
                    if (isNotEmpty(offence.getOffenceResults())) {
                        offence.getOffenceResults().forEach(offenceResult -> buildInformantRegister(informantRegisters, documentRequest, courtSession, defendant, caseOrApplication, null, offence, offenceResult));
                    } else {
                        buildInformantRegister(informantRegisters, documentRequest, courtSession, defendant, caseOrApplication, null, offence, null);
                    }

                });
            });
        }));
    }

    @SuppressWarnings({"squid:S00107"})
    private void buildInformantRegister(final List<InformantRegisterDocument> informantRegisters, final InformantRegisterDocumentRequest documentRequest, final InformantRegisterHearing courtSession, final InformantRegisterDefendant defendant, final InformantRegisterCaseOrApplication caseOrApplication, final InformantRegisterResult result, final InformantRegisterOffence offence, final InformantRegisterResult offenceResult) {

        final InformantRegisterDocument.Builder informantRegisterDocumentBuilder = InformantRegisterDocument.informantRegisterDocument()
                .withInfDestID(EMPTY_STRING)
                .withName(documentRequest.getProsecutionAuthorityName())
                .withHearingStartTime(documentRequest.getHearingDate().format(DateTimeFormatter.ISO_INSTANT))
                .withLjaName(documentRequest.getHearingVenue().getLjaName())
                .withCourtHouse(documentRequest.getHearingVenue().getCourtHouse())
                .withCourtRoom(courtSession.getCourtRoom())
                .withSessionType(EMPTY_STRING)
                .withTitle(defendant.getTitle())
                .withForeNames(defendant.getFirstName())
                .withSurName(defendant.getLastName())
                .withAddress(getAddress(defendant))
                .withPostCode(defendant.getPostCode())
                .withDateOfBirth(defendant.getDateOfBirth())
                .withCaseNumber(EMPTY_STRING)
                .withSeqNo(EMPTY_STRING);

        if (nonNull(caseOrApplication)) {
            informantRegisterDocumentBuilder
                    .withCaseOrApplicationReference(caseOrApplication.getCaseOrApplicationReference())
                    .withArrestSummonsNumber(caseOrApplication.getArrestSummonsNumber());
        }

        if (nonNull(offence)) {
            buildOffenceDetails(informantRegisterDocumentBuilder, offence, offenceResult);
        }

        if (nonNull(result)) {
            buildResult(informantRegisterDocumentBuilder, result);
        }

        informantRegisters.add(informantRegisterDocumentBuilder.build());
    }

    private void buildOffenceDetails(final InformantRegisterDocument.Builder builder, final InformantRegisterOffence offence, final InformantRegisterResult offenceResult) {
        builder.withOffenceCode(offence.getOffenceCode());
        builder.withOffenceTitle(offence.getOffenceTitle());
        builder.withPleaValue(offence.getPleaValue());
        builder.withVerdictCode(offence.getVerdictCode());

        if (nonNull(offenceResult)) {
            buildResult(builder, offenceResult);
        }
    }

    private void buildResult(final InformantRegisterDocument.Builder builder, final InformantRegisterResult offenceResult) {
        builder.withCjsResultCode(offenceResult.getCjsResultCode());
        builder.withResultText(offenceResult.getResultText().trim());
    }

    private String getAddress(final InformantRegisterDefendant defendant) {
        final List<String> addressAsList = Arrays.asList(defendant.getAddress1(), defendant.getAddress2(), defendant.getAddress3(), defendant.getAddress4(), defendant.getAddress5());
        return addressAsList
                .stream()
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining(", "));
    }
}
