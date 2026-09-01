package com.watashi.adapters.out.ingestor.pdf;

import com.watashi.core.domain.candidate.CandidatePreferences;
import com.watashi.core.domain.candidate.CandidateProfile;
import com.watashi.core.domain.common.SeniorityLevel;
import com.watashi.core.domain.common.Skill;
import com.watashi.core.domain.discovery.SkillExtractor;
import com.watashi.core.ports.out.CandidateProfileIngestor;
import java.io.IOException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

public class PdfCandidateIngestor implements CandidateProfileIngestor {

    @Override
    public CandidateProfile ingest(byte[] pdfBytes, CandidatePreferences preferences) {
        Objects.requireNonNull(pdfBytes, "pdfBytes cannot be null");
        Objects.requireNonNull(preferences, "preferences cannot be null");

        String pdfText;
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            pdfText = stripper.getText(document);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read PDF document", e);
        }

        if (pdfText == null) {
            pdfText = "";
        }

        String title = pdfText.lines()
                .map(String::strip)
                .filter(line -> !line.isEmpty())
                .findFirst()
                .orElse("Candidate Profile");

        Set<Skill> skills = SkillExtractor.extractSkills(pdfText);

        SeniorityLevel inferredSeniority = SkillExtractor.inferSeniority(pdfText);
        Set<SeniorityLevel> targetSeniorities = new HashSet<>(preferences.targetSeniorities());
        if (inferredSeniority != null) {
            targetSeniorities.add(inferredSeniority);
        }

        return new CandidateProfile(
                UUID.randomUUID().toString(),
                title,
                pdfText.strip(),
                skills,
                targetSeniorities,
                preferences.preferredWorkModes(),
                preferences.desiredSalary(),
                preferences.preferredLocations());
    }
}
