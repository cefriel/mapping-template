/*
 * Copyright (c) 2019-2021 Cefriel.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cefriel;

import com.cefriel.lowerer.TemplateLowerer;
import com.cefriel.utils.LoweringUtils;
import com.cefriel.io.rdf.RDFReader;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

public class RDFReaderTests {

    private static RDFReader reader;

    @BeforeAll
    static void initAll() {
        Repository repo = new SailRepository(new MemoryStore());
        reader = new RDFReader(repo);
        reader.setBaseIRI("http://www.cefriel.com/data/");
    }

    private String resolvePath(String folder, String file) {
        return "demo/" + folder + "/" + file;
    }

    @Test
    public void agencyTest() throws Exception {
        String folder = "agency";
        reader.addFile(resolvePath(folder, "input.ttl"), RDFFormat.TURTLE);
        TemplateLowerer lowerer = new TemplateLowerer(reader, new LoweringUtils());
        File template = new File(resolvePath(folder, "template.vm"));
        InputStream templateStream = new FileInputStream(template);
        String expectedOutput = Files.readString(Paths.get(resolvePath(folder, "agency.csv")), StandardCharsets.UTF_8);
        assert(expectedOutput.equals(lowerer.lower(templateStream)));
    }

    @Test
    public void agencyMultipleInput() throws Exception {
        String folder = "agency-multiple-input";
        reader.addFile(resolvePath(folder, "input.ttl"), RDFFormat.TURTLE);
        reader.addFile(resolvePath(folder, "input2.ttl"), RDFFormat.TURTLE);
        TemplateLowerer lowerer = new TemplateLowerer(reader, new LoweringUtils());
        File template = new File(resolvePath(folder, "template.vm"));
        InputStream templateStream = new FileInputStream(template);
        String expectedOutput = Files.readString(Paths.get(resolvePath(folder, "agency.csv")), StandardCharsets.UTF_8);
        assert(expectedOutput.equals(lowerer.lower(templateStream)));
    }

    @Test
    public void agencyParametric() throws Exception {
        String folder = "agency-parametric";

        reader.addFile(resolvePath(folder, "input.ttl"), RDFFormat.TURTLE);
        TemplateLowerer lowerer = new TemplateLowerer(reader, new LoweringUtils());
        File template = new File(resolvePath(folder, "template.vm"));
        InputStream templateStream = new FileInputStream(template);

        String queryPath = resolvePath(folder, "query.txt");
        Map<String, String> output = lowerer.lower(templateStream, queryPath);
        for(String id : output.keySet()) {
            String expectedOutput = Files
                    .readString(Paths.get(resolvePath(folder, "agency-" + id + ".csv")),
                            StandardCharsets.UTF_8);
            assert(expectedOutput.equals(output.get(id)));
        }
    }

    // TODO Add tests without stream
    // TODO Add tests for MapConfigurator
    // TODO Add tests for XMLReader
    // TODO Add tests for LoweringUtils
}
