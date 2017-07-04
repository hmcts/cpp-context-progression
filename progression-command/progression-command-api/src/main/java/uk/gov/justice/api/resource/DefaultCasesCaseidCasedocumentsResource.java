package uk.gov.justice.api.resource;

import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.status;
import static uk.gov.justice.services.messaging.DefaultJsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataOf;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.slf4j.Logger;

import uk.gov.justice.services.core.accesscontrol.AccessControlFailureMessageGenerator;
import uk.gov.justice.services.core.accesscontrol.AccessControlService;
import uk.gov.justice.services.core.accesscontrol.AccessControlViolation;
import uk.gov.justice.services.core.annotation.Adapter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.audit.AuditService;
import uk.gov.justice.services.file.api.sender.FileData;
import uk.gov.justice.services.file.api.sender.FileSender;
import uk.gov.justice.services.messaging.JsonEnvelope;

@Adapter(Component.COMMAND_API)
public class DefaultCasesCaseidCasedocumentsResource implements UploadCaseDocumentsResource {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(DefaultCasesCaseidCasedocumentsResource.class);
    private final String INVALID_FILE_NAME = "Supported files are .pdf, .doc or .docx";

    @Inject
    private UploadCaseDocumentsFormParser uploadCaseDocumentsFormParser;

    @Inject
    private UploadFileServiceSender uploadFileServiceSender;

    @Inject
    private FileSender fileSender;

    @Inject
    private AuditService auditService;

    @Inject
    private AccessControlService accessControlService;

    @Inject
    private AccessControlFailureMessageGenerator accessControlFailureMessageGenerator;


    @Override
    public Response uploadCaseDocument(final MultipartFormDataInput multipartFormDataInput,
                    final String userId, final String session, final String correlationId,
                    final String caseId) {

        LOG.info(String.format(
                        "Received Document upload request from userId= %s sessionId= %s correlationId= %s caseId= %s",
                        userId, session, correlationId, caseId));

        final JsonEnvelope envelope = envelopeFrom(
                metadataOf(randomUUID(), "progression.command.upload-case-documents")
                        .withUserId(userId)
                        .build(),
                createObjectBuilder()
                        .add("caseId", caseId)
                        .build());

        auditService.audit(envelope);

        final Optional<AccessControlViolation> violation = accessControlService.checkAccessControl(envelope);

        if (violation.isPresent()) {
            final String errorMessage = accessControlFailureMessageGenerator.errorMessageFrom(
                    envelope,
                    violation.get());

            final JsonObject responseErrorMsg = createObjectBuilder()
                    .add("error", errorMessage)
                    .build();

            return status(FORBIDDEN).entity(responseErrorMsg.toString()).build();
        }

        try{

            KeyValue<Optional<String>, Optional<InputStream>> fileNameAndContent = uploadCaseDocumentsFormParser.parse(multipartFormDataInput);

            if (!fileNameAndContent.getKey().isPresent()) {
                LOG.error(getErrorMsg(userId, session, correlationId, caseId, "file name absent"));
                return Response.status(BAD_REQUEST).build();
            }

            final String fileName = fileNameAndContent.getKey().get();

            if (!((fileName.endsWith(".pdf") || fileName.endsWith(".doc") || fileName.endsWith(".docx")))) {
              LOG.error(INVALID_FILE_NAME);
              return Response.status(BAD_REQUEST).entity(INVALID_FILE_NAME).build();
            }
            if (!fileNameAndContent.getValue().isPresent()) {
                LOG.error(getErrorMsg(userId, session, correlationId, caseId,
                        "file content missing"));
                return Response.status(BAD_REQUEST).build();
            }

            LOG.info(String.format(
                            "Parsed Document upload request from userId= %s sessionId= %s "
                                            + "clientCorrelationId= %s, caseId= %s",
                            userId, session, correlationId, caseId));


            // File is sent
            final FileData fileData = fileSender.send(fileName, fileNameAndContent.getValue().get());

            LOG.info(String.format(
                            "Uploaded document from userId= %s sessionId= %s "
                                            + "clientCorrelationId= %s, caseId= %s, fileName= %s, fileId= %s, "
                                            + "fileMimeType= %s ",
                            userId, session, correlationId, caseId, fileName, fileData.fileId(),
                            fileData.fileMimeType()));

            // Send the file meta data
            final JsonObject uploadFileMetadataMessage = buildMessage(caseId, fileName, fileData);

            uploadFileServiceSender.doSend(uploadFileMetadataMessage, userId, session, correlationId);

            return Response.status(ACCEPTED)
                    .entity(new GenericEntity<String>(createObjectBuilder()
                        .add("materialId", fileData.fileId())
                        .build().toString()) {})
                    .build();

        } catch (IOException e) {
            LOG.error(getErrorMsg(userId, session, correlationId, caseId, "exception"), e);
            return Response.status(BAD_REQUEST).build();
        }
    }

    private JsonObject buildMessage(final String caseId, final String fileName,
                                    final FileData fileData) {
        return createObjectBuilder().add("cppCaseId", caseId).add("fileId", fileData.fileId())
                .add("fileMimeType", fileData.fileMimeType()).add("fileName", fileName)
                .build();
    }

    private String getErrorMsg(final String userId, final String session,
                               final String clientCorrelationId, final String caseId, final String msg) {
        return format(
                "Error handling request from userId= %s sessionId= %s "
                        + "clientCorrelationId= %s caseId= %s cause= %s",
                userId, session, clientCorrelationId, caseId, msg);
    }

    public UploadCaseDocumentsFormParser getUploadCaseDocumentsFormParser() {
        return uploadCaseDocumentsFormParser;
    }

    public void setUploadCaseDocumentsFormParser(
            UploadCaseDocumentsFormParser uploadCaseDocumentsFormParser) {
        this.uploadCaseDocumentsFormParser = uploadCaseDocumentsFormParser;
    }

    public UploadFileServiceSender getUploadFileServiceSender() {
        return uploadFileServiceSender;
    }

    public void setUploadFileServiceSender(UploadFileServiceSender uploadFileServiceSender) {
        this.uploadFileServiceSender = uploadFileServiceSender;
    }

    public FileSender getFileSender() {
        return fileSender;
    }

    public void setFileSender(FileSender fileSender) {
        this.fileSender = fileSender;
    }


}
