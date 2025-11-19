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

import com.mku.liveuml.format.Formatter;
import com.mku.liveuml.model.diagram.UMLClass;
import com.mku.liveuml.model.diagram.UMLDiagram;
import com.mku.liveuml.model.diagram.UMLRelationship;
import com.mku.liveuml.model.diagram.UMLRelationshipType;
import com.mku.liveuml.model.entities.EnumConstant;
import com.mku.liveuml.model.entities.Field;
import com.mku.liveuml.model.entities.Method;
import org.jgrapht.Graph;
import org.jungrapht.visualization.*;
import org.jungrapht.visualization.control.GraphMouseListener;
import org.jungrapht.visualization.layout.algorithms.FRLayoutAlgorithm;
import org.jungrapht.visualization.layout.algorithms.LayoutAlgorithm;
import org.jungrapht.visualization.layout.model.LayoutModel;
import org.jungrapht.visualization.renderers.BiModalRenderer;
import org.jungrapht.visualization.renderers.GradientVertexRenderer;
import org.jungrapht.visualization.renderers.VertexLabelAsShapeRenderer;
import org.jungrapht.visualization.transform.shape.GraphicsDecorator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

@SuppressWarnings("unchecked")
public class GraphPanel extends JPanel {
    private VisualizationViewer<UMLClass, UMLRelationship> viewer;
    private final LayoutAlgorithm<UMLClass> layoutAlgorithm;
    private VisualizationScrollPane visualizationScrollPane;
    private final HashMap<UMLClass, Shape> verticesBounds = new HashMap<>();
    private boolean collapsedAll = true;
    private Consumer<Graphics> onImagePainted;
    private UMLDiagram diagram;
    private Function<UMLClass, String> onGetVertexLabel;
    private VisualizationModel<UMLClass, UMLRelationship> visualizationModel;

    /**
     * create an instance of a simple graph with basic controls
     */
    public GraphPanel() {
        setLayout(new BorderLayout());
        layoutAlgorithm = new FRLayoutAlgorithm<>();
    }

    public void toggleCompact(UMLClass obj) {
        obj.setCompact(!obj.isCompact());
    }


    private RoundRectangle2D.Double getObjectBounds(Dimension size) {
        int margin = 4;
        return new RoundRectangle2D.Double(-size.width / 2 - margin, -size.height / 2 - margin,
                size.width + 2 * margin, size.height + 2 * margin, size.width / 16, size.width / 16);
    }

    public void display(UMLDiagram diagram) {
        display(diagram, null);
    }

    @SuppressWarnings("unchecked")
    public void display(UMLDiagram diagram, Map<UMLClass, org.jungrapht.visualization.layout.model.Point> positions) {
        this.diagram = diagram;
        Dimension preferredSize = estimateGraphSize(diagram.getGraph());
        visualizationModel = VisualizationModel.builder(diagram.getGraph())
                .layoutAlgorithm(layoutAlgorithm)
                .layoutSize(preferredSize)
                .build();
        if (positions != null) {
            Map<String, org.jungrapht.visualization.layout.model.Point> currentPositions
                    = convertClassPositionsToFullNamePositions(positions);
            visualizationModel.getLayoutModel().setInitializer(obj -> {
                if (currentPositions.containsKey(obj.toString())) {
                    org.jungrapht.visualization.layout.model.Point point = currentPositions.get(obj.toString());
                    return org.jungrapht.visualization.layout.model.Point.of(point.x, point.y);
                }
                return org.jungrapht.visualization.layout.model.Point.of(0, 0);
            });
        }
        visualizationModel.getLayoutModel().setRelaxing(true);
        viewer = VisualizationViewer.builder(visualizationModel)
                .viewSize(preferredSize)
                .build();
        Map<?, ?> desktopHints = (Map<?, ?>) Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints");
        if (desktopHints != null) {
            HashMap<RenderingHints.Key, Object> renderingHints = new HashMap<>();
            for (Map.Entry<?, ?> kv : desktopHints.entrySet()) {
                renderingHints.put((RenderingHints.Key) kv.getKey(), kv.getValue());
            }
            viewer.setRenderingHints(renderingHints);
        }

        viewer.setBackground(Color.WHITE);

        setupVertices();
        setupEdges();
        setupConnections();

        addVisualizationPane(viewer);
        setMouseListener(viewer);
        refit();
    }

