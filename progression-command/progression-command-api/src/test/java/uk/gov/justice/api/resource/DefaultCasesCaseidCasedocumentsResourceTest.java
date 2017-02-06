package uk.gov.justice.api.resource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;

import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import uk.gov.justice.services.file.api.sender.FileData;
import uk.gov.justice.services.file.api.sender.FileSender;

@RunWith(MockitoJUnitRunner.class)
public class DefaultCasesCaseidCasedocumentsResourceTest {

    @Mock
    private UploadCaseDocumentsFormParser uploadCaseDocumentsFormParser;

    @Mock
    UploadFileServiceSender uploadFileServiceSender;

    @Mock
    private FileSender fileSender;

    @InjectMocks
    private DefaultCasesCaseidCasedocumentsResource resource;

    
    @Test
    public void shouldReturnBadRequestWhenFormisEmpty() throws IOException {

        final MultipartFormDataInput multipartFormDataInput = mock(MultipartFormDataInput.class);

        when(uploadCaseDocumentsFormParser.parse(multipartFormDataInput))
                        .thenReturn(getEmptyKeyValue());

        Response response = resource.uploadCaseDocument(multipartFormDataInput, "userId", "session",
                        "clientCorrelationId", "caseId");

        assertThat(Response.Status.BAD_REQUEST.getStatusCode(), equalTo(response.getStatus()));

    }

    @Test
    public void shouldReturnBadRequestWhenFormFileNameisEmpty() throws IOException {

        final MultipartFormDataInput multipartFormDataInput = mock(MultipartFormDataInput.class);

        when(uploadCaseDocumentsFormParser.parse(multipartFormDataInput))
                        .thenReturn(getEmptyKey("data"));

        Response response = resource.uploadCaseDocument(multipartFormDataInput, "userId", "session",
                        "clientCorrelationId", "caseId");

        assertThat(Response.Status.BAD_REQUEST.getStatusCode(), equalTo(response.getStatus()));

    }

    @Test
    public void shouldReturnBadRequestWhenFormFileNameisInvalid() throws IOException {

        final MultipartFormDataInput multipartFormDataInput = mock(MultipartFormDataInput.class);

        when(uploadCaseDocumentsFormParser.parse(multipartFormDataInput)).thenReturn(getInvalidKey("data"));

        Response response = resource.uploadCaseDocument(multipartFormDataInput, "userId", "session",
                "clientCorrelationId", "caseId");

        assertThat(response.getStatus(), equalTo(Response.Status.BAD_REQUEST.getStatusCode()));
        assertThat(response.getEntity(), equalTo("Supported files are .pdf, .doc or .docx"));

    }

    @Test
    public void shouldReturnBadRequestWhenFormFileContentisEmpty() throws IOException {

        final MultipartFormDataInput multipartFormDataInput = mock(MultipartFormDataInput.class);

        when(uploadCaseDocumentsFormParser.parse(multipartFormDataInput))
                        .thenReturn(getEmptyValue("fileName"));

        Response response = resource.uploadCaseDocument(multipartFormDataInput, "userId", "session",
                        "clientCorrelationId", "caseId");

        assertThat(Response.Status.BAD_REQUEST.getStatusCode(), equalTo(response.getStatus()));

    }

    @Test
    public void shouldReturnSuccessForValidForm() throws IOException {

        final MultipartFormDataInput multipartFormDataInput = mock(MultipartFormDataInput.class);

        when(uploadCaseDocumentsFormParser.parse(multipartFormDataInput))
                        .thenReturn(getKeyValue("fileName.pdf", "data"));

        final FileData fd = new FileData(UUID.randomUUID().toString(), "application/pdf");
        
        when(fileSender.send(Mockito.eq("fileName.pdf"), any(InputStream.class)))
                        .thenReturn(fd);
       
        Response response = resource.uploadCaseDocument(multipartFormDataInput, "userId", "session",
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

    public void setFileSender(FileSender fileSender) {
        this.fileSender = fileSender;
    }



}
