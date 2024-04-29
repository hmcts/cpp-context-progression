package uk.gov.moj.cpp.progression.service;

import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.api.FileStorer;

import java.io.InputStream;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class FileServiceTest {

    @Mock
    private Sender sender;
    @InjectMocks
    private FileService fileService;
    @Mock
    private FileStorer fileStorer;

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

}