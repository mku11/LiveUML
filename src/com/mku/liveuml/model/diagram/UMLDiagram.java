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

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.mku.liveuml.model.entities.EnumConstant;
import com.mku.liveuml.model.entities.Field;
import com.mku.liveuml.model.entities.Method;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultGraphType;
import org.jgrapht.graph.builder.GraphTypeBuilder;
import org.jgrapht.util.SupplierUtil;

import java.io.File;
import java.util.*;

public class UMLDiagram {
    private HashSet<String> sources = new HashSet<>();
    private ReflectionTypeSolver reflectionTypeSolver;
    private CombinedTypeSolver combinedSolver;
    private Graph<UMLClass, UMLRelationship> graph;
    private final UMLFinder finder;
    private ParserConfiguration parserConfiguration;
    private HashMap<String, UMLClass> vertices;
    private final HashSet<UMLClass> selectedVertices = new HashSet<>();
    private final HashSet<UMLRelationship> selectedEdges = new HashSet<>();
    private final HashSet<Method> selectedMethods = new HashSet<>();
    private final HashSet<Field> selectedFields = new HashSet<>();
    private final HashSet<EnumConstant> selectedEnumConsts = new HashSet<>();
    private final HashSet<UMLClass> classes = new HashSet<>();

    public HashSet<UMLClass> getSelectedVertices() {
        return selectedVertices;
    }

    public HashSet<UMLRelationship> getSelectedEdges() {
        return selectedEdges;
    }

    public HashSet<Method> getSelectedMethods() {
        return selectedMethods;
    }

    public HashSet<Field> getSelectedFields() {
        return selectedFields;
    }

    public HashSet<EnumConstant> getSelectedEnumConsts() {
        return selectedEnumConsts;
    }
    public HashSet<String> getSources() {
        return sources;
    }

    public HashSet<UMLClass> getClasses() {
        return new HashSet<>(classes);
    }

    public void setSources(HashSet<String> sources) {
        this.sources = sources;
    }

    public UMLParser getParser() {
        return parser;
    }

    private UMLParser parser;

    public String getFilepath() {
        return filepath;
    }

    private String filepath;

    public UMLDiagram(String filepath, UMLParser parser) {
        this(parser);
        this.filepath = filepath;
    }

    public UMLDiagram(UMLParser parser) {
        this.parser = parser;
        this.finder = new UMLFinder();
        createGraph();
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
            UMLClass from = vertices.getOrDefault(rel.getFrom().toString(), null);
            UMLClass to = vertices.getOrDefault(rel.getTo().toString(), null);
            from.getRelationships().put(rel.toString(), rel);
            to.getRelationships().put(rel.toString(), rel);
        }
    }

    public void updateRelationships(List<UMLClass> umlClasses, Graph<UMLClass, UMLRelationship> graph) {
        for (UMLClass obj : umlClasses) {
            for (Map.Entry<String, UMLRelationship> rel : obj.getRelationships().entrySet()) {
                if (rel.getValue().getFrom() == rel.getValue().getTo())
                    continue;
                if (!graph.containsVertex(rel.getValue().getFrom()) || !graph.containsVertex(rel.getValue().getTo())) {
                    continue;
                }
                try {
                    graph.addEdge(rel.getValue().getFrom(), rel.getValue().getTo(), rel.getValue());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    public Graph<UMLClass, UMLRelationship> getGraph() {
        return this.graph;
    }

    public void clear() {
        graph = null;
        classes.clear();
        reflectionTypeSolver = null;
        parser.clear();
    }

    public UMLClass getClassByName(String name) {
        if (vertices == null)
            return null;
        return vertices.getOrDefault(name, null);
    }

    public void clearSelections() {
        getSelectedEnumConsts().clear();
        getSelectedFields().clear();
        getSelectedMethods().clear();
        getSelectedEdges().clear();
        getSelectedVertices().clear();
    }

    public UMLClass getOwnerByName(String owner) {
        UMLClass cls = getParser().getClassByName(owner);
        if (cls == null) {
            cls = getClassByName(owner);
        }
        return cls;
    }

    public void setFilePath(String filepath) {
        this.filepath = filepath;
    }

    public void refresh() {
        HashSet<String> compactClasses = getCompactClasses();
        clear();
        for(String source : sources) {
            File dir = new File(source);
            setupFolder(dir);
        }

        // we need to do 2 passes to resolve all missing deps
        HashMap<String, HashSet<UMLClass>> parsedDirClasses = new HashMap<>();
        for(String source : sources) {
            File dir = new File(source);
            HashSet<UMLClass> parsedClasses = parser.getClasses(dir);
            for (UMLClass obj : parsedClasses) {
                obj.setFileSource(dir.getAbsolutePath());
            }
            parsedDirClasses.put(dir.getAbsolutePath(), parsedClasses);
            this.classes.addAll(parsedClasses);
        }
        for(String source : sources) {
            File dir = new File(source);
            parser.getObjectsAttrs(parsedDirClasses.get(dir.getAbsolutePath()), dir);
        }
        for(String source : sources) {
            File dir = new File(source);
            parser.parseDependencies(dir);
        }
        parser.resolveDependencies(classes);
        addClasses(new ArrayList<>(classes));

        for(UMLClass object : classes) {
            if(compactClasses.contains(object.toString()))
                object.setCompact(true);
        }
    }

    private HashSet<String> getCompactClasses() {
        HashSet<String> compactClasses = new HashSet<>();
        for(UMLClass object : this.classes)
            if(object.isCompact())
                compactClasses.add(object.toString());
        return compactClasses;
    }

    public void setClasses(Set<UMLClass> vertexSet) {
        classes.clear();
        classes.addAll(vertexSet);
    }

    public UMLFinder getFinder() {
        return finder;
    }
}
