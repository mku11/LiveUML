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
import com.mku.liveuml.model.entities.*;
import com.mku.liveuml.utils.FileUtils;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class ContextMenu {

    public JPopupMenu getContextMenu(UMLClass object, UMLDiagram diagram, GraphPanel panel) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem nameItem = new JMenuItem(object.getName());
        nameItem.setEnabled(false);
        menu.add(nameItem);
        menu.addSeparator();

        JMenuItem item = new JMenuItem(object.isCompact() ? "Collapse" : "Expand");
        item.addActionListener(e -> EventQueue.invokeLater(() -> {
            panel.toggleCompact(object);
            panel.getViewer().repaint();
        }));
        menu.add(item);

        // find references
        JMenu refMenu = new JMenu("Find references");
        menu.add(refMenu);

        JMenuItem cItem = new JMenuItem("All references");
        cItem.addActionListener(e -> {
            panel.clearSelections();
            java.util.List<HashSet<?>> refs = diagram.findClassReference(object);
            panel.updateRefs(refs);
            panel.getViewer().repaint();
        });
        refMenu.add(cItem);

        java.util.List<JMenuItem> items = new ArrayList<>();
        if (object.getEnumConstants().size() > 0) {
            for (EnumConstant enumConstant : object.getEnumConstants()) {
                JMenuItem fItem = new JMenuItem(enumConstant.getName());
                fItem.addActionListener(e -> {
                    panel.clearSelections();
                    java.util.List<HashSet<?>> refs = diagram.findEnumConstReference(object, enumConstant);
                    panel.updateRefs(refs);
                    panel.getViewer().repaint();
                });
                items.add(fItem);
            }
        }
        if (items.size() > 0) {
            refMenu.addSeparator();
            JLabel tItem = new JLabel("Enum Constants");
            tItem.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            refMenu.add(tItem);
            for (JMenuItem fItem : items)
                refMenu.add(fItem);
        }

        items.clear();
        if (object.getFields().size() > 0) {
            for (Field f : object.getFields()) {
                if (f.getAccessModifiers().contains(AccessModifier.Private))
                    continue;
                JMenuItem fItem = new JMenuItem(Formatter.getFieldFormatted(f));
                fItem.addActionListener(e -> {
                    panel.clearSelections();
                    java.util.List<HashSet<?>> refs = diagram.findFieldReference(object, f);
                    panel.updateRefs(refs);
                    panel.getViewer().repaint();
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
        if (object.getMethods().size() > 0) {
            for (Method m : object.getMethods()) {
                if (!(m instanceof Constructor))
                    continue;
                if (m.getAccessModifiers().contains(AccessModifier.Private))
                    continue;
                String methodName = Formatter.getMethodSignature(m, true, false);
                JMenuItem mItem = new JMenuItem(methodName);
                mItem.addActionListener(e -> {
                    panel.clearSelections();
                    java.util.List<HashSet<?>> refs = diagram.findMethodReference(object, m);
                    panel.updateRefs(refs);
                    panel.getViewer().repaint();
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
        if (object.getMethods().size() > 0) {
            for (Method m : object.getMethods()) {
                if (m instanceof Constructor)
                    continue;
                if (m.getAccessModifiers().contains(AccessModifier.Private))
                    continue;
                String methodName = Formatter.getMethodSignature(m, true, false);
                JMenuItem mItem = new JMenuItem(methodName);
                mItem.addActionListener(e -> {
                    panel.clearSelections();
                    List<HashSet<?>> refs = diagram.findMethodReference(object, m);
                    panel.updateRefs(refs);
                    panel.getViewer().repaint();
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
        JMenu goToMenu = new JMenu("View in Text Editor / IDE");
        menu.add(goToMenu);

        cItem = new JMenuItem("Class " + object.getName());
        cItem.addActionListener(e -> {
            goToClassReference(object);
            panel.getViewer().repaint();
        });
        goToMenu.add(cItem);

        items.clear();
        if (object.getFields().size() > 0) {
            for (Field f : object.getFields()) {
                JMenuItem fItem = new JMenuItem(Formatter.getFieldFormatted(f));
                fItem.addActionListener(e -> {
                    goToFieldReference(object, f);
                    panel.getViewer().repaint();
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
        if (object.getMethods().size() > 0) {
            for (Method m : object.getMethods()) {
                if (!(m instanceof Constructor))
                    continue;
                String methodName = Formatter.getMethodSignature(m, true, false);
                JMenuItem mItem = new JMenuItem(methodName);
                mItem.addActionListener(e -> {
                    goToMethodReference(object, m);
                    panel.getViewer().repaint();
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
        if (object.getMethods().size() > 0) {
            for (Method m : object.getMethods()) {
                if (m instanceof Constructor)
                    continue;
                String methodName = Formatter.getMethodSignature(m, true, false);
                JMenuItem mItem = new JMenuItem(methodName);
                mItem.addActionListener(e -> {
                    goToMethodReference(object, m);
                    panel.getViewer().repaint();
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
        return menu;
    }

    public void goToClassReference(UMLClass s) {
        FileUtils.openFileAtLine(s.getFilePath(), s.getLine());
    }

    private void goToMethodReference(UMLClass s, Method m) {
        FileUtils.openFileAtLine(s.getFilePath(), m.getLine());
    }

    private void goToFieldReference(UMLClass s, Field f) {
        FileUtils.openFileAtLine(s.getFilePath(), f.getLine());
    }

}
