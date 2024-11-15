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

import com.mku.liveuml.entities.Class;
import com.mku.liveuml.entities.Enumeration;
import com.mku.liveuml.entities.Interface;

public class UMLClassFactory {
    public static UMLClass create(String name) {
        String[] parts = name.split(":");
        UMLClass.Type type = UMLClass.Type.valueOf(parts[0]);
        String packageName = parts[1].substring(0, parts[1].lastIndexOf("."));
        String className = parts[1].substring(parts[1].lastIndexOf(".") + 1);
        UMLClass obj = null;
        switch (type) {
            case Class:
                obj = new Class(className);
                break;
            case Interface:
                obj = new Interface(className);
                break;
            case Enumeration:
                obj = new Enumeration(className);
                break;
        }
        if (obj == null)
            throw new RuntimeException("Unknown uml type");
        obj.setPackageName(packageName);
        return obj;
    }
}
