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
package com.mku.liveuml.gen;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.mku.liveuml.entities.EnumConstant;
import com.mku.liveuml.entities.Field;
import com.mku.liveuml.entities.Method;
import com.mku.liveuml.graph.UMLClass;
import com.mku.liveuml.graph.UMLRelationship;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultGraphType;
import org.jgrapht.graph.builder.GraphTypeBuilder;
import org.jgrapht.util.SupplierUtil;

import javax.swing.*;
import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class UMLGenerator {
    private ReflectionTypeSolver reflectionTypeSolver;
    private CombinedTypeSolver combinedSolver;
    private Graph<UMLClass, UMLRelationship> graph;
    private final UMLFinder finder;
    private ParserConfiguration parserConfiguration;
    private HashMap<String, UMLClass> vertices;

    public UMLParser getParser() {
        return parser;
    }

    private UMLParser parser;

    public UMLGenerator() {
        this.finder = new UMLFinder();
        this.parser = new UMLParser();
        createGraph();
    }

    public void importSourcesDir(File root) {
        setupFolder(root);
        List<UMLClass> classes = parser.getClasses(root);
        addClasses(classes);
    }

    private void setupFolder(File sourceFolder) {
        if (reflectionTypeSolver == null) {
            reflectionTypeSolver = new ReflectionTypeSolver();
            combinedSolver = new CombinedTypeSolver();
            combinedSolver.add(reflectionTypeSolver);
            parserConfiguration = new ParserConfiguration();
        }
        JavaParserTypeSolver javaParserTypeSolver = new JavaParserTypeSolver(sourceFolder);
        combinedSolver.add(javaParserTypeSolver);
        parserConfiguration.setSymbolResolver(new JavaSymbolSolver(combinedSolver));
        StaticJavaParser.setConfiguration(parserConfiguration);
    }

    public void createGraph() {
        graph = GraphTypeBuilder.<UMLClass, UMLRelationship>forGraphType(DefaultGraphType.directedMultigraph())
                .edgeSupplier(SupplierUtil.createSupplier(UMLRelationship.class))
                .buildGraph();
    }

    public void addClasses(List<UMLClass> umlClasses) {
        if (graph == null)
            createGraph();
        for (UMLClass obj : umlClasses) {
            graph.addVertex(obj);
        }
        updateRelationships(umlClasses, graph);
    }

    public void updateVertices(HashMap<String, UMLClass> vertices) {
        this.vertices = vertices;
        for (UMLRelationship rel : graph.edgeSet()) {
            UMLClass from = vertices.getOrDefault(rel.from.toString(), null);
            UMLClass to = vertices.getOrDefault(rel.to.toString(), null);
            from.relationships.put(rel.toString(), rel);
            to.relationships.put(rel.toString(), rel);
        }
    }

    public void updateRelationships(List<UMLClass> umlClasses, Graph<UMLClass, UMLRelationship> graph) {
        for (UMLClass obj : umlClasses) {
            boolean hasValidRelationships = false;
            for (Map.Entry<String, UMLRelationship> rel : obj.relationships.entrySet()) {
                if (rel.getValue().from == rel.getValue().to)
                    continue;
                if (!graph.containsVertex(rel.getValue().from) || !graph.containsVertex(rel.getValue().to)) {
                    continue;
                }
                try {
                    graph.addEdge(rel.getValue().from, rel.getValue().to, rel.getValue());
                    hasValidRelationships = true;
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            if (!hasValidRelationships)
                graph.removeVertex(obj);
        }
    }

    public Graph<UMLClass, UMLRelationship> getGraph() {
        return this.graph;
    }

    public List<HashSet<?>> findMethodReference(UMLClass s, Method m) {
        return finder.findMethodReference(s, m);
    }

    public List<HashSet<?>> findClassReference(UMLClass s) {
        return finder.findClassReference(s);
    }

    public List<HashSet<?>> findFieldReference(UMLClass s, Field f) {
        return finder.findFieldReference(s, f);
    }

    public List<HashSet<?>> findEnumConstReference(UMLClass s, EnumConstant ec) {
        return finder.findEnumConstReference(s, ec);
    }

    public void clear() {
        graph = null;
        reflectionTypeSolver = null;
        parser.clear();
    }

    public UMLClass getClassByName(String name) {
        if(vertices == null)
            return null;
        return vertices.getOrDefault(name, null);
    }
}
