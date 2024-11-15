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
package com.mku.liveuml;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.google.gson.Gson;
import com.google.gson.internal.StringMap;
import com.mku.liveuml.gen.Generator;
import com.mku.liveuml.graph.UMLClassFactory;
import com.mku.liveuml.entities.*;
import com.mku.liveuml.graph.UMLRelationship;
import com.mku.liveuml.graph.UMLClass;
import com.mku.liveuml.view.GraphPanel;
import org.jgrapht.Graph;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.AttributeType;
import org.jgrapht.nio.DefaultAttribute;
import org.jgrapht.nio.graphml.GraphMLExporter;
import org.jgrapht.nio.graphml.GraphMLImporter;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.prefs.Preferences;

public class Main {

    public static void main(String[] args) {
        JFrame f = new JFrame();
        f.setTitle("LiveUML");
        f.setIconImage(Toolkit.getDefaultToolkit().getImage(Main.class.getResource("/icons/logo.png")));
        f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        JMenuBar menubar = new JMenuBar();
        JMenu menu = new JMenu("File");
        menubar.add(menu);
        f.setJMenuBar(menubar);
        JMenuItem newGraphItem = new JMenuItem("New");
        menu.add(newGraphItem);
        JMenuItem openGraphItem = new JMenuItem("Open");
        menu.add(openGraphItem);
        JMenuItem saveGraphItem = new JMenuItem("Save");
        menu.add(saveGraphItem);
        JMenuItem exportGraphItem = new JMenuItem("Export");
        menu.add(exportGraphItem);

        menu = new JMenu("Source");
        menubar.add(menu);
        f.setJMenuBar(menubar);
        JMenuItem importSourceFilesItem = new JMenuItem("Import Source Files");
        menu.add(importSourceFilesItem);
        JMenuItem clearSourceFilesItem = new JMenuItem("Clear Source Files");
        menu.add(clearSourceFilesItem);

        menu = new JMenu("Settings");
        menubar.add(menu);
        f.setJMenuBar(menubar);
        JMenuItem chooseEditorItem = new JMenuItem("Choose Text Editor");
        menu.add(chooseEditorItem);

        GraphPanel panel = new GraphPanel();
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.add(panel);
        p.setBorder(new EmptyBorder(10, 10, 10, 10));

        f.getContentPane().add(p);
        f.setMinimumSize(new Dimension(1200, 800));
        f.pack();

        importSourceFilesItem.addActionListener((e) -> {
            Preferences prefs = Preferences.userRoot().node(Main.class.getName());
            JFileChooser fc = new JFileChooser(prefs.get("LAST_SOURCE_FOLDER",
                    new File(".").getAbsolutePath()));
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int returnVal = fc.showOpenDialog(f);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File root = fc.getSelectedFile();
                prefs.put("LAST_SOURCE_FOLDER", root.getPath());

                setupFolder(root);
                List<UMLClass> classes = new Generator().getClasses(root);
                panel.addClasses(classes);
                panel.display(null);
                panel.revalidate();
            }
        });


