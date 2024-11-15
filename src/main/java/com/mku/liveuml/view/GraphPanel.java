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
package com.mku.liveuml.view;

import com.mku.liveuml.Main;
import com.mku.liveuml.format.Formatter;
import com.mku.liveuml.meta.Constructor;
import com.mku.liveuml.meta.Field;
import com.mku.liveuml.meta.Method;
import com.mku.liveuml.graph.Relationship;
import com.mku.liveuml.graph.UMLObject;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultGraphType;
import org.jgrapht.graph.builder.GraphTypeBuilder;
import org.jgrapht.util.SupplierUtil;
import org.jungrapht.visualization.VisualizationModel;
import org.jungrapht.visualization.VisualizationScrollPane;
import org.jungrapht.visualization.VisualizationViewer;
import org.jungrapht.visualization.control.GraphMouseListener;
import org.jungrapht.visualization.layout.algorithms.*;
import org.jungrapht.visualization.renderers.BiModalRenderer;
import org.jungrapht.visualization.renderers.GradientVertexRenderer;
import org.jungrapht.visualization.renderers.VertexLabelAsShapeRenderer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.prefs.Preferences;

public class GraphPanel extends JPanel {

    public Graph<UMLObject, Relationship> graph;

    VisualizationViewer<UMLObject, Relationship> vv;

    LayoutAlgorithm<UMLObject> layoutAlgorithm;
    private Dimension preferredSize;
    private VisualizationScrollPane visualizationScrollPane;
    public boolean allowLoops = false;

    HashSet<UMLObject> selectedVertices = new HashSet<>();
    HashSet<Relationship> selectedEdges = new HashSet<>();
    HashSet<Method> selectedMethods = new HashSet<>();
    HashSet<Field> selectedFields = new HashSet<>();

    private boolean isCompactDisplay = true;

    /**
     * create an instance of a simple graph with basic controls
     */
    public GraphPanel() {
        setLayout(new BorderLayout());
        layoutAlgorithm = new FRLayoutAlgorithm<>();
    }

    public void toggleCompact(UMLObject obj) {
        obj.compact = !obj.compact;
    }

    public void createAndDisplay(List<UMLObject> classes) {
        graph = getGraph(classes);
        display(graph, null);
    }

    public Graph<UMLObject, Relationship> createGraph() {
        graph = GraphTypeBuilder.<UMLObject, Relationship>forGraphType(DefaultGraphType.directedMultigraph())
                .allowingSelfLoops(allowLoops)
                .edgeSupplier(SupplierUtil.createSupplier(Relationship.class))
                .buildGraph();
        return graph;
    }

