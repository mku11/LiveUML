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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Parameter {

    private String name;
    private String primitiveType;
    private String typeName;
    private String typePackageName;
    private final List<String> typeParents = new ArrayList<>();
    private final List<Modifier> modifiers = new ArrayList<>();
    private boolean isArray;
    private boolean typeVariable;
    private boolean generic;
    private boolean upperBound;
    private boolean lowerBound;
    private final List<String> bounds = new ArrayList<>();
    private final List<String> boundsFullNames = new ArrayList<>();

    public Parameter(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPrimitiveType() {
        return primitiveType;
    }

    public void setPrimitiveType(String primitiveType) {
        this.primitiveType = primitiveType;
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

    public boolean isArray() {
        return isArray;
    }

    public void setArray(boolean array) {
        isArray = array;
    }

    public List<Modifier> getModifiers() {
        return Collections.unmodifiableList(modifiers);
    }

    public void setModifiers(List<Modifier> modifiers) {
        this.modifiers.clear();
        this.modifiers.addAll(modifiers);
    }

    public String getTypeName() {
        return typeName;
    }

    public boolean isPrimitiveType() {
        return primitiveType != null;
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

    public List<String> getTypeParents() {
        return typeParents;
    }
    public String getTypeFullName() {
        return Package.getFullName(typePackageName, typeName, typeParents);
    }

    public void setTypeParents(List<String> parents) {
        this.typeParents.clear();
        if(parents!=null)
            this.typeParents.addAll(parents);
    }

    public void setGeneric(boolean value) {
        this.generic = value;
    }

    public boolean isGeneric() {
        return generic;
    }


    public boolean isUpperBound() {
        return upperBound;
    }

    public void setUpperBound(boolean upperBound) {
        this.upperBound = upperBound;
    }

    public boolean isLowerBound() {
        return lowerBound;
    }

    public void setLowerBound(boolean lowerBound) {
        this.lowerBound = lowerBound;
    }

    public List<String> getBounds() {
        return Collections.unmodifiableList(this.bounds);
    }

    public void setBounds(List<String> bounds) {
        this.bounds.clear();
        this.bounds.addAll(bounds);
    }

    public List<String> getBoundsFullNames() {
        return boundsFullNames;
    }

    public void setBoundsFullNames(List<String> bounds) {
        this.boundsFullNames.clear();
        this.boundsFullNames.addAll(bounds);
    }
}
