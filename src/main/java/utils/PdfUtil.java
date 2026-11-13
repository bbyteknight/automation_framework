package utils;

import exceptions.FrameworkException;
import io.qameta.allure.Allure;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

/**
 * Create (for test fixtures) and inspect {@code .pdf} files via Apache PDFBox — text, page count,
 * and document metadata — so steps/tests can verify the content of a generated PDF (invoice,
 * statement, report, ...) without a scenario needing to open the file itself.
 */
public final class PdfUtil {

    private static final float FONT_SIZE = 12f;
    private static final float LINE_HEIGHT = 18f;
    private static final float MARGIN_TOP = 50f;
    private static final float MARGIN_LEFT = 50f;

    private PdfUtil() {
    }

    /**
     * Creates a PDF at {@code destPath} with one page per entry of {@code pages}, each page's
     * lines drawn top to bottom. {@code title}/{@code author} are set as document metadata when
     * non-null.
     */
    public static File createPdf(String destPath, String title, String author, List<List<String>> pages) {
        File destFile = new File(destPath);
        FileHelper.ensureDirectoryExists(destFile.getAbsoluteFile().getParent());

        try (PDDocument document = new PDDocument()) {
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            for (List<String> lines : pages) {
                PDPage page = new PDPage();
                document.addPage(page);
                try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                    contentStream.beginText();
                    contentStream.setFont(font, FONT_SIZE);
                    contentStream.newLineAtOffset(MARGIN_LEFT, page.getMediaBox().getHeight() - MARGIN_TOP);
                    for (String line : lines) {
                        contentStream.showText(line);
                        contentStream.newLineAtOffset(0, -LINE_HEIGHT);
                    }
                    contentStream.endText();
                }
            }

            PDDocumentInformation info = document.getDocumentInformation();
            if (title != null) {
                info.setTitle(title);
            }
            if (author != null) {
                info.setAuthor(author);
            }

            document.save(destFile);
            return destFile;
        } catch (IOException e) {
            throw new FrameworkException("❌ Failed to create PDF: " + destPath, e);
        }
    }

    /** Extracts all text in the PDF, pages concatenated in order. */
    public static String extractText(String pdfPath) {
        try (PDDocument document = Loader.loadPDF(new File(pdfPath))) {
            return new PDFTextStripper().getText(document);
        } catch (IOException e) {
            throw new FrameworkException("❌ Failed to extract text from PDF: " + pdfPath, e);
        }
    }

    public static int pageCount(String pdfPath) {
        try (PDDocument document = Loader.loadPDF(new File(pdfPath))) {
            return document.getNumberOfPages();
        } catch (IOException e) {
            throw new FrameworkException("❌ Failed to read PDF page count: " + pdfPath, e);
        }
    }

    /** PDFBox's own document-information model (title/author/subject/keywords/creation date/...). */
    public static PDDocumentInformation metadata(String pdfPath) {
        try (PDDocument document = Loader.loadPDF(new File(pdfPath))) {
            return document.getDocumentInformation();
        } catch (IOException e) {
            throw new FrameworkException("❌ Failed to read PDF metadata: " + pdfPath, e);
        }
    }

    /**
     * Attaches the PDF as evidence on the active Allure test/step. Guarded on an active test/step
     * actually existing, the same as {@code db.DbClient.attachQueryEvidence} — avoids noisy
     * AllureLifecycle errors if ever called outside a running Cucumber/Allure-wrapped test.
     */
    public static void attachToAllure(String label, String pdfPath) {
        if (Allure.getLifecycle().getCurrentTestCaseOrStep().isEmpty()) {
            return;
        }
        try (FileInputStream inputStream = new FileInputStream(pdfPath)) {
            Allure.addAttachment(label, "application/pdf", inputStream, ".pdf");
        } catch (IOException e) {
            throw new FrameworkException("❌ Failed to attach PDF to Allure report: " + pdfPath, e);
        }
    }
}
