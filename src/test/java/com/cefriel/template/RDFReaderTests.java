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
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
}
