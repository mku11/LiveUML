package com.mku.liveuml.gen;

import com.mku.liveuml.entities.Field;
import com.mku.liveuml.entities.Method;
import com.mku.liveuml.graph.UMLClass;
import com.mku.liveuml.graph.UMLRelationship;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class UMLFinder {

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

        for(UMLRelationship rel : s.relationships.values()) {
            if(rel.type == UMLRelationship.Type.Dependency) {
                if(rel.accessedBy.containsKey(f)) {
                    Method accessorMethod = rel.accessedBy.get(f);
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
        HashSet<Field> fieldRefs = new HashSet<>();
        HashSet<UMLClass> classRefs = new HashSet<>();
        HashSet<UMLRelationship> relationshipRefs = new HashSet<>();

        List<HashSet<?>> results = new ArrayList<>();
        results.add(methodRefs);
        results.add(fieldRefs);
        results.add(classRefs);
        results.add(relationshipRefs);

        for(UMLRelationship rel : s.relationships.values()) {
            if(rel.type == UMLRelationship.Type.Dependency) {
                if(s == rel.to) {
                    methodRefs.addAll(rel.classAccessors);
                    for(Method method : rel.callTo.keySet()) {
                        methodRefs.add(method);
                        methodRefs.add(rel.callTo.get(method));
                    }
                    classRefs.add(rel.from);
                    classRefs.add(rel.to);
                    relationshipRefs.add(rel);
                }
            } else if(rel.type == UMLRelationship.Type.Aggregation
                    || rel.type == UMLRelationship.Type.Composition
                    || rel.type == UMLRelationship.Type.Association
            ) {
                if(s == rel.to) {
                    fieldRefs.addAll(rel.fieldAssociation);
                    classRefs.add(rel.from);
                    classRefs.add(rel.to);
                    relationshipRefs.add(rel);
                }
            } else if(rel.type == UMLRelationship.Type.Realization
                    || rel.type == UMLRelationship.Type.Inheritance) {
                if(s == rel.to) {
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

        for(UMLRelationship rel : s.relationships.values()) {
            if(rel.type == UMLRelationship.Type.Dependency) {
                if(rel.calledBy.containsKey(m)) {
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
