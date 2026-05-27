package uk.gov.moj.cpp.progression.command.api.interceptors;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;

import uk.gov.justice.services.adapter.rest.multipart.FileInputDetails;
import uk.gov.justice.services.core.interceptor.InterceptorChain;
import uk.gov.justice.services.core.interceptor.InterceptorContext;
import uk.gov.justice.services.messaging.JsonEnvelope;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProgressionServiceFileInterceptorTest {

    private static final String FIELD_NAME = "fileServiceId";
    private static final String FILE_NAME = "test-document.pdf";

    @Mock
    private BlobContainerClient blobContainerClient;

    @Mock
    private InterceptorContext interceptorContext;

    @Mock
    private InterceptorContext interceptorContextExpected;

    @Mock
    private InterceptorChain interceptorChain;

    @InjectMocks
    private ProgressionServiceFileInterceptor interceptor;

    @Test
    public void shouldStoreFileAndInjectFileIdIntoEnvelope() {
        final JsonEnvelope originalEnvelope = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("progression.upload-document").withUserId(randomUUID().toString()).build(),
                createObjectBuilder().build()
        );

        final FileInputDetails fileDetails = mock(FileInputDetails.class);
        when(fileDetails.getFileName()).thenReturn(FILE_NAME);
        when(fileDetails.getFieldName()).thenReturn(FIELD_NAME);
        when(fileDetails.getInputStream()).thenReturn(new ByteArrayInputStream("file content".getBytes()));

        final BlobClient blobClient = mock(BlobClient.class);

        when(interceptorContext.getInputParameter(FileInputDetails.FILE_INPUT_DETAILS_LIST)).thenReturn(of(List.of(fileDetails)));
        when(interceptorContext.inputEnvelope()).thenReturn(originalEnvelope);

        final ArgumentCaptor<String> blobNameCaptor = ArgumentCaptor.forClass(String.class);
        when(blobContainerClient.getBlobClient(blobNameCaptor.capture())).thenReturn(blobClient);

        final ArgumentCaptor<JsonEnvelope> envelopeCaptor = ArgumentCaptor.forClass(JsonEnvelope.class);
        when(interceptorContext.copyWithInput(envelopeCaptor.capture())).thenReturn(interceptorContextExpected);
        when(interceptorChain.processNext(interceptorContextExpected)).thenReturn(interceptorContextExpected);

        interceptor.process(interceptorContext, interceptorChain);

        verify(interceptorChain).processNext(interceptorContextExpected);

        final String blobName = blobNameCaptor.getValue();
        assertThat(UUID.fromString(blobName), is(notNullValue()));

        final JsonEnvelope modifiedEnvelope = envelopeCaptor.getValue();
        assertThat(modifiedEnvelope.payloadAsJsonObject().getString(FIELD_NAME), is(blobName));
    }

    @Test
    public void shouldPassThroughWhenNoFilesPresent() {
        when(interceptorContext.getInputParameter(FileInputDetails.FILE_INPUT_DETAILS_LIST)).thenReturn(empty());
        when(interceptorChain.processNext(interceptorContext)).thenReturn(interceptorContext);

        interceptor.process(interceptorContext, interceptorChain);

        verify(interceptorChain).processNext(interceptorContext);
    }

    @Test
    public void shouldWrapIOExceptionInRuntimeException() {
        final InputStream failingStream = new InputStream() {
            @Override
            public int read() {
                return -1;
            }

            @Override
            public void close() throws IOException {
                throw new IOException("stream close failed");
            }
        };

        final FileInputDetails fileDetails = mock(FileInputDetails.class);
        when(fileDetails.getFileName()).thenReturn(FILE_NAME);
        when(fileDetails.getFieldName()).thenReturn(FIELD_NAME);
        when(fileDetails.getInputStream()).thenReturn(failingStream);

        final BlobClient blobClient = mock(BlobClient.class);
        when(blobContainerClient.getBlobClient(anyString())).thenReturn(blobClient);
        when(interceptorContext.getInputParameter(FileInputDetails.FILE_INPUT_DETAILS_LIST)).thenReturn(of(List.of(fileDetails)));

        assertThrows(RuntimeException.class, () -> interceptor.process(interceptorContext, interceptorChain));
    }
}
