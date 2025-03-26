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
package com.mku.liveuml;

import com.mku.liveuml.gen.UMLGenerator;
import com.mku.liveuml.graph.UMLClass;
import com.mku.liveuml.utils.Exporter;
import com.mku.liveuml.utils.ImageExporter;
import com.mku.liveuml.utils.Importer;
import com.mku.liveuml.view.GraphPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.geom.Point2D;
import java.io.File;
import java.util.HashMap;
import java.util.prefs.Preferences;

public class Main {
    private static final String version = "0.9.0";
    private static UMLGenerator generator;
    private static GraphPanel panel;

    public static void main(String[] args) {
        generator = new UMLGenerator();

        JFrame frame = new JFrame();
        frame.setTitle("LiveUML");
        frame.setIconImage(getIconImage());
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        createMenu(frame);
        createPanel(frame);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static void createPanel(JFrame f) {
        panel = new GraphPanel(generator);
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.add(panel);
        p.setBorder(new EmptyBorder(10, 10, 10, 10));
        f.getContentPane().add(p);
    }

    private static Image getIconImage() {
        return Toolkit.getDefaultToolkit().getImage(Main.class.getResource("/icons/logo.png"));
    }

    private static void createMenu(JFrame f) {
        JMenuBar menubar = new JMenuBar();
        JMenu menu = new JMenu("File");
        menubar.add(menu);
        f.setJMenuBar(menubar);
        JMenuItem newGraphItem = new JMenuItem("New Diagram");
        menu.add(newGraphItem);
        JMenuItem openGraphItem = new JMenuItem("Open Diagram");
        menu.add(openGraphItem);
        JMenuItem saveGraphItem = new JMenuItem("Save Diagram");
        menu.add(saveGraphItem);
        JMenuItem exportGraphItem = new JMenuItem("Export Image");
        menu.add(exportGraphItem);

        menu = new JMenu("Source");
        menubar.add(menu);
        f.setJMenuBar(menubar);
        JMenuItem importSourceFilesItem = new JMenuItem("Import Source");
        menu.add(importSourceFilesItem);
        JMenuItem listSourceFilesItem = new JMenuItem("List Sources");
        menu.add(listSourceFilesItem);
        JMenuItem refreshSourceFilesItem = new JMenuItem("Refresh Sources");
        menu.add(refreshSourceFilesItem);

        menu = new JMenu("Settings");
        menubar.add(menu);
        f.setJMenuBar(menubar);
        JMenuItem chooseEditorItem = new JMenuItem("Choose Text Editor");
        menu.add(chooseEditorItem);

        menu = new JMenu("Help");
        menubar.add(menu);
        f.setJMenuBar(menubar);
        JMenuItem helpItem = new JMenuItem("Help");
        menu.add(helpItem);
        JMenuItem aboutItem = new JMenuItem("About");
        menu.add(aboutItem);

        f.setMinimumSize(new Dimension(1200, 800));
        f.pack();

        importSourceFilesItem.addActionListener((e) -> {
            Preferences prefs = Preferences.userRoot().node(Main.class.getName());
            JFileChooser fc = new JFileChooser(prefs.get("LAST_SOURCE_FOLDER",
                    new File(".").getAbsolutePath()));
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int returnVal = fc.showOpenDialog(f);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File root = fc.getSelectedFile();
                prefs.put("LAST_SOURCE_FOLDER", root.getPath());
                generator.importSourcesDir(root);
                panel.display(null);
                panel.revalidate();
            }
        });

        newGraphItem.addActionListener((e) -> {
            panel.clear();
            panel.revalidate();
        });

        openGraphItem.addActionListener((e) -> {
            Preferences prefs = Preferences.userRoot().node(Main.class.getName());
            JFileChooser fc = new JFileChooser(prefs.get("LAST_GRAPH_FILE",
                    new File(".").getAbsolutePath()));
            fc.setDialogTitle("Choose graph file to load");
            fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
            FileNameExtensionFilter filter = new FileNameExtensionFilter("GraphML files", "graphml");
            fc.setFileFilter(filter);
            int returnVal = fc.showOpenDialog(f);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();
                prefs.put("LAST_GRAPH_FILE", file.getPath());
                HashMap<UMLClass, Point2D.Double> verticesPositions = new HashMap<>();
                new Importer().importGraph(file, generator, verticesPositions);
                panel.display(verticesPositions);
                panel.revalidate();
            }
        });


        saveGraphItem.addActionListener((e) -> {
            Preferences prefs = Preferences.userRoot().node(Main.class.getName());
            JFileChooser fc = new JFileChooser(prefs.get("LAST_GRAPH_FILE",
                    new File(".").getAbsolutePath()));
            fc.setDialogTitle("Choose graph file to save");
            fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
            FileNameExtensionFilter filter = new FileNameExtensionFilter("GraphML files", "graphml");
            fc.setFileFilter(filter);
            int returnVal = fc.showSaveDialog(f);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();
                prefs.put("LAST_GRAPH_FILE", file.getPath());
                new Exporter().exportGraph(file, generator, panel.getVertexPositions());
            }
        });

        chooseEditorItem.addActionListener((e) -> {
            Preferences prefs = Preferences.userRoot().node(Main.class.getName());
            JFileChooser fc = new JFileChooser(prefs.get("LAST_TEXT_EDITOR_FILE",
                    new File(".").getAbsolutePath()));
            fc.setDialogTitle("Choose text editor");
            fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
            FileNameExtensionFilter filter = new FileNameExtensionFilter("Executable files", "exe");
            fc.setFileFilter(filter);
            int returnVal = fc.showOpenDialog(f);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();
                prefs.put("LAST_TEXT_EDITOR_FILE", file.getPath());
            }
        });


        exportGraphItem.addActionListener((e) -> {
            Preferences prefs = Preferences.userRoot().node(Main.class.getName());
            JFileChooser fc = new JFileChooser(prefs.get("LAST_EXPORT_FILE",
                    new File(".").getAbsolutePath()));
            fc.setDialogTitle("Choose file to export");
            fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
            FileNameExtensionFilter filter = new FileNameExtensionFilter("PNG files", "png");
            fc.setFileFilter(filter);
            int returnVal = fc.showSaveDialog(f);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();
                prefs.put("LAST_EXPORT_FILE", file.getPath());
                new Thread(() -> {
                    new ImageExporter().saveImage(file, panel);
                }).start();
            }
        });

        helpItem.addActionListener((e) -> {
            JOptionPane.showMessageDialog(null,
                    "Ctrl+Click: Selects UML Class or Relationship \n"
                            + "Ctrl+Mouse Move: Move UML Class or Relationship \n"
                            + "Right Click: Display options for a UML Class \n"
                            + "Double Click: Expand/Collapse UML Class \n"
                            + "Mouse Scroll: Zoom in/out \n",
                    "Help", JOptionPane.INFORMATION_MESSAGE, new ImageIcon(getIconImage()));
        });

        aboutItem.addActionListener((e) -> {
            JOptionPane.showMessageDialog(null, "LiveUML v" + version + " \n"
                            + "License: MIT \n"
                            + "Project site: https://github.com/mku11/LiveUML \n\n"
                            + "Parts of this software include products with open source licenses:  \n"
                            + "JavaParser https://github.com/javaparser/javaparser  \n"
                            + "JGraphT https://github.com/jgrapht/jgrapht  \n"
                            + "JUNGRAPHT-VISUALIZATION https://github.com/tomnelson/jungrapht-visualization \n"
                            + "Gson https://github.com/google/gson ",
                    "About", JOptionPane.INFORMATION_MESSAGE, new ImageIcon(getIconImage()));
        });
    }
}