    private Map<String, org.jungrapht.visualization.layout.model.Point>
    convertClassPositionsToFullNamePositions(Map<UMLClass, org.jungrapht.visualization.layout.model.Point> positions) {
        HashMap<String, org.jungrapht.visualization.layout.model.Point> classPositions = new HashMap<>();
        for (UMLClass object : positions.keySet()) {
            org.jungrapht.visualization.layout.model.Point position = positions.get(object);
            classPositions.put(object.toString(), org.jungrapht.visualization.layout.model.Point.of(position.x, position.y));
        }
        return classPositions;
    }

    private void setupConnections() {
        viewer.getRenderContext().setArrowFillPaintFunction((rel) -> {
            if (rel.getType() == UMLRelationshipType.Composition) {
                if (diagram.getSelectedEdges().contains(rel)
                        || viewer.getSelectedEdges().contains(rel))
                    return Color.decode(Formatter.classSelectedColor);
                return Color.BLACK;
            }
            return Color.WHITE;
        });
        viewer.getRenderContext().setArrowDrawPaintFunction((rel) -> {
            if (diagram.getSelectedEdges().contains(rel)
                    || viewer.getSelectedEdges().contains(rel))
                return Color.decode(Formatter.classSelectedColor);
            return Color.BLACK;
        });

        // FIXME: workaround: setting custom arrows does not work since the setupArrows()
        // always overrides, so we inject our shape here:
        viewer.getRenderContext().setEdgeArrowStrokeFunction((rel) -> {
            Shape shape = getArrowShape(rel);
            viewer.getRenderContext().setEdgeArrow(shape);
            viewer.getRenderContext().setRenderEdgeArrow(true);
            if (diagram.getSelectedEdges().contains(rel)
                    || viewer.getSelectedEdges().contains(rel))
                return new BasicStroke(4f);
            return new BasicStroke(2f);
        });
    }

    private void setupEdges() {
        viewer.getRenderContext().setEdgeStrokeFunction(rel -> {
            if (rel.getType() == UMLRelationshipType.Dependency || rel.getType() == UMLRelationshipType.Realization) {
                // dashed line
                return new BasicStroke(diagram.getSelectedEdges().contains(rel)
                        || viewer.getSelectedEdges().contains(rel) ? 4 : 2,
                        BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
                        0, new float[]{12}, 0);
            }
            return new BasicStroke(diagram.getSelectedEdges().contains(rel)
                    || viewer.getSelectedEdges().contains(rel)
                    ? 4 : 2);
        });
        viewer.getRenderContext().setEdgeDrawPaintFunction(relationship -> {
            if (diagram.getSelectedEdges().contains(relationship)
                    || viewer.getSelectedEdges().contains(relationship))
                return Color.decode(Formatter.classSelectedColor);
            return Color.BLACK;
        });

        viewer.getRenderContext().setSelectedEdgeDrawPaintFunction(relationship -> {
            return Color.decode(Formatter.classSelectedColor);
        });

    }

