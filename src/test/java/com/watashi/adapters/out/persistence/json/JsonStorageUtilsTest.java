package com.watashi.adapters.out.persistence.json;

import java.nio.file.Files;
import java.nio.file.Path;
import junit.framework.TestCase;

public class JsonStorageUtilsTest extends TestCase {

    public void testAtomicWriteAndRead() throws Exception {
        Path tempFile = Files.createTempFile("orchestrator-test", ".json");
        try {
            SampleData data = new SampleData("test-id", 42);
            JsonStorageUtils.writeJsonAtomic(tempFile, data);

            assertTrue(Files.exists(tempFile));
            SampleData loaded = JsonStorageUtils.readJson(tempFile, SampleData.class);
            assertEquals("test-id", loaded.id());
            assertEquals(42, loaded.value());
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    public void testReadNonExistentFileReturnsNull() throws Exception {
        Path nonExistent = Path.of("target", "non-existent-test-file-" + System.currentTimeMillis() + ".json");
        assertFalse(Files.exists(nonExistent));
        SampleData loaded = JsonStorageUtils.readJson(nonExistent, SampleData.class);
        assertNull(loaded);
    }

    public void testWriteJsonAtomicNullChecks() throws Exception {
        Path tempFile = Files.createTempFile("orchestrator-test-null", ".json");
        try {
            try {
                JsonStorageUtils.writeJsonAtomic(null, new SampleData("a", 1));
                fail("Should throw NullPointerException when path is null");
            } catch (NullPointerException e) {
                // Expected
            }

            try {
                JsonStorageUtils.writeJsonAtomic(tempFile, null);
                fail("Should throw NullPointerException when data is null");
            } catch (NullPointerException e) {
                // Expected
            }
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    public void testReadJsonNullChecks() throws Exception {
        try {
            JsonStorageUtils.readJson(null, SampleData.class);
            fail("Should throw NullPointerException when path is null");
        } catch (NullPointerException e) {
            // Expected
        }

        Path tempFile = Files.createTempFile("orchestrator-test-read-null", ".json");
        try {
            try {
                JsonStorageUtils.readJson(tempFile, null);
                fail("Should throw NullPointerException when clazz is null");
            } catch (NullPointerException e) {
                // Expected
            }
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    public record SampleData(String id, int value) {}
}
