package uk.gov.moj.cpp.progression.service;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.fileservice.api.FileRetriever;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.api.FileStorer;
import uk.gov.justice.services.fileservice.domain.FileReference;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class FileServiceTest {

    @Mock
    private Sender sender;
    @InjectMocks
    private FileService fileService;
    @Mock
    private FileStorer fileStorer;
    @Mock
    private FileRetriever fileRetriever;

    @Test
    public void shouldStorePayloadIntoFileService() throws FileServiceException {

        final UUID fileId = UUID.randomUUID();

        final String fileName = "PrisonCourtRegister.pdf";

        final String templateName = "prison_court_register_template";

        when(fileStorer.store(any(JsonObject.class), any(InputStream.class))).thenReturn(fileId);

        final UUID id = fileService.storePayload(createObjectBuilder().build(), fileName, templateName);

        assertThat(id, equalTo(fileId));
    }

    @Test
    public void shouldThrowExceptionWhenFileServiceFailedToStore() throws FileServiceException {

        final String fileName = "PrisonCourtRegister.pdf";

        final String templateName = "prison_court_register_template";

        when(fileStorer.store(any(JsonObject.class), any(InputStream.class))).thenThrow(FileServiceException.class);

        assertThrows(RuntimeException.class, () -> fileService.storePayload(createObjectBuilder().build(), fileName, templateName));

    }

    @Test
    void retrievePayloadShouldReturnParsedJsonObject() throws FileServiceException {
        final UUID fileId = randomUUID();
        final String json = "{\"courtHouse\":\"Southwark Crown Court\",\"registerDate\":\"2024-10-01\"}";
        final FileReference fileRef = new FileReference(
                fileId,
                Json.createObjectBuilder().build(),
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
        when(fileRetriever.retrieve(fileId)).thenReturn(Optional.of(fileRef));

        final Optional<JsonObject> result = fileService.retrievePayload(fileId);

        assertTrue(result.isPresent());
        assertThat(result.get().getString("courtHouse"), equalTo("Southwark Crown Court"));
        assertThat(result.get().getString("registerDate"), equalTo("2024-10-01"));
    }

    @Test
    void retrievePayloadShouldReturnEmptyWhenFileNotFound() throws FileServiceException {
        final UUID fileId = randomUUID();
        when(fileRetriever.retrieve(fileId)).thenReturn(Optional.empty());

        final Optional<JsonObject> result = fileService.retrievePayload(fileId);

        assertTrue(result.isEmpty());
    }

    @Test
    void retrievePayloadShouldNavigateToJsonPayloadWhenFileIdPointsToPdf() throws FileServiceException {
        final UUID pdfFileId = randomUUID();
        final UUID jsonPayloadFileId = randomUUID();
        final String json = "{\"courtHouse\":\"Southwark Crown Court\",\"registerDate\":\"2024-10-01\"}";

        final JsonObject pdfMetadata = Json.createObjectBuilder()
                .add("payloadFileServiceId", jsonPayloadFileId.toString())
                .build();
        final FileReference pdfRef = new FileReference(pdfFileId, pdfMetadata,
                new ByteArrayInputStream("%PDF-1.6".getBytes(StandardCharsets.UTF_8)));
        final FileReference jsonRef = new FileReference(jsonPayloadFileId,
                Json.createObjectBuilder().build(),
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));

        when(fileRetriever.retrieve(pdfFileId)).thenReturn(Optional.of(pdfRef));
        when(fileRetriever.retrieve(jsonPayloadFileId)).thenReturn(Optional.of(jsonRef));

        final Optional<JsonObject> result = fileService.retrievePayload(pdfFileId);

        assertTrue(result.isPresent());
        assertThat(result.get().getString("courtHouse"), equalTo("Southwark Crown Court"));
        assertThat(result.get().getString("registerDate"), equalTo("2024-10-01"));
    }

    @Test
    void retrievePayloadShouldParseDirectlyWhenMetadataHasNoPayloadFileServiceId() throws FileServiceException {
        final UUID fileId = randomUUID();
        final String json = "{\"courtHouse\":\"Southwark Crown Court\"}";
        final FileReference fileRef = new FileReference(fileId,
                Json.createObjectBuilder().build(),
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
        when(fileRetriever.retrieve(fileId)).thenReturn(Optional.of(fileRef));

        final Optional<JsonObject> result = fileService.retrievePayload(fileId);

        assertTrue(result.isPresent());
        assertThat(result.get().getString("courtHouse"), equalTo("Southwark Crown Court"));
    }

    @Test
    void retrieveRawPayloadShouldReturnRawBytesWithoutParsingJson() throws FileServiceException {
        final UUID fileId = randomUUID();
        final String json = "{\"wording\":\"Count 1: guilty\\nCount 2: not guilty\"}";
        final FileReference fileRef = new FileReference(fileId,
                Json.createObjectBuilder().build(),
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
        when(fileRetriever.retrieve(fileId)).thenReturn(Optional.of(fileRef));

        final Optional<String> result = fileService.retrieveRawPayload(fileId);

        assertTrue(result.isPresent());
        assertThat(result.get(), equalTo(json));
    }

    @Test
    void retrieveRawPayloadShouldReturnEmptyWhenFileNotFound() throws FileServiceException {
        final UUID fileId = randomUUID();
        when(fileRetriever.retrieve(fileId)).thenReturn(Optional.empty());

        final Optional<String> result = fileService.retrieveRawPayload(fileId);

        assertTrue(result.isEmpty());
    }

    @Test
    void retrieveRawPayloadShouldNavigateToJsonPayloadWhenFileIdPointsToPdf() throws FileServiceException {
        final UUID pdfFileId = randomUUID();
        final UUID jsonPayloadFileId = randomUUID();
        final String json = "{\"wording\":\"Count 1: guilty\\nCount 2: not guilty\"}";

        final JsonObject pdfMetadata = Json.createObjectBuilder()
                .add("payloadFileServiceId", jsonPayloadFileId.toString())
                .build();
        final FileReference pdfRef = new FileReference(pdfFileId, pdfMetadata,
                new ByteArrayInputStream("%PDF-1.6".getBytes(StandardCharsets.UTF_8)));
        final FileReference jsonRef = new FileReference(jsonPayloadFileId,
                Json.createObjectBuilder().build(),
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));

        when(fileRetriever.retrieve(pdfFileId)).thenReturn(Optional.of(pdfRef));
        when(fileRetriever.retrieve(jsonPayloadFileId)).thenReturn(Optional.of(jsonRef));

        final Optional<String> result = fileService.retrieveRawPayload(pdfFileId);

        assertTrue(result.isPresent());
        assertThat(result.get(), equalTo(json));
    }

}