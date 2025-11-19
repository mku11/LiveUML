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
import com.google.gson.internal.StringMap;
import com.mku.liveuml.model.diagram.UMLDiagram;
import com.mku.liveuml.model.diagram.UMLClass;
import com.mku.liveuml.model.diagram.UMLClassFactory;
import com.mku.liveuml.model.diagram.UMLRelationship;
import com.mku.liveuml.model.diagram.UMLRelationshipType;
import com.mku.liveuml.model.entities.*;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.graphml.GraphMLImporter;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.*;

@SuppressWarnings({"rawtypes", "unchecked"})
public class Importer {
    public void importGraph(File file, UMLDiagram diagram, HashMap<UMLClass, Point2D.Double> verticesPositions) {
        GraphMLImporter<UMLClass, UMLRelationship> importer = new GraphMLImporter<>();
        importer.setSchemaValidation(false);

        importer.setVertexFactory(UMLClassFactory::create);
        HashMap<String, UMLClass> vertices = new HashMap<>();
        importer.addVertexAttributeConsumer((pair, attribute) -> setVertexAttrs(pair.getFirst(),
                pair.getSecond(), attribute, verticesPositions, vertices));
        // TODO: remove this since we update the relationships from the vertices attrs
        importer.addEdgeAttributeConsumer((pair, attribute) -> setEdgeAttrs(pair.getFirst(),
                pair.getSecond(), attribute, vertices));

        try (InputStreamReader inputStreamReader = new FileReader(file)) {
            diagram.createGraph();
            importer.importGraph(diagram.getGraph(), inputStreamReader);
            diagram.setClasses(diagram.getGraph().vertexSet());
            HashSet<String> sources = new HashSet<>();
            for (UMLClass object : diagram.getGraph().vertexSet()) {
                if (object.getFileSource() != null && !object.getFileSource().equals("null"))
                    sources.add(object.getFileSource());
            }
            diagram.setSources(sources);
            diagram.updateVertices(vertices);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private void setVertexAttrs(UMLClass obj, String key, Attribute attribute, Map<UMLClass, Point2D.Double> vertexPositions, HashMap<String, UMLClass> vertices) {
        if (!vertices.containsKey(obj.toString())) {
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
            case "fileSource":
                obj.setFileSource(attribute.getValue());
                break;
            case "packageName":
                obj.setPackageName(attribute.getValue());
                break;
            case "parents":
                obj.setParents((List<String>) new Gson().fromJson(attribute.getValue(), List.class));
                break;
            case "fields":
                obj.setFields(parseFields((List<StringMap>) new Gson().fromJson(attribute.getValue(), List.class), obj, vertices));
                break;
            case "enumConstants":
                obj.setEnumConstants(parseEnumConsts((List<StringMap>) new Gson().fromJson(attribute.getValue(), List.class), obj, vertices));
                break;
            case "methods":
                obj.setMethods(parseMethods((List<StringMap>) new Gson().fromJson(attribute.getValue(), List.class), obj, vertices));
                break;
            case "compact":
                obj.setCompact(Boolean.parseBoolean(attribute.getValue()));
                break;
            case "modifiers":
                obj.setModifiers(parseModifiers((List<String>) new Gson().fromJson(attribute.getValue(), List.class), obj, vertices));
                break;
            case "accessModifiers":
                obj.setAccessModifiers(parseAccessModifiers((List<String>) new Gson().fromJson(attribute.getValue(), List.class), obj, vertices));
                break;
            case "typeParameters":
                obj.setTypeParameters(parseTypeParameters((List<StringMap>) new Gson().fromJson(attribute.getValue(), List.class), obj, vertices));
                break;
        }
    }

    private void setEdgeAttrs(UMLRelationship relationship, String key, Attribute attribute, HashMap<String, UMLClass> vertices) {
        if (relationship == null)
            return;
        switch (key) {
            case "type":
                relationship.setType(UMLRelationshipType.valueOf(attribute.getValue()));
                break;
            case "from":
                if (vertices.containsKey(attribute.getValue()))
                    relationship.setFrom(vertices.get(attribute.getValue()));
                break;
            case "to":
                if (vertices.containsKey(attribute.getValue()))
                    relationship.setTo(vertices.get(attribute.getValue()));
                break;
            case "fieldsAccessingClass":
                HashMap<String, String> fieldOwnerMap = (HashMap<String, String>) new Gson().fromJson(attribute.getValue(), HashMap.class);
                relationship.setFieldsAccessingClass(new HashSet<>(getFields(fieldOwnerMap, vertices)));
                break;
            case "methodsAccessingClass":
                HashMap<String, String> classOwnerMap = (HashMap<String, String>) new Gson().fromJson(attribute.getValue(), HashMap.class);
                relationship.setMethodsAccessingClass(new HashSet<>(getMethods(classOwnerMap, vertices)));
                break;

            case "fieldsAccessedByMethods":
                HashMap<String, List<StringMap>> accessedBy = (HashMap<String, List<StringMap>>) new Gson().fromJson(attribute.getValue(), HashMap.class);
                relationship.setFieldsAccessedByMethods(getFieldMethodMap(accessedBy, vertices));
                break;
            case "methodsAccessingFields":
                HashMap<String, List<StringMap>> accessing = (HashMap<String, List<StringMap>>) new Gson().fromJson(attribute.getValue(), HashMap.class);
                relationship.setMethodsAccessingFields(getMethodFieldMap(accessing, vertices));
                break;

            case "enumsAccessedByMethods":
                HashMap<String, List<StringMap>> accessedEnumConstsBy = (HashMap<String, List<StringMap>>) new Gson().fromJson(attribute.getValue(), HashMap.class);
                relationship.setEnumsAccessedByMethods(getEnumConstMethodMap(accessedEnumConstsBy, vertices));
                break;
            case "methodsAccessingEnums":
                HashMap<String, List<StringMap>> accessingEnumConsts = (HashMap<String, List<StringMap>>) new Gson().fromJson(attribute.getValue(), HashMap.class);
                relationship.setMethodsAccessingEnums(getMethodEnumConstMap(accessingEnumConsts, vertices));
                break;

            case "methodsAccessedByMethods":
                HashMap<String, List<StringMap>> calledBy = (HashMap<String, List<StringMap>>) new Gson().fromJson(attribute.getValue(), HashMap.class);
                relationship.setMethodsAccessedByMethods(getMethodMethodMap(calledBy, vertices));
                break;
            case "methodsAccesingMethods":
                HashMap<String, List<StringMap>> callTo = (HashMap<String, List<StringMap>>) new Gson().fromJson(attribute.getValue(), HashMap.class);
                relationship.setMethodsAccesingMethods(getMethodMethodMap(callTo, vertices));
                break;
        }
    }


    private static HashMap<Field, HashSet<Method>> getFieldMethodMap(HashMap<String, List<StringMap>> accessedBy,
                                                                     HashMap<String, UMLClass> vertices) {
        HashMap<Field, HashSet<Method>> fieldMethodHashMap = new HashMap<>();
        for (String fieldName : accessedBy.keySet()) {
            HashSet<Method> methods = new HashSet<>();
            Field field = null;
            for (StringMap ownerMap : accessedBy.get(fieldName)) {
                String fieldOwner = (String) ownerMap.get("fieldOwner");
                String methodName = (String) ownerMap.getOrDefault("methodName", null);
                String methodOwner = (String) ownerMap.getOrDefault("methodOwner", null);

                // key field
                if (field == null) {
                    if (!vertices.containsKey(fieldOwner)) {
                        continue;
                    }
                    UMLClass fieldOwnerObj = vertices.get(fieldOwner);

                    for (Field f : fieldOwnerObj.getFields()) {
                        if (f.getName().equals(fieldName)) {
                            field = f;
                            break;
                        }
                    }
                    if (field == null) {
                        continue;
                    }
                }

                // value method
                UMLClass methodOwnerObj = null;
                if (vertices.containsKey(methodOwner)) {
                    methodOwnerObj = vertices.get(methodOwner);
                }
                Method method = null;
                if (methodName != null && methodOwnerObj != null) {
                    for (Method m : methodOwnerObj.getMethods()) {
                        if (m.getSignature().equals(methodName)) {
                            method = m;
                            break;
                        }
                    }
                }
                methods.add(method);
            }
            fieldMethodHashMap.put(field, methods);
        }
        return fieldMethodHashMap;
    }


    private static HashMap<Method, HashSet<Field>> getMethodFieldMap(HashMap<String, List<StringMap>> map,
                                                                     HashMap<String, UMLClass> vertices) {
        HashMap<Method, HashSet<Field>> methodFieldHashMap = new HashMap<>();
        for (String methodName : map.keySet()) {
            HashSet<Field> fields = new HashSet<>();
            Method method = null;
            for (StringMap ownerMap : map.get(methodName)) {
                String methodOwner = (String) ownerMap.get("methodOwner");
                String fieldName = (String) ownerMap.getOrDefault("fieldName", null);
                String fieldOwner = (String) ownerMap.getOrDefault("fieldOwner", null);

                // key method
                if (method == null) {
                    if (!vertices.containsKey(methodOwner)) {
                        continue;
                    }
                    UMLClass methodOwnerObj = vertices.get(methodOwner);
                    for (Method m : methodOwnerObj.getMethods()) {
                        if (m.getSignature().equals(methodName)) {
                            method = m;
                            break;
                        }
                    }
                    if (method == null)
                        continue;
                }

                // fields methods
                UMLClass fieldOwnerObj = null;
                if (vertices.containsKey(fieldOwner)) {
                    fieldOwnerObj = vertices.get(fieldOwner);
                }
                Field field = null;
                if (fieldName != null && fieldOwnerObj != null) {
                    for (Field f : fieldOwnerObj.getFields()) {
                        if (f.getName().equals(fieldName)) {
                            field = f;
                            break;
                        }
                    }
                }
                fields.add(field);
            }
            methodFieldHashMap.put(method, fields);
        }
        return methodFieldHashMap;
    }


    private static HashMap<EnumConstant, HashSet<Method>> getEnumConstMethodMap(HashMap<String, List<StringMap>> accessedBy,
                                                                                HashMap<String, UMLClass> vertices) {
        HashMap<EnumConstant, HashSet<Method>> enumConstMethodHashMap = new HashMap<>();
        for (String enumConstName : accessedBy.keySet()) {
            HashSet<Method> methods = new HashSet<>();
            EnumConstant enumConstant = null;

            for (StringMap ownerMap : accessedBy.get(enumConstName)) {
                String enumConstOwner = (String) ownerMap.get("enumConstOwner");
                String methodName = (String) ownerMap.getOrDefault("methodName", null);
                String methodOwner = (String) ownerMap.getOrDefault("methodOwner", null);
                if (enumConstant == null) {
                    if (!vertices.containsKey(enumConstOwner)) {
                        continue;
                    }
                    UMLClass enumConstOwnerObj = vertices.get(enumConstOwner);
                    for (EnumConstant ec : enumConstOwnerObj.getEnumConstants()) {
                        if (ec.getName().equals(enumConstName)) {
                            enumConstant = ec;
                            break;
                        }
                    }
                    if (enumConstant == null)
                        continue;
                }

                // value methods
                UMLClass methodOwnerObj = null;
                if (vertices.containsKey(methodOwner)) {
                    methodOwnerObj = vertices.get(methodOwner);
                }
                Method method = null;
                if (methodName != null && methodOwnerObj != null) {
                    for (Method m : methodOwnerObj.getMethods()) {
                        if (m.getSignature().equals(methodName)) {
                            method = m;
                            break;
                        }
                    }
                }
                methods.add(method);
            }
            enumConstMethodHashMap.put(enumConstant, methods);
        }
        return enumConstMethodHashMap;
    }

    private static HashMap<Method, HashSet<EnumConstant>> getMethodEnumConstMap(HashMap<String, List<StringMap>> map,
                                                                                HashMap<String, UMLClass> vertices) {
        HashMap<Method, HashSet<EnumConstant>> methodFieldHashMap = new HashMap<>();
        for (String methodName : map.keySet()) {
            HashSet<EnumConstant> enumConstants = new HashSet<>();
            Method method = null;
            for (StringMap ownerMap : map.get(methodName)) {
                String methodOwner = (String) ownerMap.get("methodOwner");
                String enumConstName = (String) ownerMap.getOrDefault("enumConstName", null);
                String enumConstOwner = (String) ownerMap.getOrDefault("enumConstOwner", null);

                // key method
                if (method == null) {
                    if (!vertices.containsKey(methodOwner)) {
                        continue;
                    }
                    UMLClass methodOwnerObj = vertices.get(methodOwner);

                    for (Method m : methodOwnerObj.getMethods()) {
                        if (m.getSignature().equals(methodName)) {
                            method = m;
                            break;
                        }
                    }
                    if (method == null) {
                        continue;
                    }
                }

                // value enum constants
                UMLClass enumConstOwnerObj = null;
                if (vertices.containsKey(enumConstOwner)) {
                    enumConstOwnerObj = vertices.get(enumConstOwner);
                }
                EnumConstant enumConst = null;
                if (enumConstName != null && enumConstOwnerObj != null) {
                    for (EnumConstant ec : enumConstOwnerObj.getEnumConstants()) {
                        if (ec.getName().equals(enumConstName)) {
                            enumConst = ec;
                            break;
                        }
                    }
                }
                enumConstants.add(enumConst);
            }
            methodFieldHashMap.put(method, enumConstants);
        }
        return methodFieldHashMap;
    }

    private static HashMap<Method, HashSet<Method>> getMethodMethodMap(HashMap<String, List<StringMap>> map, HashMap<String, UMLClass> vertices) {
        HashMap<Method, HashSet<Method>> methodMethodHashMap = new HashMap<>();
        for (String methodName : map.keySet()) {
            Method method = null;
            HashSet<Method> methods = new HashSet<>();
            for (StringMap ownerMap : map.get(methodName)) {

                // key method
                if (method == null) {
                    String methodOwner = (String) ownerMap.get("methodOwner");
                    if (!vertices.containsKey(methodOwner)) {
                        continue;
                    }
                    UMLClass methodOwnerObj = vertices.get(methodOwner);

                    for (Method m : methodOwnerObj.getMethods()) {
                        if (m.getSignature().equals(methodName)) {
                            method = m;
                            break;
                        }
                    }
                    if (method == null)
                        continue;
                }

                // value method
                String methodName2 = (String) ownerMap.getOrDefault("methodName2", null);
                String methodOwner2 = (String) ownerMap.getOrDefault("methodOwner2", null);
                UMLClass methodOwnerObj2 = null;
                if (vertices.containsKey(methodOwner2)) {
                    methodOwnerObj2 = vertices.get(methodOwner2);
                }
                Method method2 = null;
                if (methodName2 != null && methodOwnerObj2 != null) {
                    for (Method m2 : methodOwnerObj2.getMethods()) {
                        if (m2.getSignature().equals(methodName2)) {
                            method2 = m2;
                            break;
                        }
                    }
                }
                methods.add(method2);
            }
            methodMethodHashMap.put(method, methods);
        }
        return methodMethodHashMap;
    }

    private static List<Field> getFields(HashMap<String, String> map, HashMap<String, UMLClass> vertices) {
        List<Field> fields = new ArrayList<>();
        for (String name : map.keySet()) {
            String ownerName = map.get(name);
            if (!vertices.containsKey(ownerName)) {
                continue;
            }
            UMLClass obj = vertices.get(ownerName);
            Field field = null;
            for (Field f : obj.getFields()) {
                if (f.getName().equals(name)) {
                    field = f;
                    break;
                }
            }
            if (field != null)
                fields.add(field);
        }
        return fields;
    }

    private static List<Field> parseFields(List<StringMap> map, UMLClass obj, HashMap<String, UMLClass> vertices) {
        List<Field> fields = new ArrayList<>();
        for (StringMap fmap : map) {
            String ownerName = (String) fmap.getOrDefault("owner", null);
            if (obj == null && !vertices.containsKey(ownerName))
                continue;
            if (obj == null)
                obj = vertices.get(ownerName);
            String name = (String) fmap.getOrDefault("name", null);
            Field field = null;
            for (Field f : obj.getFields()) {
                if (f.getName().equals(name)) {
                    field = f;
                    break;
                }
            }
            if (field == null) {
                field = new Field(name);
                field.setName(name);
                field.setOwner(obj.toString());
                field.setPrimitiveType((String) fmap.getOrDefault("primitiveType", null));
                field.setTypeName((String) fmap.getOrDefault("typeName", null));
                field.setTypePackageName((String) fmap.getOrDefault("typePackageName", null));
                field.setTypeParents((List<String>) fmap.getOrDefault("typeParents", null));
                field.setLine(((Double) fmap.getOrDefault("line", 0)).intValue());
                List<String> mods = (List<String>) fmap.getOrDefault("modifiers", null);
                List<Modifier> modifiers = new ArrayList<>();
                if (mods != null) {
                    for (String mod : mods) {
                        modifiers.add(Modifier.valueOf(mod));
                    }
                }
                field.setModifiers(modifiers);
                List<String> accessMods = (List<String>) fmap.getOrDefault("accessModifiers", null);
                List<AccessModifier> accessModifiers = new ArrayList<>();
                if (accessMods != null) {
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

    private static List<EnumConstant> parseEnumConsts(List<StringMap> map, UMLClass obj, HashMap<String, UMLClass> vertices) {
        List<EnumConstant> enumConstants = new ArrayList<>();
        for (StringMap fmap : map) {
            String ownerName = (String) fmap.getOrDefault("owner", null);
            if (obj == null && !vertices.containsKey(ownerName))
                continue;
            if (obj == null)
                obj = vertices.get(ownerName);
            String name = (String) fmap.getOrDefault("name", null);
            int num = ((Double) fmap.getOrDefault("num", null)).intValue();
            EnumConstant enumConstant = null;

            for (EnumConstant ec : obj.getEnumConstants()) {
                if (ec.getName().equals(name)) {
                    enumConstant = ec;
                    break;
                }
            }
            if (enumConstant == null) {
                enumConstant = new EnumConstant(name, num);
                enumConstant.setOwner(obj.toString());
            }
            enumConstants.add(enumConstant);
        }
        return enumConstants;
    }

    private static List<Method> getMethods(HashMap<String, String> map, HashMap<String, UMLClass> vertices) {
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
            if (method != null)
                methods.add(method);
        }
        return methods;
    }

    private static List<Method> parseMethods(List<StringMap> list, UMLClass obj, HashMap<String, UMLClass> vertices) {
        List<Method> methods = new ArrayList<>();
        for (StringMap mmap : list) {
            String ownerName = (String) mmap.getOrDefault("owner", null);
            if (obj == null && !vertices.containsKey(ownerName))
                continue;
            if (obj == null)
                obj = vertices.get(ownerName);
            String name = (String) mmap.getOrDefault("name", null);
            Method method = null;
            for (Method m : obj.getMethods()) {
                if (m.getName().equals(name)) {
                    method = m;
                    break;
                }
            }
            if (method == null) {
                method = new Method(name);
                method.setName(name);
                method.setOwner(obj.toString());
                method.setLine(((Double) mmap.getOrDefault("line", 0)).intValue());
                method.setReturnPrimitiveType((String) mmap.getOrDefault("returnPrimitiveType", null));
                method.setReturnTypeName((String) mmap.getOrDefault("returnTypeName", null));
                method.setReturnTypePackageName((String) mmap.getOrDefault("returnTypePackageName", null));
                method.setReturnTypeParents((List<String>) mmap.getOrDefault("returnTypeParents", null));
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
        parameter.setTypeParents((List<String>) map.getOrDefault("typeParents", null));
        List<String> modifiers = (List<String>) map.getOrDefault("modifiers", null);
        if (modifiers != null) {
            List<Modifier> lModifiers = new ArrayList<>();
            for (String modifier : modifiers)
                lModifiers.add(Modifier.valueOf(modifier));
            parameter.setModifiers(lModifiers);
        }
        parameter.setArray((boolean) map.getOrDefault("isArray", false));
        parameter.setTypeVariable((boolean) map.getOrDefault("typeVariable", false));
        parameter.setGeneric((boolean) map.getOrDefault("generic", false));
        parameter.setUpperBound((boolean) map.getOrDefault("upperBound", false));
        parameter.setLowerBound((boolean) map.getOrDefault("lowerBound", false));
        List<String> bounds = (List<String>) map.getOrDefault("bounds", null);
        parameter.setBounds(bounds);
        List<String> boundsFullNames = (List<String>) map.getOrDefault("boundsFullNames", null);
        parameter.setBounds(boundsFullNames);
        return parameter;
    }

    private List<Modifier> parseModifiers(List<String> list, UMLClass obj, HashMap<String, UMLClass> vertices) {
        List<Modifier> typeParameters = new ArrayList<>();
        for (String typeParameter : list) {
            typeParameters.add(Enum.valueOf(Modifier.class, typeParameter));
        }
        return typeParameters;
    }

    private List<AccessModifier> parseAccessModifiers(List<String> list, UMLClass obj, HashMap<String, UMLClass> vertices) {
        List<AccessModifier> typeParameters = new ArrayList<>();
        for (String typeParameter : list) {
            typeParameters.add(Enum.valueOf(AccessModifier.class, typeParameter));
        }
        return typeParameters;
    }

    private List<Parameter> parseTypeParameters(List<StringMap> list, UMLClass obj, HashMap<String, UMLClass> vertices) {
        List<Parameter> typeParameters = new ArrayList<>();
        for (StringMap mmap : list) {
            Parameter parameter = parseParameter(mmap);
            typeParameters.add(parameter);
        }
        return typeParameters;
    }

}
