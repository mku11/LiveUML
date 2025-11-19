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

import com.mku.liveuml.model.diagram.*;

import java.util.function.Consumer;
import javax.swing.*;
import java.awt.*;

public class SourcesListContextMenu {
    private final String source;
    private final UMLDiagram diagram;
    private final GraphPanel panel;
    private Consumer<String> onDelete;

    public SourcesListContextMenu(String source, UMLDiagram diagram, GraphPanel panel) {
        this.source = source;
        this.diagram = diagram;
        this.panel = panel;
    }

    public JPopupMenu getContextMenu() {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem nameItem = new JMenuItem(source);
        nameItem.setEnabled(false);
        menu.add(nameItem);
        menu.addSeparator();

        menu.add(createDeleteItem());
        return menu;
    }

    private Component createDeleteItem() {
        JMenuItem item = new JMenuItem("Delete");
        item.addActionListener(e -> EventQueue.invokeLater(() -> {
            onDelete.accept(source);
        }));
        return item;
    }

    public void setOnDelete(Consumer<String> onDelete) {
        this.onDelete = onDelete;
    }
}
