/*
 * Copyright (c) 2003, The JUNG Authors
 *
 * All rights reserved.
 *
 * This software is open-source under the BSD license; see either
 * "license.txt" or
 * https://github.com/tomnelson/jungrapht-visualization/blob/master/LICENSE for a description.
 */
/*
 * Created on Feb 17, 2004
 */

/*
TODO: Workaround for mouse listener fix, remove when fix is released
see: https://github.com/tomnelson/jungrapht-visualization/issues/28
 */
package com.mku.liveuml.view;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import org.jungrapht.visualization.VisualizationViewer;
import org.jungrapht.visualization.control.GraphElementAccessor;
import org.jungrapht.visualization.control.GraphMouseListener;
import org.jungrapht.visualization.layout.model.LayoutModel;

/**
 * This class translates mouse clicks into vertex clicks
 *
 * @param <V> vertex type
 * @param <E> edge type
 */
public class UMLMouseListenerTranslator<V, E> extends MouseAdapter {

    private VisualizationViewer<V, E> visualizationViewer;
    private GraphMouseListener<V> graphMouseListener;

    /**
     * @param graphMouseListener listens for mouse events
     * @param visualizationViewer the viewer used for visualization
     */
    public UMLMouseListenerTranslator(
            GraphMouseListener<V> graphMouseListener, VisualizationViewer<V, E> visualizationViewer) {
        this.graphMouseListener = graphMouseListener;
        this.visualizationViewer = visualizationViewer;
    }

    /**
     * Transform the point to the coordinate system in the VisualizationViewer, then use either
     * PickSuuport (if available) or Layout to find a Vertex
     *
     * @param point
     * @return
     */
    private V getVertex(Point2D point) {
        GraphElementAccessor<V, E> pickSupport = visualizationViewer.getPickSupport();
        V v = null;
        if (pickSupport != null) {
            // adjust for scale and offset in the VisualizationViewer
            point =
                    visualizationViewer.getRenderContext().getMultiLayerTransformer().inverseTransform(point);
            LayoutModel<V> layoutModel = visualizationViewer.getVisualizationModel().getLayoutModel();
            v = pickSupport.getVertex(layoutModel, point.getX(), point.getY());
        }
        return v;
    }

    /** @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent) */
    @Override
    public void mouseClicked(MouseEvent e) {
        V v = getVertex(e.getPoint());
        if (v != null) {
            graphMouseListener.graphClicked(v, e);
        }
    }

    /** @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent) */
    @Override
    public void mousePressed(MouseEvent e) {
        V v = getVertex(e.getPoint());
        if (v != null) {
            graphMouseListener.graphPressed(v, e);
        }
    }

    /** @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent) */
    @Override
    public void mouseReleased(MouseEvent e) {
        V v = getVertex(e.getPoint());
        if (v != null) {
            graphMouseListener.graphReleased(v, e);
        }
    }
}