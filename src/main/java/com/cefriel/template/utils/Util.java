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

package com.cefriel.template.utils;

import com.cefriel.template.TemplateMap;
import com.cefriel.template.io.Formatter;
import com.cefriel.template.io.Reader;
import com.cefriel.template.io.csv.CSVReader;
import com.cefriel.template.io.json.JSONFormatter;
import com.cefriel.template.io.json.JSONReader;
import com.cefriel.template.io.rdf.RDFFormatter;
import com.cefriel.template.io.rdf.RDFReader;
import com.cefriel.template.io.xml.XMLFormatter;
import com.cefriel.template.io.xml.XMLReader;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.apache.velocity.tools.generic.*;
import org.eclipse.rdf4j.common.exception.ValidationException;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.http.HTTPRepository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.shacl.ShaclSail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

public class Util {
    private static final Logger log = LoggerFactory.getLogger(Util.class);
    public static boolean validInputFormat(String format) {
        return MappingTemplateConstants.INPUT_FORMATS.contains(format);
    }
    public static Reader createNonRdfReaderFromInput(String input, String format) throws Exception {
        if (validInputFormat(format)) {
            Reader reader = null;
            if (format.equals("xml")) {
                reader = new XMLReader(input);
            }
            else if (format.equals("json")) {
                reader = new JSONReader(input);
            }
            else if (format.equals("csv")) {
                reader = new CSVReader(input);
            }
            else
                throw new IllegalArgumentException("A reader for FORMAT: " + format + " is not supported");
            return reader;
        }
        else throw new IllegalArgumentException("A reader for FORMAT: " + format + " is not supported");
    }
    public static Reader createNonRdfReader(String filePath, String format) throws Exception {
        if (validInputFormat(format)) {
            File f = new File(filePath);
            Reader reader = null;
            if (format.equals("xml")) {
                reader = new XMLReader(f);
            }
            else if (format.equals("json")) {
                reader = new JSONReader(f);
            }
            else if (format.equals("csv")) {
                reader = new CSVReader(f);
            }
            else
                throw new IllegalArgumentException("A reader for FORMAT: " + format + " is not supported");

            return reader;
        }
        else throw new IllegalArgumentException("A reader for FORMAT: " + format + " is not supported");
    }
    public static RDFReader createRDFReader(String graphName, String baseIri, Repository repository) {
        RDFReader reader = new RDFReader(repository);
        reader.setContext(graphName);
        reader.setBaseIRI(baseIri);
        return reader;
    }
    public static RDFReader createRDFReader(List<String> inputFilesPaths, String repositoryUrl, String repositoryId, String graphName, String baseIri) throws Exception {
        if (inputFilesPaths != null) {
            Repository repo;
            if ((repositoryUrl != null) && (repositoryId != null)) {
                repo = new HTTPRepository(repositoryUrl, repositoryId);
            } else {
                repo = new SailRepository(new MemoryStore());
            }
            RDFReader rdfReader = createRDFReader(graphName, baseIri, repo);
            RDFFormat format;
            for (String triplesPath : inputFilesPaths)
                if ((new File(triplesPath)).exists()) {
                    format = Rio.getParserFormatForFileName(triplesPath).orElse(RDFFormat.TURTLE);
                    rdfReader.addFile(triplesPath, format);
                }
            return rdfReader;
        }
        return null;
    }

    public static boolean validFormatterFormat(String format) {
        return MappingTemplateConstants.FORMATTER_FORMATS.contains(format);
    }

    public static Formatter createFormatter(String format) {
        if (validFormatterFormat(format)) {
            switch (format) {
                case "xml":
                    return new XMLFormatter();
                case "json":
                    return new JSONFormatter();
                case "turtle":
                    return new RDFFormatter(RDFFormat.TURTLE);
                case "rdfxml":
                    return new RDFFormatter(RDFFormat.RDFXML);
                case "nt":
                    return new RDFFormatter(RDFFormat.NTRIPLES);
                case "nq":
                    return new RDFFormatter(RDFFormat.NQUADS);
                case "jsonld":
                    return new RDFFormatter(RDFFormat.JSONLD);
                default:
                    throw new IllegalArgumentException("FORMATTER for FORMAT " + format + " not found");
            }
        }
        else throw new IllegalArgumentException("FORMAT " + format + " not listed as valid value for the option");
    }