    private void setupVertices() {
        // VERTICES
        // custom vertex shape for the UML classes
        VertexLabelAsShapeRenderer<UMLClass, UMLRelationship> vlasr = new VertexLabelAsShapeRenderer<>(
                visualizationModel.getLayoutModel(), viewer.getRenderContext()) {
            public Shape apply(UMLClass v) {
                Component component = this.prepareRenderer(this.renderContext, this.renderContext.getVertexLabelFunction().apply(v), this.renderContext.getSelectedVertexState().isSelected(v), v);
                Dimension size = component.getPreferredSize();
                RoundRectangle2D.Double bounds = getObjectBounds(size);
                if (!verticesBounds.containsKey(v))
                    verticesBounds.put(v, bounds);
                return bounds;
            }

            public void labelVertex(RenderContext<UMLClass, UMLRelationship> renderContext, LayoutModel<UMLClass> layoutModel, UMLClass v, String label) {
                if (renderContext.getVertexIncludePredicate().test(v)) {
                    GraphicsDecorator g = renderContext.getGraphicsContext();
                    Component component = this.prepareRenderer(renderContext, label, renderContext.getSelectedVertexState().isSelected(v), v);
                    Dimension d = component.getPreferredSize();
                    int h_offset = -d.width / 2;
                    int v_offset = -d.height / 2;
                    org.jungrapht.visualization.layout.model.Point p = layoutModel.apply(v);
                    Point2D p2d = renderContext.getMultiLayerTransformer().transform(MultiLayerTransformer.Layer.LAYOUT, p.x, p.y);
                    int x = (int) p2d.getX();
                    int y = (int) p2d.getY();
                    g.draw(component, renderContext.getRendererPane(), x + h_offset, y + v_offset, d.width, d.height, true);
                    Dimension size = component.getPreferredSize();
                    RoundRectangle2D.Double bounds = getObjectBounds(size);
                    this.shapes.put(v, bounds);
                    verticesBounds.put(v, bounds);
                }
            }
        };
        if (onGetVertexLabel != null)
            viewer.getRenderContext().setVertexLabelFunction(object -> onGetVertexLabel.apply(object));
        viewer.getRenderContext().setVertexShapeFunction(vlasr);
        viewer.getRenderer().setVertexLabelRenderer(BiModalRenderer.HEAVYWEIGHT, vlasr);
        viewer.getRenderer().setVertexRenderer(BiModalRenderer.HEAVYWEIGHT,
                new GradientVertexRenderer<>(Color.WHITE, Color.WHITE, true));
        viewer.setVertexToolTipFunction(UMLClass::getName);
//        viewer.getRenderContext().setVertexFontFunction(v -> new Font(Font.MONOSPACED, Font.PLAIN, 12));
        viewer.getRenderContext().setVertexStrokeFunction((object) -> {
            // WORKAROUND
            if (viewer.getRenderContext().getSelectedVertexState().isSelected(object))
                return new BasicStroke(6.0f);
            else
                return new BasicStroke(4.0f);
        });
        viewer.getRenderContext().setSelectedVertexStrokeFunction((object) -> {
            // FIXME: not working?
            return new BasicStroke(4.0f);
        });
        viewer.getRenderContext().setVertexDrawPaintFunction((object) -> {
            // WORKAROUND
            if (viewer.getRenderContext().getSelectedVertexState().isSelected(object))
                return Color.decode(Formatter.classSelectedColor);
            else
                return Color.BLACK;
        });
        viewer.getRenderContext().setSelectedVertexDrawPaintFunction((object) -> {
            return Color.decode(Formatter.classSelectedColor);
        });
        viewer.getRenderContext().setVertexFillPaintFunction((object) -> {
            if (viewer.getRenderContext().getSelectedVertexState().isSelected(object))
                return Color.decode(Formatter.classSelectedColor);
            else
                return Color.WHITE;
        });
        viewer.getRenderContext().setSelectedVertexFillPaintFunction((e) -> {
            // FIXME: not working?
            return Color.decode(Formatter.classSelectedColor);
        });
    }

    private void refit() {
        double minx = Double.MAX_VALUE;
        double miny = Double.MAX_VALUE;
        for (Map.Entry<UMLClass, org.jungrapht.visualization.layout.model.Point> entry
                : this.viewer.getVisualizationModel().getLayoutModel().getLocations().entrySet()) {
            minx = Math.min(minx, entry.getValue().x);
            miny = Math.min(miny, entry.getValue().y);
        }
        if (minx < 0 || miny < 0) {
            moveVertices(minx < 0 ? -minx : 0, miny < 0 ? -miny : 0);
        }

        double maxx = 400;
        double maxy = 400;
        for (Map.Entry<UMLClass, org.jungrapht.visualization.layout.model.Point> entry
                : this.viewer.getVisualizationModel().getLayoutModel().getLocations().entrySet()) {
            maxx = Math.max(maxx, entry.getValue().x);
            maxy = Math.max(maxy, entry.getValue().y);
        }
        resizeViewer((int) maxx, (int) maxy);
    }


