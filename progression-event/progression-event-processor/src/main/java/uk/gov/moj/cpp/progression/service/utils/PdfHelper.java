package uk.gov.moj.cpp.progression.service.utils;


import static org.apache.pdfbox.io.MemoryUsageSetting.setupMainMemoryOnly;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.stream.Stream;

import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;

public class PdfHelper {

    public byte[] mergePdfDocuments(final byte[]... pdfs) throws IOException {
        final PDFMergerUtility pdfMerger = new PDFMergerUtility();
        final ByteArrayOutputStream resultingDocument = new ByteArrayOutputStream();

        Stream.of(pdfs).forEach(pdf -> {
            final ByteArrayInputStream pdfByteArrayInputStream = new ByteArrayInputStream(pdf);
            pdfMerger.addSource(pdfByteArrayInputStream);
        });

        pdfMerger.setDestinationStream(resultingDocument);
        pdfMerger.mergeDocuments(setupMainMemoryOnly());
        return resultingDocument.toByteArray();
    }

    public byte[] insertEmptyPage(final byte[] pdf, final byte [] emptyPagePdf, final int index) throws IOException {
        try(final PDDocument document = PDDocument.load(pdf);
            final PDDocument emptyDocument = PDDocument.load(emptyPagePdf)) {
            document.getPages().insertAfter(emptyDocument.getPage(0), document.getPage(index < 1 ? 0 : index - 1));
            final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            document.save(byteArrayOutputStream);
            return byteArrayOutputStream.toByteArray();
        }
    }
}
