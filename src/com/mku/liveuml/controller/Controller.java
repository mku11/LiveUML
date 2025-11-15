package com.mku.liveuml.controller;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.mku.liveuml.Main;
import com.mku.liveuml.format.Formatter;
import com.mku.liveuml.model.*;
import com.mku.liveuml.Config;
import com.mku.liveuml.utils.*;
import com.mku.liveuml.view.ClassesPane;
import com.mku.liveuml.view.ContextMenu;
import com.mku.liveuml.view.GraphPanel;
import com.mku.liveuml.view.MenuBar;
import org.jungrapht.visualization.VisualizationViewer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.prefs.Preferences;

public class Controller {
    private UMLDiagram diagram;
    private GraphPanel graphPanel;
    private JLabel status;
    private JButton errors;
    private String msg;
    private Formatter formatter;
    private JFrame frame;
    private ClassesPane classesScrollPane;
    private ExecutorService executor = Executors.newCachedThreadPool();
    private MenuBar menuBar;
    private Preferences prefs;
    private UMLParser parser;

    public void init() throws IOException {
        FlatDarculaLaf.setup();
        prefs = Preferences.userRoot().node(Main.class.getName());
        diagram = createDiagram();
        createFormatter();
        createFrame();
    }

    private UMLDiagram createDiagram() {
        parser = new UMLParser();
        parser.setNotifyProgress((progress) -> {
            status.setText(progress);
        });
        return new UMLDiagram(parser);
    }

    private void createFormatter() throws IOException {
        String classHtmlTemplate = Resources.getResourceAsString("/html/class.html");
        String propertyHtmlTemplate = Resources.getResourceAsString("/html/property.html");
        String dividerHtmlTemplate = Resources.getResourceAsString("/html/divider.html");
        formatter = new Formatter(classHtmlTemplate, propertyHtmlTemplate, dividerHtmlTemplate);
    }