    public void display(Graph<UMLObject, Relationship> graph, Map<UMLObject, Point2D.Double> positions) {
        preferredSize = estimateGraphSize(graph);
        this.graph = graph;
        final VisualizationModel<UMLObject, Relationship> visualizationModel =
                VisualizationModel.builder(graph)
                        .layoutAlgorithm(layoutAlgorithm)
                        .layoutSize(preferredSize)
                        .build();

        if(positions != null)
            visualizationModel.getLayoutModel().setInitializer(obj -> {
                if(positions.containsKey(obj)) {
                    Point2D.Double point = positions.get(obj);
                    return org.jungrapht.visualization.layout.model.Point.of(point.x, point.y);
                }
                return null;
            });
        visualizationModel.getLayoutModel().setRelaxing(true);
        vv = VisualizationViewer.builder(visualizationModel)
                .viewSize(preferredSize)
                .build();

        Map<?, ?> desktopHints = (Map<?, ?>) Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints");
        if (desktopHints != null) {
            HashMap<RenderingHints.Key, Object> renderingHints = new HashMap<>();
            for (Map.Entry<?, ?> kv : desktopHints.entrySet()) {
                renderingHints.put((RenderingHints.Key) kv.getKey(), kv.getValue());
            }
            vv.setRenderingHints(renderingHints);
        }

        // this class will provide both label drawing and vertex shapes
        VertexLabelAsShapeRenderer<UMLObject, Relationship> vlasr =
                new VertexLabelAsShapeRenderer<>(visualizationModel.getLayoutModel(), vv.getRenderContext());

        vv.getRenderContext().setVertexLabelFunction(object -> {
            return com.mku.liveuml.format.Formatter.display(object, isCompactDisplay && !object.compact,
                    selectedVertices, selectedEdges, selectedMethods, selectedFields);
        });
        vv.getRenderContext().setVertexShapeFunction(vlasr);
        vv.getRenderContext().setEdgeStrokeFunction(rel -> {
            if (rel.type == Relationship.Type.Dependency || rel.type == Relationship.Type.Realization) {
                // dashed line
                return new BasicStroke(selectedEdges.contains(rel)?6:3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0);
            }
            return new BasicStroke(selectedEdges.contains(rel)?4:2);
        });
        vv.getRenderContext().setEdgeDrawPaintFunction(relationship -> {
            if (selectedEdges.contains(relationship))
                return Color.BLUE;
            return Color.BLACK;
        });

        vv.getRenderer().setVertexRenderer(BiModalRenderer.HEAVYWEIGHT, new GradientVertexRenderer<>(Color.white, Color.white, true));
        vv.getRenderer().setVertexLabelRenderer(BiModalRenderer.HEAVYWEIGHT, vlasr);
        vv.setBackground(Color.white);
        vv.setVertexToolTipFunction(n -> n.name);

        // FIXME: workaround: setting custom arrows does not work since the setupArrows()
        // always overrides, so we inject our shape here:
        vv.getRenderContext().setEdgeArrowStrokeFunction((rel) -> {
            Shape shape = getArrowShape(rel);
            vv.getRenderContext().setEdgeArrow(shape);
            vv.getRenderContext().setRenderEdgeArrow(true);
            return new BasicStroke(2f);
        });
        vv.getRenderContext().setArrowFillPaintFunction((rel) -> {
            if (rel.type == Relationship.Type.Composition) {
                if (selectedEdges.contains(rel))
                    return Color.BLUE;
                return Color.BLACK;
            }
            return Color.WHITE;
        });

        vv.getRenderContext().setArrowDrawPaintFunction((rel) -> {
            if (rel.type == Relationship.Type.Composition) {
                if (selectedEdges.contains(rel))
                    return Color.BLUE;
            }
            return Color.BLACK;
        });

        addVisualizationPane(vv);
        setMouseListener(vv);
    }

    private Dimension estimateGraphSize(Graph<UMLObject, Relationship> graph) {
        int vertices = graph.vertexSet().size();
        int width = (int) Math.sqrt(vertices) * 500;
        if(width == 0)
            width = 1200;
        return new Dimension(width, width * 2/3);
    }

    private void addVisualizationPane(VisualizationViewer<UMLObject, Relationship> vv) {
        if (visualizationScrollPane != null)
            remove(visualizationScrollPane);
        visualizationScrollPane = new VisualizationScrollPane(vv);
        add(visualizationScrollPane);
    }

    private void setMouseListener(VisualizationViewer<UMLObject, Relationship> vv) {

        // we use the latest fix in UMLMouseListenerTranslator until we upgard to jungrapht 1.5
        ((Component) vv).addMouseListener(new UMLMouseListenerTranslator<>(new GraphMouseListener<>() {
            @Override
            public void graphClicked(UMLObject s, MouseEvent me) {
                vv.getVisualizationModel().getLayoutModel().get(s);
                if (me.getButton() == MouseEvent.BUTTON3) {
                    // show context menu
                    showContextMenu(s, me);
                    me.consume();
                } else if (me.getButton() == MouseEvent.BUTTON1 && me.getClickCount() == 2) {
                    toggleCompact(s);
                    me.consume();
                }
            }

            @Override
            public void graphPressed(UMLObject s, MouseEvent me) {
            }

            @Override
            public void graphReleased(UMLObject s, MouseEvent me) {

            }
        }, vv));
    }

