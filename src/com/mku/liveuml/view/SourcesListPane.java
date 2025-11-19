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

import com.mku.liveuml.model.diagram.UMLClass;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.*;
import java.util.function.BiConsumer;

public class SourcesListPane extends JScrollPane {
    private JList<String> sources;
    private ArrayList<String> sourcesList;
    private BiConsumer<String, MousePosition> onMouseRightClick;

    public SourcesListPane() {
        super(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        sources = new JList<>();
        sources.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) {
                    if (sources.getSelectedValuesList().size() == 0)
                        return;
                    String source = sources.getSelectedValuesList().get(0);
                    if (onMouseRightClick != null)
                        onMouseRightClick.accept(source, new MousePosition(sources, 5, sources.getCellBounds(
                                sources.getSelectedIndex(),
                                sources.getSelectedIndex() + 1).y));
                }
            }
        });
        setViewportView(sources);
    }

    public void clear() {
        setSources(new String[0]);
    }

    public class MousePosition {
        public Component component;
        public Integer x;
        public Integer y;

        public MousePosition(Component component, int x, int y) {
            this.component = component;
            this.x = x;
            this.y = y;
        }
    }

    public void setOnMouseRightClick(BiConsumer<String, MousePosition> listener) {
        this.onMouseRightClick = listener;
    }

    public void removeOnMouseRightClick() {
        this.onMouseRightClick = null;
    }

    public void setSources(String[] classesArr) {
        Arrays.sort(classesArr);
        sourcesList = new ArrayList<>(List.of(classesArr));
        sources.setListData(classesArr);
    }

    public void clearSelection() {
        sources.clearSelection();
    }

    public void selectClasses(Set<UMLClass> selectedVertices) {
        for (UMLClass cls : selectedVertices) {
            int idx = sourcesList.indexOf(cls);
            sources.addSelectionInterval(idx, idx);
        }
    }
}
