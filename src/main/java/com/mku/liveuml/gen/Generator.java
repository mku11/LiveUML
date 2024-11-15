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
package com.mku.liveuml.gen;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithMembers;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedArrayType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.google.common.base.Strings;
import com.mku.liveuml.graph.UMLRelationship;
import com.mku.liveuml.graph.UMLClass;
import com.mku.liveuml.meta.Parameter;
import com.mku.liveuml.meta.*;
import com.mku.liveuml.meta.Class;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class Generator {
    public HashMap<String, UMLClass> objects = new HashMap<>();

    public List<UMLClass> getClasses(File projectDir) {
        objects.clear();
        List<UMLClass> list = new LinkedList<>();
        getObjects(list, projectDir);
        getMethodCalls(list, projectDir);
        resolveTypes(list);
        return list;
    }

    private void resolveTypes(List<UMLClass> list) {
        for (UMLClass fieldOwner : list) {
            for (Field field : fieldOwner.fields) {
                if (!field.isPrimitiveType()) {
                    String fullName = field.getTypeFullName();
                    if (objects.containsKey(fullName)) {
                        UMLClass fieldType = objects.get(fullName);
                        // we allow loops if it's aggregation or composition
                        createFieldAggregationRelationship(field, fieldOwner, fieldType);
                    }
                }
            }
        }
    }

    private void createFieldAggregationRelationship(Field field, UMLClass fieldOwner, UMLClass fieldType) {
        UMLRelationship.Type relType = UMLRelationship.Type.Aggregation;
        if (field.accessModifiers.contains(Field.AccessModifier.Private) && !hasPublicSetter(field, fieldOwner, fieldType))
            relType = UMLRelationship.Type.Composition;
        UMLRelationship rel = new UMLRelationship(fieldOwner, fieldType, relType);
        String key = rel.toString();
        if (fieldOwner.relationships.containsKey(key)) {
            rel = fieldOwner.relationships.get(key);
        } else {
            fieldOwner.relationships.put(key, rel);
        }
        if (fieldType.relationships.containsKey(key)) {
            rel = fieldType.relationships.get(key);
        } else {
            fieldType.relationships.put(key, rel);
        }
        rel.addFieldAssociation(field);
    }

    // TODO: check the method body it it's setting the specific field
    private boolean hasPublicSetter(Field field, UMLClass fieldOwner, UMLClass fieldType) {
        for (Method method : fieldOwner.methods) {
            // check public constructor param
            if (method.name.equals(fieldOwner.name) && !method.accessModifiers.contains(Method.AccessModifier.Private)) {
                for (Parameter param : method.parameters) {
                    if (param.getTypeName() != null && param.getTypeName().equals(fieldType.name)) {
                        return true;
                    }
                }
            }
            // check public setter
            if (method.name.toLowerCase().startsWith(("set" + field.name).toLowerCase())
                    && !method.accessModifiers.contains(Method.AccessModifier.Private)) {
                for (Parameter param : method.parameters) {
                    if (param.getTypeName() != null && param.getTypeName().equals(fieldType.name)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void getMethodCalls(List<UMLClass> list, File projectDir) {
        new DirExplorer((level, path, file) -> path.endsWith(".java"), (level, path, file) -> {
            System.out.println(path);
            System.out.println(Strings.repeat("=", path.length()));
            try {
                new VoidVisitorAdapter<UMLClass>() {
                    @Override
                    public void visit(MethodCallExpr n, UMLClass arg) {
                        super.visit(n, arg);
                        System.out.println("Method Call: [" + n.getBegin().get().line + "] " + n);
                        UMLClass caller = getMethodCallerObject(n);
                        UMLClass callee = getMethodCalleeObject(n);
                        if (callee != null && caller != null
                            && callee != caller) {
                            Method callerMethod = getMethodCallerMethod(caller, n);
                            Method calleeMethod = getMethodCalleeMethod(callee, n);
                            if (calleeMethod != null) {
                                createMethodCallRelationship(caller, callerMethod, callee, calleeMethod);
                            }
                        }
                    }

                    @Override
                    public void visit(final ObjectCreationExpr n, final UMLClass arg) {
                        System.out.println("ObjectCreationExpr: [" + n.getBegin().get().line + "] " + n);
                        UMLClass caller = getMethodCallerObject(n);
                        UMLClass callee = getMethodCalleeObject(n);
                        if (caller != null && callee != null) {
                            Method callerMethod = getMethodCallerMethod(caller, n);
                            Constructor constructor = getConstructor(callee, n);
                            if(constructor != null) {
                                createObjectCreationRelationship(caller, callerMethod, callee, constructor);
                            }
                            if(constructor == null && getConstructorCount(callee) == 0) {
                                createObjectCreationRelationship(caller, callerMethod, callee);
                            }
                        }
                    }
                    @Override
                    public void visit(final FieldAccessExpr n, final UMLClass arg) {
                        super.visit(n, arg);
                        System.out.println("Field Access: [" + n.getBegin().get().line + "] " + n);
                        UMLClass accessor = getFieldAccessorObject(n);
                        UMLClass accessedFieldObject = getFieldAccessedObject(n);
                        if (accessor != null && accessedFieldObject != null
                            && accessor != accessedFieldObject) {
                            Method accessorMethod = getFieldAccessorMethod(accessor, n);
                            Field accessedField = getFieldAccessed(accessedFieldObject, n);
                            if (accessedField != null) {
                                createFieldAccessRelationship(accessor, accessorMethod, accessedFieldObject, accessedField);
                            }
                        }
                    }

                }.visit(StaticJavaParser.parse(file), null);
                System.out.println(); // empty line
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).explore(projectDir);
    }

    private Method getMethodCallerMethod(UMLClass caller, ObjectCreationExpr n) {
        CallableDeclaration methodDeclaration = null;
        Node node = n;
        while (node.hasParentNode()) {
            node = node.getParentNode().get();
            if (node instanceof MethodDeclaration) {
                methodDeclaration = (MethodDeclaration) node;
                break;
            } else if (node instanceof ConstructorDeclaration) {
                methodDeclaration = (ConstructorDeclaration) node;
                break;
            }
        }
        if(methodDeclaration == null)
            return null;
        for (Method m : caller.methods) {
            if (m.getSignature().equals(methodDeclaration.getSignature().toString())) {
                return m;
            }
        }
        return null;
    }

    private void createObjectCreationRelationship(UMLClass caller, Method callerMethod, UMLClass callee, Constructor constructor) {
        UMLRelationship.Type type = UMLRelationship.Type.Dependency;
        UMLRelationship rel = new UMLRelationship(caller, callee, type);
        String key = rel.toString();
        if (caller.relationships.containsKey(key)) {
            rel = caller.relationships.get(key);
        } else {
            caller.relationships.put(key, rel);
        }
        if (callee.relationships.containsKey(key)) {
            rel = callee.relationships.get(key);
        } else {
            callee.relationships.put(key, rel);
        }
        rel.addMethodCall(callerMethod, constructor);
    }

    private void createObjectCreationRelationship(UMLClass caller, Method callerMethod, UMLClass callee) {
        UMLRelationship.Type type = UMLRelationship.Type.Dependency;
        UMLRelationship rel = new UMLRelationship(caller, callee, type);
        String key = rel.toString();
        if (caller.relationships.containsKey(key)) {
            rel = caller.relationships.get(key);
        } else {
            caller.relationships.put(key, rel);
        }
        if (callee.relationships.containsKey(key)) {
            rel = callee.relationships.get(key);
        } else {
            callee.relationships.put(key, rel);
        }
        rel.addClassAccess(callerMethod);
    }


    private int getConstructorCount(UMLClass obj) {
        int constructors = 0;
        for(Method method : obj.methods) {
            if(method instanceof Constructor) {
                constructors++;
            }
        }
        return constructors;
    }

    private Constructor getConstructor(UMLClass obj, ObjectCreationExpr n) {
        for(Method method : obj.methods) {
            if(method instanceof Constructor) {
                if(method.parameters.size() == n.getArguments().size())
                    return (Constructor) method;
            }
        }
        return null;
    }

    private UMLClass getMethodCallerObject(ObjectCreationExpr n) {
        String packageName = null;
        String name = null;
        Node node = n;
        while (node.hasParentNode()) {
            node = node.getParentNode().get();
            if (node instanceof ClassOrInterfaceDeclaration) {
                name = ((ClassOrInterfaceDeclaration) node).getNameAsString();
            } else if (node instanceof CompilationUnit) {
                packageName = ((CompilationUnit) node).getPackageDeclaration().get().getName().asString();
                break;
            }
        }
        String fullName = packageName + "." + name;
        if (objects.containsKey(fullName)) {
            return objects.get(fullName);
        }
        return null;
    }

    private UMLClass getMethodCalleeObject(ObjectCreationExpr n) {
        try {
            ResolvedReferenceTypeDeclaration decl = n.calculateResolvedType().asReferenceType().getTypeDeclaration().get();
            String fullName = decl.getPackageName() + "." + decl.getClassName();
            if (objects.containsKey(fullName)) {
                return objects.get(fullName);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    private UMLClass getMethodCalleeObject(MethodCallExpr n) {
        if (n.getScope().isEmpty())
            return null;
        try {
            ResolvedType type = n.getScope().get().calculateResolvedType();
            String fullName = null;
            if(type.isReferenceType()) {
                ResolvedReferenceTypeDeclaration decl = type.asReferenceType().getTypeDeclaration().get();
                fullName = decl.getPackageName() + "." + decl.getClassName();
            }
            if (objects.containsKey(fullName)) {
                return objects.get(fullName);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }


    private Method getMethodCalleeMethod(UMLClass callee, MethodCallExpr n) {
        try {
            String methodName = n.getNameAsString();
            for (Method m : callee.methods) {
                if (m.name.equals(methodName) && m.parameters.size() == n.getArguments().size()) {
                    return m;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    private UMLClass getMethodCallerObject(MethodCallExpr n) {
        String packageName = null;
        String name = null;
        Node node = n;
        while (node.hasParentNode()) {
            node = node.getParentNode().get();
            if (node instanceof ClassOrInterfaceDeclaration) {
                name = ((ClassOrInterfaceDeclaration) node).getNameAsString();
            } else if (node instanceof CompilationUnit) {
                packageName = ((CompilationUnit) node).getPackageDeclaration().get().getName().asString();
                break;
            }
        }

        String fullName = packageName + "." + name;
        if (objects.containsKey(fullName)) {
            return objects.get(fullName);
        }
        return null;
    }

    private Method getMethodCallerMethod(UMLClass caller, MethodCallExpr n) {
        CallableDeclaration methodDeclaration = null;
        Node node = n;
        while (node.hasParentNode()) {
            node = node.getParentNode().get();
            if (node instanceof MethodDeclaration) {
                methodDeclaration = (MethodDeclaration) node;
                break;
            } else if (node instanceof ConstructorDeclaration) {
                methodDeclaration = (ConstructorDeclaration) node;
                break;
            }
        }
        if(methodDeclaration == null)
            return null;

        for (Method m : caller.methods) {
            if (m.getSignature().equals(methodDeclaration.getSignature().toString())) {
                return m;
            }
        }
        return null;
    }

    private void createMethodCallRelationship(UMLClass caller, Method callerMethod, UMLClass callee, Method calleeMethod) {
        UMLRelationship.Type type = UMLRelationship.Type.Dependency;
        UMLRelationship rel = new UMLRelationship(caller, callee, type);
        String key = rel.toString();
        if (caller.relationships.containsKey(key)) {
            rel = caller.relationships.get(key);
        } else {
            caller.relationships.put(key, rel);
        }
        if (callee.relationships.containsKey(key)) {
            rel = callee.relationships.get(key);
        } else {
            callee.relationships.put(key, rel);
        }
        rel.addMethodCall(callerMethod, calleeMethod);
    }

    // fields accessors
    private UMLClass getFieldAccessorObject(FieldAccessExpr n) {
        String packageName = null;
        String name = null;
        Node node = n;
        while (node.hasParentNode()) {
            node = node.getParentNode().get();
            if (node instanceof ClassOrInterfaceDeclaration) {
                name = ((ClassOrInterfaceDeclaration) node).getNameAsString();
            } else if (node instanceof CompilationUnit) {
                packageName = ((CompilationUnit) node).getPackageDeclaration().get().getName().asString();
                break;
            }
        }
        String fullName = packageName + "." + name;
        if (objects.containsKey(fullName)) {
            return objects.get(fullName);
        }
        return null;
    }


    private Method getFieldAccessorMethod(UMLClass callee, FieldAccessExpr n) {
        CallableDeclaration methodDeclaration = null;
        Node node = n;
        while (node.hasParentNode()) {
            node = node.getParentNode().get();
            if (node instanceof MethodDeclaration) {
                methodDeclaration = (MethodDeclaration) node;
                break;
            } else if (node instanceof ConstructorDeclaration) {
                methodDeclaration = (ConstructorDeclaration) node;
                break;
            }
        }
        if(methodDeclaration == null)
            return null;

        try {
            for (Method m : callee.methods) {
                if (m.getSignature().equals(methodDeclaration.getSignature().toString())) {
                    return m;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

    private UMLClass getFieldAccessedObject(FieldAccessExpr n) {
        if (n.getScope() == null || n.getScope().toString().equals("java")
                || n.getScope().toString().startsWith("java.")
                || n.getScope().toString().equals("System")
                || n.getScope().toString().startsWith("System."))
            return null;
        try {
            ResolvedType type = n.getScope().calculateResolvedType();
            String fullName = null;
            if(type.isReferenceType()) {
                ResolvedReferenceTypeDeclaration decl = type.asReferenceType().getTypeDeclaration().get();
                fullName = decl.getPackageName() + "." + decl.getClassName();
            }
            if (fullName != null && objects.containsKey(fullName)) {
                return objects.get(fullName);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    private Field getFieldAccessed(UMLClass accessed, FieldAccessExpr n) {
        for (Field f: accessed.fields) {
            if (f.name.equals(n.getNameAsString())) {
                return f;
            }
        }
        return null;
    }

    private void createFieldAccessRelationship(UMLClass accessor, Method accessorMethod, UMLClass accessed, Field accessedField) {
        UMLRelationship.Type type = UMLRelationship.Type.Dependency;
        UMLRelationship rel = new UMLRelationship(accessor, accessed, type);
        String key = rel.toString();
        if (accessor.relationships.containsKey(key)) {
            rel = accessor.relationships.get(key);
        } else {
            accessor.relationships.put(key, rel);
        }
        if (accessed.relationships.containsKey(key)) {
            rel = accessed.relationships.get(key);
        } else {
            accessed.relationships.put(key, rel);
        }
        rel.addFieldAccess(accessorMethod, accessedField);
    }

    private void getObjects(List<UMLClass> list, File projectDir) {
        new DirExplorer((level, path, file) -> path.endsWith(".java"), (level, path, file) -> {
            System.out.println(path);
            System.out.println(Strings.repeat("=", path.length()));
            try {
                new VoidVisitorAdapter<UMLClass>() {
                    @Override
                    public void visit(ClassOrInterfaceDeclaration n, UMLClass arg) {
                        super.visit(n, arg);
                        System.out.println(n.isInterface() ? "Interface" : "Class: " + n.getName());
                        UMLClass obj = parseClassOrInterface(n, file.getPath());
                        list.add(obj);
                    }
                }.visit(StaticJavaParser.parse(file), null);
                System.out.println();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            try {
                new VoidVisitorAdapter<UMLClass>() {
                    @Override
                    public void visit(EnumDeclaration n, UMLClass arg) {
                        super.visit(n, arg);
                        System.out.println("Enum: " + n.getName());
                        UMLClass obj = parseEnum(n, file.getPath());
                        list.add(obj);
                    }
                }.visit(StaticJavaParser.parse(file), null);
                System.out.println(); // empty line
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).explore(projectDir);
    }

    private UMLClass parseEnum(EnumDeclaration n, String filePath) {
        UMLClass obj = new Enumeration(n.getName().asString());
        obj.setFilePath(filePath);
        Node node = n;
        while (node.hasParentNode()) {
            node = node.getParentNode().get();
            if (node instanceof CompilationUnit) {
                obj.packageName = ((CompilationUnit) node).getPackageDeclaration().get().getName().asString();
                break;
            }
        }
        obj.setLine(n.getBegin().get().line);
        obj.fields = parseFields(n, obj);
        obj.methods.addAll(parseConstructors(obj, n));
        obj.methods.addAll(parseMethods(obj, n));
        objects.put(obj.packageName + "." + obj.name, obj);
        return obj;
    }

    private UMLClass parseClassOrInterface(ClassOrInterfaceDeclaration n, String filePath) {
        UMLClass obj = getOrCreateObject(n, filePath);
        Class superClassObj = getSuperClass(n, filePath);
        if (superClassObj != null) {
            UMLRelationship rel = getSuperClassRel(obj, superClassObj);
            obj.relationships.put(rel.toString(), rel);
            superClassObj.relationships.put(rel.toString(), rel);
        }

        List<Interface> interfacesImplemented = getImplementedInterfaces(n, filePath);
        if (interfacesImplemented != null) {
            for (Interface interfaceImplementedObj : interfacesImplemented) {
                UMLRelationship rel = getInterfaceRel(obj, interfaceImplementedObj);
                obj.relationships.put(rel.toString(), rel);
                interfaceImplementedObj.relationships.put(rel.toString(), rel);
            }
        }

        obj.fields = parseFields(n, obj);
        obj.methods.addAll(parseConstructors(obj, n));
        obj.methods.addAll(parseMethods(obj, n));
        return obj;
    }

    private UMLClass getOrCreateObject(ClassOrInterfaceDeclaration n, String filePath) {
        String packageName = getPackageName(n);
        String className = n.getName().asString();
        return getOrCreateObject(packageName, className, n.isInterface(), filePath, n.getBegin().get().line);
    }

    private UMLClass getOrCreateObject(String packageName, String name, boolean isInterface, String filePath,
                                              int line) {
        UMLClass obj = null;
        String fullName = packageName + "." + name;
        if (objects.containsKey(fullName)) {
            obj = objects.get(fullName);
        } else {
            if (isInterface) {
                obj = new Interface(name);
            } else {
                obj = new Class(name);
            }
            objects.put(fullName, obj);
        }
        obj.setFilePath(filePath);
        obj.setLine(line);
        obj.packageName = packageName;
        return obj;
    }

    private UMLRelationship getSuperClassRel(UMLClass derivedClass, UMLClass superClass) {
        return new UMLRelationship(derivedClass, superClass, UMLRelationship.Type.Inheritance);
    }

    private Class getSuperClass(ClassOrInterfaceDeclaration node, String filePath) {
        Class superClassObj = null;
        if (node.getExtendedTypes().size() > 0) {
            ClassOrInterfaceType extType = node.getExtendedTypes(0);
            ResolvedType decl = extType.resolve();
            if (decl.isReferenceType()) {
                ResolvedReferenceTypeDeclaration typeDecl = decl.asReferenceType().getTypeDeclaration().get();
                String superClassPackageName = typeDecl.getPackageName();
                String superClassName = typeDecl.getName();
                String superClassFullName = superClassPackageName + "." + superClassName;
                if (objects.containsKey(superClassFullName)) {
                    superClassObj = (Class) objects.get(superClassFullName);
                } else {
                    superClassObj = new Class(superClassName);
                    superClassObj.setFilePath(filePath);
                    superClassObj.packageName = superClassPackageName;
                    objects.put(superClassFullName, superClassObj);
                }
            }
            return superClassObj;
        }
        return null;
    }


    private UMLRelationship getInterfaceRel(UMLClass derivedClass, UMLClass superClass) {
        return new UMLRelationship(derivedClass, superClass, UMLRelationship.Type.Realization);
    }

    private List<Interface> getImplementedInterfaces(ClassOrInterfaceDeclaration node, String filePath) {
        List<Interface> implementedInterfaces = new ArrayList<>();
        if (node.getImplementedTypes().size() > 0) {
            for (int i = 0; i < node.getImplementedTypes().size(); i++) {
                ClassOrInterfaceType interfaceType = node.getImplementedTypes(i);
                ResolvedType decl = interfaceType.resolve();
                if (decl.isReferenceType()) {
                    ResolvedReferenceTypeDeclaration typeDecl = decl.asReferenceType().getTypeDeclaration().get();
                    String interfacePackageName = typeDecl.getPackageName();
                    String interfaceName = typeDecl.getName();
                    String interfaceFullName = interfacePackageName + "." + interfaceName;
                    Interface interfaceObj;
                    if (objects.containsKey(interfaceFullName)) {
                        interfaceObj = (Interface) objects.get(interfaceFullName);
                    } else {
                        interfaceObj = new Interface(interfaceName);
                        interfaceObj.setFilePath(filePath);
                        interfaceObj.packageName = interfacePackageName;
                        objects.put(interfaceFullName, interfaceObj);
                    }
                    implementedInterfaces.add(interfaceObj);
                }
            }
            return implementedInterfaces;
        }
        return null;
    }

    private String getPackageName(Node node) {
        String packageName = null;
        while (node.hasParentNode()) {
            node = node.getParentNode().get();
            if (node instanceof CompilationUnit) {
                packageName = ((CompilationUnit) node).getPackageDeclaration().get().getName().asString();
                break;
            }
        }
        return packageName;
    }

    private List<Field> parseFields(NodeWithMembers n, UMLClass obj) {
        List<Field> fields = new LinkedList<>();
        List<FieldDeclaration> flds = n.getFields();
        for (FieldDeclaration f : flds) {
            List<Field.Modifier> modifiers = parseFieldModifiers(f);
            List<Field.AccessModifier> accessModifiers = parseFieldAccessModifiers(f);
            for (VariableDeclarator variableDeclarator : f.getVariables()) {

                Field field = new Field(variableDeclarator.getNameAsString());
                field.owner = obj.toString();
                try {
                    ResolvedType variableType = variableDeclarator.resolve().getType();
                    if (variableType.isPrimitive()) {
                        field.primitiveType = variableType.asPrimitive().name().toLowerCase();
                    } else if (variableType.isReferenceType()) {
                        ResolvedReferenceTypeDeclaration typeDecl = variableType.asReferenceType().getTypeDeclaration().get();
                        field.typeName = typeDecl.getName();
                        field.typePackageName = typeDecl.getPackageName();
                    } else if (variableType.isArray()) {
                        field.isArray = true;
                        ResolvedType baseType = ((ResolvedArrayType) variableType).getComponentType();
                        while(baseType instanceof ResolvedArrayType) {
                            baseType = ((ResolvedArrayType) baseType).getComponentType();
                        }
                        if(baseType.isPrimitive()) {
                            field.primitiveType = variableType.describe();
                        } else if (baseType.isReferenceType()) {
                            ResolvedReferenceTypeDeclaration parameterTypeDecl = baseType.asReferenceType().getTypeDeclaration().get();
                            field.typePackageName = parameterTypeDecl.getPackageName();
                            field.typeName = variableType.describe();
                            if(variableType.describe().startsWith(field.typePackageName))
                                field.typeName = field.typeName.substring(field.typePackageName.length()+1);
                        }
                    } else if (!variableType.isArray()) {
                        System.err.println("Could not get type: " + variableType);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                field.setLine(variableDeclarator.getBegin().get().line);
                field.modifiers = modifiers;
                field.accessModifiers = accessModifiers;
                fields.add(field);
            }
        }
        return fields;
    }

    private List<Method> parseMethods(UMLClass obj, NodeWithMembers n) {
        List<Method> objMethods = new LinkedList<>();
        List<MethodDeclaration> methods = n.getMethods();
        for (MethodDeclaration decl : methods) {
            Method method = new Method(decl.getName().asString());
            method.owner = obj.toString();
            NodeList<com.github.javaparser.ast.body.Parameter> params = decl.getParameters();
            method.modifiers = parseMethodModifiers(decl);
            method.accessModifiers = parseMethodAccessModifiers(decl);
            Type returnType = decl.getType();
            ResolvedType resolvedReturnType = returnType.resolve();
            if (resolvedReturnType.isReferenceType()) {
                ResolvedReferenceTypeDeclaration returnTypeDecl = resolvedReturnType.asReferenceType().getTypeDeclaration().get();
                method.returnTypeName = returnTypeDecl.getName();
                method.returnTypePackageName = returnTypeDecl.getPackageName();
            } else if (resolvedReturnType.isPrimitive()) {
                method.returnPrimitiveType = returnType.asPrimitiveType().asString().toLowerCase();
            } else if (!resolvedReturnType.isArray() && !resolvedReturnType.isVoid() && !resolvedReturnType.isTypeVariable()) {
                System.err.println("Could not get resolvedReturnType: " + resolvedReturnType);
            }
            for (com.github.javaparser.ast.body.Parameter param : params) {
                String paramName = param.getNameAsString();
                Parameter parameter = new Parameter(paramName);
                parseParameterType(parameter, param);
                parameter.modifiers = parseParameterModifiers(param);
                method.parameters.add(parameter);
                if(!parameter.isPrimitiveType())
                    createParameterTypeRelationship(obj, method, parameter);
            }
            method.setLine(decl.getBegin().get().line);
            objMethods.add(method);
        }
        return objMethods;
    }

    private void createParameterTypeRelationship(UMLClass obj, Method method, Parameter parameter) {
        UMLRelationship.Type type = UMLRelationship.Type.Dependency;
        UMLClass refClass = objects.getOrDefault(parameter.getTypePackageName() + "." + parameter.getTypeName(), null);
        if(refClass == null || refClass == obj)
            return;
        UMLRelationship rel = new UMLRelationship(obj, refClass, type);
        String key = rel.toString();
        if (obj.relationships.containsKey(key)) {
            rel = obj.relationships.get(key);
        } else {
            obj.relationships.put(key, rel);
        }
        if (refClass.relationships.containsKey(key)) {
            rel = refClass.relationships.get(key);
        } else {
            refClass.relationships.put(key, rel);
        }
        rel.addClassAccess(method);
    }

    private void parseParameterType(Parameter parameter, com.github.javaparser.ast.body.Parameter param) {
        try {
            ResolvedType type = param.resolve().getType();
            if (type.isReferenceType()) {
                ResolvedReferenceTypeDeclaration parameterTypeDecl = type.asReferenceType().getTypeDeclaration().get();
                parameter.setTypeName(parameterTypeDecl.getName());
                parameter.setTypePackageName(parameterTypeDecl.getPackageName());
            } else if (type.isPrimitive()) {
                parameter.setPrimitiveType(type.asPrimitive().name().toLowerCase());
            } else if (type.isArray()) {
                parameter.setArray(true);
                ResolvedType baseType = ((ResolvedArrayType) type).getComponentType();
                while(baseType instanceof ResolvedArrayType) {
                    baseType = ((ResolvedArrayType) baseType).getComponentType();
                }
                if(baseType.isPrimitive()) {
                    parameter.setPrimitiveType(type.describe());
                } else if (baseType.isReferenceType()) {
                    ResolvedReferenceTypeDeclaration parameterTypeDecl = baseType.asReferenceType().getTypeDeclaration().get();
                    parameter.setTypePackageName(parameterTypeDecl.getPackageName());
                    parameter.setTypeName(type.describe());
                    if(type.describe().startsWith(parameter.getTypePackageName()))
                        parameter.setTypeName(parameter.getTypeName().substring(parameter.getTypePackageName().length()+1));
                }
            } else if (!type.isArray() && !type.isVoid() && !type.isTypeVariable()) {
                System.err.println("Could not get param type: " + type);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private List<Method.Modifier> parseMethodModifiers(CallableDeclaration methodDecl) {
        NodeList<Modifier> modifiers = methodDecl.getModifiers();
        List<Method.Modifier> methodModifiers = new LinkedList<>();
        for (Modifier modifier : modifiers) {
            if (modifier.getKeyword() == Modifier.Keyword.STATIC)
                methodModifiers.add(Method.Modifier.Static);
            else if (modifier.getKeyword() == Modifier.Keyword.FINAL)
                methodModifiers.add(Method.Modifier.Final);
            else if (modifier.getKeyword() == Modifier.Keyword.ABSTRACT)
                methodModifiers.add(Method.Modifier.Abstract);
            else if (modifier.getKeyword() == Modifier.Keyword.SYNCHRONIZED)
                methodModifiers.add(Method.Modifier.Synchronized);
            else if (modifier.getKeyword() == Modifier.Keyword.NATIVE)
                methodModifiers.add(Method.Modifier.Native);
            else if (modifier.getKeyword() == Modifier.Keyword.STRICTFP)
                methodModifiers.add(Method.Modifier.StrictFP);
        }
        return methodModifiers;
    }

    private List<Method.AccessModifier> parseMethodAccessModifiers(CallableDeclaration methodDecl) {
        NodeList<Modifier> modifiers = methodDecl.getModifiers();
        List<Method.AccessModifier> methodAccessModifiers = new LinkedList<>();
        for (Modifier modifier : modifiers) {
            if (modifier.getKeyword() == Modifier.Keyword.PUBLIC)
                methodAccessModifiers.add(Method.AccessModifier.Public);
            else if (modifier.getKeyword() == Modifier.Keyword.PROTECTED)
                methodAccessModifiers.add(Method.AccessModifier.Protected);
            else if (modifier.getKeyword() == Modifier.Keyword.PRIVATE)
                methodAccessModifiers.add(Method.AccessModifier.Private);
            else if (modifier.getKeyword() == Modifier.Keyword.DEFAULT)
                methodAccessModifiers.add(Method.AccessModifier.Default);
        }
        return methodAccessModifiers;
    }

    private List<Parameter.Modifier> parseParameterModifiers(com.github.javaparser.ast.body.Parameter parameter) {
        NodeList<Modifier> modifiers = parameter.getModifiers();
        List<Parameter.Modifier> accessModifiers = new LinkedList<>();
        for (Modifier modifier : modifiers) {
            if (modifier.getKeyword() == Modifier.Keyword.FINAL)
                accessModifiers.add(Parameter.Modifier.Final);
        }
        return accessModifiers;
    }


    private List<Field.Modifier> parseFieldModifiers(FieldDeclaration fieldDecl) {
        NodeList<Modifier> modifiers = fieldDecl.getModifiers();
        List<Field.Modifier> methodModifiers = new LinkedList<>();
        for (Modifier modifier : modifiers) {
            if (modifier.getKeyword() == Modifier.Keyword.STATIC)
                methodModifiers.add(Field.Modifier.Static);
            else if (modifier.getKeyword() == Modifier.Keyword.FINAL)
                methodModifiers.add(Field.Modifier.Final);
            else if (modifier.getKeyword() == Modifier.Keyword.TRANSIENT)
                methodModifiers.add(Field.Modifier.Transient);
            else if (modifier.getKeyword() == Modifier.Keyword.VOLATILE)
                methodModifiers.add(Field.Modifier.Volatile);
        }
        return methodModifiers;
    }

    private List<Field.AccessModifier> parseFieldAccessModifiers(FieldDeclaration fieldDecl) {
        NodeList<Modifier> modifiers = fieldDecl.getModifiers();
        List<Field.AccessModifier> methodAccessModifiers = new LinkedList<>();
        for (Modifier modifier : modifiers) {
            if (modifier.getKeyword() == Modifier.Keyword.PUBLIC)
                methodAccessModifiers.add(Field.AccessModifier.Public);
            else if (modifier.getKeyword() == Modifier.Keyword.PROTECTED)
                methodAccessModifiers.add(Field.AccessModifier.Protected);
            else if (modifier.getKeyword() == Modifier.Keyword.PRIVATE)
                methodAccessModifiers.add(Field.AccessModifier.Private);
            else if (modifier.getKeyword() == Modifier.Keyword.DEFAULT)
                methodAccessModifiers.add(Field.AccessModifier.Default);
        }
        return methodAccessModifiers;
    }

    private List<Method> parseConstructors(UMLClass obj, NodeWithMembers n) {
        List<Method> objConstructors = new LinkedList<>();
        List<ConstructorDeclaration> constructors = n.getConstructors();
        for (ConstructorDeclaration decl : constructors) {
            Constructor constructor = new Constructor(decl.getNameAsString());
            constructor.owner = obj.toString();
            constructor.modifiers = parseMethodModifiers(decl);
            constructor.accessModifiers = parseMethodAccessModifiers(decl);
            NodeList<com.github.javaparser.ast.body.Parameter> params = decl.getParameters();
            for (com.github.javaparser.ast.body.Parameter param : params) {
                String paramName = param.getNameAsString();
                Parameter parameter = new Parameter(paramName);
                parseParameterType(parameter, param);
                parameter.modifiers = parseParameterModifiers(param);
                constructor.parameters.add(parameter);
                if(!parameter.isPrimitiveType())
                    createParameterTypeRelationship(obj, constructor, parameter);
            }
            constructor.setLine(decl.getBegin().get().line);
            objConstructors.add(constructor);
        }
        return objConstructors;
    }
}
