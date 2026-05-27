package uk.gov.moj.cpp.progression.blobstore;

import uk.gov.justice.services.common.configuration.Value;

import java.time.Duration;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class AzureBlobConfiguration {

    @Inject
    @Value(key = "azure.filestore.connection-string", defaultValue = "DefaultAzureCredential")
    private String connectionString;

    @Inject
    @Value(key = "azure.filestore.endpoint")
    private String endpoint;

    @Inject
    @Value(key = "azure.filestore.container-name")
    private String containerName;

    public String getConnectionString() {
        return connectionString;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getContainerName() {
        return containerName;
    }

    public boolean hasConnectionString() {
        return connectionString != null && !connectionString.isBlank() && !"DefaultAzureCredential".equals(connectionString);
    }

    public Duration getTransferTimeout() {
        return Duration.ofSeconds(300);
    }
}
