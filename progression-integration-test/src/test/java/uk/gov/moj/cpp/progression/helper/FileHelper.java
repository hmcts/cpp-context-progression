package uk.gov.moj.cpp.progression.helper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import org.apache.commons.io.IOUtils;

public class FileHelper {

    public static String read(final String path, final Object... placeholders) {
        try (final InputStream resourceAsStream = FileHelper.class.getClassLoader().getResourceAsStream(path)) {
            if (resourceAsStream == null) {
                throw new AssertionError("File does not exist. " + path);
            }

            final String template = IOUtils.toString(resourceAsStream);
            return placeholders.length > 0 ? String.format(template, placeholders) : template;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
