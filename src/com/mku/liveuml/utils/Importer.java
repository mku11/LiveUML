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
import com.mku.liveuml.entities.*;
import com.mku.liveuml.model.UMLDiagram;
import com.mku.liveuml.model.UMLClass;
import com.mku.liveuml.model.UMLClassFactory;
import com.mku.liveuml.model.UMLRelationship;
import com.mku.liveuml.model.UMLRelationshipType;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.graphml.GraphMLImporter;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.*;

@SuppressWarnings({"rawtypes", "unchecked"})
public class Importer {
    public void importGraph(File file, UMLDiagram generator, HashMap<UMLClass, Point2D.Double> verticesPositions) {
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
            generator.createGraph();
            importer.importGraph(generator.getGraph(), inputStreamReader);
            generator.updateVertices(vertices);
        } catch (Exception ex) {
            ex.printStackTrace();
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
            case "packageName":
                obj.setPackageName(attribute.getValue());
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
        }
    }

    private void setEdgeAttrs(UMLRelationship relationship, String key, Attribute attribute, HashMap<String, UMLClass> vertices) {
        if (relationship == null)
            return;
        switch (key) {
            case "type":
                relationship.type = UMLRelationshipType.valueOf(attribute.getValue());
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
                HashMap<String, String> fieldOwnerMap = (HashMap<String, String>) new Gson().fromJson(attribute.getValue(), HashMap.class);
                relationship.fieldAssociation = new HashSet<>(getFields(fieldOwnerMap, vertices));
                break;
            case "classAccessors":
                HashMap<String, String> classOwnerMap = (HashMap<String, String>) new Gson().fromJson(attribute.getValue(), HashMap.class);
                relationship.classAccessors = new HashSet<>(getMethods(classOwnerMap, vertices));
                break;

            case "accessedBy":
                HashMap<String, StringMap> accessedBy = (HashMap<String, StringMap>) new Gson().fromJson(attribute.getValue(), HashMap.class);
                relationship.accessedFieldsBy = getFieldMethodMap(accessedBy, vertices);
                break;
            case "accessing":
                HashMap<String, StringMap> accessing = (HashMap<String, StringMap>) new Gson().fromJson(attribute.getValue(), HashMap.class);
                relationship.accessingFields = getMethodFieldMap(accessing, vertices);
                break;

            case "accessedEnumConstsBy":
                HashMap<String, StringMap> accessedEnumConstsBy = (HashMap<String, StringMap>) new Gson().fromJson(attribute.getValue(), HashMap.class);
                relationship.accessedEnumConstsBy = getEnumConstMethodMap(accessedEnumConstsBy, vertices);
                break;
            case "accessingEnumConsts":
                HashMap<String, StringMap> accessingEnumConsts = (HashMap<String, StringMap>) new Gson().fromJson(attribute.getValue(), HashMap.class);
                relationship.accessingEnumConsts = getMethodEnumConstMap(accessingEnumConsts, vertices);
                break;

            case "calledBy":
                HashMap<String, StringMap> calledBy = (HashMap<String, StringMap>) new Gson().fromJson(attribute.getValue(), HashMap.class);
                relationship.calledBy = getMethodMethodMap(calledBy, vertices);
                break;
            case "callTo":
                HashMap<String, StringMap> callTo = (HashMap<String, StringMap>) new Gson().fromJson(attribute.getValue(), HashMap.class);
                relationship.callTo = getMethodMethodMap(callTo, vertices);
                break;
        }
    }


    private static HashMap<Field, Method> getFieldMethodMap(HashMap<String, StringMap> accessedBy, HashMap<String, UMLClass> vertices) {
        HashMap<Field, Method> fieldMethodHashMap = new HashMap<>();
        for (String fieldName : accessedBy.keySet()) {
            StringMap ownerMap = accessedBy.get(fieldName);
            String fieldOwner = (String) ownerMap.get("fieldOwner");
            String methodName = (String) ownerMap.getOrDefault("methodName", null);
            String methodOwner = (String) ownerMap.getOrDefault("methodOwner", null);
            if (!vertices.containsKey(fieldOwner)) {
                continue;
            }
            UMLClass fieldOwnerObj = vertices.get(fieldOwner);
            Field field = null;
            for (Field f : fieldOwnerObj.getFields()) {
                if (f.getName().equals(fieldName)) {
                    field = f;
                    break;
                }
            }
            if (field == null) {
                continue;
            }

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
            fieldMethodHashMap.put(field, method);
        }
        return fieldMethodHashMap;
    }


    private static HashMap<Method, Field> getMethodFieldMap(HashMap<String, StringMap> map, HashMap<String, UMLClass> vertices) {
        HashMap<Method, Field> methodFieldHashMap = new HashMap<>();
        for (String methodName : map.keySet()) {
            StringMap ownerMap = map.get(methodName);
            String methodOwner = (String) ownerMap.get("methodOwner");
            String fieldName = (String) ownerMap.getOrDefault("fieldName", null);
            String fieldOwner = (String) ownerMap.getOrDefault("fieldOwner", null);
            if (!vertices.containsKey(methodOwner)) {
                continue;
            }
            UMLClass methodOwnerObj = vertices.get(methodOwner);
            Method method = null;
            for (Method m : methodOwnerObj.getMethods()) {
                if (m.getSignature().equals(methodName)) {
                    method = m;
                    break;
                }
            }
            if (method == null) {
                continue;
            }

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
            methodFieldHashMap.put(method, field);
        }
        return methodFieldHashMap;
    }


    private static HashMap<EnumConstant, Method> getEnumConstMethodMap(HashMap<String, StringMap> accessedBy, HashMap<String, UMLClass> vertices) {
        HashMap<EnumConstant, Method> enumConstMethodHashMap = new HashMap<>();
        for (String enumConstName : accessedBy.keySet()) {
            StringMap ownerMap = accessedBy.get(enumConstName);
            String enumConstOwner = (String) ownerMap.get("enumConstOwner");
            String methodName = (String) ownerMap.getOrDefault("methodName", null);
            String methodOwner = (String) ownerMap.getOrDefault("methodOwner", null);
            if (!vertices.containsKey(enumConstOwner)) {
                continue;
            }
            UMLClass enumConstOwnerObj = vertices.get(enumConstOwner);
            EnumConstant enumConstant = null;
            for (EnumConstant ec : enumConstOwnerObj.getEnumConstants()) {
                if (ec.getName().equals(enumConstName)) {
                    enumConstant = ec;
                    break;
                }
            }
            if (enumConstant == null) {
                continue;
            }

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
            enumConstMethodHashMap.put(enumConstant, method);
        }
        return enumConstMethodHashMap;
    }

    private static HashMap<Method, EnumConstant> getMethodEnumConstMap(HashMap<String, StringMap> map, HashMap<String, UMLClass> vertices) {
        HashMap<Method, EnumConstant> methodFieldHashMap = new HashMap<>();
        for (String methodName : map.keySet()) {
            StringMap ownerMap = map.get(methodName);
            String methodOwner = (String) ownerMap.get("methodOwner");
            String enumConstName = (String) ownerMap.getOrDefault("enumConstName", null);
            String enumConstOwner = (String) ownerMap.getOrDefault("enumConstOwner", null);
            if (!vertices.containsKey(methodOwner)) {
                continue;
            }
            UMLClass methodOwnerObj = vertices.get(methodOwner);
            Method method = null;
            for (Method m : methodOwnerObj.getMethods()) {
                if (m.getSignature().equals(methodName)) {
                    method = m;
                    break;
                }
            }
            if (method == null) {
                continue;
            }

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
            methodFieldHashMap.put(method, enumConst);
        }
        return methodFieldHashMap;
    }

    private static HashMap<Method, Method> getMethodMethodMap(HashMap<String, StringMap> map, HashMap<String, UMLClass> vertices) {
        HashMap<Method, Method> methodMethodHashMap = new HashMap<>();
        for (String methodName : map.keySet()) {
            StringMap ownerMap = map.get(methodName);
            String methodOwner = (String) ownerMap.get("methodOwner");
            String methodName2 = (String) ownerMap.getOrDefault("methodName2", null);
            String methodOwner2 = (String) ownerMap.getOrDefault("methodOwner2", null);
            if (!vertices.containsKey(methodOwner)) {
                continue;
            }
            UMLClass methodOwnerObj = vertices.get(methodOwner);
            Method method = null;
            for (Method m : methodOwnerObj.getMethods()) {
                if (m.getSignature().equals(methodName)) {
                    method = m;
                    break;
                }
            }
            if (method == null) {
                continue;
            }

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
            methodMethodHashMap.put(method, method2);
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
                field.setBaseTypeName((String) fmap.getOrDefault("baseTypeName", null));
                field.setPrimitiveType((String) fmap.getOrDefault("primitiveType", null));
                field.setTypeName((String) fmap.getOrDefault("typeName", null));
                field.setTypePackageName((String) fmap.getOrDefault("typePackageName", null));
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
            if(enumConstant == null) {
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

    private static List<Method> parseMethods(List<StringMap> map, UMLClass obj, HashMap<String, UMLClass> vertices) {
        List<Method> methods = new ArrayList<>();
        for (StringMap mmap : map) {
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

}
