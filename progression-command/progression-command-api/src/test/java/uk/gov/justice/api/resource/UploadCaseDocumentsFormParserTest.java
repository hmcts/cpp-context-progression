package uk.gov.justice.api.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.io.IOUtils;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.jboss.resteasy.util.CaseInsensitiveMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class UploadCaseDocumentsFormParserTest {

    private static final String CSV = "row1,col1\nrow2,col2\n";

    private UploadCaseDocumentsFormParser uploadCaseDocumentsFormParser;

    @BeforeEach
    public void setup(){
        uploadCaseDocumentsFormParser = new UploadCaseDocumentsFormParser();
    }


    @Test
    public void shouldReturnEmptyForNullInput() throws IOException {
        final KeyValue<Optional<String>, Optional<InputStream>> result = uploadCaseDocumentsFormParser.parse(null);

        assertFalse(result.getKey().isPresent());
        assertFalse(result.getValue().isPresent());
    }

    @Test
    public void shouldReturnEmptyForEmptyUploadform() throws IOException {

        final Map<String, List<InputPart>> form = getEmptyUploadForm();

        final MultipartFormDataInput input = mock(MultipartFormDataInput.class);
        when(input.getFormDataMap()).thenReturn(form);

        final KeyValue<Optional<String>, Optional<InputStream>> result = uploadCaseDocumentsFormParser.parse(input);

        assertFalse(result.getKey().isPresent());
        assertFalse(result.getValue().isPresent());
    }

    @Test
    public void shouldReturnEmptyForUploadformWithoutFileName() throws IOException {

        final Map<String, List<InputPart>> form = getFormWithoutFileName();

        final MultipartFormDataInput input = mock(MultipartFormDataInput.class);
        when(input.getFormDataMap()).thenReturn(form);

        final KeyValue<Optional<String>, Optional<InputStream>> result = uploadCaseDocumentsFormParser.parse(input);

        assertFalse(result.getKey().isPresent());
        assertFalse(result.getValue().isPresent());
    }

    @Test
    public void shouldReturnEmptyForUploadformWithoutFileContent() throws IOException {

        final Map<String, List<InputPart>> form = getFormWithoutFileContent();

        final MultipartFormDataInput input = mock(MultipartFormDataInput.class);
        when(input.getFormDataMap()).thenReturn(form);

        final KeyValue<Optional<String>, Optional<InputStream>> result = uploadCaseDocumentsFormParser.parse(input);

        assertTrue(result.getKey().isPresent());
        assertFalse(result.getValue().isPresent());
    }

    @Test
    public void shouldThrowExceptionForMalformedUploadform() throws IOException {

        final Map<String, List<InputPart>> form = getFormWithMalformedContentDisposition();

        final MultipartFormDataInput input = mock(MultipartFormDataInput.class);
        when(input.getFormDataMap()).thenReturn(form);

        assertThrows(NullPointerException.class, () -> {
            final KeyValue<Optional<String>, Optional<InputStream>> result = uploadCaseDocumentsFormParser.parse(input);

            assertFalse(result.getKey().isPresent());
            assertFalse(result.getValue().isPresent());
        });
    }

    @Test
    public void shouldParseValidForm() throws IOException {

        final Map<String, List<InputPart>> form = getValidForm();

        final MultipartFormDataInput input = mock(MultipartFormDataInput.class);
        when(input.getFormDataMap()).thenReturn(form);

        final KeyValue<Optional<String>, Optional<InputStream>> result = uploadCaseDocumentsFormParser.parse(input);

        assertEquals("test.csv", result.getKey().get());

        assertTrue(Arrays.equals(CSV.getBytes(), IOUtils.toByteArray(result.getValue().get())));
    }

    private Map<String, List<InputPart>> getValidForm() throws IOException {

        final List<InputPart> inputPart = new ArrayList<>();

        final InputPart inputCsvPart = mock(InputPart.class);

        final Map<String, List<InputPart>> uploadForm = new HashMap<>();

        final InputStream csvInputStream = IOUtils.toInputStream(CSV);

        final MultivaluedMap<String, String> headers = new CaseInsensitiveMap<>();
        headers.put("Content-Disposition", Arrays.asList("name=\"file\"; filename=\"test.csv\""));
        headers.put("Content-Type", Arrays
                        .asList("application/vnd.progression.command.upload-case-documents+json"));

        when(inputCsvPart.getHeaders()).thenReturn(headers);

        when(inputCsvPart.getBody(InputStream.class, null)).thenReturn(csvInputStream);

        inputPart.add(inputCsvPart);

        uploadForm.put("file", inputPart);

        return uploadForm;
    }

    private Map<String, List<InputPart>> getEmptyUploadForm() throws IOException {
        final Map<String, List<InputPart>> uploadForm = new HashMap<>();
        return uploadForm;
    }

    private Map<String, List<InputPart>> getFormWithoutFileName() throws IOException {

        final List<InputPart> inputPart = new ArrayList<>();

        final InputPart inputCsvPart = mock(InputPart.class);

        final Map<String, List<InputPart>> uploadForm = new HashMap<>();

        final InputStream csvInputStream = IOUtils.toInputStream(CSV);

        final MultivaluedMap<String, String> headers = new CaseInsensitiveMap<>();
        headers.put("Content-Disposition", Arrays.asList("name=\"file\"; filename=\"\""));
        headers.put("Content-Type", Arrays
                        .asList("application/vnd.progression.command.upload-case-documents+json"));

        when(inputCsvPart.getHeaders()).thenReturn(headers);

        when(inputCsvPart.getBody(InputStream.class, null)).thenReturn(csvInputStream);

        inputPart.add(inputCsvPart);

        uploadForm.put("file", inputPart);

        return uploadForm;
    }


    private Map<String, List<InputPart>> getFormWithoutFileContent() throws IOException {

        final List<InputPart> inputPart = new ArrayList<>();

        final InputPart inputCsvPart = mock(InputPart.class);

        final Map<String, List<InputPart>> uploadForm = new HashMap<>();

        final InputStream csvInputStream = null;

        final MultivaluedMap<String, String> headers = new CaseInsensitiveMap<>();
        headers.put("Content-Disposition", Arrays.asList("name=\"file\"; filename=\"test.csv\""));
        headers.put("Content-Type", Arrays
                        .asList("application/vnd.progression.command.upload-case-documents+json"));

        when(inputCsvPart.getHeaders()).thenReturn(headers);

        when(inputCsvPart.getBody(InputStream.class, null)).thenReturn(csvInputStream);

        inputPart.add(inputCsvPart);

        uploadForm.put("file", inputPart);

        return uploadForm;
    }

    private Map<String, List<InputPart>> getFormWithMalformedContentDisposition() throws IOException {

        final List<InputPart> inputPart = new ArrayList<>();

        final InputPart inputCsvPart = mock(InputPart.class);

        final Map<String, List<InputPart>> uploadForm = new HashMap<>();

        final InputStream csvInputStream = IOUtils.toInputStream(CSV);

        final MultivaluedMap<String, String> headers = new CaseInsensitiveMap<>();
        headers.put("Content-Disposi", Arrays.asList("name=\"file\"; filename=\"test.csv\""));
        headers.put("Content-Type", Arrays
                        .asList("application/vnd.progression.command.upload-case-documents+json"));

        when(inputCsvPart.getHeaders()).thenReturn(headers);

        when(inputCsvPart.getBody(InputStream.class, null)).thenReturn(csvInputStream);

        inputPart.add(inputCsvPart);

        uploadForm.put("file", inputPart);

        return uploadForm;
    }

}
