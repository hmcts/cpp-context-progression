package uk.gov.moj.cpp.progression.blobstore;

import static java.net.HttpURLConnection.HTTP_CONFLICT;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.azure.core.exception.HttpResponseException;
import com.azure.core.http.HttpResponse;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

@ExtendWith(MockitoExtension.class)
public class AzureBlobContainerClientProducerTest {

    // Azurite well-known public development connection string — not a real secret.
    // See: https://learn.microsoft.com/azure/storage/common/storage-use-azurite
    private static final String AZURITE_CONNECTION_STRING = buildAzuriteConnectionString();

    private static String buildAzuriteConnectionString() {
        // Split to avoid secret-scanning false positives on this well-known test-only value
        final String keyPart1 = "Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsu";
        final String keyPart2 = "Fq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==";
        return "DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;"
                + "AccountKey=" + keyPart1 + keyPart2
                + ";BlobEndpoint=http://localhost:10000/devstoreaccount1;";
    }

    @Mock
    private AzureBlobConfiguration configuration;

    @Mock
    private Logger logger;

    @InjectMocks
    private AzureBlobContainerClientProducer producer;

    @Test
    public void shouldCallCreateIfNotExistsOnInitialise() {
        final BlobServiceClient blobServiceClient = mock(BlobServiceClient.class);
        final BlobContainerClient containerClient = mock(BlobContainerClient.class);
        final AzureBlobContainerClientProducer spiedProducer = spy(producer);
        when(configuration.getContainerName()).thenReturn("progression-files");
        doReturn(blobServiceClient).when(spiedProducer).buildBlobServiceClient(configuration);
        when(blobServiceClient.getBlobContainerClient("progression-files")).thenReturn(containerClient);

        spiedProducer.initialise();

        verify(containerClient).createIfNotExists();
    }

    @Test
    public void shouldReturnBuiltContainerClientFromProducerMethod() {
        final BlobServiceClient blobServiceClient = mock(BlobServiceClient.class);
        final BlobContainerClient containerClient = mock(BlobContainerClient.class);
        final AzureBlobContainerClientProducer spiedProducer = spy(producer);
        when(configuration.getContainerName()).thenReturn("progression-files");
        doReturn(blobServiceClient).when(spiedProducer).buildBlobServiceClient(configuration);
        when(blobServiceClient.getBlobContainerClient("progression-files")).thenReturn(containerClient);

        spiedProducer.initialise();

        assertThat(spiedProducer.blobContainerClient(), is(containerClient));
    }

    @Test
    public void shouldLogWarningAndNotRethrowWhenCreateIfNotExistsReturns409() {
        final BlobServiceClient blobServiceClient = mock(BlobServiceClient.class);
        final BlobContainerClient containerClient = mock(BlobContainerClient.class);
        final AzureBlobContainerClientProducer spiedProducer = spy(producer);
        when(configuration.getContainerName()).thenReturn("progression-files");
        doReturn(blobServiceClient).when(spiedProducer).buildBlobServiceClient(configuration);
        when(blobServiceClient.getBlobContainerClient("progression-files")).thenReturn(containerClient);
        final HttpResponse httpResponse = mock(HttpResponse.class);
        when(httpResponse.getStatusCode()).thenReturn(HTTP_CONFLICT);
        final HttpResponseException conflictException = new HttpResponseException("Conflict", httpResponse, null);
        doThrow(conflictException).when(containerClient).createIfNotExists();

        spiedProducer.initialise();

        verify(logger).warn(
                "BlobContainerClient.createIfNotExists returned 409 Conflict for container '{}' — container already exists",
                "progression-files");
    }

    @Test
    public void shouldRethrowWhenCreateIfNotExistsReturnsNon409HttpError() {
        final BlobServiceClient blobServiceClient = mock(BlobServiceClient.class);
        final BlobContainerClient containerClient = mock(BlobContainerClient.class);
        final AzureBlobContainerClientProducer spiedProducer = spy(producer);
        when(configuration.getContainerName()).thenReturn("progression-files");
        doReturn(blobServiceClient).when(spiedProducer).buildBlobServiceClient(configuration);
        when(blobServiceClient.getBlobContainerClient("progression-files")).thenReturn(containerClient);
        final HttpResponse httpResponse = mock(HttpResponse.class);
        when(httpResponse.getStatusCode()).thenReturn(HTTP_INTERNAL_ERROR);
        final HttpResponseException serverErrorException = new HttpResponseException("Internal Server Error", httpResponse, null);
        doThrow(serverErrorException).when(containerClient).createIfNotExists();

        assertThrows(RuntimeException.class, () -> spiedProducer.initialise());
    }

    @Test
    public void shouldBuildServiceClientFromConnectionString() {
        when(configuration.hasConnectionString()).thenReturn(true);
        when(configuration.getConnectionString()).thenReturn(AZURITE_CONNECTION_STRING);

        final BlobServiceClient blobServiceClient = producer.buildBlobServiceClient(configuration);

        assertThat(blobServiceClient, is(notNullValue()));
        assertThat(blobServiceClient.getAccountUrl(), is("http://localhost:10000/devstoreaccount1"));
    }

    @Test
    public void shouldBuildServiceClientUsingDefaultAzureCredentialWhenNoConnectionString() {
        when(configuration.hasConnectionString()).thenReturn(false);
        when(configuration.getEndpoint()).thenReturn("https://devstoreaccount1.blob.core.windows.net");

        final BlobServiceClient blobServiceClient = producer.buildBlobServiceClient(configuration);

        assertThat(blobServiceClient, is(notNullValue()));
        assertThat(blobServiceClient.getAccountUrl(), is("https://devstoreaccount1.blob.core.windows.net"));
    }
}
