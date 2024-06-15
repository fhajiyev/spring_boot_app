package com.generac.ces.systemgateway.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class TestUtil {
    public static String readFromFile(String filePath) {
        try {
            return new String(Files.readAllBytes(Paths.get(filePath)));
        } catch (IOException e) {
            throw new RuntimeException("Failed to read input state.", e);
        }
    }
}
