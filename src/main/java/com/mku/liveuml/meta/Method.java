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
package com.mku.liveuml.meta;

import java.util.LinkedList;
import java.util.List;

public class Method {

    public enum AccessModifier {
        Default, Public, Private, Protected
    }

    public enum Modifier {
        Final, Static, Abstract, Synchronized, Native, StrictFP
    }

    public String name;
    public String returnTypeName;
    public String returnTypePackageName;
    public String returnPrimitiveType;
    public String owner;
    public int line;
    public List<Modifier> modifiers = new LinkedList<>();
    public List<AccessModifier> accessModifiers = new LinkedList<>();

    public List<Parameter> parameters = new LinkedList<>();

    public Method(String name) {
        this.name = name;
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

    public String getReturnTypeName () {
        return isReturnTypePrimitive()?returnPrimitiveType:returnTypeName;
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
}
