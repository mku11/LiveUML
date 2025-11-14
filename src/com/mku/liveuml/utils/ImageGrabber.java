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

import com.mku.liveuml.view.GraphPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class ImageGrabber {
    private final GraphPanel panel;
    private BufferedImage img;

    public ImageGrabber(GraphPanel panel) {
        this.panel = panel;
    }

    public BufferedImage getImage() {
        this.panel.setOnImagePainted((g)-> {
            if (img != null)
                g.drawImage(img, 0, 0, panel);
        });
        try {
            Dimension size = panel.getSize();
            JFrame secondFrame = new JFrame();
            Container parent = panel.getParent();
            panel.getParent().remove(panel);
            secondFrame.add(panel);
            secondFrame.setVisible(true);

            panel.getVisualizationScrollPane().getHorizontalScrollBar().setValue(0);
            panel.getVisualizationScrollPane().getVerticalScrollBar().setValue(0);
            Dimension newSize = panel.getViewer().getVisualizationModel().getLayoutSize();
            secondFrame.setSize(newSize);
            secondFrame.setPreferredSize(newSize);
            panel.setSize(newSize);
            panel.setPreferredSize(newSize);
            secondFrame.invalidate();
            panel.getViewer().repaint();
            panel.repaint();
            secondFrame.repaint();

            layoutComponent(panel);
            img = new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TRANSLUCENT);
            Graphics2D graphics = img.createGraphics();
            panel.print(graphics);
            graphics.dispose();

            secondFrame.remove(panel);
            secondFrame.setVisible(false);
            parent.add(panel);

            panel.setSize(size);
            panel.setPreferredSize(size);
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            this.panel.removeOnImagePainted();
        }
        return img;
    }

    private void layoutComponent(Component c) {
        synchronized (c.getTreeLock()) {
            c.doLayout();
            if (c instanceof Container) {
                for (Component child : ((Container) c).getComponents()) {
                    layoutComponent(child);
                }
            }
        }
    }
}
