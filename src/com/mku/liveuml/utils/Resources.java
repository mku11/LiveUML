package com.mku.liveuml.utils;

import java.io.IOException;
import java.io.InputStream;

public class Resources {
    public static String getResourceAsString(String path) throws IOException {
        InputStream stream = Resources.class.getResourceAsStream(path);
        byte[] data = stream.readAllBytes();
        String contents = new String(data);
        return contents;
    }

    public static String getVersion() {
        return Resources.class.getPackage().getImplementationVersion();
    }
}
