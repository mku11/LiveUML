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

import com.mku.liveuml.model.UMLClass;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ClassesPane extends JScrollPane {
    private JList<UMLClass> classes;
    private ArrayList<UMLClass> classesList;
    private BiConsumer<UMLClass,MousePosition> onMouseRightClick;
    private Consumer<List<UMLClass>> onClassSelected;

    public ClassesPane() {
        super(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        classes = new JList<>();
        classes.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    // reselect the class
                    if(onClassSelected != null)
                        onClassSelected.accept(classes.getSelectedValuesList());
                } else if (e.getButton() == MouseEvent.BUTTON3) {
                    if (classes.getSelectedValuesList().size() == 0)
                        return;
                    UMLClass cls = classes.getSelectedValuesList().get(0);
                    if(onMouseRightClick != null)
                        onMouseRightClick.accept(cls, new MousePosition(classes, 5, classes.getCellBounds(
                                classes.getSelectedIndex() + 1,
                                classes.getSelectedIndex() + 1).y));
                }
            }
        });
        setViewportView(classes);
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

    public void setOnMouseRightClick(BiConsumer<UMLClass,MousePosition> listener) {
        this.onMouseRightClick = listener;
    }

    public void removeOnMouseRightClick() {
        this.onMouseRightClick = null;
    }

    public void setOnClassSelected(Consumer<List<UMLClass>> listener) {
        this.onClassSelected = listener;
    }

    public void removeOnClassSelected() {
        this.onClassSelected = null;
    }

    public void setClasses(UMLClass[] classesArr) {
        Arrays.sort(classesArr, Comparator.comparing(UMLClass::toString));
        classesList = new ArrayList<>(java.util.List.of(classesArr));
        classes.setListData(classesArr);
    }

    public void clearSelection() {
        classes.clearSelection();
    }

    public void selectClasses(Set<UMLClass> selectedVertices) {
        for (UMLClass cls : selectedVertices) {
            int idx = classesList.indexOf(cls);
            classes.addSelectionInterval(idx, idx);
        }
    }
}
