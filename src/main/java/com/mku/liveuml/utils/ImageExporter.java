package com.mku.liveuml.utils;

import com.mku.liveuml.view.GraphPanel;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ImageExporter {

    public void saveImage(File file, GraphPanel panel) {
        try {
            BufferedImage image = panel.getImage();
            ImageIO.write(image,"png", file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