        openGraphItem.addActionListener((e) -> {
            Preferences prefs = Preferences.userRoot().node(Main.class.getName());
            JFileChooser fc = new JFileChooser(prefs.get("LAST_GRAPH_FILE",
                    new File(".").getAbsolutePath()));
            fc.setDialogTitle("Choose graph file to load");
            fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
            FileNameExtensionFilter filter = new FileNameExtensionFilter("GraphML files", "graphml");
            fc.setFileFilter(filter);
            int returnVal = fc.showOpenDialog(f);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();
                prefs.put("LAST_GRAPH_FILE", file.getPath());
                GraphMLImporter<UMLClass, UMLRelationship> importer = new GraphMLImporter<>();
                importer.setSchemaValidation(false);

                importer.setVertexFactory(UMLClassFactory::create);
                HashMap<UMLClass, Point2D.Double> verticesPositions = new HashMap<>();

                HashMap<String, UMLClass> vertices = new HashMap<>();
                importer.addVertexAttributeConsumer((pair, attribute) -> Main.setVertexAttrs(pair.getFirst(),
                        pair.getSecond(), attribute, verticesPositions, vertices));
                // TODO: remove this since we update the relationships from the vertices attrs
                importer.addEdgeAttributeConsumer((pair, attribute) -> Main.setEdgeAttrs(pair.getFirst(),
                        pair.getSecond(), attribute, vertices));

                try (InputStreamReader inputStreamReader = new FileReader(file)) {
                    panel.createGraph();
                    importer.importGraph(panel.graph, inputStreamReader);
                    panel.updateVertices(vertices);
                    panel.display(verticesPositions);
                    panel.revalidate();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });


        saveGraphItem.addActionListener((e) -> {
            Preferences prefs = Preferences.userRoot().node(Main.class.getName());
            JFileChooser fc = new JFileChooser(prefs.get("LAST_GRAPH_FILE",
                    new File(".").getAbsolutePath()));
            fc.setDialogTitle("Choose graph file to save");
            fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
            FileNameExtensionFilter filter = new FileNameExtensionFilter("GraphML files", "graphml");
            fc.setFileFilter(filter);
            int returnVal = fc.showSaveDialog(f);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();
                prefs.put("LAST_GRAPH_FILE", file.getPath());
                GraphMLExporter<UMLClass, UMLRelationship> exporter = new GraphMLExporter<>();


                exporter.setVertexIdProvider(obj -> obj.getClass().getSimpleName() + ":" + obj);
                registerVertexAttrs(exporter);
                exporter.setVertexAttributeProvider(obj -> getVertexAttrs(obj, panel.getVertexPositions()));

                 exporter.setEdgeIdProvider(obj -> {
                     String text = obj.toString();
                     return text;
                 });
                 registerEdgeAttrs(exporter);
                 exporter.setEdgeAttributeProvider(Main::getEdgeAttrs);

                try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file))) {
                    exporter.exportGraph(panel.graph, writer);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        chooseEditorItem.addActionListener((e) -> {
            Preferences prefs = Preferences.userRoot().node(Main.class.getName());
            JFileChooser fc = new JFileChooser(prefs.get("LAST_TEXT_EDITOR_FILE",
                    new File(".").getAbsolutePath()));
            fc.setDialogTitle("Choose text editor");
            fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
            FileNameExtensionFilter filter = new FileNameExtensionFilter("Executable files", "exe");
            fc.setFileFilter(filter);
            int returnVal = fc.showOpenDialog(f);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();
                prefs.put("LAST_TEXT_EDITOR_FILE", file.getPath());
            }
        });