    private void moveVertices(double dx, double dy) {
        for (Map.Entry<UMLClass, org.jungrapht.visualization.layout.model.Point> entry
                : this.viewer.getVisualizationModel().getLayoutModel().getLocations().entrySet()) {
            org.jungrapht.visualization.layout.model.Point p
                    = org.jungrapht.visualization.layout.model.Point.of(
                    entry.getValue().x + dx, entry.getValue().y + dy);
            this.viewer.getVisualizationModel().getLayoutModel().set(entry.getKey(), p);
        }
        viewer.repaint();
    }

    private void resizeViewer(int width, int height) {
        viewer.getVisualizationModel().getLayoutModel().setSize(width, height);
        viewer.getVisualizationModel().getLayoutModel().setPreferredSize(width, height);
        EventQueue.invokeLater(() -> {
            visualizationScrollPane.getHorizontalScrollBar().setValue(
                    viewer.getVisualizationModel().getLayoutModel().getWidth() / 2);
            visualizationScrollPane.getVerticalScrollBar().setValue(
                    viewer.getVisualizationModel().getLayoutModel().getHeight() / 2);
            viewer.repaint();
            repaint();
        });
    }

    private Dimension estimateGraphSize(Graph<UMLClass, UMLRelationship> graph) {
        int vertices = graph.vertexSet().size();
        int width = (int) Math.sqrt(vertices) * 1000;
        if (width == 0)
            width = 1200;
        return new Dimension(width, width * 2 / 3);
    }

    private void addVisualizationPane(VisualizationViewer<UMLClass, UMLRelationship> vv) {
        if (visualizationScrollPane != null)
            remove(visualizationScrollPane);
        visualizationScrollPane = new VisualizationScrollPane(vv);
        visualizationScrollPane.setPreferredSize(this.getPreferredSize());
        add(visualizationScrollPane);
    }

