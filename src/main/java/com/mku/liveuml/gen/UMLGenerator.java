package com.mku.liveuml.gen;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.mku.liveuml.entities.Field;
import com.mku.liveuml.entities.Method;
import com.mku.liveuml.graph.UMLClass;
import com.mku.liveuml.graph.UMLRelationship;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultGraphType;
import org.jgrapht.graph.builder.GraphTypeBuilder;
import org.jgrapht.util.SupplierUtil;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class UMLGenerator {
    private Graph<UMLClass, UMLRelationship> graph;
    private UMLFinder finder;

    public UMLGenerator() {
        this.finder = new UMLFinder();
        createGraph();
    }

    public void importSourcesDir(File root) {
        setupFolder(root);
        List<UMLClass> classes = new UMLParser().getClasses(root);
        addClasses(classes);
    }

    private static void setupFolder(File sourceFolder) {
        ReflectionTypeSolver reflectionTypeSolver = new ReflectionTypeSolver();
        JavaParserTypeSolver javaParserTypeSolver = new JavaParserTypeSolver(sourceFolder);
        CombinedTypeSolver combinedSolver = new CombinedTypeSolver();
        combinedSolver.add(reflectionTypeSolver);
        combinedSolver.add(javaParserTypeSolver);

        ParserConfiguration parserConfiguration = new ParserConfiguration()
                .setSymbolResolver(new JavaSymbolSolver(combinedSolver));
        StaticJavaParser.setConfiguration(parserConfiguration);
    }

    public void createGraph() {
        graph = GraphTypeBuilder.<UMLClass, UMLRelationship>forGraphType(DefaultGraphType.directedMultigraph())
                .edgeSupplier(SupplierUtil.createSupplier(UMLRelationship.class))
                .buildGraph();
    }

    public void addClasses(List<UMLClass> umlClasses) {
        for (UMLClass obj : umlClasses) {
            graph.addVertex(obj);
        }
        updateRelationships(umlClasses, graph);
    }

    public void updateVertices(HashMap<String, UMLClass> vertices) {
        for (UMLRelationship rel: graph.edgeSet()) {
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
}
