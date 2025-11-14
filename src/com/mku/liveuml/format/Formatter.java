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

import com.mku.liveuml.entities.*;
import com.mku.liveuml.graph.UMLRelationship;
import com.mku.liveuml.graph.UMLClass;

import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Formatter {
    private static final int MAX_CHARS = 80;
    private final String classHtmlTemplate;
    private final String propertyHtmlTemplate;
    private final String dividerHtmlTemplate;
    private static final String classHeaderColor = "black";
    private static final String classHeaderBackgroundColor = "white";
    private static final String classSelectedHeaderColor = "#6388E6";
    private static final String classSelectedHeaderBackgroundColor = "white";

    private static final String propertyColor = "black";
    private static final String propertyBackgroundColor = "white";
    private static final String propertySelectedColor = "#6388E6";
    private static final String propertySelectedBackgroundColor = "#C1D1E3";

    public Formatter(String classHtmlTemplate, String propertyHtmlTemplate, String dividerHtmlTemplate) {
        this.classHtmlTemplate = classHtmlTemplate;
        this.propertyHtmlTemplate = propertyHtmlTemplate;
        this.dividerHtmlTemplate = dividerHtmlTemplate;
    }

    public String getUmlAsHtml(UMLClass object, boolean compact,
                               HashSet<UMLClass> selectedVertices, HashSet<UMLRelationship> selectedEdges,
                               HashSet<Method> selectedMethods, HashSet<Field> selectedFields,
                               HashSet<EnumConstant> selectedEnumConsts) {
        boolean classSelected = isClassSelected(object, selectedVertices, selectedEdges, selectedMethods, selectedFields);
        String formattedHtml = classHtmlTemplate
                .replaceAll(Pattern.quote("${name}"),
                        Matcher.quoteReplacement(object.getName()))
                .replaceAll(Pattern.quote("${class-color}"),
                        Matcher.quoteReplacement(classSelected ?
                                classSelectedHeaderColor : classHeaderColor))
                .replaceAll(Pattern.quote("${class-background-color}"),
                        Matcher.quoteReplacement(classSelected ?
                                classSelectedHeaderBackgroundColor : classHeaderBackgroundColor));

        // get enums
        String formattedEnums = getFormattedEnums(object, selectedEnumConsts, compact);
        formattedHtml = formattedHtml.replaceAll(Pattern.quote("${enums}"),
                Matcher.quoteReplacement(formattedEnums.toString()));

        // get fields
        String formattedFields = getFormattedFields(object, selectedFields, compact);
        formattedHtml = formattedHtml.replaceAll(Pattern.quote("${fields}"),
                Matcher.quoteReplacement(formattedFields.toString()));

        // get methods
        String formattedMethods = getFormattedMethods(object, selectedMethods, compact);
        formattedHtml = formattedHtml.replaceAll(Pattern.quote("${methods}"),
                Matcher.quoteReplacement(formattedMethods.toString()));

        return formattedHtml;
    }

    private String getFormattedEnums(UMLClass object, HashSet<EnumConstant> selectedEnums, boolean compact) {
        StringBuilder enums = new StringBuilder();
        if (!compact && object.getEnumConstants().size() > 0) {
            enums.append(dividerHtmlTemplate).append("\n");
            for (EnumConstant enumConst : object.getEnumConstants()) {
                String property = propertyHtmlTemplate.replaceAll(Pattern.quote("${content}"),
                                Matcher.quoteReplacement(enumConst.getName()))
                        .replaceAll(Pattern.quote("${property-color}"),
                                Matcher.quoteReplacement(selectedEnums.contains(enumConst) ?
                                        propertySelectedColor : propertyColor))
                        .replaceAll(Pattern.quote("${property-background-color}"),
                                Matcher.quoteReplacement(selectedEnums.contains(enumConst) ?
                                        propertySelectedBackgroundColor : propertyBackgroundColor));
                enums.append(property).append("\n");
            }
        }
        return enums.toString();
    }

    private String getFormattedFields(UMLClass object, HashSet<Field> selectedFields, boolean compact) {
        StringBuilder fields = new StringBuilder();
        if (!compact && object.getFields().size() > 0) {
            fields.append(dividerHtmlTemplate).append("\n");
            for (Field field : object.getFields()) {
                String property = propertyHtmlTemplate.replaceAll(Pattern.quote("${content}"),
                                Matcher.quoteReplacement(getFieldFormatted(field)))
                        .replaceAll(Pattern.quote("${property-color}"),
                                Matcher.quoteReplacement(selectedFields.contains(field) ?
                                        propertySelectedColor : propertyColor))
                        .replaceAll(Pattern.quote("${property-background-color}"),
                                Matcher.quoteReplacement(selectedFields.contains(field) ?
                                        propertySelectedBackgroundColor : propertyBackgroundColor));
                fields.append(property).append("\n");
            }
        }
        return fields.toString();
    }

    private String getFormattedMethods(UMLClass object, HashSet<Method> selectedMethods, boolean compact) {
        StringBuilder fields = new StringBuilder();
        if (!compact && object.getFields().size() > 0) {
            fields.append(dividerHtmlTemplate).append("\n");
            for (Method method : object.getMethods()) {
                String property = propertyHtmlTemplate.replaceAll(Pattern.quote("${content}"),
                                Matcher.quoteReplacement(getMethodSignature(method, true)))
                        .replaceAll(Pattern.quote("${property-color}"),
                                Matcher.quoteReplacement(selectedMethods.contains(method) ?
                                        propertySelectedColor : propertyColor))
                        .replaceAll(Pattern.quote("${property-background-color}"),
                                Matcher.quoteReplacement(selectedMethods.contains(method) ?
                                        propertySelectedBackgroundColor : propertyBackgroundColor));
                fields.append(property).append("\n");
            }
        }
        return fields.toString();
    }

    private static boolean isClassSelected(UMLClass object, HashSet<UMLClass> selectedVertices, HashSet<UMLRelationship> selectedEdges,
                                           HashSet<Method> selectedMethods, HashSet<Field> selectedFields) {
        if (selectedVertices.contains(object))
            return true;
        for (UMLRelationship rel : object.relationships.values()) {
            if (selectedEdges.contains(rel))
                return true;
        }
        for (Field field : object.getFields()) {
            if (selectedFields.contains(field))
                return true;
        }
        for (Method method : object.getMethods()) {
            if (selectedMethods.contains(method))
                return true;
        }
        return false;
    }

    public String getFieldFormatted(Field field) {
        return getFieldQualifier(field) + " " + field.getName() + " : " + field.getTypeName();
    }

    public String getMethodSignature(Method method, boolean usePrefix) {
        return getMethodSignature(method, usePrefix, true);
    }

    public String getMethodSignature(Method method, boolean usePrefix, boolean truncate) {
        String signature = "";
        if (usePrefix)
            signature += getMethodQualifier(method) + " ";
        signature += method.getName() + " (";
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
        if (truncate && signature.length() > MAX_CHARS)
            signature = signature.substring(0, MAX_CHARS) + "...";
        return signature;
    }

    private String getMethodQualifier(Method method) {
        if (method.getAccessModifiers().contains(AccessModifier.Protected))
            return "#";
        else if (method.getAccessModifiers().contains(AccessModifier.Private))
            return "-";
        else if (method.getAccessModifiers().contains(AccessModifier.Public))
            return "+";
        return "";
    }

    private String getFieldQualifier(Field field) {
        if (field.getAccessModifiers().contains(AccessModifier.Protected))
            return "#";
        else if (field.getAccessModifiers().contains(AccessModifier.Private))
            return "-";
        else if (field.getAccessModifiers().contains(AccessModifier.Public))
            return "+";
        return "";
    }
}
