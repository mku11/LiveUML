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
import java.util.HashSet;
import java.util.List;

public class UMLFinder {

    public enum ReferenceType {
        From,
        To,
        Both
    }

    public List<HashSet<?>> findRelReference(UMLRelationship rel) {
        HashSet<Method> methodRefs = new HashSet<>();
        HashSet<EnumConstant> enumConstRefs = new HashSet<>();
        HashSet<Field> fieldRefs = new HashSet<>();
        HashSet<UMLClass> classRefs = new HashSet<>();
        HashSet<UMLRelationship> relationshipRefs = new HashSet<>();

        List<HashSet<?>> results = new ArrayList<>();
        results.add(methodRefs);
        results.add(enumConstRefs);
        results.add(fieldRefs);
        results.add(classRefs);
        results.add(relationshipRefs);

        methodRefs.addAll(rel.getMethodsAccessingClass());
        methodRefs.addAll(rel.getMethodsAccessingEnums().keySet());
        methodRefs.addAll(rel.getMethodsAccessingFields().keySet());
        methodRefs.addAll(rel.getMethodsAccessedByMethods().keySet());
        methodRefs.addAll(rel.getMethodsAccesingMethods().keySet());
        enumConstRefs.addAll(rel.getEnumsAccessedByMethods().keySet());
        fieldRefs.addAll(rel.getFieldsAccessingClass());
        fieldRefs.addAll(rel.getFieldsAccessedByMethods().keySet());
        classRefs.add(rel.getFrom());
        classRefs.add(rel.getTo());
        relationshipRefs.add(rel);

        return results;
    }

    public List<HashSet<?>> findClassReference(UMLClass s,
                                               HashSet<UMLRelationshipType> relFilter,
                                               ReferenceType type) {
        HashSet<Method> methodRefs = new HashSet<>();
        HashSet<EnumConstant> enumConstRefs = new HashSet<>();
        HashSet<Field> fieldRefs = new HashSet<>();
        HashSet<UMLClass> classRefs = new HashSet<>();
        HashSet<UMLRelationship> relationshipRefs = new HashSet<>();

        List<HashSet<?>> results = new ArrayList<>();
        results.add(methodRefs);
        results.add(enumConstRefs);
        results.add(fieldRefs);
        results.add(classRefs);
        results.add(relationshipRefs);

        for (UMLRelationship rel : s.getRelationships().values()) {
            if (type == ReferenceType.From && s == rel.getFrom())
                continue;
            if (type == ReferenceType.To && s == rel.getTo())
                continue;
            if (relFilter != null && !relFilter.contains(rel.getType()))
                continue;
            methodRefs.addAll(rel.getMethodsAccessingClass());
            methodRefs.addAll(rel.getMethodsAccessingEnums().keySet());
            methodRefs.addAll(rel.getMethodsAccessingFields().keySet());
            methodRefs.addAll(rel.getMethodsAccessedByMethods().keySet());
            methodRefs.addAll(rel.getMethodsAccesingMethods().keySet());
            enumConstRefs.addAll(rel.getEnumsAccessedByMethods().keySet());
            fieldRefs.addAll(rel.getFieldsAccessingClass());
            fieldRefs.addAll(rel.getFieldsAccessedByMethods().keySet());
            classRefs.add(rel.getFrom());
            classRefs.add(rel.getTo());
            relationshipRefs.add(rel);
        }
        return results;
    }

    public List<HashSet<?>> findEnumConstReference(UMLClass s, EnumConstant ec, ReferenceType type) {
        HashSet<Method> methodRefs = new HashSet<>();
        HashSet<EnumConstant> enumConstRefs = new HashSet<>();
        HashSet<Field> fieldRefs = new HashSet<>();
        HashSet<UMLClass> classRefs = new HashSet<>();
        HashSet<UMLRelationship> relationshipRefs = new HashSet<>();

        List<HashSet<?>> results = new ArrayList<>();
        results.add(methodRefs);
        results.add(enumConstRefs);
        results.add(fieldRefs);
        results.add(classRefs);
        results.add(relationshipRefs);

        for (UMLRelationship rel : s.getRelationships().values()) {
            if (type == ReferenceType.From && s == rel.getFrom())
                continue;
            if (type == ReferenceType.To && s == rel.getTo())
                continue;
            if (type == ReferenceType.From && rel.getEnumsAccessedByMethods().containsKey(ec)) {
                for (Method accessorMethod : rel.getEnumsAccessedByMethods().get(ec)) {
                    methodRefs.add(accessorMethod);
                    enumConstRefs.add(ec);
                    classRefs.add(rel.getFrom());
                    classRefs.add(rel.getTo());
                    relationshipRefs.add(rel);
                }
            }
        }
        return results;
    }