    private void createFrame() {
        frame = new JFrame();
        frame.setTitle(Config.APPNAME);
        frame.setIconImage(getIconImage());
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        menuBar = new MenuBar();
        frame.setJMenuBar(menuBar);
        setMenuListeners();
        frame.pack();
        createPanes();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void setMenuListeners() {
        menuBar.setListener(MenuBar.Action.New, (e) -> createNewDiagram());
        menuBar.setListener(MenuBar.Action.Open, (e) -> promptOpenDiagram());
        menuBar.setListener(MenuBar.Action.Save, (e) -> saveDiagram(diagram.getFilepath()));
        menuBar.setListener(MenuBar.Action.SaveAs, (e) -> saveDiagramAs());
        menuBar.setListener(MenuBar.Action.Close, (e) -> promptCloseDiagram());
        menuBar.setListener(MenuBar.Action.ExportImage, (e) -> promptExportImage());
        menuBar.setListener(MenuBar.Action.Exit, (e) -> promptExit());

        menuBar.setListener(MenuBar.Action.ToggleExpand, (e) -> toggleExpand());

        menuBar.setListener(MenuBar.Action.ImportSource, (e) -> promptImportSource());
        menuBar.setListener(MenuBar.Action.RefreshSources, (e) -> promptRefreshSources());

        menuBar.setListener(MenuBar.Action.ChooseViewer, (e) -> promptChooseViewer());

        menuBar.setListener(MenuBar.Action.Help, (e) -> showHelp());
        menuBar.setListener(MenuBar.Action.About, (e) -> showLicense());
    }

    private void promptRefreshSources() {
        int response = JOptionPane.showConfirmDialog(null, "This will refresh your diagram and " +
                        "you might lose positional information, continue?", "Confirm",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (response == JOptionPane.NO_OPTION)
            return;
        refreshSources();
    }

    private void refreshSources() {
        executor.submit(()-> {
            classesScrollPane.clear();
            diagram.refresh();
            graphPanel.display(diagram, graphPanel.getVertexPositions());
            updateErrors(diagram);
            UMLClass[] classesArr = diagram.getGraph().vertexSet().toArray(new UMLClass[0]);
            classesScrollPane.setClasses(classesArr);
        });
    }

    private void promptExit() {
        int response = JOptionPane.showConfirmDialog(null, "Save before exit?", "Confirm",
                JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (response == JOptionPane.YES_OPTION) {
            saveDiagram(diagram.getFilepath());
            frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
        } else if (response == JOptionPane.NO_OPTION) {
            frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
        }
    }

    private void toggleExpand() {
        boolean collapsed = graphPanel.toggleCollapse();
        menuBar.setLabel(MenuBar.Action.ToggleExpand, collapsed ? "Expand All" : "Collapse All");
    }

    private void createNewDiagram() {
        classesScrollPane.clear();
        graphPanel.clear();
        graphPanel.revalidate();
        diagram = createDiagram();
        setTitle(null);
        graphPanel.display(diagram);
    }

    private void promptExportImage() {
        JFileChooser fc = new JFileChooser(prefs.get("LAST_EXPORT_FILE",
                new File(".").getAbsolutePath()));
        fc.setDialogTitle("Choose file to export");
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        if (diagram.getFilepath() != null) {
            String filename = new File(diagram.getFilepath()).getName();
            filename = FileUtils.getFilenameWithoutExtension(filename);
            fc.setSelectedFile(new File(filename + ".png"));
        } else {
            fc.setSelectedFile(new File("diagram.png"));
        }
        FileNameExtensionFilter filter = new FileNameExtensionFilter("PNG files", "png");
        fc.setFileFilter(filter);
        int returnVal = fc.showSaveDialog(frame);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            if (!shouldOverwriteFile(fc.getSelectedFile()))
                return;
            prefs.put("LAST_EXPORT_FILE", fc.getSelectedFile().getPath());
            exportImage(fc.getSelectedFile());
        }
    }

    private void exportImage(File file) {
        executor.submit(() -> {
            setStatus("Exporting image");
            ImageGrabber imageGrabber = new ImageGrabber(graphPanel);
            BufferedImage image = imageGrabber.getImage();
            new ImageExporter().saveImage(file, image);
            setStatus("Image exported", 3000);
        });
    }

    private void promptChooseViewer() {
        JFileChooser fc = new JFileChooser(prefs.get("LAST_TEXT_EDITOR_FILE",
                new File(".").getAbsolutePath()));
        fc.setDialogTitle("Choose text editor");
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Executable files", "exe");
        fc.setFileFilter(filter);
        int returnVal = fc.showOpenDialog(frame);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            prefs.put("LAST_TEXT_EDITOR_FILE", file.getPath());
        }
    }

    private void promptCloseDiagram() {
        classesScrollPane.clear();
        graphPanel.clear();
        graphPanel.revalidate();
        diagram = null;
        setTitle(null);
    }

    private void promptImportSource() {
        JFileChooser fc = new JFileChooser(prefs.get("LAST_SOURCE_FOLDER",
                new File(".").getAbsolutePath()));
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int returnVal = fc.showOpenDialog(frame);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            importSource(fc.getSelectedFile());
        }
    }

