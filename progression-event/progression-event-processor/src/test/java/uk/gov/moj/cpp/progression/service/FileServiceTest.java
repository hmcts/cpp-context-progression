package uk.gov.moj.cpp.progression.service;

import static com.azure.core.util.Context.NONE;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.progression.blobstore.AzureBlobConfiguration;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.options.BlobParallelUploadOptions;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class FileServiceTest {

    @Mock
    private BlobContainerClient blobContainerClient;

    @Mock
    private BlobClient blobClient;

    @Mock
    private AzureBlobConfiguration configuration;

    @InjectMocks
    private FileService fileService;

    @Test
    public void shouldStorePayloadAndReturnFileId() {
        final String fileName = "PrisonCourtRegister.pdf";
        final String templateName = "prison_court_register_template";

        final ArgumentCaptor<String> blobNameCaptor = ArgumentCaptor.forClass(String.class);
        when(blobContainerClient.getBlobClient(blobNameCaptor.capture())).thenReturn(blobClient);
        when(configuration.getTransferTimeout()).thenReturn(Duration.ofSeconds(300));

        final UUID id = fileService.storePayload(createObjectBuilder().build(), fileName, templateName);

        assertThat(id, is(notNullValue()));
        assertThat(id, is(UUID.fromString(blobNameCaptor.getValue())));
        verify(blobClient).uploadWithResponse(any(BlobParallelUploadOptions.class), eq(Duration.ofSeconds(300)), eq(NONE));
    }

    @Test
    public void shouldThrowRuntimeExceptionWhenUploadFails() {
        final String fileName = "PrisonCourtRegister.pdf";
        final String templateName = "prison_court_register_template";

        when(blobContainerClient.getBlobClient(any())).thenReturn(blobClient);
        when(configuration.getTransferTimeout()).thenReturn(Duration.ofSeconds(300));
        when(blobClient.uploadWithResponse(any(), any(), any())).thenThrow(new RuntimeException("upload failed"));

        assertThrows(RuntimeException.class, () -> fileService.storePayload(createObjectBuilder().build(), fileName, templateName));
    }
}
