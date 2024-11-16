package com.mku.liveuml.utils;

import com.mku.liveuml.Main;

import javax.swing.*;
import java.io.IOException;
import java.util.prefs.Preferences;

public class FileUtils {

    public static void openFileLine(String filePath, int line) {
        Preferences prefs = Preferences.userRoot().node(Main.class.getName());
        String execPath = prefs.get("LAST_TEXT_EDITOR_FILE", null);
        if(execPath == null) {
            JOptionPane.showMessageDialog(null, "No text editor selected");
            return;
        }
        try {
            if(execPath.endsWith("idea.exe") || execPath.endsWith("idea64.exe") ||
                    execPath.endsWith("idea") || execPath.endsWith("idea64") ||
                    execPath.endsWith("eclipsec.exe") || execPath.endsWith("eclipse.exe")
                    || execPath.endsWith("eclipse") )
                Runtime.getRuntime().exec(new String[]{execPath, filePath+":"+line});
            else if(execPath.endsWith("notepad++.exe"))
                Runtime.getRuntime().exec(new String[]{execPath, filePath, "-n"+line});
            else
                Runtime.getRuntime().exec(new String[]{execPath, filePath});
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Could not open editor: " + e);
            throw new RuntimeException(e);
        }
    }
}
