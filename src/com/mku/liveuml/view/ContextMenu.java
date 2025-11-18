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
import com.mku.liveuml.model.diagram.*;
import com.mku.liveuml.model.entities.*;
import com.mku.liveuml.utils.FileUtils;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class ContextMenu {
    private final UMLClass object;
    private final UMLDiagram diagram;
    private final GraphPanel panel;

    public ContextMenu(UMLClass object, UMLDiagram diagram, GraphPanel panel) {
        this.object = object;
        this.diagram = diagram;
        this.panel = panel;
    }

    public JPopupMenu getContextMenu() {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem nameItem = new JMenuItem(object.getName());
        nameItem.setEnabled(false);
        menu.add(nameItem);
        menu.addSeparator();

        menu.add(createToggleExpandItem());
        menu.add(createFindSubMenu("Referenced", UMLFinder.ReferenceType.From));
        menu.add(createFindSubMenu("Referencing", UMLFinder.ReferenceType.To));
        menu.add(createGoToSubMenu());
        return menu;
    }

    private Component createToggleExpandItem() {
        JMenuItem item = new JMenuItem(object.isCompact() ? "Collapse" : "Expand");
        item.addActionListener(e -> EventQueue.invokeLater(() -> {
            panel.toggleCompact(object);
            panel.getViewer().repaint();
        }));
        return item;
    }

    private Component createFindSubMenu(String title, UMLFinder.ReferenceType type) {
        JMenu menu = new JMenu(title);
        menu.add(createAllFindRefItem(type));
        createRelFindRefSection(menu, type);
        if(type != UMLFinder.ReferenceType.To) {
            createEnumFindRefSection(menu, type);
            createFieldsFindRefSection(menu, type);
        }
        createConstructorsFindRefSection(menu, type);
        createMethodsFindRefSection(menu, type);
        return menu;
    }

    private Component createGoToSubMenu() {
        JMenu menu = new JMenu("View in Text Editor / IDE");
        createClassGoToSection(menu);
        createFieldsGoToSection(menu);
        createConstructorsGoToSection(menu);
        createMethodsGoToSection(menu);
        return menu;
    }

    private JMenuItem createAllFindRefItem(UMLFinder.ReferenceType type) {
        JMenuItem item = new JMenuItem("All references");
        item.addActionListener(e -> {
            panel.clearSelections();
            java.util.List<HashSet<?>> refs = diagram.getFinder().findClassReference(object, null, type);
            panel.updateRefs(refs);
        });
        return item;
    }

    private void createFieldsFindRefSection(JMenu menu, UMLFinder.ReferenceType type) {
        java.util.List<JMenuItem> items = new ArrayList<>();
        if (object.getFields().size() > 0) {
            for (Field f : object.getFields()) {
                if (type == UMLFinder.ReferenceType.From
                        && f.getAccessModifiers().contains(AccessModifier.Private))
                    continue;
                if(!fieldHasRelationship(f, type))
                    continue;
                JMenuItem fItem = new JMenuItem(Formatter.getFieldFormatted(f, false));
                fItem.addActionListener(e -> {
                    panel.clearSelections();
                    java.util.List<HashSet<?>> refs = diagram.getFinder().findFieldReference(object, f, type);
                    panel.updateRefs(refs);
                });
                items.add(fItem);
            }
        }
        if (items.size() > 0)
            addSection("Fields", menu, items);
    }

    private void createConstructorsFindRefSection(JMenu menu, UMLFinder.ReferenceType type) {
        java.util.List<JMenuItem> items = new ArrayList<>();
        if (object.getMethods().size() > 0) {
            for (Method m : object.getMethods()) {
                if (!(m instanceof Constructor))
                    continue;
                if (type == UMLFinder.ReferenceType.From
                        && m.getAccessModifiers().contains(AccessModifier.Private))
                    continue;
                if(!methodHasRelationship(m, type))
                    continue;
                String methodName = Formatter.getMethodSignature(m, true, false);
                JMenuItem mItem = new JMenuItem(methodName);
                mItem.addActionListener(e -> {
                    panel.clearSelections();
                    java.util.List<HashSet<?>> refs = diagram.getFinder().findMethodReference(object, m, type);
                    panel.updateRefs(refs);
                });
                items.add(mItem);
            }
        }
        if (items.size() > 0)
            addSection("Constructors", menu, items);
    }

    private void createMethodsFindRefSection(JMenu menu, UMLFinder.ReferenceType type) {
        java.util.List<JMenuItem> items = new ArrayList<>();
        if (object.getMethods().size() > 0) {
            for (Method m : object.getMethods()) {
                if (m instanceof Constructor)
                    continue;
                if (type == UMLFinder.ReferenceType.From
                        && m.getAccessModifiers().contains(AccessModifier.Private))
                    continue;
                if(!methodHasRelationship(m, type))
                    continue;
                String methodName = Formatter.getMethodSignature(m, true, false);
                JMenuItem mItem = new JMenuItem(methodName);
                mItem.addActionListener(e -> {
                    panel.clearSelections();
                    List<HashSet<?>> refs = diagram.getFinder().findMethodReference(object, m, type);
                    panel.updateRefs(refs);
                });
                items.add(mItem);
            }
        }
        if (items.size() > 0)
            addSection("Methods", menu, items);
    }

    private void createMethodsGoToSection(JMenu menu) {
        java.util.List<JMenuItem> items = new ArrayList<>();
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
        if (items.size() > 0)
            addSection("Methods", menu, items);
    }

    private void addSection(String title, JMenu menu, List<JMenuItem> items) {
        menu.addSeparator();
        JLabel tItem = new JLabel(title);
        tItem.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        menu.add(tItem);
        for (JMenuItem fItem : items)
            menu.add(fItem);
    }

    private void createRelFindRefSection(JMenu menu, UMLFinder.ReferenceType type) {
        ArrayList<JMenuItem> items = new ArrayList<>();
        HashSet<UMLRelationshipType> relTypes = new HashSet<>();
        for (UMLRelationship rel : object.getRelationships().values()) {
            if(type == UMLFinder.ReferenceType.From && object == rel.getFrom())
                continue;
            if(type == UMLFinder.ReferenceType.To && object == rel.getTo())
                continue;
            relTypes.add(rel.getType());
        }
        for (UMLRelationshipType relType : relTypes) {
            JMenuItem depItem = new JMenuItem(relType + "");
            depItem.addActionListener(e -> {
                panel.clearSelections();
                java.util.List<HashSet<?>> refs = diagram.getFinder().findClassReference(object,
                        new HashSet<>(List.of(relType)), type);
                panel.updateRefs(refs);
            });
            items.add(depItem);
        }
        addSection("Relationships", menu, items);
    }

    private void createClassGoToSection(JMenu menu) {
        JMenuItem item = new JMenuItem("Class " + object.getName());
        item.addActionListener(e -> {
            goToClassReference(object);
            panel.getViewer().repaint();
        });
        menu.add(item);
    }

    private void createFieldsGoToSection(JMenu menu) {
        java.util.List<JMenuItem> items = new ArrayList<>();
        if (object.getFields().size() > 0) {
            for (Field f : object.getFields()) {
                JMenuItem fItem = new JMenuItem(Formatter.getFieldFormatted(f, false));
                fItem.addActionListener(e -> {
                    goToFieldReference(object, f);
                    panel.getViewer().repaint();
                });
                items.add(fItem);
            }
        }
        if (items.size() > 0)
            addSection("Fields", menu, items);
    }

    private void createConstructorsGoToSection(JMenu menu) {
        java.util.List<JMenuItem> items = new ArrayList<>();
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
        if (items.size() > 0)
            addSection("Constructors", menu, items);
    }

    private void createEnumFindRefSection(JMenu menu, UMLFinder.ReferenceType type) {
        java.util.List<JMenuItem> items = new ArrayList<>();
        if (object.getEnumConstants().size() > 0) {
            for (EnumConstant enumConstant : object.getEnumConstants()) {
                if(!enumHasRelationship(enumConstant, type))
                    continue;
                JMenuItem fItem = new JMenuItem(enumConstant.getName());
                fItem.addActionListener(e -> {
                    panel.clearSelections();
                    java.util.List<HashSet<?>> refs = diagram.getFinder()
                            .findEnumConstReference(object, enumConstant, type);
                    panel.updateRefs(refs);
                });
                items.add(fItem);
            }
        }
        if (items.size() > 0)
            addSection("Enum Constants", menu, items);
    }

    private boolean enumHasRelationship(EnumConstant enumConstant, UMLFinder.ReferenceType type) {
        for(UMLRelationship rel : object.getRelationships().values()) {
            if(type == UMLFinder.ReferenceType.From && object == rel.getFrom())
                continue;
            if(type == UMLFinder.ReferenceType.To && object == rel.getTo())
                continue;
            if(rel.getEnumsAccessedByMethods().containsKey(enumConstant))
                return true;
        }
        return false;
    }

    private boolean methodHasRelationship(Method m, UMLFinder.ReferenceType type) {
        for(UMLRelationship rel : object.getRelationships().values()) {
            if(type == UMLFinder.ReferenceType.From && object == rel.getFrom())
                continue;
            if(type == UMLFinder.ReferenceType.To && object == rel.getTo())
                continue;
            if(type == UMLFinder.ReferenceType.From && rel.getMethodsAccessedByMethods().containsKey(m))
                return true;
            if(type == UMLFinder.ReferenceType.To && rel.getMethodsAccesingMethods().containsKey(m))
                return true;
            if(type == UMLFinder.ReferenceType.To && rel.getMethodsAccessingEnums().containsKey(m))
                return true;
            if(type == UMLFinder.ReferenceType.To && rel.getMethodsAccessingFields().containsKey(m))
                return true;
            if(type == UMLFinder.ReferenceType.To && rel.getMethodsAccessingClass().contains(m))
                return true;
        }
        return false;
    }

    private boolean fieldHasRelationship(Field f, UMLFinder.ReferenceType type) {
        for(UMLRelationship rel : object.getRelationships().values()) {
            if(type == UMLFinder.ReferenceType.From && object == rel.getFrom())
                continue;
            if(type == UMLFinder.ReferenceType.To && object == rel.getTo())
                continue;
            if(type == UMLFinder.ReferenceType.From && rel.getFieldsAccessedByMethods().containsKey(f))
                return true;
        }
        return false;
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