    private void showContextMenu(UMLObject s, MouseEvent me) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem item = new JMenuItem(s.compact?"Collapse":"Expand");
        item.addActionListener(e -> EventQueue.invokeLater(() -> {
            toggleCompact(s);
            vv.repaint();
        }));
        menu.add(item);

        // find references
        JMenu refMenu = new JMenu("Find references");
        menu.add(refMenu);

        JMenuItem cItem = new JMenuItem("Class " + s.name);
        cItem.addActionListener(e -> {
            findClassReference(s);
            vv.repaint();
        });
        refMenu.add(cItem);

        List<JMenuItem> items = new ArrayList<>();
        if(s.fields.size() > 0) {
            for (Field f : s.fields) {
                if (f.accessModifiers.contains(Field.AccessModifier.Private))
                    continue;
                JMenuItem fItem = new JMenuItem(com.mku.liveuml.format.Formatter.getFieldFormatted(f));
                fItem.addActionListener(e -> {
                    findFieldReference(s, f);
                    vv.repaint();
                });
                items.add(fItem);
            }
        }
        if(items.size() > 0) {
            refMenu.addSeparator();
            JLabel tItem = new JLabel("Fields");
            tItem.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            refMenu.add(tItem);
            for (JMenuItem fItem : items)
                refMenu.add(fItem);
        }

        items.clear();
        if(s.methods.size() >0) {
            for (Method m : s.methods) {
                if(!(m instanceof Constructor))
                    continue;
                if (m.accessModifiers.contains(Method.AccessModifier.Private))
                    continue;
                String methodName = com.mku.liveuml.format.Formatter.getMethodSignature(m, true);
                JMenuItem mItem = new JMenuItem(methodName);
                mItem.addActionListener(e -> {
                    findMethodReference(s, m);
                    vv.repaint();
                });
                items.add(mItem);
            }
        }
        if(items.size() > 0) {
            refMenu.addSeparator();
            JLabel tItem = new JLabel("Constructors");
            tItem.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            refMenu.add(tItem);
            for (JMenuItem fItem : items)
                refMenu.add(fItem);
        }

        items.clear();
        if(s.methods.size() >0) {
            for (Method m : s.methods) {
                if(m instanceof Constructor)
                    continue;
                if (m.accessModifiers.contains(Method.AccessModifier.Private))
                    continue;
                String methodName = com.mku.liveuml.format.Formatter.getMethodSignature(m, true);
                JMenuItem mItem = new JMenuItem(methodName);
                mItem.addActionListener(e -> {
                    findMethodReference(s, m);
                    vv.repaint();
                });
                items.add(mItem);
            }
        }
        if(items.size() > 0) {
            refMenu.addSeparator();
            JLabel tItem = new JLabel("Methods");
            tItem.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            refMenu.add(tItem);
            for (JMenuItem fItem : items)
                refMenu.add(fItem);
        }

        // go to
        JMenu goToMenu = new JMenu("View in Text Editor");
        menu.add(goToMenu);

        cItem = new JMenuItem("Class " + s.name);
        cItem.addActionListener(e -> {
            goToClassReference(s);
            vv.repaint();
        });
        goToMenu.add(cItem);

        items.clear();
        if(s.fields.size() > 0) {
            for (Field f : s.fields) {
                JMenuItem fItem = new JMenuItem(com.mku.liveuml.format.Formatter.getFieldFormatted(f));
                fItem.addActionListener(e -> {
                    goToFieldReference(s, f);
                    vv.repaint();
                });
                items.add(fItem);
            }
        }
        if(items.size() > 0) {
            goToMenu.addSeparator();
            JLabel tItem = new JLabel("Fields");
            tItem.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            goToMenu.add(tItem);
            for (JMenuItem fItem : items)
                goToMenu.add(fItem);
        }

