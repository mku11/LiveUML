package com.mku.liveuml.utils;

import com.mku.liveuml.Main;

import java.io.IOException;
import java.io.InputStream;

public class ResourceUtils {
    public static String getResourceAsString(String path) throws IOException {
        InputStream stream = Main.class.getResourceAsStream(path);
        byte[] data = stream.readAllBytes();
        String contents = new String(data);
        return contents;
    }
}
