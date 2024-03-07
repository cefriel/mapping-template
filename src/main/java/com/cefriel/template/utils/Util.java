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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidParameterException;
import java.util.List;
import java.util.Map;

public class Util {
    private static final Logger log = LoggerFactory.getLogger(Util.class);
    public static boolean validInputFormat(String format) {
        return MappingTemplateConstants.INPUT_FORMATS.contains(format);
    }
    public static Reader createRemoteReader(String inputFormat, String dbUrl, String dbId, String username, String password,String graphName, String baseIri) {
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
    public static Reader createInMemoryReader(String inputFormat, List<String> inputFilesPaths, String graphName, String baseIri) throws Exception {
        if (!validInputFormat(inputFormat))
            throw new UnsupportedOperationException("Unsupported Reader format: " + inputFormat);

        if(inputFilesPaths.isEmpty())
            throw new InvalidParameterException("Cannot create Reader with no input file");

        switch (inputFormat) {
            case "json":
                if (inputFilesPaths.size() > 1)
                    throw new InvalidParameterException("Cannot create JSONReader with more than one input file.");
                else
                    return new JSONReader(inputFilesPaths.get(0));
            case "xml":
                if (inputFilesPaths.size() > 1)
                    throw new InvalidParameterException("Cannot create XMLReader with more than one input file.");
                else
                    return new XMLReader(inputFilesPaths.get(0));
            case "csv":
                if (inputFilesPaths.size() > 1)
                    throw new InvalidParameterException("Cannot create CSVReader with more than one input file.");
                else
                    return new CSVReader(inputFilesPaths.get(0));
            case "rdf":
                RDFReader rdfReader = new RDFReader(inputFilesPaths);
                if (graphName != null)
                    rdfReader.setContext(graphName);
                if (baseIri != null)
                    rdfReader.setBaseIRI(baseIri);
                return rdfReader;
            default: throw new InvalidParameterException("Cannot create Reader for inputFormat: " + inputFormat);
        }
    }

    public static Reader createReader(String inputFormat, List<String> inputFilesPaths, String dbAddress, String dbId, String graphName, String baseIri, String username, String password) throws Exception {
        // determine if trying to create remote or in memory Reader
        // if dbadress is specified then it is remote

        if (inputFilesPaths == null && inputFormat == null) {
            //case when Reader is created directly in the template
            return null;
        }

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
