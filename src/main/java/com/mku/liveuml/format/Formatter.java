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
package com.mku.liveuml.format;

import com.mku.liveuml.graph.Relationship;
import com.mku.liveuml.graph.UMLObject;
import com.mku.liveuml.meta.Variable;
import com.mku.liveuml.meta.Field;
import com.mku.liveuml.meta.Method;

import java.util.HashSet;

public class Formatter {
    private static int MAX_CHARS = 50;
    private static String selectionBackgroundColor = "background-color: yellow;";

    public static String display(UMLObject object, boolean compact,
                                 HashSet<UMLObject> selectedVertices, HashSet<Relationship> selectedEdges,
                                 HashSet<Method> selectedMethods, HashSet<Field> selectedFields) {
        String classBackgroundColor = getSelectionColor(object, selectedVertices, selectedEdges, selectedMethods, selectedFields);
        String body = "<html><body style='font-family: monospace; font-size: 10px; font-weight: bold;'>";
        body += "<div><div><center style='margin: 4px; font-size: 14px;" + classBackgroundColor + "'>" + object.name + "</center><div>";
        if(!compact) {
            if (object.fields.size() > 0) {
                body += "<hr class=\"solid\">";
                body += "<div style='padding: 8px;'>";
                for (Field field : object.fields) {
                    String fieldBackgroundColor = "";
                    if(selectedFields.contains(field))
                        fieldBackgroundColor =selectionBackgroundColor;
                    body += "<div style='" + fieldBackgroundColor + "'>" + getFieldFormatted(field) + "</div>" + "<br>";
                }
                body += "</div>";
            }
            if (object.methods.size() > 0) {
                body += "<hr class=\"solid\">";
                body += "<div style='padding: 8px;'>";
                for (Method method : object.methods) {
                    String signature = getMethodSignature(method, true);
                    String methodBackgroundColor = "";
                    if(selectedMethods.contains(method))
                        methodBackgroundColor =selectionBackgroundColor;
                    signature = "<div style='" + methodBackgroundColor + "'>" + signature + "</div>";
                    signature += "<br>";
                    body += signature;
                }
                body += "</div><br>";
            }
        }
        body += "<div></body></html>";
        return body;
    }

    private static String getSelectionColor(UMLObject object, HashSet<UMLObject> selectedVertices, HashSet<Relationship> selectedEdges,
                                            HashSet<Method> selectedMethods, HashSet<Field> selectedFields) {
        if(selectedVertices.contains(object))
            return selectionBackgroundColor;
        for (Relationship rel : object.relationships.values()) {
            if (selectedEdges.contains(rel)) {
                return selectionBackgroundColor;
            }
        }
        for (Field field: object.fields) {
            if (selectedFields.contains(field)) {
                return selectionBackgroundColor;
            }
        }
        for (Method method : object.methods) {
            if (selectedMethods.contains(method)) {
                return selectionBackgroundColor;
            }
        }
        return "";
    }

    public static String getFieldFormatted(Field field) {
        return getFieldQualifier(field) + " " + field.name + " : " + field.getTypeName();
    }

    public static String getMethodSignature(Method method, boolean usePrefix) {
        String signature = "";
        if(usePrefix)
            signature += getMethodQualifier(method) + " ";
        signature += method.name + "(";
        String params = "";
        for (Variable variable : method.parameters) {
            if (params.length() > 0)
                params += ", ";
            params += variable.name + " : " + variable.getTypeName();
        }
        signature += params;
        signature += ")";
        if (!method.isReturnTypeVoid())
            signature += " : " + method.getReturnTypeName();
        if(signature.length() > MAX_CHARS)
            signature = signature.substring(0,MAX_CHARS) + "...";
        return signature;
    }

    private static String getMethodQualifier(Method method) {
        if (method.accessModifiers.contains(Method.AccessModifier.Protected))
            return "#";
        else if (method.accessModifiers.contains(Method.AccessModifier.Private))
            return "-";
        else if (method.accessModifiers.contains(Method.AccessModifier.Public))
            return "+";
        return "";
    }

    private static String getFieldQualifier(Field field) {
        if (field.accessModifiers.contains(Field.AccessModifier.Protected))
            return "#";
        else if (field.accessModifiers.contains(Field.AccessModifier.Private))
            return "-";
        else if (field.accessModifiers.contains(Field.AccessModifier.Public))
            return "+";
        return "";
    }
}
