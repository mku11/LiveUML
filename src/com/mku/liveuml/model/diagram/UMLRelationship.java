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

import com.mku.liveuml.model.entities.EnumConstant;
import com.mku.liveuml.model.entities.Field;
import com.mku.liveuml.model.entities.Method;

import java.util.*;

public class UMLRelationship {
    private UMLRelationshipType type;
    private UMLClass from;
    private UMLClass to;

    private HashMap<Method, HashSet<Method>> methodsAccessedByMethods = new HashMap<>();
    private HashMap<Method, HashSet<Method>> methodsAccesingMethods = new HashMap<>();

    private HashMap<Field, HashSet<Method>> fieldsAccessedByMethods = new HashMap<>();
    private HashMap<Method, HashSet<Field>> methodsAccessingFields = new HashMap<>();

    private HashMap<EnumConstant, HashSet<Method>> enumsAccessedByMethods = new HashMap<>();
    private HashMap<Method, HashSet<EnumConstant>> methodsAccessingEnums = new HashMap<>();

    private HashSet<Method> methodsAccessingClass = new HashSet<>();

    private HashSet<Field> fieldsAccessingClass = new HashSet<>();

    // DO NOT REMOVE needed by importer
    public UMLRelationship() {

    }

    public UMLRelationship(UMLClass from, UMLClass to, UMLRelationshipType type) {
        this.from = from;
        this.to = to;
        this.type = type;
    }


    public UMLRelationshipType getType() {
        return type;
    }

    public void setType(UMLRelationshipType type) {
        this.type = type;
    }

    public UMLClass getFrom() {
        return from;
    }

    public void setFrom(UMLClass from) {
        this.from = from;
    }

    public UMLClass getTo() {
        return to;
    }

    public void setTo(UMLClass to) {
        this.to = to;
    }

    public Map<Method, HashSet<Method>> getMethodsAccessedByMethods() {
        return Collections.unmodifiableMap(methodsAccessedByMethods);
    }

    public void setMethodsAccessedByMethods(HashMap<Method, HashSet<Method>> methodsAccessedByMethods) {
        this.methodsAccessedByMethods = methodsAccessedByMethods;
    }

    public Map<Method, HashSet<Method>> getMethodsAccesingMethods() {
        return Collections.unmodifiableMap(methodsAccesingMethods);
    }

    public void setMethodsAccesingMethods(HashMap<Method, HashSet<Method>> methodsAccesingMethods) {
        this.methodsAccesingMethods = methodsAccesingMethods;
    }

    public Map<Field, HashSet<Method>> getFieldsAccessedByMethods() {
        return Collections.unmodifiableMap(fieldsAccessedByMethods);
    }

    public void setFieldsAccessedByMethods(HashMap<Field, HashSet<Method>> fieldsAccessedByMethods) {
        this.fieldsAccessedByMethods = fieldsAccessedByMethods;
    }

    public Map<Method, HashSet<Field>> getMethodsAccessingFields() {
        return Collections.unmodifiableMap(methodsAccessingFields);
    }

    public void setMethodsAccessingFields(HashMap<Method, HashSet<Field>> methodsAccessingFields) {
        this.methodsAccessingFields = methodsAccessingFields;
    }

    public Map<EnumConstant, HashSet<Method>> getEnumsAccessedByMethods() {
        return Collections.unmodifiableMap(enumsAccessedByMethods);
    }

    public void setEnumsAccessedByMethods(HashMap<EnumConstant, HashSet<Method>> enumsAccessedByMethods) {
        this.enumsAccessedByMethods = enumsAccessedByMethods;
    }

    public Map<Method, HashSet<EnumConstant>> getMethodsAccessingEnums() {
        return Collections.unmodifiableMap(methodsAccessingEnums);
    }

    public void setMethodsAccessingEnums(HashMap<Method, HashSet<EnumConstant>> methodsAccessingEnums) {
        this.methodsAccessingEnums = methodsAccessingEnums;
    }

    public Set<Method> getMethodsAccessingClass() {
        return Collections.unmodifiableSet(methodsAccessingClass);
    }

    public void setMethodsAccessingClass(HashSet<Method> methodsAccessingClass) {
        this.methodsAccessingClass = methodsAccessingClass;
    }

    public Set<Field> getFieldsAccessingClass() {
        return Collections.unmodifiableSet(fieldsAccessingClass);
    }

    public void setFieldsAccessingClass(HashSet<Field> fieldsAccessingClass) {
        this.fieldsAccessingClass = fieldsAccessingClass;
    }

    public void addMethodCall(Method callerMethod, Method calleeMethod) {
        HashSet<Method> calleeMethods = methodsAccesingMethods.getOrDefault(callerMethod, new HashSet<>());
        methodsAccesingMethods.put(callerMethod, calleeMethods);
        calleeMethods.add(calleeMethod);

        HashSet<Method> callerMethods = methodsAccesingMethods.getOrDefault(calleeMethod, new HashSet<>());
        methodsAccessedByMethods.put(calleeMethod, callerMethods);
        callerMethods.add(callerMethod);
    }

    public void addFieldAccess(Method accessorMethod, Field accessedField) {
        HashSet<Field> accessedFields = methodsAccessingFields.getOrDefault(accessorMethod, new HashSet<>());
        methodsAccessingFields.put(accessorMethod, accessedFields);
        accessedFields.add(accessedField);

        HashSet<Method> accessorMethods = fieldsAccessedByMethods.getOrDefault(accessedField, new HashSet<>());
        fieldsAccessedByMethods.put(accessedField, accessorMethods);
        accessorMethods.add(accessorMethod);
    }

    public void addEnumConstAccess(Method accessorMethod, EnumConstant accessedField) {
        HashSet<EnumConstant> accessedFields = methodsAccessingEnums.getOrDefault(accessorMethod, new HashSet<>());
        methodsAccessingEnums.put(accessorMethod, accessedFields);
        accessedFields.add(accessedField);

        HashSet<Method> accessorMethods = enumsAccessedByMethods.getOrDefault(accessedField, new HashSet<>());
        enumsAccessedByMethods.put(accessedField, accessorMethods);
        accessorMethods.add(accessorMethod);
    }

    public void addClassAccess(Method accessorMethod) {
        methodsAccessingClass.add(accessorMethod);
    }

    public void addFieldAssociation(Field field) {
        fieldsAccessingClass.add(field);
    }

    @Override
    public String toString() {
        return from + ":" + type.name() + ":" + to;
    }
}
