package uk.gov.justice.api.resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

@Named
public class UploadCaseDocumentsFormParser {


    private static final String FILE_PART_KEY = "file";

    /**
     *
     * @param multipartFormDataInput
     * @return
     * @throws IOException
     */
    public KeyValue<Optional<String>, Optional<InputStream>> parse(
                    final MultipartFormDataInput multipartFormDataInput) throws IOException {
        if (multipartFormDataInput == null) {
            return getEmptyKeyValue();
        }

        final Optional<InputPart> inputPartOptional = getFilePart(multipartFormDataInput);


        if (!inputPartOptional.isPresent()) {
            return getEmptyKeyValue();
        }

        final InputPart filePart = inputPartOptional.get();

        return parse(filePart);
    }

    private KeyValue<Optional<String>, Optional<InputStream>> parse(final InputPart filePart)
                    throws IOException {

        final Optional<String> fileNameUnSplitted = Pattern.compile(";")
                        .splitAsStream(filePart.getHeaders().getFirst("Content-Disposition"))
                        .filter(s -> s.trim().toLowerCase().startsWith("filename")).findFirst();

        if (!fileNameUnSplitted.isPresent()) {
            return getEmptyKeyValue();
        }

        final String fileName = Pattern.compile("=").splitAsStream(fileNameUnSplitted.get().trim())
                        .collect(Collectors.toList()).get(1).trim().replaceAll("\"", "");

        if (StringUtils.isBlank(fileName)) {
            return getEmptyKeyValue();
        }

        final InputStream inputStream = filePart.getBody(InputStream.class, null);

        if (inputStream == null) {
            return getEmptyKeyValue(fileName);
        }

        return new KeyValue<>(Optional.of(fileName), Optional.of(inputStream));

    }

    public Optional<InputPart> getFilePart(final MultipartFormDataInput multipartFormDataInput) {

        final Map<String, List<InputPart>> uploadForm = multipartFormDataInput.getFormDataMap();

        final List<InputPart> file = uploadForm.get(FILE_PART_KEY);

        if (file == null || file.isEmpty()) {
            return Optional.empty();
        }

        final InputPart filePart = file.get(0);

        return Optional.of(filePart);
    }

    private KeyValue<Optional<String>, Optional<InputStream>> getEmptyKeyValue() {

        return new KeyValue<>(Optional.empty(), Optional.empty());
    }

    private KeyValue<Optional<String>, Optional<InputStream>> getEmptyKeyValue(final String key) {

        return new KeyValue<>(Optional.of(key), Optional.empty());
    }
}