        exportGraphItem.addActionListener((e) -> {
            Preferences prefs = Preferences.userRoot().node(Main.class.getName());
            JFileChooser fc = new JFileChooser(prefs.get("LAST_EXPORT_FILE",
                    new File(".").getAbsolutePath()));
            fc.setDialogTitle("Choose file to export");
            fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
            FileNameExtensionFilter filter = new FileNameExtensionFilter("PNG files", "png");
            fc.setFileFilter(filter);
            int returnVal = fc.showSaveDialog(f);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();
                prefs.put("LAST_EXPORT_FILE", file.getPath());
                new Thread(()-> {
                    BufferedImage image = panel.getImage();
                    saveImage(file, image);
                }).start();
            }
        });

        f.setLocationRelativeTo(null);
        f.setVisible(true);
    }

    private static void registerEdgeAttrs(GraphMLExporter<UMLClass, UMLRelationship> exporter) {
        exporter.registerAttribute("type", GraphMLExporter.AttributeCategory.EDGE, AttributeType.STRING);
        exporter.registerAttribute("from", GraphMLExporter.AttributeCategory.EDGE, AttributeType.STRING);
        exporter.registerAttribute("to", GraphMLExporter.AttributeCategory.EDGE, AttributeType.STRING);
        exporter.registerAttribute("fieldAssociation", GraphMLExporter.AttributeCategory.EDGE, AttributeType.STRING);
        exporter.registerAttribute("classAccessors", GraphMLExporter.AttributeCategory.EDGE, AttributeType.STRING);
        exporter.registerAttribute("accessedBy", GraphMLExporter.AttributeCategory.EDGE, AttributeType.STRING);
        exporter.registerAttribute("accessing", GraphMLExporter.AttributeCategory.EDGE, AttributeType.STRING);
        exporter.registerAttribute("calledBy", GraphMLExporter.AttributeCategory.EDGE, AttributeType.STRING);
        exporter.registerAttribute("callTo", GraphMLExporter.AttributeCategory.EDGE, AttributeType.STRING);
    }

    private static Map<String, Attribute> getEdgeAttrs(UMLRelationship UMLRelationship) {
        HashMap<String, Attribute> map = new HashMap<>();
        map.put("type", new DefaultAttribute<>(UMLRelationship.type.name(), AttributeType.STRING));
        map.put("from", new DefaultAttribute<>(UMLRelationship.from.toString(), AttributeType.STRING));
        map.put("to", new DefaultAttribute<>(UMLRelationship.to.toString(), AttributeType.STRING));
        map.put("fieldAssociation", new DefaultAttribute<>(new Gson().toJson(getFieldsOwnerMap(UMLRelationship.fieldAssociation)), AttributeType.STRING));
        map.put("classAccessors", new DefaultAttribute<>(new Gson().toJson(getMethodOwnerMap(UMLRelationship.classAccessors)), AttributeType.STRING));
        map.put("accessedBy", new DefaultAttribute<>(new Gson().toJson(getFieldMethodOwnerMap(UMLRelationship.accessedBy)), AttributeType.STRING));
        map.put("accessing", new DefaultAttribute<>(new Gson().toJson(getMethodFieldOwnerMap(UMLRelationship.accessing)), AttributeType.STRING));
        map.put("calledBy", new DefaultAttribute<>(new Gson().toJson(getMethodMethodOwnerMap(UMLRelationship.calledBy)), AttributeType.STRING));
        map.put("callTo", new DefaultAttribute<>(new Gson().toJson(getMethodMethodOwnerMap(UMLRelationship.callTo)), AttributeType.STRING));
        return map;
    }

    private static HashMap<String,HashMap<String,String>> getFieldMethodOwnerMap(HashMap<Field, Method> fieldMethodMap) {
        HashMap<String,HashMap<String,String>> fieldMethodOwnerMap = new HashMap<>();
        for(Field f: fieldMethodMap.keySet()) {
            Method m = fieldMethodMap.get(f);
            HashMap<String, String> ownerMap = new HashMap<>();
            fieldMethodOwnerMap.put(f.getName(), ownerMap);
            ownerMap.put("fieldOwner", f.getOwner());
            ownerMap.put("methodName", m == null ? null:m.getSignature());
            ownerMap.put("methodOwner", m == null?null:m.getOwner());
        }
        return fieldMethodOwnerMap;
    }

    private static HashMap<String,HashMap<String,String>> getMethodFieldOwnerMap(HashMap<Method, Field> fieldMethodMap) {
        HashMap<String,HashMap<String,String>> fieldMethodOwnerMap = new HashMap<>();
        for(Method m: fieldMethodMap.keySet()) {
            Field f = fieldMethodMap.get(m);
            HashMap<String, String> ownerMap = new HashMap<>();
            fieldMethodOwnerMap.put(m.getSignature(), ownerMap);
            ownerMap.put("methodName", m.getName());
            ownerMap.put("methodOwner", m.getOwner());
            ownerMap.put("fieldName", f == null ? null:f.getName());
            ownerMap.put("fieldOwner", f == null?null:f.getOwner());
        }
        return fieldMethodOwnerMap;
    }

    private static HashMap<String,HashMap<String,String>> getMethodMethodOwnerMap(HashMap<Method, Method> fieldMethodMap) {
        HashMap<String,HashMap<String,String>> methodMethodOwnerMap = new HashMap<>();
        for(Method m: fieldMethodMap.keySet()) {
            Method mv = fieldMethodMap.get(m);
            HashMap<String, String> ownerMap = new HashMap<>();
            methodMethodOwnerMap.put(m.getSignature(), ownerMap);
            ownerMap.put("methodName", m.getName());
            ownerMap.put("methodOwner", m.getOwner());
            ownerMap.put("methodName2", mv == null ? null:mv.getSignature());
            ownerMap.put("methodOwner2", mv == null?null:mv.getOwner());
        }
        return methodMethodOwnerMap;
    }

    private static HashMap<String,String> getFieldsOwnerMap(HashSet<Field> fields) {
        HashMap<String,String> fieldOwnerMap = new HashMap<>();
        for(Field f: fields) {
            fieldOwnerMap.put(f.getName(), f.getOwner());
        }
        return fieldOwnerMap;
    }

    private static HashMap<String,String> getMethodOwnerMap(HashSet<Method> methods) {
        HashMap<String,String> methodOwnerMap = new HashMap<>();
        for(Method m: methods) {
            methodOwnerMap.put(m.getSignature(), m.getOwner());
        }
        return methodOwnerMap;
    }

    private static void setEdgeAttrs(UMLRelationship relationship, String key, Attribute attribute, HashMap<String, UMLClass> vertices) {
        if(relationship == null)
            return;
        switch (key) {
            case "type":
                relationship.type = UMLRelationship.Type.valueOf(attribute.getValue());
                break;
            case "from":
                if (vertices.containsKey(attribute.getValue()))
                    relationship.from = vertices.get(attribute.getValue());
                break;
            case "to":
                if (vertices.containsKey(attribute.getValue()))
                    relationship.to = vertices.get(attribute.getValue());
                break;
            case "fieldAssociation":
                HashMap<String,String> fieldOwnerMap = (HashMap<String,String>) new Gson().fromJson(attribute.getValue(), HashMap.class);
                relationship.fieldAssociation = new HashSet<>(getFields(fieldOwnerMap, vertices));
                break;
            case "classAccessors":
                HashMap<String,String> classOwnerMap = (HashMap<String,String>) new Gson().fromJson(attribute.getValue(), HashMap.class);
                relationship.classAccessors = new HashSet<>(getMethods(classOwnerMap, vertices));
                break;
            case "accessedBy":
                HashMap<String,StringMap> accessedBy = (HashMap<String,StringMap>) new Gson().fromJson(attribute.getValue(), HashMap.class);
                relationship.accessedBy = getFieldMethodMap(accessedBy, vertices);
                break;
            case "accessing":
                HashMap<String,StringMap> accessing = (HashMap<String,StringMap>) new Gson().fromJson(attribute.getValue(), HashMap.class);
                relationship.accessing = getMethodFieldMap(accessing, vertices);
                break;
            case "calledBy":
                HashMap<String,StringMap> calledBy = (HashMap<String,StringMap>) new Gson().fromJson(attribute.getValue(), HashMap.class);
                relationship.calledBy = getMethodMethodMap(calledBy, vertices);
                break;
            case "callTo":
                HashMap<String,StringMap> callTo = (HashMap<String,StringMap>) new Gson().fromJson(attribute.getValue(), HashMap.class);
                relationship.callTo = getMethodMethodMap(callTo, vertices);
                break;
        }
    }

    private static HashMap<Field, Method> getFieldMethodMap(HashMap<String, StringMap> accessedBy, HashMap<String, UMLClass> vertices) {
        HashMap<Field, Method> fieldMethodHashMap = new HashMap<>();
        for(String fieldName : accessedBy.keySet()) {
            StringMap ownerMap = accessedBy.get(fieldName);
            String fieldOwner = (String) ownerMap.get("fieldOwner");
            String methodName = (String) ownerMap.getOrDefault("methodName", null);
            String methodOwner = (String) ownerMap.getOrDefault("methodOwner", null);
            if (!vertices.containsKey(fieldOwner)) {
                continue;
            }
            UMLClass fieldOwnerObj = vertices.get(fieldOwner);
            Field field = null;
            for(Field f : fieldOwnerObj.getFields()) {
                if(f.getName().equals(fieldName)) {
                    field = f;
                    break;
                }
            }
            if(field == null) {
                continue;
            }

            UMLClass methodOwnerObj = null;
            if (vertices.containsKey(methodOwner)) {
                methodOwnerObj = vertices.get(methodOwner);
            }
            Method method = null;
            if(methodName != null && methodOwnerObj != null) {
                for(Method m : methodOwnerObj.getMethods()) {
                    if(m.getSignature().equals(methodName)) {
                        method = m;
                        break;
                    }
                }
            }
            fieldMethodHashMap.put(field, method);
        }
        return fieldMethodHashMap;
    }


    private static HashMap<Method, Field> getMethodFieldMap(HashMap<String, StringMap> map, HashMap<String, UMLClass> vertices) {
        HashMap<Method, Field> methodFieldHashMap = new HashMap<>();
        for(String methodName : map.keySet()) {
            StringMap ownerMap = map.get(methodName);
            String methodOwner = (String) ownerMap.get("methodOwner");
            String fieldName = (String) ownerMap.getOrDefault("fieldName", null);
            String fieldOwner = (String) ownerMap.getOrDefault("fieldOwner", null);
            if (!vertices.containsKey(methodOwner)) {
                continue;
            }
            UMLClass methodOwnerObj = vertices.get(methodOwner);
            Method method = null;
            for(Method m : methodOwnerObj.getMethods()) {
                if(m.getSignature().equals(methodName)) {
                    method = m;
                    break;
                }
            }
            if(method == null) {
                continue;
            }

            UMLClass fieldOwnerObj = null;
            if (vertices.containsKey(fieldOwner)) {
                fieldOwnerObj = vertices.get(fieldOwner);
            }
            Field field = null;
            if(fieldName != null && fieldOwnerObj != null) {
                for(Field f : fieldOwnerObj.getFields()) {
                    if(f.getName().equals(fieldName)) {
                        field = f;
                        break;
                    }
                }
            }
            methodFieldHashMap.put(method, field);
        }
        return methodFieldHashMap;
    }


    private static HashMap<Method, Method> getMethodMethodMap(HashMap<String, StringMap> map, HashMap<String, UMLClass> vertices) {
        HashMap<Method, Method> methodMethodHashMap = new HashMap<>();
        for(String methodName : map.keySet()) {
            StringMap ownerMap = map.get(methodName);
            String methodOwner = (String) ownerMap.get("methodOwner");
            String methodName2 = (String) ownerMap.getOrDefault("methodName2", null);
            String methodOwner2 = (String) ownerMap.getOrDefault("methodOwner2", null);
            if (!vertices.containsKey(methodOwner)) {
                continue;
            }
            UMLClass methodOwnerObj = vertices.get(methodOwner);
            Method method = null;
            for(Method m : methodOwnerObj.getMethods()) {
                if(m.getSignature().equals(methodName)) {
                    method = m;
                    break;
                }
            }
            if(method == null) {
                continue;
            }

            UMLClass methodOwnerObj2 = null;
            if (vertices.containsKey(methodOwner2)) {
                methodOwnerObj2 = vertices.get(methodOwner2);
            }
            Method method2 = null;
            if(methodName2 != null && methodOwnerObj2 != null) {
                for(Method m2 : methodOwnerObj2.getMethods()) {
                    if(m2.getSignature().equals(methodName2)) {
                        method2 = m2;
                        break;
                    }
                }
            }
            methodMethodHashMap.put(method, method2);
        }
        return methodMethodHashMap;
    }

    private static List<Field> getFields(HashMap<String,String> map, HashMap<String, UMLClass> vertices) {
        List<Field> fields = new ArrayList<>();
        for (String name : map.keySet()) {
            String ownerName = map.get(name);
            if (!vertices.containsKey(ownerName)) {
                continue;
            }
            UMLClass obj = vertices.get(ownerName);
            Field field = null;
            for(Field f : obj.getFields()) {
                if(f.getName().equals(name)) {
                    field = f;
                    break;
                }
            }
            if(field != null)
                fields.add(field);
        }
        return fields;
    }

    private static List<Field> parseFields(List<StringMap> map, UMLClass obj, HashMap<String, UMLClass> vertices) {
        List<Field> fields = new ArrayList<>();
        for(StringMap fmap : map) {
            String ownerName = (String) fmap.getOrDefault("owner", null);
            if(obj == null && !vertices.containsKey(ownerName))
                continue;
            if(obj == null)
                obj = vertices.get(ownerName);
            String name = (String) fmap.getOrDefault("name", null);
            Field field = null;
            for(Field f : obj.getFields()) {
                if(f.getName().equals(name)) {
                    field = f;
                    break;
                }
            }
            if(field == null) {
                field = new Field(name);
                field.setName(name);
                field.setOwner(obj.toString());
                field.setBaseTypeName((String) fmap.getOrDefault("baseTypeName", null));
                field.setPrimitiveType((String) fmap.getOrDefault("primitiveType", null));
                field.setTypeName((String) fmap.getOrDefault("typeName", null));
                field.setTypePackageName((String) fmap.getOrDefault("typePackageName", null));
                field.setLine(((Double) fmap.getOrDefault("line", 0)).intValue());
                List<String> mods = (List<String>) fmap.getOrDefault("modifiers", null);
                List<Modifier> modifiers = new ArrayList<>();
                if (modifiers != null) {
                    for (String mod : mods) {
                        modifiers.add(Modifier.valueOf(mod));
                    }
                }
                field.setModifiers(modifiers);
                List<String> accessMods = (List<String>) fmap.getOrDefault("accessModifiers", null);
                List<AccessModifier> accessModifiers = new ArrayList<>();
                if (accessModifiers != null) {
                    for (String accessMod : accessMods) {
                        accessModifiers.add(AccessModifier.valueOf(accessMod));
                    }
                }
                field.setAccessModifiers(accessModifiers);
                field.setArray((boolean) fmap.getOrDefault("isArray", false));
            }
            fields.add(field);
        }
        return fields;
    }

    private static List<Method> getMethods(HashMap<String,String> map, HashMap<String, UMLClass> vertices) {
        List<Method> methods = new ArrayList<>();
        for (String name : map.keySet()) {
            String ownerName = map.getOrDefault(name, null);
            if (!vertices.containsKey(ownerName))
                continue;
            UMLClass obj = vertices.get(ownerName);
            Method method = null;
            for (Method m : obj.getMethods()) {
                if (m.getSignature().equals(name)) {
                    method = m;
                    break;
                }
            }
            if(method != null)
                methods.add(method);
        }
        return methods;
    }

    private static List<Method> parseMethods(List<StringMap> map, UMLClass obj, HashMap<String, UMLClass> vertices) {
        List<Method> methods = new ArrayList<>();
        for(StringMap mmap : map) {
            String ownerName = (String) mmap.getOrDefault("owner", null);
            if(obj == null && !vertices.containsKey(ownerName))
                continue;
            if(obj == null)
                obj = vertices.get(ownerName);
            String name = (String) mmap.getOrDefault("name", null);
            Method method = null;
            for(Method m : obj.getMethods()) {
                if(m.getName().equals(name)) {
                    method = m;
                    break;
                }
            }
            if(method == null) {
                method = new Method(name);
                method.setName(name);
                method.setOwner(obj.toString());
                method.setLine(((Double) mmap.getOrDefault("line", 0)).intValue());
                method.setReturnPrimitiveType((String) mmap.getOrDefault("returnPrimitiveType", null));
                method.setReturnTypeName((String) mmap.getOrDefault("returnTypeName", null));
                method.setReturnTypePackageName((String) mmap.getOrDefault("returnTypePackageName", null));
                List<String> mods = (List<String>) mmap.getOrDefault("modifiers", null);
                List<Modifier> modifiers = new ArrayList<>();
                if (mods != null) {
                    for (String mod : mods) {
                        modifiers.add(Modifier.valueOf(mod));
                    }
                }
                method.setModifiers(modifiers);

                List<String> accessMods = (List<String>) mmap.getOrDefault("accessModifiers", null);
                List<AccessModifier> accessModifiers = new ArrayList<>();
                if (accessMods != null) {
                    for (String accessMod : accessMods) {
                        accessModifiers.add(AccessModifier.valueOf(accessMod));
                    }
                }
                method.setAccessModifiers(accessModifiers);

                List<StringMap<String>> params = (List<StringMap<String>>) mmap.getOrDefault("parameters", null);
                List<Parameter> parameters = new ArrayList<>();
                if (params != null) {
                    for (StringMap<String> param : params) {
                        Parameter parameter = parseParameter(param);
                        parameters.add(parameter);
                    }
                }
                method.setParameters(parameters);
            }
            methods.add(method);
        }
        return methods;
    }

    private static Parameter parseParameter(StringMap map) {
        Parameter parameter = new Parameter((String) map.get("name"));
        parameter.setPrimitiveType((String) map.getOrDefault("primitiveType", null));
        parameter.setTypeName((String) map.getOrDefault("typeName", null));
        parameter.setTypePackageName((String) map.getOrDefault("typePackageName", null));
        List<String> modifiers = (List<String>) map.getOrDefault("modifiers", null);
        if (modifiers != null) {
            for (String modifier : modifiers) {
                parameter.modifiers.add(Modifier.valueOf(modifier));
            }
        }
        parameter.setArray((boolean) map.getOrDefault("isArray", false));
        return parameter;
    }

    private static void registerVertexAttrs(GraphMLExporter<UMLClass, UMLRelationship> exporter) {
        exporter.registerAttribute("x", GraphMLExporter.AttributeCategory.NODE, AttributeType.DOUBLE);
        exporter.registerAttribute("y", GraphMLExporter.AttributeCategory.NODE, AttributeType.DOUBLE);
        exporter.registerAttribute("line", GraphMLExporter.AttributeCategory.NODE, AttributeType.INT);
        exporter.registerAttribute("filePath", GraphMLExporter.AttributeCategory.NODE, AttributeType.STRING);
        exporter.registerAttribute("packageName", GraphMLExporter.AttributeCategory.NODE, AttributeType.STRING);
        exporter.registerAttribute("fields", GraphMLExporter.AttributeCategory.NODE, AttributeType.STRING);
        exporter.registerAttribute("methods", GraphMLExporter.AttributeCategory.NODE, AttributeType.STRING);
        exporter.registerAttribute("compact", GraphMLExporter.AttributeCategory.NODE, AttributeType.BOOLEAN);
    }

    private static void setVertexAttrs(UMLClass obj, String key, Attribute attribute, Map<UMLClass, Point2D.Double> vertexPositions, HashMap<String, UMLClass> vertices) {
        if(!vertices.containsKey(obj.toString())){
            vertices.put(obj.toString(), obj);
        }
        Point2D.Double point;
        switch (key) {
            case "x":
                point = vertexPositions.getOrDefault(obj, new Point2D.Double());
                point.x = Double.parseDouble(attribute.getValue());
                vertexPositions.put(obj, point);
                break;
            case "y":
                point = vertexPositions.getOrDefault(obj, new Point2D.Double());
                point.y = Double.parseDouble(attribute.getValue());
                vertexPositions.put(obj, point);
                break;
            case "line":
                obj.setLine(Integer.parseInt(attribute.getValue()));
                break;
            case "filePath":
                obj.setFilePath(attribute.getValue());
                break;
            case "packageName":
                obj.setPackageName(attribute.getValue());
                break;
            case "fields":
                obj.setFields(parseFields((List<StringMap>) new Gson().fromJson(attribute.getValue(), List.class), obj, vertices));
                break;
            case "methods":
                obj.setMethods(parseMethods((List<StringMap>) new Gson().fromJson(attribute.getValue(), List.class), obj, vertices));
                break;
            case "compact":
                obj.setCompact(Boolean.parseBoolean(attribute.getValue()));
                break;
        }
    }

    private static Map<String, Attribute> getVertexAttrs(UMLClass obj, Map<UMLClass, org.jungrapht.visualization.layout.model.Point> vertexPositions) {
        HashMap<String, Attribute> map = new HashMap<>();
        org.jungrapht.visualization.layout.model.Point point = vertexPositions.getOrDefault(obj, null);
        if (point != null) {
            map.put("x", new DefaultAttribute<>(point.x, AttributeType.DOUBLE));
            map.put("y", new DefaultAttribute<>(point.y, AttributeType.DOUBLE));
        }
        map.put("line", new DefaultAttribute<>(obj.getLine(), AttributeType.INT));
        map.put("filePath", new DefaultAttribute<>(obj.getFilePath(), AttributeType.STRING));
        map.put("packageName", new DefaultAttribute<>(obj.getPackageName(), AttributeType.STRING));

        map.put("fields", new DefaultAttribute<>(new Gson().toJson(obj.getFields()), AttributeType.STRING));
        map.put("methods", new DefaultAttribute<>(new Gson().toJson(obj.getMethods()), AttributeType.STRING));
        map.put("compact", new DefaultAttribute<>(obj.isCompact(), AttributeType.BOOLEAN));
        return map;
    }


    private static void setupFolder(File sourceFolder) {
        ReflectionTypeSolver reflectionTypeSolver = new ReflectionTypeSolver();
        JavaParserTypeSolver javaParserTypeSolver = new JavaParserTypeSolver(sourceFolder);
        CombinedTypeSolver combinedSolver = new CombinedTypeSolver();
        combinedSolver.add(reflectionTypeSolver);
        combinedSolver.add(javaParserTypeSolver);

        ParserConfiguration parserConfiguration = new ParserConfiguration()
                .setSymbolResolver(new JavaSymbolSolver(combinedSolver));
        StaticJavaParser.setConfiguration(parserConfiguration);
    }

    private static void saveImage(File file, BufferedImage image) {
        try {
            ImageIO.write(image,"png", file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
