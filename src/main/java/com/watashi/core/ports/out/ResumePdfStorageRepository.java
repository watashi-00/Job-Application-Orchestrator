package com.watashi.core.ports.out;

import java.util.Optional;

public interface ResumePdfStorageRepository {

    void savePdf(byte[] pdfBytes);

    Optional<byte[]> loadPdf();

    boolean exists();
}
