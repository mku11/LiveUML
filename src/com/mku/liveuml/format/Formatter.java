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

import com.mku.liveuml.model.diagram.UMLDiagram;
import com.mku.liveuml.model.diagram.UMLParser;
import com.mku.liveuml.model.diagram.UMLRelationship;
import com.mku.liveuml.model.diagram.UMLClass;
import com.mku.liveuml.model.entities.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Formatter {
    private final String classHtmlTemplate;
    private final String propertyHtmlTemplate;
    private final String dividerHtmlTemplate;
    public static final String classSelectedColor = "#6388E6";
    public static final String classHeaderColor = "black";
    public static final String classHeaderBackgroundColor = "white";
    public static final String classSelectedHeaderColor = "#6388E6";
    public static final String classSelectedHeaderBackgroundColor = "white";
    public static final String propertyColor = "black";
    public static final String propertyBackgroundColor = "white";
    public static final String propertySelectedColor = "#6388E6";
    public static final String propertySelectedBackgroundColor = "#C1D1E3";
    public static final String unresolvedSymbolColor = "#E83F83";
    private static String rightArrowQuote = "&#10219";
    private static String leftArrowQuote = "&#10218";
    private static String space = "&#10240";

    /**
     * Do not use px for styles it causes exceptions within awt
     *
     * @param classHtmlTemplate
     * @param propertyHtmlTemplate
     * @param dividerHtmlTemplate
     */
    public Formatter(String classHtmlTemplate, String propertyHtmlTemplate, String dividerHtmlTemplate) {
        this.classHtmlTemplate = classHtmlTemplate;
        this.propertyHtmlTemplate = propertyHtmlTemplate;
        this.dividerHtmlTemplate = dividerHtmlTemplate;
    }

    public String getUmlAsHtml(UMLClass object, boolean compact, UMLDiagram diagram) {
        boolean classSelected = isClassSelected(object, diagram);
        String stereotypes = getFormattedStereoTypes(object);
        String typeParams = getFormattedTypeParameters(object);
        String formattedHtml = classHtmlTemplate
                .replaceAll(Pattern.quote("${name}"),
                        Matcher.quoteReplacement(object.getName()))
                .replaceAll(Pattern.quote("${class-color}"),
                        Matcher.quoteReplacement(classSelected ?
                                classSelectedHeaderColor : classHeaderColor))
                .replaceAll(Pattern.quote("${class-background-color}"),
                        Matcher.quoteReplacement(classSelected ?
                                classSelectedHeaderBackgroundColor : classHeaderBackgroundColor));
        formattedHtml = formattedHtml.replaceAll(Pattern.quote("${stereotypes}"),
                Matcher.quoteReplacement(stereotypes.length() > 0 ? "<div>" + stereotypes + "</div>" : ""));
        formattedHtml = formattedHtml.replaceAll(Pattern.quote("${type-params}"),
                Matcher.quoteReplacement(typeParams.length() > 0 ? "<div>" + typeParams + "</div>" : ""));

        // get enums
        String formattedEnums = getFormattedEnums(object, diagram.getSelectedEnumConsts(), compact);
        formattedHtml = formattedHtml.replaceAll(Pattern.quote("${enums}"),
                Matcher.quoteReplacement(formattedEnums));

        // get fields
        String formattedFields = getFormattedFields(object, diagram.getSelectedFields(), compact);
        formattedHtml = formattedHtml.replaceAll(Pattern.quote("${fields}"),
                Matcher.quoteReplacement(formattedFields));

        // get methods
        String formattedMethods = getFormattedMethods(object, diagram.getSelectedMethods(), compact);
        formattedHtml = formattedHtml.replaceAll(Pattern.quote("${methods}"),
                Matcher.quoteReplacement(formattedMethods));

        return formattedHtml;
    }

    private String getFormattedStereoTypes(UMLClass object) {
        List<String> stereoTypes = getStereoTypes(object);
        StringBuilder stereotype = new StringBuilder();
        for (String st : stereoTypes)
            stereotype.append(leftArrowQuote).append(st).append(rightArrowQuote);
        return stereotype.toString();
    }

    private String getFormattedTypeParameters(UMLClass object) {
        StringBuilder typeParam = new StringBuilder();
        for (Parameter param : object.getTypeParameters()) {
            if (typeParam.length() > 0)
                typeParam.append(",").append(space);
            String descr = "";
            String bounds = String.join(" ", param.getBounds());
            if(param.isLowerBound())
                descr = "super" + space + bounds;
            else if(param.isUpperBound())
                descr = "extends" + space + bounds;
            typeParam.append(param.getName() + space + descr);
        }
        return typeParam.toString();
    }

    private List<String> getStereoTypes(UMLClass object) {
        List<String> stereotypes = new ArrayList<>();
        if (object instanceof Enumeration)
            stereotypes.add("enumeration");
        if (object instanceof Interface)
            stereotypes.add("interface");
        if (object instanceof Abstract)
            stereotypes.add("abstract");
        if (object.getModifiers().contains(Modifier.Final))
            stereotypes.add("final");
        if (object.getModifiers().contains(Modifier.Static))
            stereotypes.add("static");
        return stereotypes;
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
                property = property.replaceAll(Pattern.quote("${stereotypes}"), Matcher.quoteReplacement(""));
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
                                Matcher.quoteReplacement(getFieldFormatted(field, true)))
                        .replaceAll(Pattern.quote("${property-color}"),
                                Matcher.quoteReplacement(selectedFields.contains(field) ?
                                        propertySelectedColor : propertyColor))
                        .replaceAll(Pattern.quote("${property-background-color}"),
                                Matcher.quoteReplacement(selectedFields.contains(field) ?
                                        propertySelectedBackgroundColor : propertyBackgroundColor));

                String stereotypes = "";
                if (field.getModifiers().contains(Modifier.Transient))
                    stereotypes += leftArrowQuote + "transient" + rightArrowQuote;
                if (field.getModifiers().contains(Modifier.Final)) {
                    stereotypes += leftArrowQuote + "final" + rightArrowQuote;
                }
                if (field.getModifiers().contains(Modifier.Static)) {
                    stereotypes += leftArrowQuote + "static" + rightArrowQuote;
                }
                if (field.getModifiers().contains(Modifier.Native)) {
                    stereotypes += leftArrowQuote + "native" + rightArrowQuote;
                }
                if (field.getModifiers().contains(Modifier.Volatile)) {
                    stereotypes += leftArrowQuote + "volatile" + rightArrowQuote;
                }
                property = property.replaceAll(Pattern.quote("${stereotypes}"),
                        Matcher.quoteReplacement(stereotypes));
                fields.append(property).append("\n");
            }
        }
        return fields.toString();
    }

    private String getFormattedMethods(UMLClass object, HashSet<Method> selectedMethods, boolean compact) {
        StringBuilder methods = new StringBuilder();
        if (!compact && object.getMethods().size() > 0) {
            methods.append(dividerHtmlTemplate).append("\n");
            List<Method> mtds = new ArrayList<>();
            for (Method method : object.getMethods()) {
                if (method instanceof Constructor)
                    mtds.add(method);
            }
            for (Method method : object.getMethods()) {
                if (!(method instanceof Constructor))
                    mtds.add(method);
            }
            for (Method method : mtds) {
                String property = propertyHtmlTemplate.replaceAll(Pattern.quote("${content}"),
                                Matcher.quoteReplacement(getMethodSignature(method, true, true)))
                        .replaceAll(Pattern.quote("${property-color}"),
                                Matcher.quoteReplacement(selectedMethods.contains(method) ?
                                        propertySelectedColor : propertyColor))
                        .replaceAll(Pattern.quote("${property-background-color}"),
                                Matcher.quoteReplacement(selectedMethods.contains(method) ?
                                        propertySelectedBackgroundColor : propertyBackgroundColor));
                String stereotypes = "";
                if (method.getModifiers().contains(Modifier.Abstract)) {
                    stereotypes += leftArrowQuote + "abstract" + rightArrowQuote;
                }
                if (method.getModifiers().contains(Modifier.Static)) {
                    stereotypes += leftArrowQuote + "static" + rightArrowQuote;
                }
                if (method.getModifiers().contains(Modifier.Final)) {
                    stereotypes += leftArrowQuote + "final" + rightArrowQuote;
                }
                property = property.replaceAll(Pattern.quote("${stereotypes}"),
                        Matcher.quoteReplacement(stereotypes));
                methods.append(property).append("\n");
            }
        }
        return methods.toString();
    }

    private static boolean isClassSelected(UMLClass object, UMLDiagram diagram) {
        if (diagram.getSelectedVertices().contains(object))
            return true;
        for (UMLRelationship rel : object.getRelationships().values()) {
            if (diagram.getSelectedEdges().contains(rel))
                return true;
        }
        for (Field field : object.getFields()) {
            if (diagram.getSelectedFields().contains(field))
                return true;
        }
        for (Method method : object.getMethods()) {
            if (diagram.getSelectedMethods().contains(method))
                return true;
        }
        return false;
    }

    public static String getFieldFormatted(Field field, boolean isHtml) {
        String formattedField = getFieldQualifier(field) + " ";
        if (isHtml) {
            String textdecoration = "none";
            String fontStyle = "normal";
            if (field.getModifiers().contains(Modifier.Abstract)) {
                fontStyle = "italics";
            }
            if (field.getModifiers().contains(Modifier.Static)) {
                textdecoration = "underline";
            }
            formattedField += "<font style='text-decoration: " + textdecoration + "';";
            formattedField += "fontStyle: ";
            formattedField += fontStyle + ";'>";
        }
        formattedField += field.getName();
        if (isHtml) {
            formattedField += "</font>";
        }
        if (isHtml)
            formattedField += space + ":" + space;
        else
            formattedField += " : ";
        formattedField += field.getTypeName();
        if (field.isArray())
            formattedField += "[]";
        return formattedField;
    }

    public static String getMethodSignature(Method method, boolean usePrefix, boolean isHtml) {
        String signature = "";
        if (usePrefix)
            signature += getMethodQualifier(method) + " ";
        if (isHtml) {
            String textdecoration = "none";
            String fontStyle = "normal";
            if (method.getModifiers().contains(Modifier.Abstract)) {
                fontStyle = "italics";
            }
            if (method.getModifiers().contains(Modifier.Static)) {
                textdecoration = "underline";
            }
            signature += "<font style='text-decoration: " + textdecoration + "';";
            signature += "fontStyle: ";
            signature += fontStyle + ";'>";
        }
        signature += method.getName();
        if (isHtml) {
            signature += "</font>";
        }
        signature += " (";
        StringBuilder params = new StringBuilder();
        for (Parameter parameter : method.getParameters()) {
            if (params.length() > 0) {
                if (isHtml)
                    params.append("," + space);
                else
                    params.append(", ");
            }

            params.append(parameter.getName());
            if (isHtml)
                params.append(space + ":" + space);
            else
                params.append(" : ");
            params.append(parameter.getTypeName());
            if (parameter.isArray())
                params.append("[]");
        }
        signature += params;
        signature += ")";
        if (!method.isReturnTypeVoid()) {
            if (isHtml) {
                signature += space + ":" + space;
            } else {
                signature += " : ";
            }
            signature += method.getReturnTypeName();
        }
        return signature;
    }

    public static String getMethodQualifier(Method method) {
        if (method.getAccessModifiers().contains(AccessModifier.Protected))
            return "#";
        else if (method.getAccessModifiers().contains(AccessModifier.Private))
            return "-";
        else if (method.getAccessModifiers().contains(AccessModifier.Public))
            return "+";
        return "~";
    }

    public static String getFieldQualifier(Field field) {
        if (field.getAccessModifiers().contains(AccessModifier.Protected))
            return "#";
        else if (field.getAccessModifiers().contains(AccessModifier.Private))
            return "-";
        else if (field.getAccessModifiers().contains(AccessModifier.Public))
            return "+";
        return "~";
    }

    public static String formatUnresolvedSymbols(UMLParser parser) {
        StringBuilder stringBuilder = new StringBuilder();
        for (Map.Entry<String, UMLParser.SymbolInformation> entry : parser.getUnresolvedSymbols().entrySet()) {
            stringBuilder.append(entry.getKey());
            stringBuilder.append("\n");
        }
        return stringBuilder.toString();
    }
}
