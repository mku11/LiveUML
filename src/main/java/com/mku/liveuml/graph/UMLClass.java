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
import java.util.LinkedList;
import java.util.List;

public abstract class UMLClass {
    private String filePath;
    private String name;
    private String packageName;
    private boolean compact;

    private List<Method> methods = new LinkedList<>();
    private List<Field> fields = new LinkedList<>();
    private int line;

    public void addMethods(List<Method> methods) {
        this.methods.addAll(methods);
    }

    public enum Type {
        Class, Interface, Enumeration
    }

    public String getFilePath() {
        return filePath;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public boolean isCompact() {
        return compact;
    }

    public void setCompact(boolean compact) {
        this.compact = compact;
    }

    public List<Method> getMethods() {
        return methods;
    }

    public void setMethods(List<Method> methods) {
        this.methods = methods;
    }

    public List<Field> getFields() {
        return fields;
    }

    public void setFields(List<Field> fields) {
        this.fields.clear();
        this.fields.addAll(fields);
    }

    public int getLine() {
        return line;
    }

    public HashMap<String, UMLRelationship> getRelationships() {
        return relationships;
    }

    public void setRelationships(HashMap<String, UMLRelationship> relationships) {
        this.relationships = relationships;
    }

    public HashMap<String, UMLRelationship> relationships = new HashMap<>();

    protected UMLClass(String name) {
        this.name = name;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public void setLine(int line) {
        this.line = line;
    }

    @Override
    public String toString() {
        String str = "";
        if(packageName!=null)
            str += packageName + ".";
        str += name;
        return str;
    }
}
