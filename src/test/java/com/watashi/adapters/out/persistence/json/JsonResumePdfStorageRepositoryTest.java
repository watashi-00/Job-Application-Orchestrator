package com.watashi.adapters.out.persistence.json;

import java.nio.file.Files;
import java.nio.file.Path;
import junit.framework.TestCase;

public class JsonResumePdfStorageRepositoryTest extends TestCase {

    private Path tempFile;
    private JsonResumePdfStorageRepository repository;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Path tempDir = Files.createTempDirectory("pdf-repo-test");
        tempFile = tempDir.resolve("resume.pdf");
        repository = new JsonResumePdfStorageRepository(tempFile);
    }

    @Override
    protected void tearDown() throws Exception {
        if (Files.exists(tempFile)) {
            Files.delete(tempFile);
        }
        if (tempFile.getParent() != null && Files.exists(tempFile.getParent())) {
            Files.delete(tempFile.getParent());
        }
        super.tearDown();
    }

    public void testExistsReturnsFalseWhenFileDoesNotExist() {
        assertFalse(repository.exists());
        assertTrue(repository.loadPdf().isEmpty());
    }

    public void testSaveAndLoadPdf() {
        byte[] pdfContent = "%PDF-1.4 fake pdf content".getBytes();
        repository.savePdf(pdfContent);

        assertTrue(repository.exists());
        assertTrue(repository.loadPdf().isPresent());
        assertEquals(new String(pdfContent), new String(repository.loadPdf().get()));
    }

    public void testSavePdfNullThrowsException() {
        try {
            repository.savePdf(null);
            fail("Expected NullPointerException");
        } catch (NullPointerException expected) {
            assertEquals("pdfBytes cannot be null", expected.getMessage());
        }
    }

    public void testDefaultConstructor() {
        JsonResumePdfStorageRepository defaultRepo = new JsonResumePdfStorageRepository();
        assertNotNull(defaultRepo);
    }
}
