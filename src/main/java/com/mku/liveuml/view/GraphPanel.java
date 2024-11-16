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

import com.mku.liveuml.entities.AccessModifier;
import com.mku.liveuml.entities.Constructor;
import com.mku.liveuml.entities.Field;
import com.mku.liveuml.entities.Method;
import com.mku.liveuml.format.Formatter;
import com.mku.liveuml.gen.UMLGenerator;
import com.mku.liveuml.graph.UMLClass;
import com.mku.liveuml.graph.UMLRelationship;
import com.mku.liveuml.graph.UMLRelationshipType;
import com.mku.liveuml.utils.FileUtils;
import org.jgrapht.Graph;
import org.jungrapht.visualization.VisualizationModel;
import org.jungrapht.visualization.VisualizationScrollPane;
import org.jungrapht.visualization.VisualizationViewer;
import org.jungrapht.visualization.control.GraphMouseListener;
import org.jungrapht.visualization.layout.algorithms.FRLayoutAlgorithm;
import org.jungrapht.visualization.layout.algorithms.LayoutAlgorithm;
import org.jungrapht.visualization.renderers.BiModalRenderer;
import org.jungrapht.visualization.renderers.GradientVertexRenderer;
import org.jungrapht.visualization.renderers.VertexLabelAsShapeRenderer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;

@SuppressWarnings("unchecked")
public class GraphPanel extends JPanel {
    private final UMLGenerator generator;
    private VisualizationViewer<UMLClass, UMLRelationship> vv;
    private final LayoutAlgorithm<UMLClass> layoutAlgorithm;
    private VisualizationScrollPane visualizationScrollPane;
    private final HashSet<UMLClass> selectedVertices = new HashSet<>();
    private final HashSet<UMLRelationship> selectedEdges = new HashSet<>();
    private final HashSet<Method> selectedMethods = new HashSet<>();
    private final HashSet<Field> selectedFields = new HashSet<>();
    private BufferedImage img;

    /**
     * create an instance of a simple graph with basic controls
     */
    public GraphPanel(UMLGenerator generator) {
        setLayout(new BorderLayout());
        layoutAlgorithm = new FRLayoutAlgorithm<>();
        this.generator = generator;
    }

    public void toggleCompact(UMLClass obj) {
        obj.setCompact(!obj.isCompact());
    }

