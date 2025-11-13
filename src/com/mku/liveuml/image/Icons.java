package com.mku.liveuml.image;

import com.mku.liveuml.Main;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.io.IOException;
import java.util.Objects;

public class Icons {
    public static Icon getIcon(String path) {
        try {
            return new ImageIcon(ImageIO.read(Objects.requireNonNull(Icons.class.getResourceAsStream(path))));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