    private void setMouseListener(VisualizationViewer<UMLClass, UMLRelationship> vv) {

        // we use the latest fix in UMLMouseListenerTranslator until we upgard to jungrapht 1.5
        ((Component) vv).addMouseListener(new GraphMouseListenerTranslator<>(new GraphMouseListener<>() {
            @Override
            public void graphClicked(UMLClass object, MouseEvent event) {
                if ((event.getModifiersEx() & MouseEvent.CTRL_DOWN_MASK) == MouseEvent.CTRL_DOWN_MASK
                        && (event.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) != MouseEvent.SHIFT_DOWN_MASK) {
                    vv.getRenderContext().getSelectedVertexState().clear();
                    vv.getRenderContext().getSelectedVertexState().select(new ArrayList<>(List.of(object)));
                }
                if (event.getButton() == MouseEvent.BUTTON3) {
                    // show context menu
                    showContextMenu(object, event, GraphPanel.this);
                    event.consume();
                } else if (event.getButton() == MouseEvent.BUTTON1 && event.getClickCount() == 2) {
                    toggleCompact(object);
                    event.consume();
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

    private void showContextMenu(UMLClass object, MouseEvent event, GraphPanel graphPanel) {
        ContextMenu contextMenu = new ContextMenu(object, diagram, graphPanel);
        JPopupMenu menu = contextMenu.getContextMenu();
        EventQueue.invokeLater(() -> menu.show(event.getComponent(), event.getX(), event.getY()));
    }

    public void updateRefs(List<HashSet<?>> refs) {
        HashSet<UMLClass> classes = new HashSet<>();
        for (HashSet<?> href : refs) {
            for (Object obj : href) {
                if (obj instanceof UMLClass) {
                    diagram.getSelectedVertices().add((UMLClass) obj);
                    classes.add((UMLClass) obj);
                } else if (obj instanceof UMLRelationship) {
                    diagram.getSelectedEdges().add((UMLRelationship) obj);
                    classes.add(((UMLRelationship) obj).getFrom());
                } else if (obj instanceof Field) {
                    diagram.getSelectedFields().add((Field) obj);
                    String owner = ((Field) obj).getOwner();
                    UMLClass cls = diagram.getOwnerByName(owner);
                    classes.add(cls);
                } else if (obj instanceof EnumConstant) {
                    diagram.getSelectedEnumConsts().add((EnumConstant) obj);
                    String owner = ((EnumConstant) obj).getOwner();
                    UMLClass cls = diagram.getOwnerByName(owner);
                    classes.add(cls);
                } else if (obj instanceof Method) {
                    diagram.getSelectedMethods().add((Method) obj);
                    String owner = ((Method) obj).getOwner();
                    UMLClass cls = diagram.getOwnerByName(owner);
                    classes.add(cls);
                }
            }
        }
        selectClasses(new ArrayList<>(classes));
        getViewer().repaint();
    }

    public void clearSelections() {
        viewer.getSelectedVertexState().clear();
        viewer.getSelectedEdgeState().clear();
        diagram.clearSelections();
    }

    private Shape getArrowShape(UMLRelationship rel) {
        if (rel.getType() == UMLRelationshipType.Aggregation || rel.getType() == UMLRelationshipType.Composition)
            return new Shapes.Diamond(40, 20);
        else if (rel.getType() == UMLRelationshipType.Inheritance || rel.getType() == UMLRelationshipType.Realization)
            return new Shapes.ClosedArrow(40, 20);
        else if (rel.getType() == UMLRelationshipType.Association || rel.getType() == UMLRelationshipType.Dependency)
            return new Shapes.OpenArrow(40, 20);
        else if (rel.getType() == UMLRelationshipType.Nested)
            return new Shapes.CircleCross(40, 40);
        return null;
    }

    public Map<UMLClass, org.jungrapht.visualization.layout.model.Point> getVertexPositions() {
        if (viewer == null)
            return null;
        return viewer.getVisualizationModel().getLayoutModel().getLocations();
    }

    public void clear() {
        if (visualizationScrollPane != null)
            remove(visualizationScrollPane);
        visualizationScrollPane = null;
        if (diagram != null)
            diagram.clear();
        this.invalidate();
        this.repaint();
    }

    public synchronized void selectClasses(List<UMLClass> classes) {
        this.viewer.getRenderContext().getSelectedVertexState().clear();
        this.viewer.getRenderContext().getSelectedVertexState().select(classes);
        if (classes.size() == 1) {
            UMLClass obj = classes.get(0);
            org.jungrapht.visualization.layout.model.Point point = viewer.getVisualizationModel().getLayoutModel().get(obj);
            EventQueue.invokeLater(() -> {
                Shape shape = verticesBounds.get(obj);
                int newX = (int) point.x - getVisualizationScrollPane().getWidth() / 2;
                int newY = (int) point.y - getVisualizationScrollPane().getHeight() / 2;
                visualizationScrollPane.getHorizontalScrollBar().setValue(newX);
                visualizationScrollPane.getVerticalScrollBar().setValue(newY);
                viewer.repaint();
            });
        }
    }

    public VisualizationViewer<UMLClass, UMLRelationship> getViewer() {
        return viewer;
    }

    public boolean toggleCollapse() {
        for (Map.Entry<UMLClass, org.jungrapht.visualization.layout.model.Point> entry
                : this.viewer.getVisualizationModel().getLayoutModel().getLocations().entrySet()) {
            entry.getKey().setCompact(collapsedAll);
        }
        collapsedAll = !collapsedAll;
        repaint();
        return collapsedAll;
    }

    public VisualizationScrollPane getVisualizationScrollPane() {
        return visualizationScrollPane;
    }


    public void setOnImagePainted(Consumer<Graphics> listener) {
        this.onImagePainted = listener;
    }

    public void removeOnImagePainted() {
        this.onImagePainted = null;
    }

    @Override
    protected void paintComponent(java.awt.Graphics g) {
        super.paintComponent(g);
        if (onImagePainted != null)
            onImagePainted.accept(g);
    }

    public void setOnGetVertexLabel(Function<UMLClass, String> onGetVertexLabel) {
        this.onGetVertexLabel = onGetVertexLabel;
    }

    public void removeOnGetVertexLabel() {
        this.onGetVertexLabel = null;
    }

}
