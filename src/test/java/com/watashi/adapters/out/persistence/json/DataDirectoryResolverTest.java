package com.watashi.adapters.out.persistence.json;

import java.nio.file.Path;
import java.nio.file.Paths;
import junit.framework.TestCase;

public class DataDirectoryResolverTest extends TestCase {

    public void testDefaultResolution() {
        System.clearProperty("orchestrator.data.dir");
        Path defaultDir = DataDirectoryResolver.resolveDataDirectory();
        assertEquals(Paths.get("data"), defaultDir);
        assertEquals(Paths.get("data", "jobs.json"), DataDirectoryResolver.resolveFilePath("jobs.json"));
    }

    public void testSystemPropertyOverride() {
        String customDir = "/tmp/custom-orchestrator-data";
        System.setProperty("orchestrator.data.dir", customDir);
        try {
            Path resolved = DataDirectoryResolver.resolveDataDirectory();
            assertEquals(Paths.get(customDir), resolved);
            assertEquals(Paths.get(customDir, "jobs.json"), DataDirectoryResolver.resolveFilePath("jobs.json"));
        } finally {
            System.clearProperty("orchestrator.data.dir");
        }
    }
}