        items.clear();
        if(s.methods.size() >0) {
            for (Method m : s.methods) {
                if(!(m instanceof Constructor))
                    continue;
                String methodName = com.mku.liveuml.format.Formatter.getMethodSignature(m, true);
                JMenuItem mItem = new JMenuItem(methodName);
                mItem.addActionListener(e -> {
                    goToMethodReference(s, m);
                    vv.repaint();
                });
                items.add(mItem);
            }
        }
        if(items.size() > 0) {
            goToMenu.addSeparator();
            JLabel tItem = new JLabel("Constructors");
            tItem.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            goToMenu.add(tItem);
            for (JMenuItem fItem : items)
                goToMenu.add(fItem);
        }

        items.clear();
        if(s.methods.size() >0) {
            for (Method m : s.methods) {
                if(m instanceof Constructor)
                    continue;
                String methodName = Formatter.getMethodSignature(m, true);
                JMenuItem mItem = new JMenuItem(methodName);
                mItem.addActionListener(e -> {
                    goToMethodReference(s, m);
                    vv.repaint();
                });
                items.add(mItem);
            }
        }
        if(items.size() > 0) {
            goToMenu.addSeparator();
            JLabel tItem = new JLabel("Methods");
            tItem.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            goToMenu.add(tItem);
            for (JMenuItem fItem : items)
                goToMenu.add(fItem);
        }

