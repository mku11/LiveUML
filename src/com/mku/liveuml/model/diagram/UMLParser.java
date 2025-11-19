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
package com.mku.liveuml.model.diagram;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithMembers;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.TypeParameter;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedArrayType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserClassDeclaration;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserEnumDeclaration;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserInterfaceDeclaration;
import com.google.common.base.Strings;
import com.mku.liveuml.model.entities.*;
import com.mku.liveuml.file.DirExplorer;
import com.mku.liveuml.model.entities.Class;
import com.mku.liveuml.model.entities.Enumeration;
import com.mku.liveuml.model.entities.Package;
import com.mku.liveuml.model.entities.Parameter;

import java.io.File;
import java.util.*;
import java.util.function.Consumer;

public class UMLParser {
    public HashMap<String, UMLClass> objects = new HashMap<>();
    public HashMap<String, Method> methods = new HashMap<>();

    public Map<String, SymbolInformation> getUnresolvedSymbols() {
        return Collections.unmodifiableMap(unresolvedSymbols);
    }

    private final HashMap<String, SymbolInformation> unresolvedSymbols = new HashMap<>();
    private Consumer<String> notifyProgress;

    public static class SymbolInformation {
        ArrayList<UMLClass> classes = new ArrayList<>();
    }

    public void clear() {
        unresolvedSymbols.clear();
        objects.clear();
        methods.clear();
    }

    public UMLClass getClassByName(String owner) {
        return objects.getOrDefault(owner, null);
    }

    public HashSet<UMLClass> getClasses(File projectDir) {
        HashSet<UMLClass> classes = new HashSet<>();
        getObjects(classes, projectDir);
        return classes;
    }

    public void resolveDependencies(HashSet<UMLClass> list) {
        for (UMLClass object : list) {
            for (Field field : object.getFields()) {
                if (!field.isPrimitiveType()) {
                    String fullName = field.getTypeFullName();
                    if (objects.containsKey(fullName)) {
                        UMLClass fieldType = objects.get(fullName);
                        // we allow loops if it's aggregation or composition
                        createFieldAggregationRelationship(field, object, fieldType);
                    }
                }
            }
            for (Parameter parameter : object.getTypeParameters()) {
                if (parameter.isGeneric())
                    createClassTypeParameterRelationship(object, parameter);
            }
        }
    }

    private void createFieldAggregationRelationship(Field field, UMLClass fieldOwner, UMLClass fieldType) {
        UMLRelationshipType relType = UMLRelationshipType.Aggregation;
        // FIXME: detecting composition is not this simple
        if (field.getAccessModifiers().contains(AccessModifier.Private) && !hasPublicSetter(field, fieldOwner, fieldType))
            relType = UMLRelationshipType.Composition;
        UMLRelationship rel = new UMLRelationship(fieldOwner, fieldType, relType);
        String key = rel.toString();
        if (fieldOwner.getRelationships().containsKey(key)) {
            rel = fieldOwner.getRelationships().get(key);
        } else {
            fieldOwner.getRelationships().put(key, rel);
        }
        if (fieldType.getRelationships().containsKey(key)) {
            rel = fieldType.getRelationships().get(key);
        } else {
            fieldType.getRelationships().put(key, rel);
        }
        rel.addFieldAssociation(field);
    }

