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
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.geom.Point2D;
import java.io.File;
import java.util.HashMap;
import java.util.prefs.Preferences;

public class Main {
    private static final String version = "0.9.0";
    private static UMLGenerator generator;
    private static GraphPanel panel;
    private static String filepath;
    private static JLabel status;

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
        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        status = new JLabel();
        statusBar.add(status);
        p.add(statusBar);
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

        JMenuItem newGraphItem = new JMenuItem("New");
        newGraphItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_MASK));
        menu.add(newGraphItem);

        JMenuItem openGraphItem = new JMenuItem("Open");
        openGraphItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK));
        menu.add(openGraphItem);

        JMenuItem saveGraphItem = new JMenuItem("Save");
        saveGraphItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK));
        menu.add(saveGraphItem);

        JMenuItem saveAsGraphItem = new JMenuItem("Save As");
        saveAsGraphItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK | InputEvent.SHIFT_DOWN_MASK));
        menu.add(saveAsGraphItem);

        JMenuItem exportGraphItem = new JMenuItem("Export Image");
        exportGraphItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_MASK));
        menu.add(exportGraphItem);

        JMenuItem closeGraphItem = new JMenuItem("Close");
        closeGraphItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK | InputEvent.SHIFT_DOWN_MASK));
        menu.add(closeGraphItem);

        JMenuItem exitGraphItem = new JMenuItem("Exit");
        exitGraphItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_MASK));
        menu.add(exitGraphItem);

        menu = new JMenu("Source");
        menubar.add(menu);
        f.setJMenuBar(menubar);

        JMenuItem importSourceFilesItem = new JMenuItem("Import Source");
        importSourceFilesItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.CTRL_MASK));
        menu.add(importSourceFilesItem);

        JMenuItem listSourceFilesItem = new JMenuItem("List Sources");
        listSourceFilesItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_MASK));
        menu.add(listSourceFilesItem);

        JMenuItem refreshSourceFilesItem = new JMenuItem("Refresh Sources");
        refreshSourceFilesItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_MASK));
        menu.add(refreshSourceFilesItem);

        menu = new JMenu("Settings");
        menubar.add(menu);
        f.setJMenuBar(menubar);

        JMenuItem chooseEditorItem = new JMenuItem("Choose Text Editor / IDE");
        chooseEditorItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_MASK));
        menu.add(chooseEditorItem);

        menu = new JMenu("Help");
        menubar.add(menu);
        f.setJMenuBar(menubar);

        JMenuItem helpItem = new JMenuItem("Help");
        helpItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, InputEvent.CTRL_MASK));
        menu.add(helpItem);

        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_MASK));
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
                new Thread(() -> {
                    setStatus("Importing sources");
                    try {
                        generator.importSourcesDir(root);
                        EventQueue.invokeLater(() -> {
                            panel.display(null);
                            panel.revalidate();
                            setStatus("Sources imported", 3000);
                        });
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(null, "Error during import: " + ex,
                                "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }).start();
            }
        });

        newGraphItem.addActionListener((e) -> {
            panel.clear();
            panel.revalidate();
            filepath = null;
            setTitle(f, null);
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
                filepath = file.getPath();
                prefs.put("LAST_GRAPH_FILE", file.getPath());
                HashMap<UMLClass, Point2D.Double> verticesPositions = new HashMap<>();
                panel.clear();
                new Thread(() -> {
                    setStatus("Loading diagram");
                    new Importer().importGraph(file, generator, verticesPositions);
                    EventQueue.invokeLater(() -> {
                        panel.display(verticesPositions);
                        panel.revalidate();
                        setTitle(f, getFilenameWithoutExtension(file.getName()));
                        setStatus("Diagram loaded", 3000);
                    });
                }).start();
            }
        });

        saveGraphItem.addActionListener((e) -> {
            save(f, filepath);
        });


        saveAsGraphItem.addActionListener((e) -> {
            saveAs(f);
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
            if (filepath != null) {
                String filename = new File(filepath).getName();
                filename = getFilenameWithoutExtension(filename);
                fc.setSelectedFile(new File(filename + ".png"));
            } else {
                fc.setSelectedFile(new File("diagram.png"));
            }
            FileNameExtensionFilter filter = new FileNameExtensionFilter("PNG files", "png");
            fc.setFileFilter(filter);
            int returnVal = fc.showSaveDialog(f);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();
                prefs.put("LAST_EXPORT_FILE", file.getPath());
                new Thread(() -> {
                    setStatus("Exporting image");
                    new ImageExporter().saveImage(file, panel);
                    setStatus("Image exported", 3000);
                }).start();
            }
        });

        closeGraphItem.addActionListener(((e) -> {
            panel.clear();
            panel.revalidate();
        }));

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

        exitGraphItem.addActionListener(((e) -> {
            int response = JOptionPane.showConfirmDialog(null, "Save before exit?", "Confirm",
                    JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (response == JOptionPane.YES_OPTION) {
                save(f, filepath);
                f.dispatchEvent(new WindowEvent(f, WindowEvent.WINDOW_CLOSING));
            } else if (response == JOptionPane.NO_OPTION) {
                f.dispatchEvent(new WindowEvent(f, WindowEvent.WINDOW_CLOSING));
            }
        }));
    }

    private static String getFilenameWithoutExtension(String filename) {
        int index = filename.lastIndexOf(".");
        if (index >= 0)
            return filename.substring(0, index);
        return filename;
    }

    private static void save(JFrame f, String filepath) {
        if (filepath != null) {
            File file = new File(filepath);
            new Thread(() -> {
                setStatus("Saving diagram");
                new Exporter().exportGraph(file, generator, panel.getVertexPositions());
                EventQueue.invokeLater((() -> {
                    setTitle(f, getFilenameWithoutExtension(file.getName()));
                    setStatus("Diagram saved", 3000);
                }));
            }).start();
        } else {
            saveAs(f);
        }
    }

    private static void setTitle(JFrame f, String name) {
        String title = "LiveUML";
        if (name != null)
            title += " - " + name;
        f.setTitle(title);
    }

    private static void setStatus(String status) {
        setStatus(status, 0);
    }

    private static void setStatus(String status, int expiry) {
        EventQueue.invokeLater(() -> {
            if (status == null)
                Main.status.setText("");
            else
                Main.status.setText(status);
        });

        if (expiry > 0) {
            javax.swing.Timer timer = new javax.swing.Timer(expiry, event -> {
                EventQueue.invokeLater(() -> {
                    setStatus(null);
                });
            });
            timer.setRepeats(false);
            timer.start();
        }
    }

    private static void saveAs(JFrame f) {
        Preferences prefs = Preferences.userRoot().node(Main.class.getName());
        JFileChooser fc = new JFileChooser(prefs.get("LAST_GRAPH_FILE",
                new File(".").getAbsolutePath()));
        if (filepath != null)
            fc.setSelectedFile(new File(filepath));
        else
            fc.setSelectedFile(new File("diagram.graphml"));
        fc.setDialogTitle("Choose graph file to save");
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        FileNameExtensionFilter filter = new FileNameExtensionFilter("GraphML files", "graphml");
        fc.setFileFilter(filter);
        int returnVal = fc.showSaveDialog(f);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            if (file.exists()) {
                int response = JOptionPane.showConfirmDialog(null, "File exists, overwrite?", "Confirm",
                        JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (response == JOptionPane.NO_OPTION) {
                    return;
                }
            }
            prefs.put("LAST_GRAPH_FILE", file.getPath());
            save(f, file.getPath());
        }
    }
}
