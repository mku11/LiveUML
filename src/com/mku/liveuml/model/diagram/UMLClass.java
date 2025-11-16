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
package com.mku.liveuml.model.diagram;

import com.mku.liveuml.model.entities.EnumConstant;
import com.mku.liveuml.model.entities.Field;
import com.mku.liveuml.model.entities.Method;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class UMLClass {
    private String name;
    private String packageName;
    private final List<String> parents = new ArrayList<>();
    private final HashMap<String, Method> methods = new HashMap<>();
    private final HashMap<String, Field> fields = new HashMap<>();
    private final HashMap<String, EnumConstant> enumConstants = new HashMap<>();
    private final HashMap<String, UMLRelationship> relationships = new HashMap<>();

    private boolean compact;
    private String fileSource;
    private String filePath;
    private int line;

    public void addMethods(List<Method> methods) {
        for (Method method : methods) {
            if (this.methods.containsKey(method.getSignature()))
                continue;
            this.methods.put(method.getSignature(), method);
        }
    }

    public String getFilePath() {
        return filePath;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public boolean isCompact() {
        return compact;
    }

    public void setCompact(boolean compact) {
        this.compact = compact;
    }

    public List<Method> getMethods() {
        return new ArrayList<>(methods.values());
    }

    public void setMethods(List<Method> methods) {
        this.methods.clear();
        for (Method method : methods) {
            this.methods.put(method.getSignature(), method);
        }
    }

    public List<Field> getFields() {
        return new ArrayList<>(fields.values());
    }

    public void setFields(List<Field> fields) {
        this.fields.clear();
        for (Field field : fields)
            this.fields.put(field.getName(), field);
    }

    public List<EnumConstant> getEnumConstants() {
        return new ArrayList<>(enumConstants.values());
    }

    public void setEnumConstants(List<EnumConstant> enums) {
        this.enumConstants.clear();
        for (EnumConstant enumConst : enums)
            this.enumConstants.put(enumConst.getName(), enumConst);
    }

    public int getLine() {
        return line;
    }

    public HashMap<String, UMLRelationship> getRelationships() {
        return relationships;
    }

    public void setRelationships(HashMap<String, UMLRelationship> relationships) {
        this.relationships.clear();
        this.relationships.putAll(relationships);
    }


    protected UMLClass(String name) {
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
        return getFullName(this.packageName, this.name, this.parents);
    }

    public void setParents(List<String> parents) {
        this.parents.clear();
        this.parents.addAll(parents);
    }

    public String getFileSource() {
        return fileSource;
    }

    public void setFileSource(String fileSource) {
        this.fileSource = fileSource;
    }

    public List<String> getParents() {
        return parents;
    }

    public static String getFullName(String packageName, String name, List<String> parents) {
        String fullName = "";
        String parentsName = parents == null ? "" : String.join("$", parents);
        if (packageName != null && packageName.length() > 0)
            fullName = packageName;
        if (parentsName.length() > 0)
            fullName += "." + parentsName + "$" + name;
        else
            fullName += "." + name;
        return fullName;
    }

    public static String getParentFullName(String packageName, List<String> parents) {
        if (parents == null || parents.size() == 0)
            return null;
        return UMLClass.getFullName(packageName, parents.get(parents.size() - 1), parents.subList(0, parents.size() - 1));
    }

    public void addEnumConstants(List<EnumConstant> enums) {
        for (EnumConstant enumConst : enums) {
            if (this.enumConstants.containsKey(enumConst.getName()))
                continue;
            this.enumConstants.put(enumConst.getName(), enumConst);
        }
    }

    public void addFields(List<Field> fields) {
        for (Field field : fields) {
            if (this.fields.containsKey(field.getName()))
                continue;
            this.fields.put(field.getName(), field);
        }
    }
}
