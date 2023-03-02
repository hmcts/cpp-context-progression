package uk.gov.moj.cpp.progression.helper;

import static org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

import uk.gov.moj.cpp.progression.service.utils.PdfHelper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.text.PDFTextStripper;

public class PdfTestHelper {

    public static byte[] asPdf(final String text) {

        final PDDocument document = new PDDocument();
        final PDPage page = new PDPage();
        document.addPage(page);

        final PDFont font = HELVETICA_BOLD;

        try {
            final PDPageContentStream contentStream = new PDPageContentStream(document, page);
            contentStream.beginText();
            contentStream.setFont(font, 12);
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

    public static void assertPdfTextIsEqual(final byte[] pdfOneData, final byte[] pdfTwoData) throws IOException {
        final PDFTextStripper pdfTextStripper = new PDFTextStripper();

        final PDDocument pdfOneDocument = PDDocument.load(pdfOneData);
        final PDDocument pdfTwoDocument = PDDocument.load(pdfTwoData);

        final String pdfOneText = pdfTextStripper.getText(pdfOneDocument);
        final String pdfTwoText = pdfTextStripper.getText(pdfTwoDocument);

        pdfOneDocument.close();
        pdfTwoDocument.close();

        assertThat(pdfOneText, equalTo(pdfTwoText));
    }

    public static void assertPdfTextWasMergedFrom(final byte[] mergedPdfData, final byte[]... pdfDataComponents) throws IOException {

        final PdfHelper pdfHelper = new PdfHelper();
        final byte[] mergedComponentPdfData = pdfHelper.mergePdfDocuments(pdfDataComponents);

        assertPdfTextIsEqual(mergedPdfData, mergedComponentPdfData);
    }
}
