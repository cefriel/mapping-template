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
import com.cefriel.template.io.json.JSONReader;
import com.cefriel.template.io.rdf.RDFFormatter;
import com.cefriel.template.io.rdf.RDFReader;
import com.cefriel.template.io.xml.XMLFormatter;
import com.cefriel.template.io.xml.XMLReader;
import org.apache.lucene.analysis.util.ClasspathResourceLoader;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.tools.generic.DateTool;
import org.apache.velocity.tools.generic.MathTool;
import org.apache.velocity.tools.generic.NumberTool;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.http.HTTPRepository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class Util {
    private static final Logger log = LoggerFactory.getLogger(Util.class);
    public static boolean validInputFormat(String format) {
        return MappingTemplateConstants.INPUT_FORMATS.contains(format);
    }
    public static Reader createNonRdfReaderFromInput(String input, String format, boolean verbose) throws Exception {
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

            reader.setVerbose(verbose);
            return reader;
        }
        else throw new IllegalArgumentException("A reader for FORMAT: " + format + " is not supported");
    }
    public static Reader createNonRdfReader(String filePath, String format, boolean verbose) throws Exception {
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

            reader.setVerbose(verbose);
            return reader;
        }
        else throw new IllegalArgumentException("A reader for FORMAT: " + format + " is not supported");
    }
    public static RDFReader createRDFReader(String graphName, String baseIri, Repository repository, boolean verbose) {
        RDFReader reader = new RDFReader(repository);
        reader.setContext(graphName);
        reader.setBaseIRI(baseIri);
        reader.setVerbose(verbose);
        return reader;
    }
    public static RDFReader createRDFReader(List<String> inputFilesPaths, String dbAddress, String repositoryId, String graphName, String baseIri, boolean verbose) throws Exception {
        if (inputFilesPaths != null) {
            Repository repo;
            if ((dbAddress != null) && (repositoryId != null)) {
                repo = new HTTPRepository(dbAddress, repositoryId);
            } else {
                repo = new SailRepository(new MemoryStore());
            }
            RDFReader rdfReader = createRDFReader(graphName, baseIri, repo, verbose);
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
            if (format.equals("xml")) {
                return new XMLFormatter();
            }
            else if (format.equals("turtle")) {
                return new RDFFormatter(RDFFormat.TURTLE);
            }
            else if (format.equals("rdfxml")) {
                return new RDFFormatter(RDFFormat.RDFXML);
            }
            else if (format.equals("nt")) {
                return new RDFFormatter(RDFFormat.NTRIPLES);
            }
            else if (format.equals("jsonld")) {
                return new RDFFormatter(RDFFormat.JSONLD);
            }
            else {
                throw new IllegalArgumentException("A FORMATTER for FORMAT: " + format + " is not supported");
            }
        }
        else throw new IllegalArgumentException("A FORMATTER for FORMAT: " + format + " is not supported");
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
    public static String createOutputFileName(String destinationPath, int suffixNumber) {
        if (suffixNumber == 0)
            return destinationPath;
        else {
            // if destination path specifies an extension i.e "/src/test/result.txt"
            if (destinationPath.contains(".")) {
                String extension = destinationPath.substring(destinationPath.lastIndexOf(".") + 1);
                String filePath = destinationPath.substring(0, destinationPath.lastIndexOf("."));
                return filePath + "-" + suffixNumber + "." + extension;
            }
            else
                return destinationPath + "-" + suffixNumber;
        }
    }
    public static String inputStreamToString(InputStream input) throws IOException {
        return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
    public static VelocityEngine createVelocityEngine(boolean templateInResources){
        VelocityEngine velocityEngine = new VelocityEngine();
        if (templateInResources) {
            velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
            velocityEngine.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
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

        if (templateMap != null)
            context.put("map", templateMap);

        return context;
    }

    public static VelocityContext createVelocityContext(Reader reader, TemplateMap templateMap) {
        return createVelocityContext(reader, templateMap, new TemplateFunctions());
    }


}
