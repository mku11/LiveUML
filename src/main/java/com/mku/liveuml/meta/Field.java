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
import java.util.*;

public class Field {
    public String name;
    public List<Modifier> modifiers = new LinkedList<>();
    public List<AccessModifier> accessModifiers  = new LinkedList<>();
    public boolean isArray;
    public String baseTypeName;
    public String typeName;
    public String typePackageName;
    public String primitiveType;
    public String owner;
    public int line;

    public Field(String name) {
        this.name = name;
    }
    public String getTypeFullName() {
        String fullName = typePackageName + "." + typeName;
        if(isArray) {
            int index = fullName.indexOf("[");
            if (index >= 0)
                fullName = fullName.substring(0, index);
        }
        return fullName;
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

}
