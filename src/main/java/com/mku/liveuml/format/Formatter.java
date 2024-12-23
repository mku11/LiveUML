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

import com.mku.liveuml.graph.UMLRelationship;
import com.mku.liveuml.graph.UMLClass;
import com.mku.liveuml.entities.AccessModifier;
import com.mku.liveuml.entities.Parameter;
import com.mku.liveuml.entities.Field;
import com.mku.liveuml.entities.Method;

import java.util.HashSet;

public class Formatter {
    private static final int MAX_CHARS = 50;
    private static final String selectionBackgroundColor = "background-color: yellow;";

    public static String display(UMLClass object, boolean compact,
                                 HashSet<UMLClass> selectedVertices, HashSet<UMLRelationship> selectedEdges,
                                 HashSet<Method> selectedMethods, HashSet<Field> selectedFields) {
        String classBackgroundColor = getSelectionColor(object, selectedVertices, selectedEdges, selectedMethods, selectedFields);
        StringBuilder body = new StringBuilder("<html><body style='font-family: monospace; font-size: 10px; font-weight: bold;'>");
        body.append("<div><div><center style='margin: 4px; font-size: 14px;").append(classBackgroundColor).append("'>").append(object.getName()).append("</center><div>");
        if (!compact) {
            if (object.getFields().size() > 0) {
                body.append("<hr class=\"solid\">");
                body.append("<div style='padding: 8px;'>");
                for (Field field : object.getFields()) {
                    String fieldBackgroundColor = "";
                    if (selectedFields.contains(field))
                        fieldBackgroundColor = selectionBackgroundColor;
                    body.append("<div style='").append(fieldBackgroundColor).append("'>").append(getFieldFormatted(field)).append("</div>").append("<br>");
                }
                body.append("</div>");
            }
            if (object.getMethods().size() > 0) {
                body.append("<hr class=\"solid\">");
                body.append("<div style='padding: 8px;'>");
                for (Method method : object.getMethods()) {
                    String signature = getMethodSignature(method, true);
                    String methodBackgroundColor = "";
                    if (selectedMethods.contains(method))
                        methodBackgroundColor = selectionBackgroundColor;
                    signature = "<div style='" + methodBackgroundColor + "'>" + signature + "</div>";
                    signature += "<br>";
                    body.append(signature);
                }
                body.append("</div><br>");
            }
        }
        body.append("<div></body></html>");
        return body.toString();
    }

    private static String getSelectionColor(UMLClass object, HashSet<UMLClass> selectedVertices, HashSet<UMLRelationship> selectedEdges,
                                            HashSet<Method> selectedMethods, HashSet<Field> selectedFields) {
        if (selectedVertices.contains(object))
            return selectionBackgroundColor;
        for (UMLRelationship rel : object.relationships.values()) {
            if (selectedEdges.contains(rel)) {
                return selectionBackgroundColor;
            }
        }
        for (Field field : object.getFields()) {
            if (selectedFields.contains(field)) {
                return selectionBackgroundColor;
            }
        }
        for (Method method : object.getMethods()) {
            if (selectedMethods.contains(method)) {
                return selectionBackgroundColor;
            }
        }
        return "";
    }

    public static String getFieldFormatted(Field field) {
        return getFieldQualifier(field) + " " + field.getName() + " : " + field.getTypeName();
    }

    public static String getMethodSignature(Method method, boolean usePrefix) {
        String signature = "";
        if (usePrefix)
            signature += getMethodQualifier(method) + " ";
        signature += method.getName() + "(";
        StringBuilder params = new StringBuilder();
        for (Parameter parameter : method.getParameters()) {
            if (params.length() > 0)
                params.append(", ");
            params.append(parameter.getName()).append(" : ").append(parameter.getTypeName());
        }
        signature += params;
        signature += ")";
        if (!method.isReturnTypeVoid())
            signature += " : " + method.getReturnTypeName();
        if (signature.length() > MAX_CHARS)
            signature = signature.substring(0, MAX_CHARS) + "...";
        return signature;
    }

    private static String getMethodQualifier(Method method) {
        if (method.getAccessModifiers().contains(AccessModifier.Protected))
            return "#";
        else if (method.getAccessModifiers().contains(AccessModifier.Private))
            return "-";
        else if (method.getAccessModifiers().contains(AccessModifier.Public))
            return "+";
        return "";
    }

    private static String getFieldQualifier(Field field) {
        if (field.getAccessModifiers().contains(AccessModifier.Protected))
            return "#";
        else if (field.getAccessModifiers().contains(AccessModifier.Private))
            return "-";
        else if (field.getAccessModifiers().contains(AccessModifier.Public))
            return "+";
        return "";
    }
}
