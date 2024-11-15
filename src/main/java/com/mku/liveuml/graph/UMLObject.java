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

public abstract class UMLObject {
    public String filePath;
    public String name;
    public String packageName;
    public boolean compact;

    public List<Method> methods = new LinkedList<>();
    public List<Field> fields = new LinkedList<>();
    public int line;

    public enum AccessModifier {
        Default, Public, Private, Protected
    }

    public enum Modifier {
        Final, Abstract
    }

    public enum Type {
        Class, Interface, Enumeration
    }

    public HashMap<String, Relationship> relationships = new HashMap<>();

    protected UMLObject(String name) {
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