    public static String generateRowId(Map<String, String> row, int number) {
        if (row != null)
            if (row.containsKey("id"))
                return row.get("id");
            else
                return "t-id-" + number;
        else
            return "default";
    }
    public static Path createOutputFileName(Path destinationPath, int suffixNumber) {
        if (suffixNumber == 0)
            return destinationPath;
        else {
            // if destination path specifies an extension i.e "/src/test/result.txt"
            String path = destinationPath.toString();
            if (path.contains(".")) {
                String extension = path.substring(path.lastIndexOf(".") + 1);
                String filePath = path.substring(0, path.lastIndexOf("."));
                return Paths.get(filePath + "-" + suffixNumber + "." + extension);
            }
            else
                return Paths.get(destinationPath + "-" + suffixNumber);
        }
    }
    public static String inputStreamToString(InputStream input) throws IOException {
        return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
    public static VelocityEngine createVelocityEngine(boolean templateInResources, boolean failInvalidRef){
        VelocityEngine velocityEngine = new VelocityEngine();
        // Fail on variables not found
        velocityEngine.setProperty("runtime.strict_mode.enable", failInvalidRef);
        if (templateInResources) {
            velocityEngine.setProperty("resource.loaders", "class");
            velocityEngine.setProperty("resource.loader.class.class",
                    ClasspathResourceLoader.class.getName());
        }
        velocityEngine.init();
        return velocityEngine;
    }

    public static VelocityContext createVelocityContext(Reader reader, TemplateMap templateMap, TemplateFunctions templateFunctions) {
        VelocityContext context = new VelocityContext();
        if(reader != null) {
            context.put("reader", reader);
        }
        context.put("functions", templateFunctions);
        // apache velocity generic tools
        context.put("math", new MathTool());
        context.put("number", new NumberTool());
        context.put("date", new DateTool());
        context.put("esc", new EscapeTool());

        if (templateMap != null)
            context.put("map", templateMap);

        return context;
    }

    public static VelocityContext createVelocityContext(Reader reader, TemplateMap templateMap) {
        return createVelocityContext(reader, templateMap, new TemplateFunctions());
    }


    public static void validateRML(Path templatePath, boolean verbose) {

        ShaclSail shaclSail = new ShaclSail(new MemoryStore());
        SailRepository repository = new SailRepository(shaclSail);
        repository.init();

        try (RepositoryConnection connection = repository.getConnection()) {
            try (InputStream shapesStream = Util.class.getResourceAsStream("/rml/core.ttl")) {
                Model rules = Rio.parse(shapesStream, RDFFormat.TURTLE);
                // cf. https://github.com/eclipse-rdf4j/rdf4j/discussions/4287
                rules.remove(null, SHACL.NAME, null);
                rules.remove(null, SHACL.DESCRIPTION, null);

                connection.begin();
                connection.add(rules, RDF4J.SHACL_SHAPE_GRAPH);
                connection.commit();
            }

            // Load RML
            try (InputStream dataStream = Files.newInputStream(templatePath)) {
                connection.begin();
                connection.add(dataStream, "", org.eclipse.rdf4j.rio.RDFFormat.TURTLE);

                try {
                    connection.commit();
                    log.info("RML validated correctly");
                } catch (RepositoryException exception) {
                    Throwable cause = exception.getCause();
                    log.error("RML not valid");
                    if (verbose)
                        if (cause instanceof ValidationException) {
                            Model validationReportModel = ((ValidationException) cause).validationReportAsModel();
                            Rio.write(validationReportModel, System.out, RDFFormat.TURTLE);
                        }
                    throw exception;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            repository.shutDown();
        }
    }
}
