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
import com.mku.liveuml.image.Icons;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.function.Consumer;

public class MenuBar extends JMenuBar {
    public void setLabel(Action action, String label) {
        JMenuItem menuItem = items.get(action);
        menuItem.setText(label);
    }

    public enum Action {
        New, Open, Save, SaveAs, Close, Exit, ExportImage, ToggleExpand,
        ImportSource, ListSources, RefreshSources,
        ChooseViewer,
        Help, About
    }

    // make sure you provide unique text for each menu item
    private final HashMap<Action, JMenuItem> items = new HashMap<>();

    public MenuBar() {
        JMenu menu = new JMenu("File");
        add(menu);

        JMenuItem item = new JMenuItem("New");
        item.setIcon(Icons.getIcon("/icons/menu/import_file_small.png"));
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_MASK));
        menu.add(item);
        items.put(Action.New, item);

        item = new JMenuItem("Open");
        item.setIcon(Icons.getIcon("/icons/menu/folder_menu_small.png"));
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK));
        menu.add(item);
        items.put(Action.Open, item);

        item = new JMenuItem("Save");
        item.setIcon(Icons.getIcon("/icons/menu/save_small.png"));
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK));
        menu.add(item);
        items.put(Action.Save, item);

        item = new JMenuItem("Save As");
        item.setIcon(Icons.getIcon("/icons/menu/save_small.png"));
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK | InputEvent.SHIFT_DOWN_MASK));
        menu.add(item);
        items.put(Action.SaveAs, item);

        item = new JMenuItem("Export Image");
        item.setIcon(Icons.getIcon("/icons/menu/export_file_small.png"));
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_MASK));
        menu.add(item);
        items.put(Action.ExportImage, item);

        item = new JMenuItem("Close");
        item.setIcon(Icons.getIcon("/icons/menu/cancel_small.png"));
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK | InputEvent.SHIFT_DOWN_MASK));
        menu.add(item);
        items.put(Action.Close, item);

        item = new JMenuItem("Exit");
        item.setIcon(Icons.getIcon("/icons/menu/exit_small.png"));
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_MASK));
        menu.add(item);
        items.put(Action.Exit, item);

        menu = new JMenu("View");
        add(menu);

        item = new JMenuItem("Expand All");
        item.setIcon(Icons.getIcon("/icons/menu/sort_small.png"));
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_MASK | InputEvent.SHIFT_DOWN_MASK));
        menu.add(item);
        items.put(Action.ToggleExpand, item);

        menu = new JMenu("Source");
        add(menu);

        item = new JMenuItem("Import Source");
        item.setIcon(Icons.getIcon("/icons/menu/import_folder_small.png"));
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.CTRL_MASK));
        menu.add(item);
        items.put(Action.ImportSource, item);

        item = new JMenuItem("List Sources");
        item.setIcon(Icons.getIcon("/icons/menu/text_file_small.png"));
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_MASK));
        menu.add(item);
        items.put(Action.ListSources, item);

        item = new JMenuItem("Refresh Sources");
        item.setIcon(Icons.getIcon("/icons/menu/refresh_small.png"));
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_MASK));
        menu.add(item);
        items.put(Action.RefreshSources, item);

        menu = new JMenu("Settings");
        add(menu);

        item = new JMenuItem("Choose Text Editor / IDE");
        item.setIcon(Icons.getIcon("/icons/menu/settings_small.png"));
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_MASK));
        menu.add(item);
        items.put(Action.ChooseViewer, item);

        menu = new JMenu("Help");
        add(menu);

        item = new JMenuItem("Help");
        item.setIcon(Icons.getIcon("/icons/menu/file_properties_small.png"));
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, InputEvent.CTRL_MASK));
        menu.add(item);
        items.put(Action.Help, item);

        item = new JMenuItem("About");
        item.setIcon(Icons.getIcon("/icons/menu/info_small.png"));
        menu.add(item);
        items.put(Action.About, item);
    }

    public void setListener(Action action, Consumer<ActionEvent> listener) {
        JMenuItem menuItem = items.get(action);
        menuItem.addActionListener(listener::accept);
    }
}
