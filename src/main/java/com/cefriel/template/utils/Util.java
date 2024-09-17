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
import com.cefriel.template.io.sql.SQLReader;
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
import java.security.InvalidParameterException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Util {
    private static final Logger log = LoggerFactory.getLogger(Util.class);
    public static boolean validInputFormat(String format) {
        return MappingTemplateConstants.INPUT_FORMATS.contains(format);
    }
    public static Reader createRemoteReader(String inputFormat, String dbUrl, String dbId, String username, String password,String graphName, String baseIri) throws SQLException {
        if (!validInputFormat(inputFormat))
            throw new UnsupportedOperationException("Unsupported Reader format: " + inputFormat);

        switch (inputFormat) {
            case "rdf":
                RDFReader rdfReader = new RDFReader(dbUrl, dbId);
                if (graphName != null)
                    rdfReader.setContext(graphName);
                if (baseIri != null)
                    rdfReader.setBaseIRI(baseIri);
            case "mysql":
            case "postgresql":
                return new SQLReader(inputFormat, dbUrl, dbId, username, password);
            default: throw new InvalidParameterException("Cannot create Reader for inputFormat: " + inputFormat);
        }
    }
    public static Reader createInMemoryReader(String inputFormat, List<Path> inputFilesPaths, String graphName, String baseIri) throws Exception {
        if (!validInputFormat(inputFormat))
            throw new UnsupportedOperationException("Unsupported Reader format: " + inputFormat);

        if(inputFilesPaths.isEmpty() && !inputFormat.equals("rdf"))
            // an rdf reader can be initialized (when not created from cli) and then files can be added to it with the addFile method
            throw new InvalidParameterException("Cannot create a " + inputFormat + "Reader with no input file");

        switch (inputFormat) {
            case "json":
                if (inputFilesPaths.size() > 1)
                    throw new InvalidParameterException("Cannot create JSONReader with more than one input file.");
                else
                    return new JSONReader(inputFilesPaths.get(0).toFile());
            case "xml":
                if (inputFilesPaths.size() > 1)
                    throw new InvalidParameterException("Cannot create XMLReader with more than one input file.");
                else
                    return new XMLReader(inputFilesPaths.get(0).toFile());
            case "csv":
                if (inputFilesPaths.size() > 1)
                    throw new InvalidParameterException("Cannot create CSVReader with more than one input file.");
                else
                    return new CSVReader(inputFilesPaths.get(0).toFile());
            case "rdf":
                RDFReader rdfReader = new RDFReader();
                for (Path triplesFile : inputFilesPaths)
                    rdfReader.addFile(triplesFile.toString());
                if (graphName != null)
                    rdfReader.setContext(graphName);
                if (baseIri != null)
                    rdfReader.setBaseIRI(baseIri);
                return rdfReader;
            default: throw new InvalidParameterException("Cannot create Reader for inputFormat: " + inputFormat);
        }
    }

    public static Reader createReader(String inputFormat, List<Path> inputFilesPaths, String dbAddress, String dbId, String graphName, String baseIri, String username, String password) throws Exception {
        if (inputFilesPaths == null && inputFormat == null) {
            //case when Reader is created directly in the template
            return null;
        }
        // determine if trying to create remote or in memory Reader
        // if dbadress is specified then it is remote
        boolean isRemoteReader = (dbAddress != null) ;

        if (isRemoteReader) {
            return createRemoteReader(inputFormat, dbAddress, dbId, username, password, graphName, baseIri);
        }
        else {
            return createInMemoryReader(inputFormat, inputFilesPaths, graphName, baseIri);
        }


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

    /**
     * Creates a new {@link VelocityContext} and populates it with the provided readers, template map, and template functions.
     * Additionally, it adds a set of common tools for mathematical operations, number formatting, date manipulation,
     * and escaping strings.
     *
     * @param readers A map where the key is a {@link String} representing the name, and the value is a {@link Reader}
     *                object that will be added to the context. Can be null or empty.
     * @param templateMap An instance of {@link TemplateMap} to be added to the context. Can be null.
     * @param templateFunctions An instance of {@link TemplateFunctions} providing various template-related functions to be added to the context. Must not be null.
     * @return A {@link VelocityContext} populated with the provided readers, template map, template functions,
     *         and a set of common tools (math, number, date, and escape tools).
     */
    public static VelocityContext createVelocityContext(Map<String, Reader> readers, TemplateMap templateMap, TemplateFunctions templateFunctions) {
        VelocityContext context = new VelocityContext();

        if (readers != null) {
            if (readers.size() == 1) {
                Map.Entry<String, Reader> singleReader = readers.entrySet().iterator().next();
                context.put("reader", singleReader.getValue());
                context.put("readers", Map.of(singleReader.getKey(), singleReader.getValue()));
            }
            else if (readers.size() > 1) {
                Map<String, Reader> allReaders = new HashMap<>(readers);
                context.put("readers", allReaders);
            }
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

    /**
     * Creates a new {@link VelocityContext} and populates it with the provided reader, template map, and template functions.
     * This method is a convenience overload that allows for a single reader to be added to the context.
     *
     * @param reader A {@link Reader} object that will be added to the context with the key "reader". Can be null.
     * @param templateMap An instance of {@link TemplateMap} to be added to the context. Can be null.
     * @param templateFunctions An instance of {@link TemplateFunctions} providing various template-related functions to be added to the context. Must not be null.
     * @return A {@link VelocityContext} populated with the provided reader, template map, template functions,
     *         and a set of common tools (math, number, date, and escape tools).
     */
    public static VelocityContext createVelocityContext(Reader reader, TemplateMap templateMap, TemplateFunctions templateFunctions) {
        Map<String, Reader> readers = new HashMap<>();
        if (reader != null)
            readers.put("reader", reader);
        return createVelocityContext(readers, templateMap, templateFunctions);
    }

    /**
     * Creates a new {@link VelocityContext} and populates it with the provided reader, template map.
     * The context is also populated with an instance of {@link TemplateFunctions}.
     * This method is a convenience overload that allows for a single reader to be added to the context.
     *
     * @param reader A {@link Reader} object that will be added to the context with the key "reader". Must not be null.
     * @param templateMap An instance of {@link TemplateMap} to be added to the context. Can be null.
     * @return A {@link VelocityContext} populated with the provided reader, template map, template functions,
     *         and a set of common tools (math, number, date, and escape tools).
     */
    public static VelocityContext createVelocityContext(Reader reader, TemplateMap templateMap) {
        return createVelocityContext(reader, templateMap, new TemplateFunctions());
    }

    /**
     * Creates a new {@link VelocityContext} and populates it with the provided readers, and template map.
     * The context is also populated with an instance of {@link TemplateFunctions}.
     * Additionally, it adds a set of common tools for mathematical operations, number formatting, date manipulation,
     * and escaping strings.
     *
     * @param readers A map where the key is a {@link String} representing the name, and the value is a {@link Reader}
     *                object that will be added to the context. Can be null or empty.
     * @param templateMap An instance of {@link TemplateMap} to be added to the context. Can be null.
     * @return A {@link VelocityContext} populated with the provided readers, template map, template functions,
     *         and a set of common tools (math, number, date, and escape tools).
     */
    public static VelocityContext createVelocityContext(Map<String, Reader> readers, TemplateMap templateMap) {
        return createVelocityContext(readers, templateMap, new TemplateFunctions());
    }
}
