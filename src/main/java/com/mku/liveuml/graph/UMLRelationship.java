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
package com.mku.liveuml.graph;

import com.mku.liveuml.meta.Field;
import com.mku.liveuml.meta.Method;

import java.util.HashMap;
import java.util.HashSet;

public class UMLRelationship {

    public enum Type {
        Association, Inheritance, Realization, Dependency, Aggregation, Composition
    }

    public Type type;
    public UMLClass from;
    public UMLClass to;

    public HashMap<Method, Method> calledBy = new HashMap<>();
    public HashMap<Method, Method> callTo = new HashMap<>();

    public HashMap<Field, Method> accessedBy = new HashMap<>();
    public HashMap<Method, Field> accessing = new HashMap<>();

    public HashSet<Method> classAccessors = new HashSet<>();

    public HashSet<Field> fieldAssociation = new HashSet<>();

    public UMLRelationship() {

    }
    public UMLRelationship(UMLClass from, UMLClass to, Type type) {
        this.from = from;
        this.to = to;
        this.type = type;
    }

    public void addMethodCall(Method callerMethod, Method calleeMethod) {
        if(callerMethod != null)
            callTo.put(callerMethod, calleeMethod);
        calledBy.put(calleeMethod, callerMethod);
    }

    public void addFieldAccess(Method accessorMethod, Field accessedField) {
        if(accessorMethod != null)
            accessing.put(accessorMethod, accessedField);
        accessedBy.put(accessedField, accessorMethod);
    }

    public void addClassAccess(Method accessorMethod) {
        if(accessorMethod != null)
            classAccessors.add(accessorMethod);
    }

    public void addFieldAssociation(Field field) {
        fieldAssociation.add(field);
    }

    @Override
    public String toString() {
        return from + ":" + type.name() + ":" + to;
    }
}