    @SuppressWarnings("unchecked")
    public void display(Map<UMLClass, Point2D.Double> positions) {
        Dimension preferredSize = estimateGraphSize(generator.getGraph());
        final VisualizationModel<UMLClass, UMLRelationship> visualizationModel =
                VisualizationModel.builder(generator.getGraph())
                        .layoutAlgorithm(layoutAlgorithm)
                        .layoutSize(preferredSize)
                        .build();

        if (positions != null)
            visualizationModel.getLayoutModel().setInitializer(obj -> {
                if (positions.containsKey(obj)) {
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
        VertexLabelAsShapeRenderer<UMLClass, UMLRelationship> vlasr =
                new VertexLabelAsShapeRenderer<>(visualizationModel.getLayoutModel(), vv.getRenderContext());

        vv.getRenderContext().setVertexLabelFunction(object -> Formatter.display(object, !object.isCompact(),
                selectedVertices, selectedEdges, selectedMethods, selectedFields));
        vv.getRenderContext().setVertexShapeFunction(vlasr);
        vv.getRenderContext().setEdgeStrokeFunction(rel -> {
            if (rel.type == UMLRelationshipType.Dependency || rel.type == UMLRelationshipType.Realization) {
                // dashed line
                return new BasicStroke(selectedEdges.contains(rel) ? 6 : 3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0);
            }
            return new BasicStroke(selectedEdges.contains(rel) ? 4 : 2);
        });
        vv.getRenderContext().setEdgeDrawPaintFunction(relationship -> {
            if (selectedEdges.contains(relationship))
                return Color.BLUE;
            return Color.BLACK;
        });

        vv.getRenderer().setVertexRenderer(BiModalRenderer.HEAVYWEIGHT, new GradientVertexRenderer<>(Color.white, Color.white, true));
        vv.getRenderer().setVertexLabelRenderer(BiModalRenderer.HEAVYWEIGHT, vlasr);
        vv.setBackground(Color.white);
        vv.setVertexToolTipFunction(UMLClass::getName);

        // FIXME: workaround: setting custom arrows does not work since the setupArrows()
        // always overrides, so we inject our shape here:
        vv.getRenderContext().setEdgeArrowStrokeFunction((rel) -> {
            Shape shape = getArrowShape(rel);
            vv.getRenderContext().setEdgeArrow(shape);
            vv.getRenderContext().setRenderEdgeArrow(true);
            return new BasicStroke(2f);
        });
        vv.getRenderContext().setArrowFillPaintFunction((rel) -> {
            if (rel.type == UMLRelationshipType.Composition) {
                if (selectedEdges.contains(rel))
                    return Color.BLUE;
                return Color.BLACK;
            }
            return Color.WHITE;
        });

        vv.getRenderContext().setArrowDrawPaintFunction((rel) -> {
            if (rel.type == UMLRelationshipType.Composition) {
                if (selectedEdges.contains(rel))
                    return Color.BLUE;
            }
            return Color.BLACK;
        });

        addVisualizationPane(vv);
        setMouseListener(vv);
    }

    private Dimension estimateGraphSize(Graph<UMLClass, UMLRelationship> graph) {
        int vertices = graph.vertexSet().size();
        int width = (int) Math.sqrt(vertices) * 500;
        if (width == 0)
            width = 1200;
        return new Dimension(width, width * 2 / 3);
    }

    private void addVisualizationPane(VisualizationViewer<UMLClass, UMLRelationship> vv) {
        if (visualizationScrollPane != null)
            remove(visualizationScrollPane);
        visualizationScrollPane = new VisualizationScrollPane(vv);
        add(visualizationScrollPane);
    }

    private void setMouseListener(VisualizationViewer<UMLClass, UMLRelationship> vv) {

        // we use the latest fix in UMLMouseListenerTranslator until we upgard to jungrapht 1.5
        ((Component) vv).addMouseListener(new UMLMouseListenerTranslator<>(new GraphMouseListener<>() {
            @Override
            public void graphClicked(UMLClass s, MouseEvent me) {
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
            public void graphPressed(UMLClass s, MouseEvent me) {
            }

            @Override
            public void graphReleased(UMLClass s, MouseEvent me) {

            }
        }, vv));
    }

    private void showContextMenu(UMLClass s, MouseEvent me) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem item = new JMenuItem(s.isCompact() ? "Collapse" : "Expand");
        item.addActionListener(e -> EventQueue.invokeLater(() -> {
            toggleCompact(s);
            vv.repaint();
        }));
        menu.add(item);

        // find references
        JMenu refMenu = new JMenu("Find references");
        menu.add(refMenu);

        JMenuItem cItem = new JMenuItem("Class " + s.getName());
        cItem.addActionListener(e -> {
            clearSelections();
            List<HashSet<?>> refs = generator.findClassReference(s);
            selectRefs(refs);
            vv.repaint();
        });
        refMenu.add(cItem);

        List<JMenuItem> items = new ArrayList<>();
        if (s.getFields().size() > 0) {
            for (Field f : s.getFields()) {
                if (f.getAccessModifiers().contains(AccessModifier.Private))
                    continue;
                JMenuItem fItem = new JMenuItem(com.mku.liveuml.format.Formatter.getFieldFormatted(f));
                fItem.addActionListener(e -> {
                    clearSelections();
                    List<HashSet<?>> refs = generator.findFieldReference(s, f);
                    selectRefs(refs);
                    vv.repaint();
                });
                items.add(fItem);
            }
        }
        if (items.size() > 0) {
            refMenu.addSeparator();
            JLabel tItem = new JLabel("Fields");
            tItem.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            refMenu.add(tItem);
            for (JMenuItem fItem : items)
                refMenu.add(fItem);
        }

        items.clear();
        if (s.getMethods().size() > 0) {
            for (Method m : s.getMethods()) {
                if (!(m instanceof Constructor))
                    continue;
                if (m.getAccessModifiers().contains(AccessModifier.Private))
                    continue;
                String methodName = com.mku.liveuml.format.Formatter.getMethodSignature(m, true);
                JMenuItem mItem = new JMenuItem(methodName);
                mItem.addActionListener(e -> {
                    clearSelections();
                    List<HashSet<?>> refs = generator.findMethodReference(s, m);
                    selectRefs(refs);
                    vv.repaint();
                });
                items.add(mItem);
            }
        }
        if (items.size() > 0) {
            refMenu.addSeparator();
            JLabel tItem = new JLabel("Constructors");
            tItem.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            refMenu.add(tItem);
            for (JMenuItem fItem : items)
                refMenu.add(fItem);
        }

        items.clear();
        if (s.getMethods().size() > 0) {
            for (Method m : s.getMethods()) {
                if (m instanceof Constructor)
                    continue;
                if (m.getAccessModifiers().contains(AccessModifier.Private))
                    continue;
                String methodName = com.mku.liveuml.format.Formatter.getMethodSignature(m, true);
                JMenuItem mItem = new JMenuItem(methodName);
                mItem.addActionListener(e -> {
                    clearSelections();
                    List<HashSet<?>> refs = generator.findMethodReference(s, m);
                    selectRefs(refs);
                    vv.repaint();
                });
                items.add(mItem);
            }
        }
        if (items.size() > 0) {
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

        cItem = new JMenuItem("Class " + s.getName());
        cItem.addActionListener(e -> {
            goToClassReference(s);
            vv.repaint();
        });
        goToMenu.add(cItem);

        items.clear();
        if (s.getFields().size() > 0) {
            for (Field f : s.getFields()) {
                JMenuItem fItem = new JMenuItem(com.mku.liveuml.format.Formatter.getFieldFormatted(f));
                fItem.addActionListener(e -> {
                    goToFieldReference(s, f);
                    vv.repaint();
                });
                items.add(fItem);
            }
        }
        if (items.size() > 0) {
            goToMenu.addSeparator();
            JLabel tItem = new JLabel("Fields");
            tItem.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            goToMenu.add(tItem);
            for (JMenuItem fItem : items)
                goToMenu.add(fItem);
        }

        items.clear();
        if (s.getMethods().size() > 0) {
            for (Method m : s.getMethods()) {
                if (!(m instanceof Constructor))
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
        if (items.size() > 0) {
            goToMenu.addSeparator();
            JLabel tItem = new JLabel("Constructors");
            tItem.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            goToMenu.add(tItem);
            for (JMenuItem fItem : items)
                goToMenu.add(fItem);
        }

        items.clear();
        if (s.getMethods().size() > 0) {
            for (Method m : s.getMethods()) {
                if (m instanceof Constructor)
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
        if (items.size() > 0) {
            goToMenu.addSeparator();
            JLabel tItem = new JLabel("Methods");
            tItem.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            goToMenu.add(tItem);
            for (JMenuItem fItem : items)
                goToMenu.add(fItem);
        }

        EventQueue.invokeLater(() -> menu.show(me.getComponent(), me.getX(), me.getY()));
    }

    private void selectRefs(List<HashSet<?>> refs) {
        for (HashSet<?> href : refs) {
            for (Object obj : href) {
                if (obj instanceof UMLClass)
                    selectedVertices.add((UMLClass) obj);
                if (obj instanceof UMLRelationship)
                    selectedEdges.add((UMLRelationship) obj);
                if (obj instanceof Field)
                    selectedFields.add((Field) obj);
                if (obj instanceof Method)
                    selectedMethods.add((Method) obj);
            }
        }
    }

    private void goToClassReference(UMLClass s) {
        FileUtils.openFileLine(s.getFilePath(), s.getLine());
    }

    private void goToMethodReference(UMLClass s, Method m) {
        FileUtils.openFileLine(s.getFilePath(), m.getLine());
    }

    private void goToFieldReference(UMLClass s, Field f) {
        FileUtils.openFileLine(s.getFilePath(), f.getLine());
    }

    private void clearSelections() {
        vv.getSelectedVertexState().clear();
        vv.getSelectedEdgeState().clear();
        selectedFields.clear();
        selectedMethods.clear();
        selectedEdges.clear();
        selectedVertices.clear();
    }

    private Shape getArrowShape(UMLRelationship rel) {
        if (rel.type == UMLRelationshipType.Aggregation || rel.type == UMLRelationshipType.Composition)
            return new Diamond(40, 20);
        else if (rel.type == UMLRelationshipType.Inheritance || rel.type == UMLRelationshipType.Realization)
            return new ClosedArrow(40, 20);
        else if (rel.type == UMLRelationshipType.Association || rel.type == UMLRelationshipType.Dependency)
            return new OpenArrow(40, 20);
        return null;
    }

    public Map<UMLClass, org.jungrapht.visualization.layout.model.Point> getVertexPositions() {
        return vv.getVisualizationModel().getLayoutModel().getLocations();
    }

    static class Diamond extends Path2D.Double {
        public Diamond(double width, double height) {
            moveTo(0, 0);
            lineTo(-width / 2, -height / 2);
            lineTo(-width, 0);
            lineTo(-width / 2, height / 2);
            closePath();
        }
    }

    static class OpenArrow extends Path2D.Double {
        public OpenArrow(double width, double height) {
            moveTo(0, 0);
            lineTo(-width / 2, -height / 2);
            lineTo(0, 0);
            lineTo(-width / 2, height / 2);
            closePath();
        }
    }

    static class ClosedArrow extends Path2D.Double {
        public ClosedArrow(double width, double height) {
            moveTo(0, 0);
            lineTo(-width / 2, -height / 2);
            lineTo(-width / 2, height / 2);
            closePath();
        }
    }

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
        if (img != null)
            g.drawImage(img, 0, 0, this);
    }

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
