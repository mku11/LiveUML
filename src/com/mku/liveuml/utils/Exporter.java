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
package com.mku.liveuml.utils;

import com.google.gson.Gson;
import com.mku.liveuml.model.entities.EnumConstant;
import com.mku.liveuml.model.entities.Field;
import com.mku.liveuml.model.entities.Method;
import com.mku.liveuml.model.diagram.UMLDiagram;
import com.mku.liveuml.model.diagram.UMLClass;
import com.mku.liveuml.model.diagram.UMLRelationship;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.AttributeType;
import org.jgrapht.nio.DefaultAttribute;
import org.jgrapht.nio.graphml.GraphMLExporter;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.*;

public class Exporter {
    public void exportGraph(File file, UMLDiagram diagram, Map<UMLClass, Point2D.Double> vertexPositions) {
        GraphMLExporter<UMLClass, UMLRelationship> exporter = new GraphMLExporter<>();

        exporter.setVertexIdProvider(obj -> obj.getClass().getSimpleName() + ":" + obj);
        registerVertexAttrs(exporter);
        exporter.setVertexAttributeProvider(obj -> getVertexAttrs(obj, vertexPositions));

        exporter.setEdgeIdProvider(UMLRelationship::toString);
        registerEdgeAttrs(exporter);
        exporter.setEdgeAttributeProvider(this::getEdgeAttrs);

        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file))) {
            exporter.exportGraph(diagram.getGraph(), writer);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void registerEdgeAttrs(GraphMLExporter<UMLClass, UMLRelationship> exporter) {
        exporter.registerAttribute("type", GraphMLExporter.AttributeCategory.EDGE, AttributeType.STRING);
        exporter.registerAttribute("from", GraphMLExporter.AttributeCategory.EDGE, AttributeType.STRING);
        exporter.registerAttribute("to", GraphMLExporter.AttributeCategory.EDGE, AttributeType.STRING);
        exporter.registerAttribute("methodsAccessedByMethods", GraphMLExporter.AttributeCategory.EDGE, AttributeType.STRING);
        exporter.registerAttribute("methodsAccesingMethods", GraphMLExporter.AttributeCategory.EDGE, AttributeType.STRING);
        exporter.registerAttribute("fieldsAccessedByMethods", GraphMLExporter.AttributeCategory.EDGE, AttributeType.STRING);
        exporter.registerAttribute("methodsAccessingFields", GraphMLExporter.AttributeCategory.EDGE, AttributeType.STRING);
        exporter.registerAttribute("enumsAccessedByMethods", GraphMLExporter.AttributeCategory.EDGE, AttributeType.STRING);
        exporter.registerAttribute("methodsAccessingEnums", GraphMLExporter.AttributeCategory.EDGE, AttributeType.STRING);
        exporter.registerAttribute("fieldsAccessingClass", GraphMLExporter.AttributeCategory.EDGE, AttributeType.STRING);
        exporter.registerAttribute("methodsAccessingClass", GraphMLExporter.AttributeCategory.EDGE, AttributeType.STRING);
    }

    private Map<String, Attribute> getEdgeAttrs(UMLRelationship UMLRelationship) {
        HashMap<String, Attribute> map = new HashMap<>();
        map.put("type", new DefaultAttribute<>(UMLRelationship.getType().name(), AttributeType.STRING));
        map.put("from", new DefaultAttribute<>(UMLRelationship.getFrom().toString(), AttributeType.STRING));
        map.put("to", new DefaultAttribute<>(UMLRelationship.getTo().toString(), AttributeType.STRING));
        map.put("fieldsAccessingClass", new DefaultAttribute<>(new Gson().toJson(getFieldsOwnerMap(UMLRelationship.getFieldsAccessingClass())), AttributeType.STRING));
        map.put("methodsAccessingClass", new DefaultAttribute<>(new Gson().toJson(getMethodOwnerMap(UMLRelationship.getMethodsAccessingClass())), AttributeType.STRING));
        map.put("fieldsAccessedByMethods", new DefaultAttribute<>(new Gson().toJson(getFieldMethodOwnerMap(UMLRelationship.getFieldsAccessedByMethods())), AttributeType.STRING));
        map.put("methodsAccessingFields", new DefaultAttribute<>(new Gson().toJson(getMethodFieldOwnerMap(UMLRelationship.getMethodsAccessingFields())), AttributeType.STRING));
        map.put("enumsAccessedByMethods", new DefaultAttribute<>(new Gson().toJson(getEnumConstMethodOwnerMap(UMLRelationship.getEnumsAccessedByMethods())), AttributeType.STRING));
        map.put("methodsAccessingEnums", new DefaultAttribute<>(new Gson().toJson(getMethodEnumConstOwnerMap(UMLRelationship.getMethodsAccessingEnums())), AttributeType.STRING));
        map.put("methodsAccessedByMethods", new DefaultAttribute<>(new Gson().toJson(getMethodMethodOwnerMap(UMLRelationship.getMethodsAccessedByMethods())), AttributeType.STRING));
        map.put("methodsAccesingMethods", new DefaultAttribute<>(new Gson().toJson(getMethodMethodOwnerMap(UMLRelationship.getMethodsAccesingMethods())), AttributeType.STRING));
        return map;
    }

    private HashMap<String, List<HashMap<String, String>>> getFieldMethodOwnerMap(Map<Field, HashSet<Method>> fieldMethodMap) {
        HashMap<String, List<HashMap<String, String>>> fieldMethodOwnerMap = new HashMap<>();
        for (Field f : fieldMethodMap.keySet()) {
            List<HashMap<String, String>> flist = new ArrayList<>();
            fieldMethodOwnerMap.put(f.getName(), flist);
            for(Method m : fieldMethodMap.get(f)) {
                HashMap<String, String> ownerMap = new HashMap<>();
                flist.add(ownerMap);
                ownerMap.put("fieldOwner", f.getOwner());
                ownerMap.put("methodName", m == null ? null : m.getSignature());
                ownerMap.put("methodOwner", m == null ? null : m.getOwner());
            }
        }
        return fieldMethodOwnerMap;
    }

    private HashMap<String, List<HashMap<String, String>>> getMethodFieldOwnerMap(Map<Method, HashSet<Field>> fieldMethodMap) {
        HashMap<String, List<HashMap<String, String>>> fieldMethodOwnerMap = new HashMap<>();
        for (Method m : fieldMethodMap.keySet()) {
            List<HashMap<String, String>> flist = new ArrayList<>();
            fieldMethodOwnerMap.put(m.getSignature(), flist);
            for(Field f : fieldMethodMap.get(m)) {
                HashMap<String, String> ownerMap = new HashMap<>();
                flist.add(ownerMap);
                ownerMap.put("methodName", m.getName());
                ownerMap.put("methodOwner", m.getOwner());
                ownerMap.put("fieldName", f == null ? null : f.getName());
                ownerMap.put("fieldOwner", f == null ? null : f.getOwner());
            }
        }
        return fieldMethodOwnerMap;
    }


    private HashMap<String, List<HashMap<String, String>>> getEnumConstMethodOwnerMap(Map<EnumConstant, HashSet<Method>> enumConstantMethodHashMap) {
        HashMap<String, List<HashMap<String, String>>> enumConstMethodOwnerMap = new HashMap<>();
        for (EnumConstant ec : enumConstantMethodHashMap.keySet()) {
            List<HashMap<String, String>> mlist = new ArrayList<>();
            enumConstMethodOwnerMap.put(ec.getName(), mlist);
            for(Method m : enumConstantMethodHashMap.get(ec)) {
                HashMap<String, String> ownerMap = new HashMap<>();
                mlist.add(ownerMap);
                ownerMap.put("enumConstOwner", ec.getOwner());
                ownerMap.put("methodName", m == null ? null : m.getSignature());
                ownerMap.put("methodOwner", m == null ? null : m.getOwner());
            }
        }
        return enumConstMethodOwnerMap;
    }

    private HashMap<String, List<HashMap<String, String>>> getMethodEnumConstOwnerMap(Map<Method, HashSet<EnumConstant>> enumConstMethodMap) {
        HashMap<String, List<HashMap<String, String>>> enumConstMethodOwnerMap = new HashMap<>();
        for (Method m : enumConstMethodMap.keySet()) {
            List<HashMap<String, String>> elist = new ArrayList<>();
            enumConstMethodOwnerMap.put(m.getSignature(), elist);
            for (EnumConstant ec : enumConstMethodMap.get(m)) {
                HashMap<String, String> ownerMap = new HashMap<>();
                elist.add(ownerMap);
                ownerMap.put("methodName", m.getName());
                ownerMap.put("methodOwner", m.getOwner());
                ownerMap.put("enumConstName", ec == null ? null : ec.getName());
                ownerMap.put("enumConstOwner", ec == null ? null : ec.getOwner());
            }
        }
        return enumConstMethodOwnerMap;
    }

    private HashMap<String, List<HashMap<String, String>>> getMethodMethodOwnerMap(Map<Method, HashSet<Method>> fieldMethodMap) {
        HashMap<String, List<HashMap<String, String>>> methodMethodOwnerMap = new HashMap<>();
        for (Method m : fieldMethodMap.keySet()) {
            List<HashMap<String, String>> mlist = new ArrayList<>();
            methodMethodOwnerMap.put(m.getSignature(), mlist);
            for (Method mv : fieldMethodMap.get(m)) {
                HashMap<String, String> ownerMap = new HashMap<>();
                mlist.add(ownerMap);
                ownerMap.put("methodName", m.getName());
                ownerMap.put("methodOwner", m.getOwner());
                ownerMap.put("methodName2", mv == null ? null : mv.getSignature());
                ownerMap.put("methodOwner2", mv == null ? null : mv.getOwner());
            }
        }
        return methodMethodOwnerMap;
    }

    private HashMap<String, String> getFieldsOwnerMap(Set<Field> fields) {
        HashMap<String, String> fieldOwnerMap = new HashMap<>();
        for (Field f : fields) {
            fieldOwnerMap.put(f.getName(), f.getOwner());
        }
        return fieldOwnerMap;
    }

    private HashMap<String, String> getMethodOwnerMap(Set<Method> methods) {
        HashMap<String, String> methodOwnerMap = new HashMap<>();
        for (Method m : methods) {
            methodOwnerMap.put(m.getSignature(), m.getOwner());
        }
        return methodOwnerMap;
    }


    private void registerVertexAttrs(GraphMLExporter<UMLClass, UMLRelationship> exporter) {
        exporter.registerAttribute("x", GraphMLExporter.AttributeCategory.NODE, AttributeType.DOUBLE);
        exporter.registerAttribute("y", GraphMLExporter.AttributeCategory.NODE, AttributeType.DOUBLE);
        exporter.registerAttribute("line", GraphMLExporter.AttributeCategory.NODE, AttributeType.INT);
        exporter.registerAttribute("filePath", GraphMLExporter.AttributeCategory.NODE, AttributeType.STRING);
        exporter.registerAttribute("fileSource", GraphMLExporter.AttributeCategory.NODE, AttributeType.STRING);
        exporter.registerAttribute("packageName", GraphMLExporter.AttributeCategory.NODE, AttributeType.STRING);
        exporter.registerAttribute("parents", GraphMLExporter.AttributeCategory.NODE, AttributeType.STRING);
        exporter.registerAttribute("fields", GraphMLExporter.AttributeCategory.NODE, AttributeType.STRING);
        exporter.registerAttribute("methods", GraphMLExporter.AttributeCategory.NODE, AttributeType.STRING);
        exporter.registerAttribute("enumConstants", GraphMLExporter.AttributeCategory.NODE, AttributeType.STRING);
        exporter.registerAttribute("compact", GraphMLExporter.AttributeCategory.NODE, AttributeType.BOOLEAN);
        exporter.registerAttribute("accessModifiers", GraphMLExporter.AttributeCategory.NODE, AttributeType.STRING);
        exporter.registerAttribute("modifiers", GraphMLExporter.AttributeCategory.NODE, AttributeType.STRING);
        exporter.registerAttribute("typeParameters", GraphMLExporter.AttributeCategory.NODE, AttributeType.STRING);
    }

    private Map<String, Attribute> getVertexAttrs(UMLClass obj, Map<UMLClass, Point2D.Double> vertexPositions) {
        HashMap<String, Attribute> map = new HashMap<>();
        Point2D.Double point = vertexPositions.getOrDefault(obj, null);
        if (point != null) {
            map.put("x", new DefaultAttribute<>(point.x, AttributeType.DOUBLE));
            map.put("y", new DefaultAttribute<>(point.y, AttributeType.DOUBLE));
        }
        map.put("line", new DefaultAttribute<>(obj.getLine(), AttributeType.INT));
        map.put("filePath", new DefaultAttribute<>(obj.getFilePath(), AttributeType.STRING));
        map.put("fileSource", new DefaultAttribute<>(obj.getFileSource(), AttributeType.STRING));
        map.put("packageName", new DefaultAttribute<>(obj.getPackageName(), AttributeType.STRING));
        map.put("parents", new DefaultAttribute<>(new Gson().toJson(obj.getParents()), AttributeType.STRING));

        map.put("fields", new DefaultAttribute<>(new Gson().toJson(obj.getFields()), AttributeType.STRING));
        map.put("methods", new DefaultAttribute<>(new Gson().toJson(obj.getMethods()), AttributeType.STRING));
        map.put("enumConstants", new DefaultAttribute<>(new Gson().toJson(obj.getEnumConstants()), AttributeType.STRING));
        map.put("compact", new DefaultAttribute<>(obj.isCompact(), AttributeType.BOOLEAN));

        map.put("modifiers", new DefaultAttribute<>(new Gson().toJson(obj.getModifiers()), AttributeType.STRING));
        map.put("accessModifiers", new DefaultAttribute<>(new Gson().toJson(obj.getAccessModifiers()), AttributeType.STRING));
        map.put("typeParameters", new DefaultAttribute<>(new Gson().toJson(obj.getTypeParameters()), AttributeType.STRING));
        return map;
    }
}
