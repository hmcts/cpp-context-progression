package uk.gov.moj.cpp.progression.helper;

import static org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

public class PdfTestHelper {

    public static byte[] asPdf(final String text) {

        final PDDocument document = new PDDocument();
        final PDPage page = new PDPage();
        document.addPage(page);

        try {
            final PDPageContentStream contentStream = new PDPageContentStream(document, page);
            contentStream.beginText();
            contentStream.setFont(HELVETICA_BOLD, 12);
            contentStream.showText(text);
            contentStream.endText();
            contentStream.close();

            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            document.close();

            return baos.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
