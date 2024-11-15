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
import com.mku.liveuml.graph.Relationship;
import com.mku.liveuml.graph.UMLObject;
import com.mku.liveuml.meta.Variable;
import com.mku.liveuml.meta.*;
import com.mku.liveuml.meta.Class;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class Generator {
    public static HashMap<String, UMLObject> objects = new HashMap<>();

    public static List<UMLObject> getClasses(File projectDir) {
        objects.clear();
        List<UMLObject> list = new LinkedList<>();
        getObjects(list, projectDir);
        getMethodCalls(list, projectDir);
        resolveTypes(list);
        return list;
    }

    private static void resolveTypes(List<UMLObject> list) {
        for (UMLObject fieldOwner : list) {
            for (Field field : fieldOwner.fields) {
                if (!field.isPrimitiveType()) {
                    String fullName = field.getTypeFullName();
                    if (objects.containsKey(fullName)) {
                        UMLObject fieldType = objects.get(fullName);
                        // we allow loops if it's aggregation or composition
                        createFieldAggregationRelationship(field, fieldOwner, fieldType);
                    }
                }
            }
        }
    }

    private static void createFieldAggregationRelationship(Field field, UMLObject fieldOwner, UMLObject fieldType) {
        Relationship.Type relType = Relationship.Type.Aggregation;
        if (field.accessModifiers.contains(Field.AccessModifier.Private) && !hasPublicSetter(field, fieldOwner, fieldType))
            relType = Relationship.Type.Composition;
        Relationship rel = new Relationship(fieldOwner, fieldType, relType);
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
    private static boolean hasPublicSetter(Field field, UMLObject fieldOwner, UMLObject fieldType) {
        for (Method method : fieldOwner.methods) {
            // check public constructor param
            if (method.name.equals(fieldOwner.name) && !method.accessModifiers.contains(Method.AccessModifier.Private)) {
                for (Variable param : method.parameters) {
                    if (param.typeName != null && param.typeName.equals(fieldType.name)) {
                        return true;
                    }
                }
            }
            // check public setter
            if (method.name.toLowerCase().startsWith(("set" + field.name).toLowerCase())
                    && !method.accessModifiers.contains(Method.AccessModifier.Private)) {
                for (Variable param : method.parameters) {
                    if (param.typeName != null && param.typeName.equals(fieldType.name)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static void getMethodCalls(List<UMLObject> list, File projectDir) {
        new DirExplorer((level, path, file) -> path.endsWith(".java"), (level, path, file) -> {
            System.out.println(path);
            System.out.println(Strings.repeat("=", path.length()));
            try {
                new VoidVisitorAdapter<UMLObject>() {
                    @Override
                    public void visit(MethodCallExpr n, UMLObject arg) {
                        super.visit(n, arg);
                        System.out.println("Method Call: [" + n.getBegin().get().line + "] " + n);
                        UMLObject caller = getMethodCallerObject(n);
                        UMLObject callee = getMethodCalleeObject(n);
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
                    public void visit(final ObjectCreationExpr n, final UMLObject arg) {
                        System.out.println("ObjectCreationExpr: [" + n.getBegin().get().line + "] " + n);
                        UMLObject caller = getMethodCallerObject(n);
                        UMLObject callee = getMethodCalleeObject(n);
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
                    public void visit(final FieldAccessExpr n, final UMLObject arg) {
                        super.visit(n, arg);
                        System.out.println("Field Access: [" + n.getBegin().get().line + "] " + n);
                        UMLObject accessor = getFieldAccessorObject(n);
                        UMLObject accessedFieldObject = getFieldAccessedObject(n);
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

    private static Method getMethodCallerMethod(UMLObject caller, ObjectCreationExpr n) {
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

    private static void createObjectCreationRelationship(UMLObject caller, Method callerMethod, UMLObject callee, Constructor constructor) {
        Relationship.Type type = Relationship.Type.Dependency;
        Relationship rel = new Relationship(caller, callee, type);
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

    private static void createObjectCreationRelationship(UMLObject caller, Method callerMethod, UMLObject callee) {
        Relationship.Type type = Relationship.Type.Dependency;
        Relationship rel = new Relationship(caller, callee, type);
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


    private static int getConstructorCount(UMLObject obj) {
        int constructors = 0;
        for(Method method : obj.methods) {
            if(method instanceof Constructor) {
                constructors++;
            }
        }
        return constructors;
    }

    private static Constructor getConstructor(UMLObject obj, ObjectCreationExpr n) {
        for(Method method : obj.methods) {
            if(method instanceof Constructor) {
                if(method.parameters.size() == n.getArguments().size())
                    return (Constructor) method;
            }
        }
        return null;
    }

    private static UMLObject getMethodCallerObject(ObjectCreationExpr n) {
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

    private static UMLObject getMethodCalleeObject(ObjectCreationExpr n) {
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

    private static UMLObject getMethodCalleeObject(MethodCallExpr n) {
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


    private static Method getMethodCalleeMethod(UMLObject callee, MethodCallExpr n) {
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

    private static UMLObject getMethodCallerObject(MethodCallExpr n) {
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

    private static Method getMethodCallerMethod(UMLObject caller, MethodCallExpr n) {
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

    private static void createMethodCallRelationship(UMLObject caller, Method callerMethod, UMLObject callee, Method calleeMethod) {
        Relationship.Type type = Relationship.Type.Dependency;
        Relationship rel = new Relationship(caller, callee, type);
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
    private static UMLObject getFieldAccessorObject(FieldAccessExpr n) {
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


    private static Method getFieldAccessorMethod(UMLObject callee, FieldAccessExpr n) {
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

    private static UMLObject getFieldAccessedObject(FieldAccessExpr n) {
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

    private static Field getFieldAccessed(UMLObject accessed, FieldAccessExpr n) {
        for (Field f: accessed.fields) {
            if (f.name.equals(n.getNameAsString())) {
                return f;
            }
        }
        return null;
    }

    private static void createFieldAccessRelationship(UMLObject accessor, Method accessorMethod, UMLObject accessed, Field accessedField) {
        Relationship.Type type = Relationship.Type.Dependency;
        Relationship rel = new Relationship(accessor, accessed, type);
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

    private static void getObjects(List<UMLObject> list, File projectDir) {
        new DirExplorer((level, path, file) -> path.endsWith(".java"), (level, path, file) -> {
            System.out.println(path);
            System.out.println(Strings.repeat("=", path.length()));
            try {
                new VoidVisitorAdapter<UMLObject>() {
                    @Override
                    public void visit(ClassOrInterfaceDeclaration n, UMLObject arg) {
                        super.visit(n, arg);
                        System.out.println(n.isInterface() ? "Interface" : "Class: " + n.getName());
                        UMLObject obj = parseClassOrInterface(n, file.getPath());
//                        if (n.getImplementedTypes().stream().anyMatch(implementedType -> implementedType.getNameWithScope().equals(arg))) {
//                            System.out.println(n.getFullyQualifiedName().get() + " implements " + arg);
//                        }
                        list.add(obj);
                    }
                }.visit(StaticJavaParser.parse(file), null);
                System.out.println(); // empty line
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            try {
                new VoidVisitorAdapter<UMLObject>() {
                    @Override
                    public void visit(EnumDeclaration n, UMLObject arg) {
                        super.visit(n, arg);
                        System.out.println("Enum: " + n.getName());
                        UMLObject obj = parseEnum(n, file.getPath());
                        list.add(obj);
                    }
                }.visit(StaticJavaParser.parse(file), null);
                System.out.println(); // empty line
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).explore(projectDir);
    }

    private static UMLObject parseEnum(EnumDeclaration n, String filePath) {
        UMLObject obj = new Enumeration(n.getName().asString());
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

    private static UMLObject parseClassOrInterface(ClassOrInterfaceDeclaration n, String filePath) {
        UMLObject obj = getOrCreateObject(n, filePath);
        Class superClassObj = getSuperClass(n, filePath);
        if (superClassObj != null) {
            Relationship rel = getSuperClassRel(obj, superClassObj);
            obj.relationships.put(rel.toString(), rel);
            superClassObj.relationships.put(rel.toString(), rel);
        }

        List<Interface> interfacesImplemented = getImplementedInterfaces(n, filePath);
        if (interfacesImplemented != null) {
            for (Interface interfaceImplementedObj : interfacesImplemented) {
                Relationship rel = getInterfaceRel(obj, interfaceImplementedObj);
                obj.relationships.put(rel.toString(), rel);
                interfaceImplementedObj.relationships.put(rel.toString(), rel);
            }
        }

        obj.fields = parseFields(n, obj);
        obj.methods.addAll(parseConstructors(obj, n));
        obj.methods.addAll(parseMethods(obj, n));
        return obj;
    }

    private static UMLObject getOrCreateObject(ClassOrInterfaceDeclaration n, String filePath) {
        String packageName = getPackageName(n);
        String className = n.getName().asString();
        return getOrCreateObject(packageName, className, n.isInterface(), filePath, n.getBegin().get().line);
    }

    private static UMLObject getOrCreateObject(String packageName, String name, boolean isInterface, String filePath,
                                               int line) {
        UMLObject obj = null;
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

    private static Relationship getSuperClassRel(UMLObject derivedClass, UMLObject superClass) {
        return new Relationship(derivedClass, superClass, Relationship.Type.Inheritance);
    }

    private static Class getSuperClass(ClassOrInterfaceDeclaration node, String filePath) {
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


    private static Relationship getInterfaceRel(UMLObject derivedClass, UMLObject superClass) {
        return new Relationship(derivedClass, superClass, Relationship.Type.Realization);
    }

    private static List<Interface> getImplementedInterfaces(ClassOrInterfaceDeclaration node, String filePath) {
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

    private static String getPackageName(Node node) {
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

    private static List<Field> parseFields(NodeWithMembers n, UMLObject obj) {
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

    private static List<Method> parseMethods(UMLObject obj, NodeWithMembers n) {
        List<Method> objMethods = new LinkedList<>();
        List<MethodDeclaration> methods = n.getMethods();
        for (MethodDeclaration decl : methods) {
            Method method = new Method(decl.getName().asString());
            method.owner = obj.toString();
            NodeList<Parameter> params = decl.getParameters();
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
            for (Parameter parameter : params) {
                String paramName = parameter.getNameAsString();
                Variable variable = new Variable(paramName);
                parseVariableType(variable, parameter);
                variable.modifiers = parseVariableModifiers(parameter);
                method.parameters.add(variable);
                if(!variable.isPrimitiveType())
                    createParameterTypeRelationship(obj, method, variable);
            }
            method.setLine(decl.getBegin().get().line);
            objMethods.add(method);
        }
        return objMethods;
    }

    private static void createParameterTypeRelationship(UMLObject obj, Method method, Variable variable) {
        Relationship.Type type = Relationship.Type.Dependency;
        UMLObject refClass = objects.getOrDefault(variable.typePackageName + "." + variable.getTypeName(), null);
        if(refClass == null || refClass == obj)
            return;
        Relationship rel = new Relationship(obj, refClass, type);
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

    private static void parseVariableType(Variable variable, Parameter parameter) {
        try {
            ResolvedType type = parameter.resolve().getType();
            if (type.isReferenceType()) {
                ResolvedReferenceTypeDeclaration parameterTypeDecl = type.asReferenceType().getTypeDeclaration().get();
                variable.typeName = parameterTypeDecl.getName();
                variable.typePackageName = parameterTypeDecl.getPackageName();
            } else if (type.isPrimitive()) {
                variable.primitiveType = type.asPrimitive().name().toLowerCase();
            } else if (type.isArray()) {
                variable.isArray = true;
                ResolvedType baseType = ((ResolvedArrayType) type).getComponentType();
                while(baseType instanceof ResolvedArrayType) {
                    baseType = ((ResolvedArrayType) baseType).getComponentType();
                }
                if(baseType.isPrimitive()) {
                    variable.primitiveType = type.describe();
                } else if (baseType.isReferenceType()) {
                    ResolvedReferenceTypeDeclaration parameterTypeDecl = baseType.asReferenceType().getTypeDeclaration().get();
                    variable.typePackageName = parameterTypeDecl.getPackageName();
                    variable.typeName = type.describe();
                    if(type.describe().startsWith(variable.typePackageName))
                        variable.typeName = variable.typeName.substring(variable.typePackageName.length()+1);
                }
            } else if (!type.isArray() && !type.isVoid() && !type.isTypeVariable()) {
                System.err.println("Could not get param type: " + type);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static List<Method.Modifier> parseMethodModifiers(CallableDeclaration methodDecl) {
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

    private static List<Method.AccessModifier> parseMethodAccessModifiers(CallableDeclaration methodDecl) {
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

    private static List<Variable.Modifier> parseVariableModifiers(Parameter parameter) {
        NodeList<Modifier> modifiers = parameter.getModifiers();
        List<Variable.Modifier> accessModifiers = new LinkedList<>();
        for (Modifier modifier : modifiers) {
            if (modifier.getKeyword() == Modifier.Keyword.FINAL)
                accessModifiers.add(Variable.Modifier.Final);
        }
        return accessModifiers;
    }


    private static List<Field.Modifier> parseFieldModifiers(FieldDeclaration fieldDecl) {
        NodeList<Modifier> modifiers = fieldDecl.getModifiers();
        List<Field.Modifier> methodModifiers = new LinkedList<>();
        for (Modifier modifier : modifiers) {
            if (modifier.getKeyword() == Modifier.Keyword.STATIC)
                methodModifiers.add(Field.Modifier.Static);
            else if (modifier.getKeyword() == Modifier.Keyword.FINAL)
                methodModifiers.add(Field.Modifier.Final);
        }
        return methodModifiers;
    }

    private static List<Field.AccessModifier> parseFieldAccessModifiers(FieldDeclaration fieldDecl) {
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

    private static List<Method> parseConstructors(UMLObject obj, NodeWithMembers n) {
        List<Method> objConstructors = new LinkedList<>();
        List<ConstructorDeclaration> constructors = n.getConstructors();
        for (ConstructorDeclaration decl : constructors) {
            Constructor constructor = new Constructor(decl.getNameAsString());
            constructor.owner = obj.toString();
            constructor.modifiers = parseMethodModifiers(decl);
            constructor.accessModifiers = parseMethodAccessModifiers(decl);
            NodeList<Parameter> params = decl.getParameters();
            for (Parameter parameter : params) {
                String paramName = parameter.getNameAsString();
                Variable variable = new Variable(paramName);
                parseVariableType(variable, parameter);
                variable.modifiers = parseVariableModifiers(parameter);
                constructor.parameters.add(variable);
                if(!variable.isPrimitiveType())
                    createParameterTypeRelationship(obj, constructor, variable);
            }
            constructor.setLine(decl.getBegin().get().line);
            objConstructors.add(constructor);
        }
        return objConstructors;
    }
}
