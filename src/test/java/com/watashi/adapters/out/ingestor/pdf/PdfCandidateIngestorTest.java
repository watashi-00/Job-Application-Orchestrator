package com.watashi.adapters.out.ingestor.pdf;

import com.watashi.core.domain.candidate.CandidatePreferences;
import com.watashi.core.domain.candidate.CandidateProfile;
import com.watashi.core.domain.common.SeniorityLevel;
import com.watashi.core.domain.common.WorkMode;
import com.watashi.core.ports.out.CandidateProfileIngestor;
import java.io.ByteArrayOutputStream;
import java.util.Set;
import junit.framework.TestCase;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

public class PdfCandidateIngestorTest extends TestCase {

    public void testIngestPdfDocument() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(doc, page)) {
                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 14);
                stream.newLineAtOffset(50, 700);
                stream.showText("Senior Java Developer Resume");
                stream.newLineAtOffset(0, -20);
                stream.showText("Experienced in Java, Spring Boot, Docker, PostgreSQL and REST microservices.");
                stream.endText();
            }
            doc.save(baos);
        }

        byte[] pdfBytes = baos.toByteArray();
        CandidateProfileIngestor ingestor = new PdfCandidateIngestor();
        CandidatePreferences prefs =
                new CandidatePreferences(null, Set.of(WorkMode.REMOTE), Set.of(SeniorityLevel.SENIOR), Set.of());

        CandidateProfile profile = ingestor.ingest(pdfBytes, prefs);

        assertNotNull(profile);
        assertTrue(profile.title().contains("Senior Java Developer"));
        assertTrue(profile.hasSkillNamed("Java"));
        assertTrue(profile.hasSkillNamed("Spring Boot"));
        assertTrue(profile.hasSkillNamed("Docker"));
    }
}
