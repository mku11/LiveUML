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
package com.mku.liveuml.model;

import com.mku.liveuml.entities.EnumConstant;
import com.mku.liveuml.entities.Field;
import com.mku.liveuml.entities.Method;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class UMLFinder {
    public List<HashSet<?>> findEnumConstReference(UMLClass s, EnumConstant ec) {
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

        for (UMLRelationship rel : s.relationships.values()) {
            if (rel.type == UMLRelationshipType.Dependency) {
                if (rel.accessedEnumConstsBy.containsKey(ec)) {
                    Method accessorMethod = rel.accessedEnumConstsBy.get(ec);
                    methodRefs.add(accessorMethod);
                    enumConstRefs.add(ec);
                    classRefs.add(rel.from);
                    classRefs.add(rel.to);
                    relationshipRefs.add(rel);
                }
            }
        }
        return results;
    }

    public List<HashSet<?>> findFieldReference(UMLClass s, Field f) {
        HashSet<Method> methodRefs = new HashSet<>();
        HashSet<Field> fieldRefs = new HashSet<>();
        HashSet<UMLClass> classRefs = new HashSet<>();
        HashSet<UMLRelationship> relationshipRefs = new HashSet<>();

        List<HashSet<?>> results = new ArrayList<>();
        results.add(methodRefs);
        results.add(fieldRefs);
        results.add(classRefs);
        results.add(relationshipRefs);

        for (UMLRelationship rel : s.relationships.values()) {
            if (rel.type == UMLRelationshipType.Dependency) {
                if (rel.accessedFieldsBy.containsKey(f)) {
                    Method accessorMethod = rel.accessedFieldsBy.get(f);
                    methodRefs.add(accessorMethod);
                    classRefs.add(rel.from);
                    classRefs.add(rel.to);
                    relationshipRefs.add(rel);
                }
            }
        }
        return results;
    }

    public List<HashSet<?>> findClassReference(UMLClass s) {
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

        for (UMLRelationship rel : s.relationships.values()) {
            if (rel.type == UMLRelationshipType.Dependency) {
                if (s == rel.to) {
                    methodRefs.addAll(rel.classAccessors);
                    for (Method method : rel.callTo.keySet()) {
                        methodRefs.add(method);
                        methodRefs.add(rel.callTo.get(method));
                    }
                    for (Method method : rel.accessingEnumConsts.keySet()) {
                        methodRefs.add(method);
                        enumConstRefs.add(rel.accessingEnumConsts.get(method));
                    }

                    classRefs.add(rel.from);
                    classRefs.add(rel.to);
                    relationshipRefs.add(rel);
                }
            } else if (rel.type == UMLRelationshipType.Aggregation
                    || rel.type == UMLRelationshipType.Composition
                    || rel.type == UMLRelationshipType.Association
            ) {
                if (s == rel.to) {
                    fieldRefs.addAll(rel.fieldAssociation);
                    classRefs.add(rel.from);
                    classRefs.add(rel.to);
                    relationshipRefs.add(rel);
                }
            } else if (rel.type == UMLRelationshipType.Realization
                    || rel.type == UMLRelationshipType.Inheritance) {
                if (s == rel.to) {
                    classRefs.add(rel.from);
                    classRefs.add(rel.to);
                    relationshipRefs.add(rel);
                }
            }
        }
        return results;
    }

    public List<HashSet<?>> findMethodReference(UMLClass s, Method m) {
        HashSet<Method> methodRefs = new HashSet<>();
        HashSet<Field> fieldRefs = new HashSet<>();
        HashSet<UMLClass> classRefs = new HashSet<>();
        HashSet<UMLRelationship> relationshipRefs = new HashSet<>();

        List<HashSet<?>> results = new ArrayList<>();
        results.add(methodRefs);
        results.add(fieldRefs);
        results.add(classRefs);
        results.add(relationshipRefs);

        for (UMLRelationship rel : s.relationships.values()) {
            if (rel.type == UMLRelationshipType.Dependency) {
                if (rel.calledBy.containsKey(m)) {
                    Method callerMethod = rel.calledBy.get(m);
                    methodRefs.add(callerMethod);
                    methodRefs.add(m);
                    classRefs.add(rel.from);
                    classRefs.add(rel.to);
                    relationshipRefs.add(rel);
                }
            }
        }
        return results;
    }
}
