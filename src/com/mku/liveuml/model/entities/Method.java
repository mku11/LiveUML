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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class Method {
    private String name;
    private String returnTypeName;
    private String returnTypePackageName;
    private String returnPrimitiveType;
    private final List<String> returnTypeParents = new ArrayList<>();
    private String owner;
    private int line;
    private List<Modifier> modifiers = new LinkedList<>();
    private List<AccessModifier> accessModifiers = new LinkedList<>();

    private List<Parameter> parameters = new LinkedList<>();

    public Method(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setReturnTypeName(String returnTypeName) {
        this.returnTypeName = returnTypeName;
    }

    public String getReturnTypePackageName() {
        return returnTypePackageName;
    }

    public void setReturnTypePackageName(String returnTypePackageName) {
        this.returnTypePackageName = returnTypePackageName;
    }

    public String getReturnPrimitiveType() {
        return returnPrimitiveType;
    }

    public void setReturnPrimitiveType(String returnPrimitiveType) {
        this.returnPrimitiveType = returnPrimitiveType;
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

    public List<Modifier> getModifiers() {
        return Collections.unmodifiableList(modifiers);
    }

    public void setModifiers(List<Modifier> modifiers) {
        this.modifiers.clear();
        this.modifiers.addAll(modifiers);
    }

    public List<AccessModifier> getAccessModifiers() {
        return Collections.unmodifiableList(this.accessModifiers);
    }

    public void setAccessModifiers(List<AccessModifier> accessModifiers) {
        this.accessModifiers.clear();
        this.accessModifiers.addAll(accessModifiers);
    }

    public List<Parameter> getParameters() {
        return Collections.unmodifiableList(parameters);
    }

    public void setParameters(List<Parameter> parameters) {
        this.parameters.clear();
        this.parameters.addAll(parameters);
    }

    public boolean isReturnTypeVoid() {
        return !isReturnTypePrimitive() && returnTypeName == null;
    }

    public void setLine(int line) {
        this.line = line;
    }

    public boolean isReturnTypePrimitive() {
        return returnPrimitiveType != null;
    }

    public String getReturnTypeName() {
        return isReturnTypePrimitive() ? returnPrimitiveType : returnTypeName;
    }

    @Override
    public String toString() {
        return name;
    }

    public String getSignature() {
        String signature = "";
        signature += name + "(";
        String params = "";
        for (Parameter parameter : parameters) {
            if (params.length() > 0)
                params += ", ";
            params += parameter.getTypeName();
        }
        signature += params;
        signature += ")";
        return signature;
    }

    public List<String> getReturnTypeParents() {
        return returnTypeParents;
    }

    public String getReturnTypeFullName() {
        return UMLClass.getFullName(returnTypePackageName, returnTypeName, returnTypeParents);
    }

    public void setReturnTypeParents(List<String> parents) {
        this.returnTypeParents.clear();
        if (parents != null)
            this.returnTypeParents.addAll(parents);
    }

}
