package uk.gov.moj.cpp.progression.blobstore;

import com.azure.core.http.jdk.httpclient.JdkHttpClientBuilder;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobStorageException;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.slf4j.Logger;

@ApplicationScoped
public class AzureBlobContainerClientProducer {

    private static final String SENTINEL = "DefaultAzureCredential";

    @Inject
    private Logger logger;

    @Inject
    private AzureBlobConfiguration configuration;

    private BlobContainerClient blobContainerClient;

    @PostConstruct
    public void initialise() {
        blobContainerClient = buildBlobContainerClient(configuration);
        try {
            blobContainerClient.createIfNotExists();
        } catch (final BlobStorageException e) {
            logger.warn("createIfNotExists failed for container '{}' — assuming it already exists: {}",
                    configuration.getContainerName(), e.getMessage());
        }
    }

    @Produces
    @Dependent
    @SuppressWarnings("java:S6813")
    public BlobContainerClient blobContainerClient() {
        return blobContainerClient;
    }

    protected BlobContainerClient buildBlobContainerClient(final AzureBlobConfiguration config) {
        final String connectionString = config.getConnectionString();
        final BlobServiceClient blobServiceClient;
        if (connectionString != null && !connectionString.isBlank() && !SENTINEL.equals(connectionString)) {
            blobServiceClient = new BlobServiceClientBuilder()
                    .httpClient(new JdkHttpClientBuilder().build())
                    .connectionString(connectionString)
                    .buildClient();
        } else {
            blobServiceClient = new BlobServiceClientBuilder()
                    .httpClient(new JdkHttpClientBuilder().build())
                    .credential(new DefaultAzureCredentialBuilder().build())
                    .endpoint(config.getEndpoint())
                    .buildClient();
        }
        return blobServiceClient.getBlobContainerClient(config.getContainerName());
    }
}
