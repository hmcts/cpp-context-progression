package uk.gov.justice.api.resource;

import static com.jayway.jsonassert.JsonAssert.with;
import static java.util.Optional.of;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.core.accesscontrol.AccessControlFailureMessageGenerator;
import uk.gov.justice.services.core.accesscontrol.AccessControlService;
import uk.gov.justice.services.core.accesscontrol.AccessControlViolation;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.file.api.sender.FileData;
import uk.gov.justice.services.file.api.sender.FileSender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;

import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DefaultCasesCaseidCasedocumentsResourceTest {

    @Mock
    UploadFileServiceSender uploadFileServiceSender;
    @Mock
    private UploadCaseDocumentsFormParser uploadCaseDocumentsFormParser;
    @Mock
    private FileSender fileSender;

    @Mock
    private AccessControlService accessControlService;

    @Mock
    private AccessControlFailureMessageGenerator accessControlFailureMessageGenerator;

    @InjectMocks
    private DefaultCasesCaseidCasedocumentsResource resource;


    @Test
    public void shouldReturnBadRequestWhenFormisEmpty() throws IOException {
        // given
        final MultipartFormDataInput multipartFormDataInput = mock(MultipartFormDataInput.class);
        // and
        when(accessControlService.checkAccessControl(any(String.class), any(JsonEnvelope.class))).thenReturn(Optional.empty());
        // and
        when(uploadCaseDocumentsFormParser.parse(multipartFormDataInput)).thenReturn(getEmptyKeyValue());

        // when
        final Response response = resource.uploadCaseDocument(multipartFormDataInput, "userId", "session",
                        "clientCorrelationId", "caseId");

        // then
        assertThat(Response.Status.BAD_REQUEST.getStatusCode(), equalTo(response.getStatus()));

    }

    @Test
    public void shouldReturnBadRequestWhenFormFileNameisEmpty() throws IOException {

        when(accessControlService.checkAccessControl(any(String.class), any(JsonEnvelope.class))).thenReturn(Optional.empty());

        final MultipartFormDataInput multipartFormDataInput = mock(MultipartFormDataInput.class);

        when(uploadCaseDocumentsFormParser.parse(multipartFormDataInput))
                        .thenReturn(getEmptyKey("data"));

        final Response response = resource.uploadCaseDocument(multipartFormDataInput, "userId", "session",
                        "clientCorrelationId", "caseId");

        assertThat(Response.Status.BAD_REQUEST.getStatusCode(), equalTo(response.getStatus()));

    }

    @Test
    public void shouldReturnBadRequestWhenFormFileNameisInvalid() throws IOException {

        final MultipartFormDataInput multipartFormDataInput = mock(MultipartFormDataInput.class);

        when(uploadCaseDocumentsFormParser.parse(multipartFormDataInput)).thenReturn(getInvalidKey("data"));
        // and
        when(accessControlService.checkAccessControl(eq(Component.COMMAND_API), any(JsonEnvelope.class))).thenReturn(Optional.empty());


        final Response response = resource.uploadCaseDocument(multipartFormDataInput, "userId", "session",
                "clientCorrelationId", "caseId");

        assertThat(response.getStatus(), equalTo(Response.Status.BAD_REQUEST.getStatusCode()));
        assertThat(response.getEntity(), equalTo("Supported files are .pdf, .doc or .docx"));

    }

    @Test
    public void shouldReturnBadRequestWhenFormFileContentisEmpty() throws IOException {

        when(accessControlService.checkAccessControl(any(String.class), any(JsonEnvelope.class))).thenReturn(Optional.empty());

        final MultipartFormDataInput multipartFormDataInput = mock(MultipartFormDataInput.class);

        when(uploadCaseDocumentsFormParser.parse(multipartFormDataInput))
                        .thenReturn(getEmptyValue("fileName"));

        final Response response = resource.uploadCaseDocument(multipartFormDataInput, "userId", "session",
                        "clientCorrelationId", "caseId");

        assertThat(Response.Status.BAD_REQUEST.getStatusCode(), equalTo(response.getStatus()));

    }

    @Test
    public void shouldReturn403ForbiddenIfAccessControlForbidsTheRequest() throws Exception {

        final String errorMessage = "oh dear";

        final AccessControlViolation accessControlViolation = mock(AccessControlViolation.class);

        when(accessControlService.checkAccessControl(any(String.class), any(JsonEnvelope.class))).thenReturn(of(accessControlViolation));
        when(accessControlFailureMessageGenerator.errorMessageFrom(any(JsonEnvelope.class), eq(accessControlViolation))).thenReturn(errorMessage);

        final MultipartFormDataInput multipartFormDataInput = mock(MultipartFormDataInput.class);

        final Response response = resource.uploadCaseDocument(multipartFormDataInput, "userId", "session","clientCorrelationId", "caseId");

        assertThat(response.getStatus(), is(FORBIDDEN.getStatusCode()));
        final String errorJson = response.getEntity().toString();

        with(errorJson).assertThat("$.error", is(errorMessage));

    }

    @Test
    public void shouldReturnSuccessForValidForm() throws IOException {
        when(accessControlService.checkAccessControl(any(String.class), any(JsonEnvelope.class))).thenReturn(Optional.empty());

        final MultipartFormDataInput multipartFormDataInput = mock(MultipartFormDataInput.class);

        when(uploadCaseDocumentsFormParser.parse(multipartFormDataInput))
                        .thenReturn(getKeyValue("fileName.pdf", "data"));

        final FileData fd = new FileData(UUID.randomUUID().toString(), "application/pdf");

        when(fileSender.send(eq("fileName.pdf"), any(InputStream.class)))
                        .thenReturn(fd);

        final Response response = resource.uploadCaseDocument(multipartFormDataInput, "userId", "session",
                        "clientCorrelationId", "caseId");

        assertThat(Response.Status.ACCEPTED.getStatusCode(), equalTo(response.getStatus()));

    }

    private KeyValue<Optional<String>, Optional<InputStream>> getEmptyKeyValue() {
        return new KeyValue<Optional<String>, Optional<InputStream>>(Optional.empty(),
                        Optional.empty());
    }

    private KeyValue<Optional<String>, Optional<InputStream>> getEmptyValue(final String key) {
        return new KeyValue<Optional<String>, Optional<InputStream>>(Optional.of(key),
                        Optional.empty());
    }

    private KeyValue<Optional<String>, Optional<InputStream>> getEmptyKey(final String data) {
        return new KeyValue<Optional<String>, Optional<InputStream>>(Optional.empty(),
                        Optional.of(IOUtils.toInputStream(data)));
    }

    private KeyValue<Optional<String>, Optional<InputStream>> getInvalidKey(final String data) {
        return new KeyValue<Optional<String>, Optional<InputStream>>(Optional.of("text.txt"),
                Optional.of(IOUtils.toInputStream(data)));
    }

    private KeyValue<Optional<String>, Optional<InputStream>> getKeyValue(final String key,
                    final String data) {
        return new KeyValue<Optional<String>, Optional<InputStream>>(Optional.of(key),
                        Optional.of(IOUtils.toInputStream(data)));
    }

    public FileSender getFileSender() {
        return fileSender;
    }

    public void setFileSender(final FileSender fileSender) {
        this.fileSender = fileSender;
    }



}