        EventQueue.invokeLater(() -> {
            menu.show(me.getComponent(), me.getX(), me.getY());
        });
    }

    private void goToClassReference(UMLObject s) {
        openFileLine(s.filePath, s.line);
    }

    private void goToMethodReference(UMLObject s, Method m) {
        openFileLine(s.filePath, m.line);
    }

    private void goToFieldReference(UMLObject s, Field f) {
        openFileLine(s.filePath, f.line);
    }

    private void openFileLine(String filePath, int line) {
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

    private void findMethodReference(UMLObject s, Method m) {
        clearSelections();
        for(Relationship rel : s.relationships.values()) {
            if(rel.type == Relationship.Type.Dependency) {
                if(rel.calledBy.containsKey(m)) {
                    Method callerMethod = rel.calledBy.get(m);
                    selectedMethods.add(callerMethod);
                    selectedMethods.add(m);
                    selectedVertices.add(rel.from);
                    selectedVertices.add(rel.to);
                    selectedEdges.add(rel);
                }
            }
        }
        vv.repaint();
    }

    private void clearSelections() {
        vv.getSelectedVertexState().clear();
        vv.getSelectedEdgeState().clear();
        selectedFields.clear();
        selectedMethods.clear();
        selectedEdges.clear();
        selectedVertices.clear();
    }

    private void findFieldReference(UMLObject s, Field f) {
        clearSelections();
        for(Relationship rel : s.relationships.values()) {
            if(rel.type == Relationship.Type.Dependency) {
                if(rel.accessedBy.containsKey(f)) {
                    Method accessorMethod = rel.accessedBy.get(f);
                    selectedMethods.add(accessorMethod);
                    selectedVertices.add(rel.from);
                    selectedVertices.add(rel.to);
                    selectedEdges.add(rel);
                }
            }
        }
        vv.repaint();
    }

    private void findClassReference(UMLObject s) {
        clearSelections();
        for(Relationship rel : s.relationships.values()) {
            if(rel.type == Relationship.Type.Dependency) {
                if(s == rel.to) {
                    for(Method accessorMethod : rel.classAccessors) {
                        selectedMethods.add(accessorMethod);
                    }
                    for(Method method : rel.callTo.keySet()) {
                        selectedMethods.add(method);
                        selectedMethods.add(rel.callTo.get(method));
                    }
                    selectedVertices.add(rel.from);
                    selectedVertices.add(rel.to);
                    selectedEdges.add(rel);
                }
            } else if(rel.type == Relationship.Type.Aggregation
                    || rel.type == Relationship.Type.Composition
                    || rel.type == Relationship.Type.Association
            ) {
                if(s == rel.to) {
                    for (Field field : rel.fieldAssociation) {
                        selectedFields.add(field);
                    }
                    selectedVertices.add(rel.from);
                    selectedVertices.add(rel.to);
                    selectedEdges.add(rel);
                }
            } else if(rel.type == Relationship.Type.Realization
            || rel.type == Relationship.Type.Inheritance) {
                if(s == rel.to) {
                    selectedVertices.add(rel.from);
                    selectedVertices.add(rel.to);
                    selectedEdges.add(rel);
                }
            }
        }
        vv.repaint();
    }

    private Shape getArrowShape(Relationship rel) {
        if (rel.type == Relationship.Type.Aggregation || rel.type == Relationship.Type.Composition)
            return new Diamond(40, 20);
        else if (rel.type == Relationship.Type.Inheritance || rel.type == Relationship.Type.Realization)
            return new ClosedArrow(40, 20);
        else if (rel.type == Relationship.Type.Association || rel.type == Relationship.Type.Dependency)
            return new OpenArrow(40, 20);
        return null;
    }

    private Graph<UMLObject, Relationship> getGraph(List<UMLObject> umlObjects) {
        Graph<UMLObject, Relationship> graph = createGraph();

        for (UMLObject obj : umlObjects) {
            graph.addVertex(obj);
        }

        updateRelationships(umlObjects, graph);
        return graph;
    }

    public void updateVertices(Graph<UMLObject, Relationship> graph, HashMap<String, UMLObject> vertices) {
        for (Relationship rel: graph.edgeSet()) {
            UMLObject from = vertices.getOrDefault(rel.from.toString(), null);
            UMLObject to = vertices.getOrDefault(rel.to.toString(), null);
            from.relationships.put(rel.toString(), rel);
            to.relationships.put(rel.toString(), rel);
        }
    }

    public void updateRelationships(List<UMLObject> umlObjects, Graph<UMLObject, Relationship> graph) {
        for (UMLObject obj : umlObjects) {
            boolean hasValidRelationships = false;
            for (Map.Entry<String, Relationship> rel : obj.relationships.entrySet()) {
                if (!allowLoops && rel.getValue().from == rel.getValue().to)
                    continue;
                if (!graph.containsVertex(rel.getValue().from) || !graph.containsVertex(rel.getValue().to)) {
                    continue;
                }
                try {
                    graph.addEdge(rel.getValue().from, rel.getValue().to, rel.getValue());
                    hasValidRelationships = true;
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            if (!hasValidRelationships)
                graph.removeVertex(obj);
        }
    }

    public void clear() {
        repaint();
    }

    public Map<UMLObject, org.jungrapht.visualization.layout.model.Point> getVertexPositions() {
        return vv.getVisualizationModel().getLayoutModel().getLocations();
    }

    class Diamond extends Path2D.Double {
        public Diamond(double width, double height) {
            moveTo(0, 0);
            lineTo(-width / 2, -height / 2);
            lineTo(-width, 0);
            lineTo(-width / 2, height / 2);
            closePath();
        }
    }

    class OpenArrow extends Path2D.Double {
        public OpenArrow(double width, double height) {
            moveTo(0, 0);
            lineTo(-width / 2, -height / 2);
            lineTo(0, 0);
            lineTo(-width / 2, height / 2);
            closePath();
        }
    }

    class ClosedArrow extends Path2D.Double {
        public ClosedArrow(double width, double height) {
            moveTo(0, 0);
            lineTo(-width / 2, -height / 2);
            lineTo(-width / 2, height / 2);
            closePath();
        }
    }

    BufferedImage img;

    public BufferedImage getImage() {
        Dimension size = getSize();
        setSize(getPreferredSize());
        layoutComponent(this);
        img = new BufferedImage(this.getWidth(), this.getHeight(), BufferedImage.TRANSLUCENT);
        Graphics2D graphics = img.createGraphics();
        print(graphics);
        graphics.dispose();
        setSize(size);
        setPreferredSize(size);
        return img;
    }

    @Override
    protected void paintComponent(java.awt.Graphics g) {
        super.paintComponent(g);
        if(img !=null)
            g.drawImage(img, 0, 0, this);
    };

    private static void layoutComponent(Component c) {
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