    private void promptOpenDiagram() {
        JFileChooser fc = new JFileChooser(prefs.get("LAST_GRAPH_FILE",
                new File(".").getAbsolutePath()));
        fc.setDialogTitle("Choose graph file to load");
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        FileNameExtensionFilter filter = new FileNameExtensionFilter("GraphML files", "graphml");
        fc.setFileFilter(filter);
        int returnVal = fc.showOpenDialog(frame);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            openDiagram(fc.getSelectedFile());
        }
    }

    private void showLicense() {
        try {
            JOptionPane.showMessageDialog(null, Config.APPNAME + " " + Resources.getVersion() + " \n"
                            + Resources.getResourceAsString("/help/help.txt"),
                    "About", JOptionPane.INFORMATION_MESSAGE, new ImageIcon(getIconImage()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void showHelp() {
        try {
            JOptionPane.showMessageDialog(null, Resources.getResourceAsString("/help/help.txt"),
                    "Help", JOptionPane.INFORMATION_MESSAGE, new ImageIcon(getIconImage()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void importSource(File dir) {
        executor.submit(() -> {
            setStatus("Importing sources");
            prefs.put("LAST_SOURCE_FOLDER", dir.getPath());
            try {
                diagram.importSourcesDir(dir);
                EventQueue.invokeLater(() -> {
                    graphPanel.display(diagram);
                    graphPanel.revalidate();
                    setStatus("Sources imported", 3000);
                    addClassListener();
                    updateErrors(diagram);
                    UMLClass[] classesArr = diagram.getGraph().vertexSet().toArray(new UMLClass[0]);
                    classesScrollPane.setClasses(classesArr);
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null, "Error during import: " + ex,
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private void openDiagram(File file) {
        executor.submit(() -> {
            setStatus("Loading diagram");
            diagram = createDiagram();
            diagram.setFilePath(file.getPath());
            prefs.put("LAST_GRAPH_FILE", file.getPath());
            HashMap<UMLClass, Point2D.Double> verticesPositions = new HashMap<>();
            classesScrollPane.clear();
            graphPanel.clear();
            new Importer().importGraph(file, diagram, verticesPositions);
            EventQueue.invokeLater(() -> {
                graphPanel.display(diagram, convertPointsToPositions(verticesPositions));
                graphPanel.revalidate();
                setTitle(FileUtils.getFilenameWithoutExtension(file.getName()));
                setStatus("Diagram loaded", 3000);
                addClassListener();
                UMLClass[] classesArr = diagram.getGraph().vertexSet().toArray(new UMLClass[0]);
                classesScrollPane.setClasses(classesArr);
            });
        });
    }

    private void createPanes() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        graphPanel = new GraphPanel();
        graphPanel.setOnGetVertexLabel((object) -> formatter.getUmlAsHtml(object, !object.isCompact(), diagram));

        classesScrollPane = new ClassesPane();
        classesScrollPane.setPreferredSize(new Dimension(100, 600));
        classesScrollPane.setOnClassSelected((classes) -> graphPanel.selectClasses(classes));
        classesScrollPane.setOnMouseRightClick((object, mousePosition) -> {
            ContextMenu contextMenu = new ContextMenu();
            JPopupMenu menu = contextMenu.getContextMenu(object, diagram, graphPanel);
            menu.show(mousePosition.component, mousePosition.x, mousePosition.y);
        });

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, graphPanel, classesScrollPane);
        splitPane.setBorder(new EmptyBorder(8, 8, 8, 8));
        splitPane.setResizeWeight(0.9);
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.gridheight = 1;
        gbc.weightx = 100;
        gbc.weighty = 100;
        gbc.insets = new Insets(2, 2, 2, 2);
        mainPanel.add(splitPane, gbc);

        errors = new JButton(" ");
        errors.setVisible(false);
        errors.addActionListener((e) -> {
            SwingUtilities.invokeLater(() -> {
                JTextArea message = new JTextArea();
                message.setText(msg);
                message.setEditable(false);
                message.setWrapStyleWord(true);
                JScrollPane scrollPane = new JScrollPane(message, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
                scrollPane.setPreferredSize(new Dimension(400, 200));
                JOptionPane.showMessageDialog(frame, scrollPane, "Unresolved symbols", JOptionPane.PLAIN_MESSAGE);
            });
        });
        errors.setOpaque(false);
        errors.setHorizontalAlignment(SwingConstants.LEFT);
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.insets = new Insets(6, 6, 6, 6);
        mainPanel.add(errors, gbc);

        status = new JLabel();
        status.setHorizontalAlignment(SwingConstants.RIGHT);
        status.getPreferredSize().height = 50;
        gbc.gridx = 1;
//        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
//        gbc.weightx = 50;
        gbc.weighty = 0;
        gbc.insets = new Insets(6, 6, 6, 6);
        mainPanel.add(status, gbc);

        frame.setMinimumSize(new Dimension(1200, 800));
        frame.getContentPane().add(mainPanel);

    }

    private Image getIconImage() {
        return Toolkit.getDefaultToolkit().getImage(Main.class.getResource("/icons/logo.png"));
    }

    private void updateErrors(UMLDiagram generator) {
        int errorsCount = generator.getParser().getUnresolvedSymbols().size();
        if (errorsCount > 0) {
            msg = "Unresolved symbols: " + "\n" + Formatter.formatUnresolvedSymbols(generator.getParser());
            errors.setVisible(true);
            errors.setText("<HTML>Unresolved symbols found: <U>" + errorsCount + "</U></HTML>");
        } else {
            errors.setVisible(false);
        }
    }

    private void saveDiagram(String filepath) {
        executor.submit(() -> {
            if (filepath != null) {
                File file = new File(filepath);
                setStatus("Saving diagram");
                new Exporter().exportGraph(file, diagram, convertPositionsToPoints(graphPanel.getVertexPositions()));
                EventQueue.invokeLater((() -> {
                    setTitle(FileUtils.getFilenameWithoutExtension(file.getName()));
                    setStatus("Diagram saved", 3000);
                }));
            } else {
                saveDiagramAs();
            }
        });
    }

    private void setTitle(String name) {
        String title = Config.APPNAME;
        if (name != null)
            title += " - " + name;
        frame.setTitle(title);
    }

    private void setStatus(String status) {
        setStatus(status, 0);
    }

    private void setStatus(String text, int expiry) {
        EventQueue.invokeLater(() -> status.setText(text == null ? "" : text));
        if (expiry > 0) {
            javax.swing.Timer timer = new javax.swing.Timer(expiry, event -> {
                EventQueue.invokeLater(() -> setStatus(null));
            });
            timer.setRepeats(false);
            timer.start();
        }
    }

    private void saveDiagramAs() {
        JFileChooser fc = new JFileChooser(prefs.get("LAST_GRAPH_FILE",
                new File(".").getAbsolutePath()));
        if (diagram.getFilepath() != null)
            fc.setSelectedFile(new File(diagram.getFilepath()));
        else
            fc.setSelectedFile(new File("diagram"));
        fc.setDialogTitle("Choose graph file to save");
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        FileNameExtensionFilter filter = new FileNameExtensionFilter("GraphML files", "graphml");
        fc.setFileFilter(filter);
        int returnVal = fc.showSaveDialog(frame);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            if (!file.getName().endsWith(".graphml"))
                file = new File(file.getParentFile(), file.getName() + ".graphml");
            if (!shouldOverwriteFile(file))
                return;
            prefs.put("LAST_GRAPH_FILE", file.getPath());
            saveDiagram(file.getPath());
            diagram.setFilePath(file.getPath());
        }
    }

    private boolean shouldOverwriteFile(File file) {
        if (file.exists()) {
            int response = JOptionPane.showConfirmDialog(null, "File exists, overwrite?", "Confirm",
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (response == JOptionPane.NO_OPTION)
                return false;
        }
        return true;
    }

    private void addClassListener() {
        VisualizationViewer<UMLClass, UMLRelationship> viewer = graphPanel.getViewer();
        viewer.getSelectedVertexState().addItemListener((l) -> {
            classesScrollPane.clearSelection();
            classesScrollPane.selectClasses(viewer.getSelectedVertices());
        });
    }


    private Map<UMLClass, Point2D.Double> convertPositionsToPoints(Map<UMLClass,
            org.jungrapht.visualization.layout.model.Point> vertexPositions) {
        HashMap<UMLClass, Point2D.Double> points = new HashMap<>();
        for (UMLClass object : vertexPositions.keySet()) {
            org.jungrapht.visualization.layout.model.Point position = vertexPositions.get(object);
            points.put(object, new Point2D.Double(position.x, position.y));
        }
        return points;
    }

    private Map<UMLClass, org.jungrapht.visualization.layout.model.Point>
    convertPointsToPositions(HashMap<UMLClass, Point2D.Double> verticesPoints) {
        HashMap<UMLClass, org.jungrapht.visualization.layout.model.Point> positions = new HashMap<>();
        for (UMLClass object : verticesPoints.keySet()) {
            Point2D.Double position = verticesPoints.get(object);
            positions.put(object, org.jungrapht.visualization.layout.model.Point.of(position.x, position.y));
        }
        return positions;
    }
}
