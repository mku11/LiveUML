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

        methodRefs.addAll(rel.classAccessors);
        methodRefs.addAll(rel.accessingEnumConsts.keySet());
        methodRefs.addAll(rel.accessingFields.keySet());
        methodRefs.addAll(rel.calledBy.keySet());
        methodRefs.addAll(rel.callTo.keySet());
        enumConstRefs.addAll(rel.accessedEnumConstsBy.keySet());
        fieldRefs.addAll(rel.fieldAssociation);
        fieldRefs.addAll(rel.accessedFieldsBy.keySet());
        classRefs.add(rel.from);
        classRefs.add(rel.to);
        relationshipRefs.add(rel);
        return results;
    }

    public List<HashSet<?>> findClassReference(UMLClass s,
                                               HashSet<UMLRelationshipType> relFilter) {
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
            if (relFilter == null && s != rel.to)
                continue;
            if(relFilter != null && !relFilter.contains(rel.type))
                continue;
            methodRefs.addAll(rel.classAccessors);
            methodRefs.addAll(rel.accessingEnumConsts.keySet());
            methodRefs.addAll(rel.accessingFields.keySet());
            methodRefs.addAll(rel.calledBy.keySet());
            methodRefs.addAll(rel.callTo.keySet());
            enumConstRefs.addAll(rel.accessedEnumConstsBy.keySet());
            fieldRefs.addAll(rel.fieldAssociation);
            fieldRefs.addAll(rel.accessedFieldsBy.keySet());
            classRefs.add(rel.from);
            classRefs.add(rel.to);
            relationshipRefs.add(rel);
        }
        return results;
    }

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

        for (UMLRelationship rel : s.getRelationships().values()) {
            if (s != rel.to)
                continue;
            if (rel.accessedEnumConstsBy.containsKey(ec)) {
                Method accessorMethod = rel.accessedEnumConstsBy.get(ec);
                methodRefs.add(accessorMethod);
                enumConstRefs.add(ec);
                classRefs.add(rel.from);
                classRefs.add(rel.to);
                relationshipRefs.add(rel);
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

        for (UMLRelationship rel : s.getRelationships().values()) {
            if (s != rel.to)
                continue;
            if (rel.accessedFieldsBy.containsKey(f)) {
                Method accessorMethod = rel.accessedFieldsBy.get(f);
                methodRefs.add(accessorMethod);
                fieldRefs.add(f);
                classRefs.add(rel.from);
                classRefs.add(rel.to);
                relationshipRefs.add(rel);
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

        for (UMLRelationship rel : s.getRelationships().values()) {
            if (s != rel.to)
                continue;
            if (rel.calledBy.containsKey(m)) {
                Method callerMethod = rel.calledBy.get(m);
                methodRefs.add(callerMethod);
                methodRefs.add(m);
                classRefs.add(rel.from);
                classRefs.add(rel.to);
                relationshipRefs.add(rel);
            }
        }
        return results;
    }
}
