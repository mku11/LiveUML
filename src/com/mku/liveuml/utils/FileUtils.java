/*
MIT License

Copyright (c) 2024 Max Kas

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/
package com.mku.liveuml.utils;

import com.mku.liveuml.Main;

import javax.swing.*;
import java.io.IOException;
import java.util.prefs.Preferences;

public class FileUtils {
    public static void openFileAtLine(String filePath, int line) {
        Preferences prefs = Preferences.userRoot().node(Main.class.getName());
        String execPath = prefs.get("LAST_TEXT_EDITOR_FILE", null);
        if (execPath == null) {
            JOptionPane.showMessageDialog(null, "No text editor selected");
            return;
        }
        try {
            if (execPath.endsWith("idea.exe") || execPath.endsWith("idea64.exe") ||
                    execPath.endsWith("idea") || execPath.endsWith("idea64") ||
                    execPath.endsWith("eclipsec.exe") || execPath.endsWith("eclipse.exe")
                    || execPath.endsWith("eclipse"))
                Runtime.getRuntime().exec(new String[]{execPath, filePath + ":" + line});
            else if (execPath.endsWith("notepad++.exe"))
                Runtime.getRuntime().exec(new String[]{execPath, filePath, "-n" + line});
            else
                Runtime.getRuntime().exec(new String[]{execPath, filePath});
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Could not open editor: " + e);
            throw new RuntimeException(e);
        }
    }

    public static String getFilenameWithoutExtension(String filename) {
        int index = filename.lastIndexOf(".");
        if (index >= 0)
            return filename.substring(0, index);
        return filename;
    }

}