    // TODO: check the method body it it's setting the specific field
    private boolean hasPublicSetter(Field field, UMLClass fieldOwner, UMLClass fieldType) {
        for (Method method : fieldOwner.getMethods()) {
            // check public constructor param
            if (method.getName().equals(fieldOwner.getName()) && !method.getAccessModifiers().contains(AccessModifier.Private)) {
                for (com.mku.liveuml.model.entities.Parameter param : method.getParameters()) {
                    if (param.getTypeName() != null && param.getTypeName().equals(fieldType.getName())) {
                        return true;
                    }
                }
            }
            // check public setter
            if (method.getName().toLowerCase().startsWith(("set" + field.getName()).toLowerCase())
                    && !method.getAccessModifiers().contains(AccessModifier.Private)) {
                for (com.mku.liveuml.model.entities.Parameter param : method.getParameters()) {
                    if (param.getTypeName() != null && param.getTypeName().equals(fieldType.getName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void parseDependencies(File projectDir) {
        new DirExplorer((level, path, file) -> path.endsWith(".java"), (level, path, file) -> {
            System.out.println(path);
            System.out.println(Strings.repeat("=", path.length()));
            if (notifyProgress != null)
                notifyProgress.accept(file.getName());
            try {
                new VoidVisitorAdapter<UMLClass>() {
                    @Override
                    public void visit(MethodCallExpr n, UMLClass arg) {
                        super.visit(n, arg);
                        System.out.println("Method Call: [" + file.getName() + ":" + n.getBegin().get().line + "] " + n.getName());
                        UMLClass caller = getMethodCallerObject(n);
                        UMLClass callee = null;
                        try {
                            callee = getMethodCalleeObject(n);
                        } catch (UnsolvedSymbolException ex) {
                            addUnresolvedSymbol(ex.getName(), caller);
                        }
                        if (callee != null && caller != null
                                && callee != caller) {
                            Method callerMethod = getMethodCallerMethod(caller, n);
                            Method calleeMethod = null;
                            try {
                                calleeMethod = getMethodCalleeMethod(callee, n);
                            } catch (UnsolvedSymbolException ex) {
                                addUnresolvedSymbol(ex.getName(), arg);
                            }
                            if (calleeMethod == null) {
                                calleeMethod = getInitializer(caller, n);
                            }
                            if (calleeMethod != null) {
                                createMethodCallRelationship(caller, callerMethod, callee, calleeMethod);
                            }
                        }
                    }

                    @Override
                    public void visit(final ObjectCreationExpr n, final UMLClass arg) {
                        System.out.println("ObjectCreationExpr: [" + file.getName() + ":" + n.getBegin().get().line + "] " + n.getType().getName());
                        UMLClass caller = getMethodCallerObject(n);
                        UMLClass callee = null;
                        try {
                            callee = getMethodCalleeObject(n);
                        } catch (UnsolvedSymbolException ex) {
                            addUnresolvedSymbol(ex.getName(), caller);
                        }
                        if (caller != null && callee != null) {
                            Method callerMethod = getMethodCallerMethod(caller, n);
                            Constructor constructor = getConstructor(callee, n);
                            if (callerMethod != null) {
                                if (constructor != null) {
                                    createObjectCreationRelationship(caller, callerMethod, callee, constructor);
                                }
                                if (constructor == null && getConstructorCount(callee) == 0) {
                                    createObjectCreationRelationship(caller, callerMethod, callee);
                                }
                            }
                        }
                    }

                    @Override
                    public void visit(final FieldAccessExpr n, final UMLClass arg) {
                        super.visit(n, arg);
                        System.out.println("Field Access: [" + n.getBegin().get().line + "] " + n);
                        UMLClass accessor = getFieldAccessorObject(n);
                        UMLClass accessedFieldObject = null;
                        try {
                            accessedFieldObject = getFieldAccessedObject(n);
                        } catch (UnsolvedSymbolException ex) {
                            addUnresolvedSymbol(ex.getName(), accessor);
                        }
                        if (accessor != null && accessedFieldObject != null
                                && accessor != accessedFieldObject) {
                            Method accessorMethod = getFieldAccessorMethod(accessor, n);
                            if (accessorMethod == null) {
                                accessorMethod = getInitializer(accessor, n);
                            }
                            if (accessorMethod != null) {
                                Field accessedField = getFieldAccessed(accessedFieldObject, n);
                                if (accessedField != null) {
                                    createFieldAccessRelationship(accessor, accessorMethod, accessedFieldObject, accessedField);
                                }
                                EnumConstant accessedEnumConstant = getEnumConstAccessed(accessedFieldObject, n);
                                if (accessedEnumConstant != null) {
                                    createEnumConstAccessRelationship(accessor, accessorMethod, accessedFieldObject, accessedEnumConstant);
                                }
                            }
                        }
                    }
                }.visit(StaticJavaParser.parse(file), null);
                System.out.println(); // empty line
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).explore(projectDir);
    }

    private void addUnresolvedSymbol(String name, UMLClass arg) {
        SymbolInformation info;
        if (name.startsWith("Solving ")) {
            name = name.split(" ")[1];
        }
        if (unresolvedSymbols.containsKey(name)) {
            info = unresolvedSymbols.get(name);
        } else {
            info = new SymbolInformation();
            unresolvedSymbols.put(name, info);
        }
        info.classes.add(arg);
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
        if (methodDeclaration == null)
            return null;
        String methodSignature = caller.getFullName() + "." + methodDeclaration.getSignature().toString();
        if (methods.containsKey(methodSignature)) {
            return methods.get(methodSignature);
        }
        return null;
    }

    private Method getInitializer(UMLClass caller, Expression n) {
        InitializerDeclaration initializerDecl = null;
        Node node = n;
        while (node.hasParentNode()) {
            node = node.getParentNode().get();
            if (node instanceof InitializerDeclaration) {
                initializerDecl = (InitializerDeclaration) node;
                break;
            }
        }
        if (initializerDecl == null)
            return null;
        String methodName = "init" + n.getBegin().get().line;
        String methodSignature = caller.getFullName() + "." + methodName;
        if (methods.containsKey(methodSignature)) {
            return methods.get(methodSignature);
        }
        Method method = new Method(methodName);
        method.setLine(n.getBegin().get().line);
        if (initializerDecl.isStatic())
            method.setModifiers(List.of(new Modifier[]{Modifier.Static}));
        method.setOwner(caller.toString());
        caller.addMethods(List.of(method));
        this.methods.put(methodSignature, method);
        return method;
    }

    private void createObjectCreationRelationship(UMLClass caller, Method callerMethod, UMLClass callee, Constructor constructor) {
        UMLRelationshipType type = UMLRelationshipType.Dependency;
        UMLRelationship rel = new UMLRelationship(caller, callee, type);
        String key = rel.toString();
        if (caller.getRelationships().containsKey(key)) {
            rel = caller.getRelationships().get(key);
        } else {
            caller.getRelationships().put(key, rel);
        }
        if (callee.getRelationships().containsKey(key)) {
            rel = callee.getRelationships().get(key);
        } else {
            callee.getRelationships().put(key, rel);
        }
        rel.addMethodCall(callerMethod, constructor);
    }

    private void createObjectCreationRelationship(UMLClass caller, Method callerMethod, UMLClass callee) {
        UMLRelationshipType type = UMLRelationshipType.Dependency;
        UMLRelationship rel = new UMLRelationship(caller, callee, type);
        String key = rel.toString();
        if (caller.getRelationships().containsKey(key)) {
            rel = caller.getRelationships().get(key);
        } else {
            caller.getRelationships().put(key, rel);
        }
        if (callee.getRelationships().containsKey(key)) {
            rel = callee.getRelationships().get(key);
        } else {
            callee.getRelationships().put(key, rel);
        }
        rel.addClassAccess(callerMethod);
    }


    private int getConstructorCount(UMLClass obj) {
        int constructors = 0;
        for (Method method : obj.getMethods()) {
            if (method instanceof Constructor) {
                constructors++;
            }
        }
        return constructors;
    }

    private Constructor getConstructor(UMLClass obj, ObjectCreationExpr n) {
        for (Method method : obj.getMethods()) {
            if (method instanceof Constructor) {
                if (method.getParameters().size() == n.getArguments().size())
                    return (Constructor) method;
            }
        }
        return null;
    }

    private UMLClass getMethodCallerObject(ObjectCreationExpr n) {
        String packageName = getPackageName(n);
        String name = getName(n);
        List<String> parents = getParents(n);
        if (parents.size() > 0)
            parents = parents.subList(0, parents.size() - 1);
        String fullName = Package.getFullName(packageName, name, parents);
        if (objects.containsKey(fullName)) {
            return objects.get(fullName);
        }
        return null;
    }

    private UMLClass getMethodCalleeObject(ObjectCreationExpr n) {
        ResolvedReferenceTypeDeclaration decl = n.calculateResolvedType().asReferenceType().getTypeDeclaration().get();
        String packageName = decl.getPackageName();
        String name = decl.getName();
        List<String> parents = getParents(getNode(decl));
        String fullName = Package.getFullName(packageName, name, parents);
        if (objects.containsKey(fullName)) {
            return objects.get(fullName);
        }
        return null;
    }

    private UMLClass getMethodCalleeObject(MethodCallExpr n) {
        if (n.getScope().isEmpty())
            return null;
        ResolvedType type = n.getScope().get().calculateResolvedType();
        String fullName = null;
        if (type.isReferenceType()) {
            ResolvedReferenceTypeDeclaration decl = type.asReferenceType().getTypeDeclaration().get();
            String packageName = decl.getPackageName();
            String name = decl.getName();
            List<String> parents = getParents(getNode(decl));
            fullName = Package.getFullName(packageName, name, parents);
        }
        if (objects.containsKey(fullName)) {
            return objects.get(fullName);
        }
        return null;
    }

    private Method getMethodCalleeMethod(UMLClass callee, MethodCallExpr n) {
        String methodName = n.getNameAsString();
        for (Method m : callee.getMethods()) {
            if (m.getName().equals(methodName) && m.getParameters().size() == n.getArguments().size()) {
                return m;
            }
        }
        return null;
    }

    private UMLClass getMethodCallerObject(MethodCallExpr n) {
        String packageName = getPackageName(n);
        String name = getName(n);
        List<String> parents = getParents(n);
        String fullName = Package.getFullName(packageName, name, parents.subList(0, parents.size() - 1));
        if (objects.containsKey(fullName)) {
            return objects.get(fullName);
        }
        return null;
    }

    private String getName(Node n) {
        Node node = n;
        while (node.hasParentNode()) {
            node = node.getParentNode().get();
            if (node instanceof ClassOrInterfaceDeclaration) {
                return ((ClassOrInterfaceDeclaration) node).getNameAsString();
            }
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
        if (methodDeclaration == null)
            return null;

        String methodSignature = caller.getFullName() + "." + methodDeclaration.getSignature().toString();
        if (methods.containsKey(methodSignature)) {
            return methods.get(methodSignature);
        }
        return null;
    }

    private void createMethodCallRelationship(UMLClass caller, Method callerMethod, UMLClass callee, Method calleeMethod) {
        UMLRelationshipType type = UMLRelationshipType.Dependency;
        UMLRelationship rel = new UMLRelationship(caller, callee, type);
        String key = rel.toString();
        if (caller.getRelationships().containsKey(key)) {
            rel = caller.getRelationships().get(key);
        } else {
            caller.getRelationships().put(key, rel);
        }
        if (callee.getRelationships().containsKey(key)) {
            rel = callee.getRelationships().get(key);
        } else {
            callee.getRelationships().put(key, rel);
        }
        rel.addMethodCall(callerMethod, calleeMethod);
    }

    // fields accessors
    private UMLClass getFieldAccessorObject(FieldAccessExpr n) {
        String packageName = getPackageName(n);
        String name = getName(n);
        List<String> parents = getParents(n);
        if (parents.size() > 0)
            parents = parents.subList(0, parents.size() - 1);
        String fullName = Package.getFullName(packageName, name, parents);
        if (objects.containsKey(fullName)) {
            return objects.get(fullName);
        }
        return null;
    }


    private Method getFieldAccessorMethod(UMLClass callee, FieldAccessExpr n) {
        CallableDeclaration<?> methodDeclaration = null;
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
        if (methodDeclaration == null)
            return null;

        try {
            String methodSignature = callee.getFullName() + "." + methodDeclaration.getSignature().toString();
            if (methods.containsKey(methodSignature)) {
                return methods.get(methodSignature);
            }
        } catch (UnsolvedSymbolException ex) {
            addUnresolvedSymbol(ex.getName(), callee);
        }

        return null;
    }

    private UMLClass getFieldAccessedObject(FieldAccessExpr n) {
        if (isJavaScope(n.getScope().toString()))
            return null;
        ResolvedType type = n.getScope().calculateResolvedType();
        String fullName = null;
        if (type.isReferenceType()) {
            ResolvedReferenceTypeDeclaration decl = type.asReferenceType().getTypeDeclaration().get();
            String packageName = decl.getPackageName();
            String name = decl.getName();
            List<String> parents = getParents(getNode(decl));
            fullName = Package.getFullName(packageName, name, parents);
        }
        if (objects.containsKey(fullName)) {
            return objects.get(fullName);
        }
        return null;
    }

    private boolean isJavaScope(String scope) {
        return scope == null || scope.equals("java")
                || scope.startsWith("java.")
                || scope.equals("System")
                || scope.startsWith("System.");
    }

    private Field getFieldAccessed(UMLClass accessed, FieldAccessExpr n) {
        for (Field f : accessed.getFields()) {
            if (f.getName().equals(n.getNameAsString())) {
                return f;
            }
        }
        return null;
    }

    private EnumConstant getEnumConstAccessed(UMLClass accessed, FieldAccessExpr n) {
        for (EnumConstant f : accessed.getEnumConstants()) {
            if (f.getName().equals(n.getNameAsString())) {
                return f;
            }
        }
        return null;
    }

    private void createFieldAccessRelationship(UMLClass accessor, Method accessorMethod, UMLClass accessed, Field accessedField) {
        UMLRelationshipType type = UMLRelationshipType.Dependency;
        UMLRelationship rel = new UMLRelationship(accessor, accessed, type);
        String key = rel.toString();
        if (accessor.getRelationships().containsKey(key)) {
            rel = accessor.getRelationships().get(key);
        } else {
            accessor.getRelationships().put(key, rel);
        }
        if (accessed.getRelationships().containsKey(key)) {
            rel = accessed.getRelationships().get(key);
        } else {
            accessed.getRelationships().put(key, rel);
        }
        rel.addFieldAccess(accessorMethod, accessedField);
    }

    private void createEnumConstAccessRelationship(UMLClass accessor, Method accessorMethod, UMLClass accessed,
                                                   EnumConstant accessedEnumConst) {
        UMLRelationshipType type = UMLRelationshipType.Dependency;
        UMLRelationship rel = new UMLRelationship(accessor, accessed, type);
        String key = rel.toString();
        if (accessor.getRelationships().containsKey(key)) {
            rel = accessor.getRelationships().get(key);
        } else {
            accessor.getRelationships().put(key, rel);
        }
        if (accessed.getRelationships().containsKey(key)) {
            rel = accessed.getRelationships().get(key);
        } else {
            accessed.getRelationships().put(key, rel);
        }
        rel.addEnumConstAccess(accessorMethod, accessedEnumConst);
    }


    private void getObjects(HashSet<UMLClass> list, File source) {
        new DirExplorer((level, path, file) -> path.endsWith(".java"), (level, path, file) -> {
            System.out.println(path);
            System.out.println(Strings.repeat("=", path.length()));
            if (notifyProgress != null)
                notifyProgress.accept(file.getName());
            try {
                new VoidVisitorAdapter<UMLClass>() {
                    @Override
                    public void visit(ClassOrInterfaceDeclaration n, UMLClass arg) {
                        super.visit(n, arg);
                        System.out.println(n.isInterface() ? "Interface" : "Class: " + n.getName());
                        UMLClass obj = getOrCreateObject(n, file.getPath());
                        validatePathPackageName(obj.getPackageName(), path);
                        list.add(obj);
                    }
                }.visit(StaticJavaParser.parse(file), null);
                System.out.println();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            try {
                new VoidVisitorAdapter<UMLClass>() {
                    @Override
                    public void visit(EnumDeclaration n, UMLClass arg) {
                        super.visit(n, arg);
                        System.out.println("Enum: " + n.getName());
                        UMLClass obj = getOrCreateEnum(n, file.getPath());
                        validatePathPackageName(obj.getPackageName(), path);
                        list.add(obj);
                    }
                }.visit(StaticJavaParser.parse(file), null);
                System.out.println(); // empty line
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).explore(source);
    }

    private void validatePathPackageName(String packageName, String path) {
        if(!path.substring(1).replaceAll("/",".").startsWith(packageName))
            throw new InvalidSourceException("This is does not seem to be a root source directory."
                    + "\nMake sure your packages start from the same directory."
                    + "\nSource file: " + path
                    + "\nPackage name: " + packageName);
    }


    public void getObjectsAttrs(HashSet<UMLClass> list, File projectDir) {
        new DirExplorer((level, path, file) -> path.endsWith(".java"), (level, path, file) -> {
            System.out.println(path);
            System.out.println(Strings.repeat("=", path.length()));
            if (notifyProgress != null)
                notifyProgress.accept(file.getName());
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
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            try {
                new VoidVisitorAdapter<UMLClass>() {
                    @Override
                    public void visit(EnumDeclaration n, UMLClass arg) {
                        super.visit(n, arg);
                        System.out.println("Enum: " + n.getName());
                        UMLClass obj = parseEnumConsts(n, file.getPath());
                        list.add(obj);
                    }
                }.visit(StaticJavaParser.parse(file), null);
                System.out.println(); // empty line
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).explore(projectDir);
    }

    private UMLClass parseEnumConsts(EnumDeclaration n, String filePath) {
        UMLClass obj = getOrCreateEnum(n, filePath);
        obj.addEnumConstants(parseEnums(n, obj));
        return obj;
    }


    private UMLClass parseClassOrInterface(ClassOrInterfaceDeclaration n, String filePath) {
        UMLClass obj = getOrCreateObject(n, filePath);
        Class superClassObj = null;
        try {
            superClassObj = getSuperClass(n, filePath);
        } catch (UnsolvedSymbolException ex) {
            addUnresolvedSymbol(ex.getName(), obj);
        }

        if (superClassObj != null) {
            UMLRelationship rel = getSuperClassRel(obj, superClassObj);
            obj.getRelationships().put(rel.toString(), rel);
            superClassObj.getRelationships().put(rel.toString(), rel);
        }

        List<Interface> interfacesImplemented = getImplementedInterfaces(obj, n, filePath);
        if (interfacesImplemented != null) {
            for (Interface interfaceImplementedObj : interfacesImplemented) {
                UMLRelationship rel = getInterfaceRel(obj, interfaceImplementedObj);
                obj.getRelationships().put(rel.toString(), rel);
                interfaceImplementedObj.getRelationships().put(rel.toString(), rel);
            }
        }
        obj.setLine(n.getBegin().get().line);
        obj.addFields(parseFields(n, obj));
        obj.addMethods(parseConstructors(obj, n));
        obj.addMethods(parseMethods(obj, n));
        return obj;
    }

    private UMLClass getOrCreateObject(ClassOrInterfaceDeclaration n, String filePath) {
        String packageName = getPackageName(n);
        ArrayList<String> parents = getParents(n);
        String className = n.getName().asString();
        UMLClass object = getOrCreateObject(packageName, className, parents,
                n.isInterface(), n.isAbstract(), filePath, n.getBegin().get().line);
        setModifiers(object, n);
        if (n.isGeneric()) {
            List<Parameter> params = parseGenericTypeParameters(object, n.getTypeParameters());
            object.setTypeParameters(params);
        }
        return object;
    }

    private List<Parameter> parseGenericTypeParameters(UMLClass object, NodeList<TypeParameter> parameters) {
        List<Parameter> params = new ArrayList<>();
        for (TypeParameter tp : parameters) {
            Parameter param = new Parameter(tp.getName().toString());
            NodeList<ClassOrInterfaceType> typeBound = tp.getTypeBound();
            param.setGeneric(true);
            if (tp.toString().contains(" extends ")) {
                param.setUpperBound(true);
            } else if (tp.toString().contains(" super "))
                param.setUpperBound(true);
            List<String> bounds = new ArrayList<>();
            List<String> boundsFullnames = new ArrayList<>();
            for (ClassOrInterfaceType boundCls : typeBound) {
                String className = boundCls.getNameAsString();
                bounds.add(className);
                if (boundCls.isReferenceType()) {
                    try {
                        ResolvedReferenceTypeDeclaration typeDecl = boundCls.resolve().asReferenceType().getTypeDeclaration().get();
                        String packageName = typeDecl.getPackageName();
                        ArrayList<String> parents = getParents(getNode(typeDecl));
                        String fullName = Package.getFullName(packageName, className, parents);
                        boundsFullnames.add(fullName);
                    } catch (UnsolvedSymbolException ex) {
                        addUnresolvedSymbol(ex.getName(), object);
                    }
                }
            }
            param.setBoundsFullNames(boundsFullnames);
            param.setBounds(bounds);
            params.add(param);
        }
        return params;
    }


    private UMLClass getOrCreateEnum(EnumDeclaration n, String filePath) {
        String packageName = getPackageName(n);
        ArrayList<String> parents = getParents(n);
        String className = n.getName().asString();
        UMLClass object = getOrCreateEnum(packageName, className, parents, filePath, n.getBegin().get().line);
        setModifiers(object, n);
        return object;
    }

    private void setModifiers(UMLClass object, TypeDeclaration<?> n) {
        List<AccessModifier> accessModifiers = new ArrayList<>();
        if (n.isPublic())
            accessModifiers.add(AccessModifier.Public);
        else if (n.isPrivate())
            accessModifiers.add(AccessModifier.Private);
        else if (n.isProtected())
            accessModifiers.add(AccessModifier.Protected);
        else
            accessModifiers.add(AccessModifier.Default);
        object.setAccessModifiers(accessModifiers);

        List<Modifier> modifiers = new ArrayList<>();
        if (n instanceof ClassOrInterfaceDeclaration && ((ClassOrInterfaceDeclaration) n).isFinal())
            modifiers.add(Modifier.Final);
        if (n.isStatic())
            modifiers.add(Modifier.Static);
        object.setModifiers(modifiers);
    }

    final static class Test {

    }

    private UMLClass getOrCreateEnum(String packageName, String name, ArrayList<String> parents,
                                     String filePath, int line) {
        UMLClass obj;
        String fullName = Package.getFullName(packageName, name, parents);
        if (objects.containsKey(fullName)) {
            obj = objects.get(fullName);
        } else {
            obj = new Enumeration(name);
            objects.put(fullName, obj);
        }
        obj.setParents(parents);
        obj.setFilePath(filePath);
        obj.setLine(line);
        obj.setPackageName(packageName);

        String parentFullName = UMLClass.getParentFullName(packageName, parents);
        if (objects.containsKey(parentFullName)) {
            UMLClass parentClass = objects.get(parentFullName);
            UMLRelationship rel = getNestedClassRel(obj, parentClass);
            obj.getRelationships().put(rel.toString(), rel);
            parentClass.getRelationships().put(rel.toString(), rel);
        }
        return obj;
    }

    private UMLClass getOrCreateObject(String packageName, String name, ArrayList<String> parents,
                                       boolean isInterface, boolean isAbstract, String filePath, int line) {
        UMLClass obj;
        String fullName = Package.getFullName(packageName, name, parents);
        if (objects.containsKey(fullName)) {
            obj = objects.get(fullName);
        } else {
            if (isInterface) {
                obj = new Interface(name);
            } else if (isAbstract) {
                obj = new Abstract(name);
            } else {
                obj = new Class(name);
            }
            objects.put(fullName, obj);
        }
        obj.setParents(parents);
        obj.setFilePath(filePath);
        obj.setLine(line);
        obj.setPackageName(packageName);

        String parentFullName = UMLClass.getParentFullName(packageName, parents);
        if (objects.containsKey(parentFullName)) {
            UMLClass parentClass = objects.get(parentFullName);
            UMLRelationship rel = getNestedClassRel(obj, parentClass);
            obj.getRelationships().put(rel.toString(), rel);
            parentClass.getRelationships().put(rel.toString(), rel);
        }
        return obj;
    }

    private UMLRelationship getNestedClassRel(UMLClass derivedClass, UMLClass parentClass) {
        return new UMLRelationship(derivedClass, parentClass, UMLRelationshipType.Nested);
    }

    private UMLRelationship getSuperClassRel(UMLClass derivedClass, UMLClass superClass) {
        return new UMLRelationship(derivedClass, superClass, UMLRelationshipType.Inheritance);
    }

    private Class getSuperClass(ClassOrInterfaceDeclaration node, String filePath) {
        Class superClassObj = null;
        if (node.getExtendedTypes().size() > 0) {
            ClassOrInterfaceType extType = node.getExtendedTypes(0);
            ResolvedType decl = extType.resolve();
            if (decl.isReferenceType()) {
                ResolvedReferenceTypeDeclaration typeDecl = decl.asReferenceType().getTypeDeclaration().get();
                String superClassPackageName = typeDecl.getPackageName();
                if (isJavaScope(superClassPackageName))
                    return null;
                String superClassName = typeDecl.getName();
                List<String> superParents = getParents(getNode(typeDecl));
                String superClassFullName = Package.getFullName(superClassPackageName,
                        superClassName, superParents);
                if (objects.containsKey(superClassFullName)) {
                    superClassObj = (Class) objects.get(superClassFullName);
                } else {
                    superClassObj = new Class(superClassName);
                    superClassObj.setFilePath(filePath);
                    superClassObj.setPackageName(superClassPackageName);
                    objects.put(superClassFullName, superClassObj);
                }
            }
            return superClassObj;
        }
        return null;
    }


    private UMLRelationship getInterfaceRel(UMLClass derivedClass, UMLClass superClass) {
        return new UMLRelationship(derivedClass, superClass, UMLRelationshipType.Realization);
    }

    private List<Interface> getImplementedInterfaces(UMLClass obj, ClassOrInterfaceDeclaration node, String filePath) {
        List<Interface> implementedInterfaces = new ArrayList<>();
        if (node.getImplementedTypes().size() > 0) {
            for (int i = 0; i < node.getImplementedTypes().size(); i++) {
                try {
                    ClassOrInterfaceType interfaceType = node.getImplementedTypes(i);
                    ResolvedType decl = interfaceType.resolve();
                    if (decl.isReferenceType()) {
                        ResolvedReferenceTypeDeclaration typeDecl = decl.asReferenceType().getTypeDeclaration().get();
                        String interfacePackageName = typeDecl.getPackageName();
                        String interfaceName = typeDecl.getName();
                        List<String> interfaceParents = getParents(getNode(typeDecl));
                        String interfaceFullName = Package.getFullName(interfacePackageName,
                                interfaceName, interfaceParents);
                        Interface interfaceObj;
                        if (objects.containsKey(interfaceFullName)) {
                            interfaceObj = (Interface) objects.get(interfaceFullName);
                        } else {
                            interfaceObj = new Interface(interfaceName);
                            interfaceObj.setFilePath(filePath);
                            interfaceObj.setPackageName(interfacePackageName);
                            objects.put(interfaceFullName, interfaceObj);
                        }
                        implementedInterfaces.add(interfaceObj);
                    }
                } catch (UnsolvedSymbolException ex) {
                    addUnresolvedSymbol(ex.getName(), obj);
                }
            }
            return implementedInterfaces;
        }
        return null;
    }

    private String getPackageName(Node node) {
        String packageName = null;
        if (node == null)
            return null;
        while (node.hasParentNode()) {
            node = node.getParentNode().get();
            if (node instanceof CompilationUnit) {
                packageName = ((CompilationUnit) node).getPackageDeclaration().get().getName().asString();
                break;
            }
        }
        return packageName;
    }


    private ArrayList<String> getParents(Node node) {
        ArrayList<String> parents = new ArrayList<>();
        if (node == null)
            return parents;
        while (node.hasParentNode()) {
            node = node.getParentNode().get();
            if (node instanceof ClassOrInterfaceDeclaration) {
                // nested inside a class
                String parentName = ((ClassOrInterfaceDeclaration) node).getNameAsString();
                parents.add(0, parentName);
            }
        }
        return parents;
    }

    private List<Field> parseFields(NodeWithMembers<?> n, UMLClass obj) {
        List<Field> fields = new LinkedList<>();
        List<FieldDeclaration> flds = n.getFields();
        for (FieldDeclaration f : flds) {
            List<Modifier> modifiers = parseFieldModifiers(f);
            List<AccessModifier> accessModifiers = parseFieldAccessModifiers(f);
            for (VariableDeclarator variableDeclarator : f.getVariables()) {

                Field field = new Field(variableDeclarator.getNameAsString());
                field.setOwner(obj.toString());
                if (variableDeclarator.getType().isClassOrInterfaceType())
                    field.setTypeName(variableDeclarator.getType().asClassOrInterfaceType().getNameAsString());
                try {
                    ResolvedType variableType = variableDeclarator.resolve().getType();
                    if (variableType.isPrimitive()) {
                        field.setPrimitiveType(variableType.describe());
                        field.setTypeName(variableType.asPrimitive().name().toLowerCase());
                    } else if (variableType.isReferenceType()) {
                        ResolvedReferenceTypeDeclaration typeDecl = variableType.asReferenceType().getTypeDeclaration().get();
                        field.setTypeName(typeDecl.getName());
                        field.setTypePackageName(typeDecl.getPackageName());
                        List<String> parents = getParents(getNode(typeDecl));
                        field.setTypeParents(parents);
                    } else if (variableType.isArray()) {
                        field.setArray(true);
                        ResolvedType baseType = ((ResolvedArrayType) variableType).getComponentType();
                        while (baseType instanceof ResolvedArrayType) {
                            baseType = ((ResolvedArrayType) baseType).getComponentType();
                        }
                        if (baseType.isPrimitive()) {
                            field.setPrimitiveType(baseType.describe());
                            field.setTypeName(baseType.asPrimitive().name().toLowerCase());
                        } else if (baseType.isReferenceType()) {
                            ResolvedReferenceTypeDeclaration parameterTypeDecl = baseType.asReferenceType().getTypeDeclaration().get();
                            field.setTypePackageName(parameterTypeDecl.getPackageName());
                            field.setTypeName(parameterTypeDecl.getName());
                            List<String> parents = getParents(getNode(parameterTypeDecl));
                            field.setTypeParents(parents);
                        }
                    } else if (variableType.isTypeVariable()) {
                        field.setTypeVariable(true);
                        // TODO: check if this is a reference type
                        field.setTypeName(variableType.asTypeVariable().asTypeParameter().getName());
                    } else if (!variableType.isArray()) {
                        System.err.println("Could not get type: " + variableType);
                    }
                } catch (UnsolvedSymbolException ex) {
                    addUnresolvedSymbol(ex.getName(), obj);
                }
                field.setLine(variableDeclarator.getBegin().get().line);
                field.setModifiers(modifiers);
                field.setAccessModifiers(accessModifiers);
                fields.add(field);
            }
        }
        return fields;
    }

    private List<EnumConstant> parseEnums(EnumDeclaration n, UMLClass obj) {
        List<EnumConstant> enums = new LinkedList<>();
        int num = 0;
        for (EnumConstantDeclaration constantDeclaration : n.getEntries()) {
            EnumConstant enumConst = new EnumConstant(constantDeclaration.getNameAsString(), num++);
            enumConst.setOwner(obj.toString());
            enums.add(enumConst);
        }
        return enums;
    }

    private List<Method> parseMethods(UMLClass obj, NodeWithMembers<?> n) {
        List<Method> objMethods = new LinkedList<>();
        List<MethodDeclaration> methods = n.getMethods();
        for (MethodDeclaration decl : methods) {
            if (this.methods.containsKey(obj.getFullName() + "." + decl.getSignature()))
                continue;
            Method method = new Method(decl.getName().asString());
            method.setOwner(obj.toString());
            NodeList<com.github.javaparser.ast.body.Parameter> params = decl.getParameters();
            method.setModifiers(parseMethodModifiers(decl));
            method.setAccessModifiers(parseMethodAccessModifiers(decl));
            try {
                Type returnType = decl.getType();
                ResolvedType resolvedReturnType = returnType.resolve();
                if (resolvedReturnType.isReferenceType()) {
                    ResolvedReferenceTypeDeclaration returnTypeDecl = resolvedReturnType.asReferenceType().getTypeDeclaration().get();
                    method.setReturnTypeName(returnTypeDecl.getName());
                    method.setReturnTypePackageName(returnTypeDecl.getPackageName());
                } else if (resolvedReturnType.isPrimitive()) {
                    method.setReturnPrimitiveType(returnType.asPrimitiveType().asString().toLowerCase());
                } else if (!resolvedReturnType.isArray() && !resolvedReturnType.isVoid() && !resolvedReturnType.isTypeVariable()) {
                    System.err.println("Could not get resolvedReturnType: " + resolvedReturnType);
                }
            } catch (UnsolvedSymbolException ex) {
                addUnresolvedSymbol(ex.getName(), obj);
            }
            List<com.mku.liveuml.model.entities.Parameter> parameters = new ArrayList<>();
            for (com.github.javaparser.ast.body.Parameter param : params) {
                String paramName = param.getNameAsString();
                com.mku.liveuml.model.entities.Parameter parameter = new com.mku.liveuml.model.entities.Parameter(paramName);
                try {
                    parseParameterType(parameter, param);
                } catch (UnsolvedSymbolException ex) {
                    addUnresolvedSymbol(ex.getName(), obj);
                }
                parameter.setModifiers(parseParameterModifiers(param));
                parameters.add(parameter);
                if (!parameter.isPrimitiveType())
                    createParameterTypeRelationship(obj, method, parameter);
            }
            method.setParameters(parameters);
            method.setLine(decl.getBegin().get().line);
            objMethods.add(method);
            this.methods.put(obj.getFullName() + "." + decl.getSignature(), method);
        }
        return objMethods;
    }

    private void createParameterTypeRelationship(UMLClass obj, Method method, com.mku.liveuml.model.entities.Parameter parameter) {
        UMLRelationshipType type = UMLRelationshipType.Dependency;
        UMLClass refClass = objects.get(parameter.getTypeFullName());
        if (refClass == null || refClass == obj)
            return;
        UMLRelationship rel = new UMLRelationship(obj, refClass, type);
        String key = rel.toString();
        if (obj.getRelationships().containsKey(key)) {
            rel = obj.getRelationships().get(key);
        } else {
            obj.getRelationships().put(key, rel);
        }
        if (refClass.getRelationships().containsKey(key)) {
            rel = refClass.getRelationships().get(key);
        } else {
            refClass.getRelationships().put(key, rel);
        }
        rel.addClassAccess(method);
    }


    private void createClassTypeParameterRelationship(UMLClass obj, Parameter parameter) {
        UMLRelationshipType type = UMLRelationshipType.Dependency;
        for (String boundFullName : parameter.getBoundsFullNames()) {
            UMLClass refClass = objects.get(boundFullName);
            if (refClass == null || refClass == obj)
                return;
            UMLRelationship rel = new UMLRelationship(obj, refClass, type);
            String key = rel.toString();
            if (obj.getRelationships().containsKey(key)) {
                rel = obj.getRelationships().get(key);
            } else {
                obj.getRelationships().put(key, rel);
            }
            if (refClass.getRelationships().containsKey(key)) {
                rel = refClass.getRelationships().get(key);
            } else {
                refClass.getRelationships().put(key, rel);
            }
        }
    }

    private void parseParameterType(com.mku.liveuml.model.entities.Parameter parameter, com.github.javaparser.ast.body.Parameter param) {
        if (param.getType().isClassOrInterfaceType())
            parameter.setTypeName(param.getType().asClassOrInterfaceType().getNameAsString());
        ResolvedType type = param.resolve().getType();
        if (type.isReferenceType()) {
            ResolvedReferenceTypeDeclaration parameterTypeDecl = type.asReferenceType().getTypeDeclaration().get();
            parameter.setTypeName(parameterTypeDecl.getName());
            parameter.setTypePackageName(parameterTypeDecl.getPackageName());
            parameter.setTypeParents(getParents(getNode(parameterTypeDecl)));
        } else if (type.isPrimitive()) {
            parameter.setPrimitiveType(type.describe());
            parameter.setTypeName(type.asPrimitive().name().toLowerCase());
        } else if (type.isArray()) {
            parameter.setArray(true);
            ResolvedType baseType = ((ResolvedArrayType) type).getComponentType();
            while (baseType instanceof ResolvedArrayType) {
                baseType = ((ResolvedArrayType) baseType).getComponentType();
            }
            if (baseType.isPrimitive()) {
                parameter.setPrimitiveType(baseType.describe());
                parameter.setTypeName(baseType.asPrimitive().name().toLowerCase());
            } else if (baseType.isReferenceType()) {
                ResolvedReferenceTypeDeclaration parameterTypeDecl = baseType.asReferenceType().getTypeDeclaration().get();
                parameter.setTypePackageName(parameterTypeDecl.getPackageName());
                parameter.setTypeName(parameterTypeDecl.getName());
                parameter.setTypeParents(getParents(getNode(parameterTypeDecl)));
            }
        } else if (type.isTypeVariable()) {
            parameter.setTypeVariable(true);
            // TODO: check if this is a reference type
            parameter.setTypeName(type.asTypeVariable().asTypeParameter().getName());
        } else if (!type.isArray() && !type.isVoid() && !type.isTypeVariable()) {
            System.err.println("Could not get param type: " + type);
        }
    }

    private Node getNode(ResolvedReferenceTypeDeclaration parameterTypeDecl) {
        if (parameterTypeDecl instanceof JavaParserInterfaceDeclaration)
            return ((JavaParserInterfaceDeclaration) parameterTypeDecl).getWrappedNode();
        if (parameterTypeDecl instanceof JavaParserClassDeclaration)
            return ((JavaParserClassDeclaration) parameterTypeDecl).getWrappedNode();
        if (parameterTypeDecl instanceof JavaParserEnumDeclaration)
            return ((JavaParserEnumDeclaration) parameterTypeDecl).getWrappedNode();
        return null;
    }

    private List<Modifier> parseMethodModifiers(CallableDeclaration<?> methodDecl) {
        NodeList<com.github.javaparser.ast.Modifier> modifiers = methodDecl.getModifiers();
        List<Modifier> methodModifiers = new LinkedList<>();
        for (com.github.javaparser.ast.Modifier modifier : modifiers) {
            if (modifier.getKeyword() == com.github.javaparser.ast.Modifier.Keyword.STATIC)
                methodModifiers.add(Modifier.Static);
            else if (modifier.getKeyword() == com.github.javaparser.ast.Modifier.Keyword.FINAL)
                methodModifiers.add(Modifier.Final);
            else if (modifier.getKeyword() == com.github.javaparser.ast.Modifier.Keyword.ABSTRACT)
                methodModifiers.add(Modifier.Abstract);
            else if (modifier.getKeyword() == com.github.javaparser.ast.Modifier.Keyword.SYNCHRONIZED)
                methodModifiers.add(Modifier.Synchronized);
            else if (modifier.getKeyword() == com.github.javaparser.ast.Modifier.Keyword.NATIVE)
                methodModifiers.add(Modifier.Native);
            else if (modifier.getKeyword() == com.github.javaparser.ast.Modifier.Keyword.STRICTFP)
                methodModifiers.add(Modifier.StrictFP);
        }
        return methodModifiers;
    }

    private List<AccessModifier> parseMethodAccessModifiers(CallableDeclaration<?> methodDecl) {
        NodeList<com.github.javaparser.ast.Modifier> modifiers = methodDecl.getModifiers();
        List<AccessModifier> methodAccessModifiers = new LinkedList<>();
        for (com.github.javaparser.ast.Modifier modifier : modifiers) {
            if (modifier.getKeyword() == com.github.javaparser.ast.Modifier.Keyword.PUBLIC)
                methodAccessModifiers.add(AccessModifier.Public);
            else if (modifier.getKeyword() == com.github.javaparser.ast.Modifier.Keyword.PROTECTED)
                methodAccessModifiers.add(AccessModifier.Protected);
            else if (modifier.getKeyword() == com.github.javaparser.ast.Modifier.Keyword.PRIVATE)
                methodAccessModifiers.add(AccessModifier.Private);
            else if (modifier.getKeyword() == com.github.javaparser.ast.Modifier.Keyword.DEFAULT)
                methodAccessModifiers.add(AccessModifier.Default);
        }
        return methodAccessModifiers;
    }

    private List<Modifier> parseParameterModifiers(com.github.javaparser.ast.body.Parameter parameter) {
        NodeList<com.github.javaparser.ast.Modifier> modifiers = parameter.getModifiers();
        List<Modifier> accessModifiers = new LinkedList<>();
        for (com.github.javaparser.ast.Modifier modifier : modifiers) {
            if (modifier.getKeyword() == com.github.javaparser.ast.Modifier.Keyword.FINAL)
                accessModifiers.add(Modifier.Final);
        }
        return accessModifiers;
    }


    private List<Modifier> parseFieldModifiers(FieldDeclaration fieldDecl) {
        NodeList<com.github.javaparser.ast.Modifier> modifiers = fieldDecl.getModifiers();
        List<Modifier> methodModifiers = new LinkedList<>();
        for (com.github.javaparser.ast.Modifier modifier : modifiers) {
            if (modifier.getKeyword() == com.github.javaparser.ast.Modifier.Keyword.STATIC)
                methodModifiers.add(Modifier.Static);
            else if (modifier.getKeyword() == com.github.javaparser.ast.Modifier.Keyword.FINAL)
                methodModifiers.add(Modifier.Final);
            else if (modifier.getKeyword() == com.github.javaparser.ast.Modifier.Keyword.TRANSIENT)
                methodModifiers.add(Modifier.Transient);
            else if (modifier.getKeyword() == com.github.javaparser.ast.Modifier.Keyword.VOLATILE)
                methodModifiers.add(Modifier.Volatile);
        }
        return methodModifiers;
    }

    private List<AccessModifier> parseFieldAccessModifiers(FieldDeclaration fieldDecl) {
        NodeList<com.github.javaparser.ast.Modifier> modifiers = fieldDecl.getModifiers();
        List<AccessModifier> methodAccessModifiers = new LinkedList<>();
        for (com.github.javaparser.ast.Modifier modifier : modifiers) {
            if (modifier.getKeyword() == com.github.javaparser.ast.Modifier.Keyword.PUBLIC)
                methodAccessModifiers.add(AccessModifier.Public);
            else if (modifier.getKeyword() == com.github.javaparser.ast.Modifier.Keyword.PROTECTED)
                methodAccessModifiers.add(AccessModifier.Protected);
            else if (modifier.getKeyword() == com.github.javaparser.ast.Modifier.Keyword.PRIVATE)
                methodAccessModifiers.add(AccessModifier.Private);
            else if (modifier.getKeyword() == com.github.javaparser.ast.Modifier.Keyword.DEFAULT)
                methodAccessModifiers.add(AccessModifier.Default);
        }
        return methodAccessModifiers;
    }

    private List<Method> parseConstructors(UMLClass obj, NodeWithMembers<?> n) {
        List<Method> objConstructors = new LinkedList<>();
        List<ConstructorDeclaration> constructors = n.getConstructors();
        for (ConstructorDeclaration decl : constructors) {
            if (this.methods.containsKey(obj.getFullName() + "." + decl.getSignature()))
                continue;
            Constructor constructor = new Constructor(decl.getNameAsString());
            constructor.setOwner(obj.toString());
            constructor.setModifiers(parseMethodModifiers(decl));
            constructor.setAccessModifiers(parseMethodAccessModifiers(decl));
            NodeList<com.github.javaparser.ast.body.Parameter> params = decl.getParameters();
            List<com.mku.liveuml.model.entities.Parameter> parameters = new ArrayList<>();
            for (com.github.javaparser.ast.body.Parameter param : params) {
                String paramName = param.getNameAsString();
                com.mku.liveuml.model.entities.Parameter parameter = new com.mku.liveuml.model.entities.Parameter(paramName);
                try {
                    parseParameterType(parameter, param);
                } catch (UnsolvedSymbolException ex) {
                    addUnresolvedSymbol(ex.getName(), obj);
                }
                parameter.setModifiers(parseParameterModifiers(param));
                parameters.add(parameter);
                if (!parameter.isPrimitiveType())
                    createParameterTypeRelationship(obj, constructor, parameter);
            }
            constructor.setParameters(parameters);
            constructor.setLine(decl.getBegin().get().line);
            objConstructors.add(constructor);
            this.methods.put(obj.getFullName() + "." + decl.getSignature(), constructor);
        }
        return objConstructors;
    }

    public void setNotifyProgress(Consumer<String> notifyProgress) {
        this.notifyProgress = notifyProgress;
    }

    public void removeNotifyProgress() {
        this.notifyProgress = null;
    }

    public class InvalidSourceException extends RuntimeException {
        public InvalidSourceException(String s) {
            super(s);
        }
    }
}
