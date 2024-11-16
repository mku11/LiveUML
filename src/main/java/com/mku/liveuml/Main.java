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

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.mku.liveuml.gen.Generator;
import com.mku.liveuml.graph.UMLClass;
import com.mku.liveuml.view.GraphPanel;
import com.mku.liveuml.utils.Exporter;
import com.mku.liveuml.utils.Importer;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.prefs.Preferences;

public class Main {

    public static void main(String[] args) {
        JFrame f = new JFrame();
        f.setTitle("LiveUML");
        f.setIconImage(Toolkit.getDefaultToolkit().getImage(Main.class.getResource("/icons/logo.png")));
        f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        JMenuBar menubar = new JMenuBar();
        JMenu menu = new JMenu("File");
        menubar.add(menu);
        f.setJMenuBar(menubar);
        JMenuItem newGraphItem = new JMenuItem("New");
        menu.add(newGraphItem);
        JMenuItem openGraphItem = new JMenuItem("Open");
        menu.add(openGraphItem);
        JMenuItem saveGraphItem = new JMenuItem("Save");
        menu.add(saveGraphItem);
        JMenuItem exportGraphItem = new JMenuItem("Export");
        menu.add(exportGraphItem);

        menu = new JMenu("Source");
        menubar.add(menu);
        f.setJMenuBar(menubar);
        JMenuItem importSourceFilesItem = new JMenuItem("Import Source Files");
        menu.add(importSourceFilesItem);
        JMenuItem clearSourceFilesItem = new JMenuItem("Clear Source Files");
        menu.add(clearSourceFilesItem);

        menu = new JMenu("Settings");
        menubar.add(menu);
        f.setJMenuBar(menubar);
        JMenuItem chooseEditorItem = new JMenuItem("Choose Text Editor");
        menu.add(chooseEditorItem);

        GraphPanel panel = new GraphPanel();
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.add(panel);
        p.setBorder(new EmptyBorder(10, 10, 10, 10));

        f.getContentPane().add(p);
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

                setupFolder(root);
                List<UMLClass> classes = new Generator().getClasses(root);
                panel.addClasses(classes);
                panel.display(null);
                panel.revalidate();
            }
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
                new Importer().importGraph(file, panel);
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
                new Exporter().exportGraph(file, panel);
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
                new Thread(()-> {
                    BufferedImage image = panel.getImage();
                    saveImage(file, image);
                }).start();
            }
        });

        f.setLocationRelativeTo(null);
        f.setVisible(true);
    }

    private static void setupFolder(File sourceFolder) {
        ReflectionTypeSolver reflectionTypeSolver = new ReflectionTypeSolver();
        JavaParserTypeSolver javaParserTypeSolver = new JavaParserTypeSolver(sourceFolder);
        CombinedTypeSolver combinedSolver = new CombinedTypeSolver();
        combinedSolver.add(reflectionTypeSolver);
        combinedSolver.add(javaParserTypeSolver);

        ParserConfiguration parserConfiguration = new ParserConfiguration()
                .setSymbolResolver(new JavaSymbolSolver(combinedSolver));
        StaticJavaParser.setConfiguration(parserConfiguration);
    }

    private static void saveImage(File file, BufferedImage image) {
        try {
            ImageIO.write(image,"png", file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
