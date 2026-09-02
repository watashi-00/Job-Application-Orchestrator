package com.watashi.adapters.out.persistence.json;

import java.nio.file.Path;
import java.nio.file.Paths;

public final class DataDirectoryResolver {

    private DataDirectoryResolver() {
        // Utility class
    }

    public static Path resolveDataDirectory() {
        String sysProp = System.getProperty("orchestrator.data.dir");
        if (sysProp != null && !sysProp.isBlank()) {
            return Paths.get(sysProp);
        }

        String envVar = System.getenv("ORCHESTRATOR_DATA_DIR");
        if (envVar != null && !envVar.isBlank()) {
            return Paths.get(envVar);
        }

        return Paths.get("data");
    }

    public static Path resolveFilePath(String filename) {
        return resolveDataDirectory().resolve(filename);
    }
}
