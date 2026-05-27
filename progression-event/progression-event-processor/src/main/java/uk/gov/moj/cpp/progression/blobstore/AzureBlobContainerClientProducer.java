package uk.gov.moj.cpp.progression.blobstore;

import static java.net.HttpURLConnection.HTTP_CONFLICT;

import com.azure.core.exception.HttpResponseException;
import com.azure.core.http.jdk.httpclient.JdkHttpClientBuilder;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.slf4j.Logger;

@ApplicationScoped
public class AzureBlobContainerClientProducer {

    @Inject
    private Logger logger;

    @Inject
    private AzureBlobConfiguration configuration;

    private BlobContainerClient blobContainerClient;

    @PostConstruct
    public void initialise() {
        blobContainerClient = buildBlobServiceClient(configuration)
                .getBlobContainerClient(configuration.getContainerName());
        try {
            blobContainerClient.createIfNotExists();
        } catch (final HttpResponseException e) {
            if (e.getResponse() != null && e.getResponse().getStatusCode() == HTTP_CONFLICT) {
                logger.warn("BlobContainerClient.createIfNotExists returned 409 Conflict for container '{}' — container already exists",
                        configuration.getContainerName());
            } else {
                throw new RuntimeException("Failed to create BlobContainerClient for container '" + configuration.getContainerName() + "'", e);
            }
        }
    }

    @Produces
    @Dependent
    @SuppressWarnings("java:S6813")
    public BlobContainerClient blobContainerClient() {
        return blobContainerClient;
    }

    protected BlobServiceClient buildBlobServiceClient(final AzureBlobConfiguration config) {
        if (config.hasConnectionString()) {
            return new BlobServiceClientBuilder()
                    .httpClient(new JdkHttpClientBuilder().build())
                    .connectionString(config.getConnectionString())
                    .buildClient();
        }
        return new BlobServiceClientBuilder()
                .httpClient(new JdkHttpClientBuilder().build())
                .credential(new DefaultAzureCredentialBuilder().build())
                .endpoint(config.getEndpoint())
                .buildClient();
    }
}
