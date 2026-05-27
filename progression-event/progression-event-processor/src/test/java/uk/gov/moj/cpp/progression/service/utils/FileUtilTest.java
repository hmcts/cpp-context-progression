package uk.gov.moj.cpp.progression.service.utils;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobProperties;

import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class FileUtilTest {

    @InjectMocks
    private FileUtil fileUtil;

    @Mock
    private BlobContainerClient blobContainerClient;

    @Mock
    private BlobClient blobClient;

    @Mock
    private BlobProperties blobProperties;

    @Test
    public void shouldRetrieveFileName() {
        final UUID fileId = randomUUID();
        final String expectedFileName = "MaterialFile_abc";

        when(blobContainerClient.getBlobClient(fileId.toString())).thenReturn(blobClient);
        when(blobClient.getProperties()).thenReturn(blobProperties);
        when(blobProperties.getMetadata()).thenReturn(Map.of("fileName", expectedFileName));

        final String fileName = fileUtil.retrieveFileName(fileId);

        assertThat(fileName, is(expectedFileName));
    }

    @Test
    public void shouldReturnEmptyStringWhenBlobNotFound() {
        final UUID fileId = randomUUID();

        when(blobContainerClient.getBlobClient(fileId.toString())).thenReturn(blobClient);
        when(blobClient.getProperties()).thenThrow(new RuntimeException("blob not found"));

        final String fileName = fileUtil.retrieveFileName(fileId);

        assertThat(fileName, is(StringUtils.EMPTY));
    }
}
