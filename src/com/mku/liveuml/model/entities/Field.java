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
package com.mku.liveuml.model.entities;

import com.mku.liveuml.model.diagram.UMLClass;

import java.util.*;

public class Field {
    private String name;
    private List<Modifier> modifiers = new LinkedList<>();
    private List<AccessModifier> accessModifiers = new LinkedList<>();
    private boolean isArray;
    private String baseTypeName;
    private String typeName;
    private String typePackageName;
    private final List<String> typeParents = new ArrayList<>();
    private String primitiveType;
    private String owner;
    private int line;
    private boolean typeVariable;

    public Field(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Modifier> getModifiers() {
        return Collections.unmodifiableList(modifiers);
    }

    public void setModifiers(List<Modifier> modifiers) {
        this.modifiers = modifiers;
    }

    public List<AccessModifier> getAccessModifiers() {
        return Collections.unmodifiableList(accessModifiers);
    }

    public void setAccessModifiers(List<AccessModifier> accessModifiers) {
        this.accessModifiers.clear();
        this.accessModifiers.addAll(accessModifiers);
    }

    public boolean isArray() {
        return isArray;
    }

    public void setArray(boolean array) {
        isArray = array;
    }

    public String getBaseTypeName() {
        return baseTypeName;
    }

    public void setBaseTypeName(String baseTypeName) {
        this.baseTypeName = baseTypeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public String getTypePackageName() {
        return typePackageName;
    }

    public void setTypePackageName(String typePackageName) {
        this.typePackageName = typePackageName;
    }

    public String getPrimitiveType() {
        return primitiveType;
    }

    public void setPrimitiveType(String primitiveType) {
        this.primitiveType = primitiveType;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public int getLine() {
        return line;
    }

    public void setLine(int line) {
        this.line = line;
    }

    public boolean isPrimitiveType() {
        return primitiveType != null;
    }

    public String getTypeName() {
        return isPrimitiveType() ? primitiveType : typeName;
    }

    @Override
    public String toString() {
        return name;
    }

    public void setTypeVariable(boolean value) {
        this.typeVariable = value;
    }

    public boolean isTypeVariable() {
        return typeVariable;
    }

    public String getTypeFullName() {
        return UMLClass.getFullName(typePackageName, typeName, typeParents);
    }
    public void setTypeParents(List<String> parents) {
        this.typeParents.clear();
        if(parents!=null)
            this.typeParents.addAll(parents);
    }
}
