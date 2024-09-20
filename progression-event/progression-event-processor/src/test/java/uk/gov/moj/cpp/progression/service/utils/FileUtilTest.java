package uk.gov.moj.cpp.progression.service.utils;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.fileservice.api.FileRetriever;
import uk.gov.justice.services.fileservice.domain.FileReference;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.UUID;

import javax.json.JsonObject;

import org.apache.http.client.utils.DateUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class FileUtilTest {

    @InjectMocks
    private FileUtil fileUtil;

    @Mock
    private FileRetriever fileRetriever;

    @Mock
    private FileReference fileReference1;

    private static FileReference fileReference;
    private static final String FILE_NAME = "MaterialFile";

    @BeforeEach
    public void setUp() throws IOException {
        fileReference = getFileReference();
    }

    private UUID fileStoreId = UUID.randomUUID();

    @Test
    public void shouldRetrieveFileName() throws Exception {

        when(fileRetriever.retrieve(fileStoreId)).thenReturn(java.util.Optional.of(fileReference));
        String fileName = fileUtil.retrieveFileName(fileStoreId);

        assertTrue(fileName.contains(FILE_NAME));
    }

    @Test
    public void shouldCloseFileReference() throws Exception {

        when(fileRetriever.retrieve(fileStoreId)).thenReturn(java.util.Optional.of(fileReference1));
        fileUtil.retrieveFileName(fileStoreId);

        verify(fileReference1).close();
    }

    private FileReference getFileReference() throws IOException {

        PDDocument pdDocument = new PDDocument();
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        pdDocument.save(outStream);
        InputStream inputStream = new ByteArrayInputStream(outStream.toByteArray());

        final String formatDate = DateUtils.formatDate(new Date());
        final JsonObject metaData = createObjectBuilder()
                .add("fileName",
                        FILE_NAME + "_" + randomUUID() + "_" + formatDate)
                .build();
        return new FileReference(UUID.randomUUID(), metaData, inputStream);
    }
}