    public List<HashSet<?>> findFieldReference(UMLClass s, Field f, ReferenceType type) {
        HashSet<Method> methodRefs = new HashSet<>();
        HashSet<Field> fieldRefs = new HashSet<>();
        HashSet<UMLClass> classRefs = new HashSet<>();
        HashSet<UMLRelationship> relationshipRefs = new HashSet<>();

        List<HashSet<?>> results = new ArrayList<>();
        results.add(methodRefs);
        results.add(fieldRefs);
        results.add(classRefs);
        results.add(relationshipRefs);

        for (UMLRelationship rel : s.getRelationships().values()) {
            if (type == ReferenceType.From && s == rel.getFrom())
                continue;
            if (type == ReferenceType.To && s == rel.getTo())
                continue;
            if (rel.getFieldsAccessedByMethods().containsKey(f)) {
                for (Method accessorMethod : rel.getFieldsAccessedByMethods().get(f)) {
                    methodRefs.add(accessorMethod);
                    fieldRefs.add(f);
                    classRefs.add(rel.getFrom());
                    classRefs.add(rel.getTo());
                    relationshipRefs.add(rel);
                }
            }
        }
        return results;
    }

    public List<HashSet<?>> findMethodReference(UMLClass s, Method m, ReferenceType type) {
        HashSet<Method> methodRefs = new HashSet<>();
        HashSet<EnumConstant> enumConstRefs = new HashSet<>();
        HashSet<Field> fieldRefs = new HashSet<>();
        HashSet<UMLClass> classRefs = new HashSet<>();
        HashSet<UMLRelationship> relationshipRefs = new HashSet<>();

        List<HashSet<?>> results = new ArrayList<>();
        results.add(methodRefs);
        results.add(enumConstRefs);
        results.add(fieldRefs);
        results.add(classRefs);
        results.add(relationshipRefs);

        for (UMLRelationship rel : s.getRelationships().values()) {
            if (type == ReferenceType.From && s == rel.getFrom())
                continue;
            if (type == ReferenceType.To && s == rel.getTo())
                continue;
            if (type == ReferenceType.From && rel.getMethodsAccessedByMethods().containsKey(m)) {
                for (Method callerMethod : rel.getMethodsAccessedByMethods().get(m)) {
                    methodRefs.add(callerMethod);
                    methodRefs.add(m);
                    classRefs.add(rel.getFrom());
                    classRefs.add(rel.getTo());
                    relationshipRefs.add(rel);
                }
            }
            if (type == ReferenceType.To && rel.getMethodsAccesingMethods().containsKey(m)) {
                for (Method callerMethod : rel.getMethodsAccesingMethods().get(m)) {
                    methodRefs.add(callerMethod);
                    methodRefs.add(m);
                    classRefs.add(rel.getFrom());
                    classRefs.add(rel.getTo());
                    relationshipRefs.add(rel);
                }
            }
            if (type == ReferenceType.To && rel.getMethodsAccessingFields().containsKey(m)) {
                for (Field field : rel.getMethodsAccessingFields().get(m)) {
                    fieldRefs.add(field);
                    methodRefs.add(m);
                    classRefs.add(rel.getFrom());
                    classRefs.add(rel.getTo());
                    relationshipRefs.add(rel);
                }
            }
            if (type == ReferenceType.To && rel.getMethodsAccessingEnums().containsKey(m)) {
                for (EnumConstant enumConstant : rel.getMethodsAccessingEnums().get(m)) {
                    enumConstRefs.add(enumConstant);
                    methodRefs.add(m);
                    classRefs.add(rel.getFrom());
                    classRefs.add(rel.getTo());
                    relationshipRefs.add(rel);
                }
            }
            if (type == ReferenceType.To && rel.getMethodsAccessingClass().contains(m)) {
                methodRefs.add(m);
                classRefs.add(rel.getFrom());
                classRefs.add(rel.getTo());
                relationshipRefs.add(rel);
            }
        }
        return results;
    }
}
