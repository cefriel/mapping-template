/*
 * Copyright (c) 2019-2023 Cefriel.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cefriel.template;

import com.cefriel.template.io.rdf.RDFReader;
import com.cefriel.template.utils.TemplateFunctions;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

public class RDFReaderTests {
    private String resolvePath(String folder, String file) {
        return "src/test/resources/" + folder + "/" + file;
    }
    @Test
    public void agencyTestFile() throws Exception {
        Repository repo = new SailRepository(new MemoryStore());
        RDFReader reader = new RDFReader(repo);
        reader.setBaseIRI("http://www.cefriel.com/data/");

        String folder = "agency";
        reader.addFile(resolvePath(folder, "input.ttl"), RDFFormat.TURTLE);
        TemplateExecutor executor = new TemplateExecutor( true, false, false, null);
        Path template = Paths.get(resolvePath(folder, "template.vm"));
        String expectedOutput = Files.readString(Paths.get(resolvePath(folder, "agency.csv")));
        String result = executor.executeMapping(Map.of("reader", reader), template, new TemplateFunctions(), null);
        expectedOutput = expectedOutput.replaceAll("\\r\\n", "\n");
        result = result.replaceAll("\\r\\n", "\n");

        assert(expectedOutput.equals(result));
    }
    @Test
    public void agencyMultipleInputFile() throws Exception {
        Repository repo = new SailRepository(new MemoryStore());
        RDFReader reader = new RDFReader(repo);
        reader.setBaseIRI("http://www.cefriel.com/data/");
        String folder = "agency-multiple-input";
        String baseIri = "http://www.cefriel.com/data/";

        reader.setBaseIRI(baseIri);
        reader.addFile(resolvePath(folder, "input.ttl"), RDFFormat.TURTLE);
        reader.addFile(resolvePath(folder, "input2.ttl"), RDFFormat.TURTLE);
        TemplateExecutor executor = new TemplateExecutor( true, false, false, null);
        Path template = Paths.get(resolvePath(folder, "template.vm"));

        String expectedOutput = Files.readString(Paths.get(resolvePath(folder, "agency.csv")));
        String result = executor.executeMapping(Map.of("reader", reader), template);

        expectedOutput = expectedOutput.replaceAll("\\r\\n", "\n");
        result = result.replaceAll("\\r\\n", "\n");

        assert(expectedOutput.equals(result));
    }
    @Test
    public void agencyParametric() throws Exception {
        RDFReader reader = new RDFReader();
        reader.setBaseIRI("http://www.cefriel.com/data/");
        String folder = "agency-parametric";
        reader.addFile(resolvePath(folder, "input.ttl"), RDFFormat.TURTLE);
        TemplateExecutor executor = new TemplateExecutor( true, false, false, null);
        Path template = Paths.get(resolvePath(folder, "template.vm"));

        Path queryPath = Paths.get(resolvePath(folder, "query.txt"));
        Map<String, String> output = executor.executeMappingParametric(Map.of("reader", reader), template, queryPath);

        for(String id : output.keySet()) {
            String expectedOutput = Files
                    .readString(Paths.get(resolvePath(folder, "agency-" + id + ".csv")));
            expectedOutput = expectedOutput.replaceAll("\\r\\n", "\n");
            String result = output.get(id).replaceAll("\\r\\n", "\n");
            assert(expectedOutput.equals(result));
        }
    }

    @Test
    public void agencyParametricStream() throws Exception {
        RDFReader reader = new RDFReader();
        reader.setBaseIRI("http://www.cefriel.com/data/");

        String folder = "agency-parametric";
        reader.addFile(resolvePath(folder, "input.ttl"), RDFFormat.TURTLE);
        InputStream template = new FileInputStream(Paths.get(resolvePath(folder, "template.vm")).toString());
        InputStream query = new FileInputStream(Paths.get(resolvePath(folder, "query.txt")).toString());
        TemplateExecutor executor = new TemplateExecutor( true, false, false, null);
        Map<String, String> output = executor.executeMappingParametric(Map.of("reader", reader), template, query);

        for(String id : output.keySet()) {
            String expectedOutput = Files
                    .readString(Paths.get(resolvePath(folder, "agency-" + id + ".csv")));
            expectedOutput = expectedOutput.replaceAll("\\r\\n", "\n");
            String result = output.get(id).replaceAll("\\r\\n", "\n");
            assert(expectedOutput.equals(result));
        }
    }
    // TODO Add tests for TemplateMap
    // TODO Add tests for XMLReader

    @Test
    public void dataframeWithMetadata() throws Exception {
        Repository repo = new SailRepository(new MemoryStore());
        RDFReader reader = new RDFReader(repo);
        reader.setBaseIRI("http://www.cefriel.com/data/");
        reader.addFile(resolvePath("agency", "input.ttl"), RDFFormat.TURTLE);

        String query = "PREFIX gtfs: <http://vocab.gtfs.org/terms#> " +
                "PREFIX foaf: <http://xmlns.com/foaf/0.1/> " +
                "PREFIX dct: <http://purl.org/dc/terms/> " +
                "SELECT ?s ?name ?lang WHERE { " +
                "  ?s a gtfs:Agency ; foaf:name ?name ; dct:language ?lang . }";

        List<Map<String, Map<String, String>>> rows = reader.getDataframeWithMetadata(query);

        assert rows.size() == 2;
        for (Map<String, Map<String, String>> row : rows) {
            Map<String, String> s = row.get("s");
            assert "uri".equals(s.get("type"));
            assert s.get("value").startsWith("http://sprint-transport.eu/data/agencies/");
            assert s.get("datatype") == null;

            Map<String, String> name = row.get("name");
            assert "literal".equals(name.get("type"));
            assert name.get("value").endsWith("Agency");
            assert "http://www.w3.org/2001/XMLSchema#string".equals(name.get("datatype"));

            Map<String, String> lang = row.get("lang");
            assert "en".equals(lang.get("value"));
        }

        reader.addString(
                "@prefix base: <http://www.cefriel.com/data/> ." +
                "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> ." +
                "base:event base:start \"2026-06-29T10:30:00Z\"^^xsd:dateTime ; " +
                "base:count \"42\"^^xsd:integer ; " +
                "base:label \"ciao\"@it .",
                RDFFormat.TURTLE);

        String typedQuery = "PREFIX base: <http://www.cefriel.com/data/> " +
                "SELECT ?start ?count ?label WHERE { " +
                "  base:event base:start ?start ; base:count ?count ; base:label ?label . }";
        List<Map<String, Map<String, String>>> typedRows = reader.getDataframeWithMetadata(typedQuery);

        assert typedRows.size() == 1;
        Map<String, Map<String, String>> typedRow = typedRows.get(0);

        Map<String, String> start = typedRow.get("start");
        assert "literal".equals(start.get("type"));
        assert "2026-06-29T10:30:00Z".equals(start.get("value"));
        assert "http://www.w3.org/2001/XMLSchema#dateTime".equals(start.get("datatype"));

        Map<String, String> count = typedRow.get("count");
        assert "42".equals(count.get("value"));
        assert "http://www.w3.org/2001/XMLSchema#integer".equals(count.get("datatype"));

        Map<String, String> label = typedRow.get("label");
        assert "ciao".equals(label.get("value"));
        assert "it".equals(label.get("lang"));

        reader.shutDown();
    }

    @Test
    public void dataframeWithMetadataMapping() throws Exception {
        Repository repo = new SailRepository(new MemoryStore());
        RDFReader reader = new RDFReader(repo);
        reader.setBaseIRI("http://www.cefriel.com/data/");
        String folder = "metadata";
        reader.addFile(resolvePath(folder, "input.ttl"), RDFFormat.TURTLE);

        TemplateExecutor executor = new TemplateExecutor(true, false, false, null);
        Path template = Paths.get(resolvePath(folder, "template.vm"));
        String result = executor.executeMapping(Map.of("reader", reader), template, new TemplateFunctions(), null);

        String expectedOutput = Files.readString(Paths.get(resolvePath(folder, "output.ttl")));
        Model resultModel = parseTurtle(result);
        Model expectedModel = parseTurtle(expectedOutput);
        assert Models.isomorphic(resultModel, expectedModel);
        reader.shutDown();
    }

    private static Model parseTurtle(String rdf) throws Exception {
        RDFParser parser = Rio.createParser(RDFFormat.TURTLE);
        Model model = new TreeModel();
        parser.setRDFHandler(new StatementCollector(model));
        parser.parse(new StringReader(rdf), "http://example.com/base/");
        return model;
    }
}
