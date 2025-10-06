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

import com.cefriel.template.io.Reader;
import com.cefriel.template.utils.RMLCompilerUtils;
import com.cefriel.template.utils.TemplateFunctions;
import com.cefriel.template.utils.Util;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class RMLTests {

    final static Path FOLDER = Path.of("src/test/resources/rml/");

    @Test
    public void invalidRMLTest() throws Exception {
        Path rmlMappings = FOLDER.resolve(Path.of("invalid-mapping.ttl"));
        try (InputStream rmlMapping = Files.newInputStream(rmlMappings)) {
            assertThrows(RuntimeException.class,
                    () -> Util.validateRML(rmlMapping, false));
        }
    }

    @Test
    public void validRMLTest() throws Exception {
        Path rmlMapping = FOLDER.resolve(Path.of("mapping.ttl"));
        String expectedOutput = Files.readString(FOLDER.resolve("output.nq"));

        try (InputStream rmlMappingStream = Files.newInputStream(rmlMapping)) {
            Util.validateRML(rmlMappingStream, false);
        }

        Reader compilerReader = TemplateFunctions.getRDFReaderFromFile(rmlMapping.toString());

        Map<String,String> rmlMap = new HashMap<>();
        rmlMap.put("basePath", null);

        TemplateExecutor templateExecutor = new TemplateExecutor(false, false, false, null);
        try (InputStream rmlMappingStream = Files.newInputStream(rmlMapping)) {
            Path mappingMTL = Util.compiledMTLMapping(rmlMappingStream, null, FOLDER, false, false);
            String result = templateExecutor.executeMapping(Map.of("reader", compilerReader), FOLDER.resolve(mappingMTL.getFileName()));
            Files.delete(mappingMTL);

            Model resultModel = parseRDFString(result);
            Model expectedOutputModel = parseRDFString(expectedOutput);
            assert(Models.isomorphic(resultModel, expectedOutputModel));
        }
    }

    private static Model parseRDFString(String rdfString) throws Exception {
        RDFParser rdfParser = Rio.createParser(RDFFormat.NQUADS);
        Model model = new TreeModel();
        rdfParser.setRDFHandler(new StatementCollector(model));
        rdfParser.parse(new StringReader(rdfString), "http://example.com/base/");
        return model;
    }
}