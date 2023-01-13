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
            Reader reader = switch (format) {
                case "xml" -> new XMLReader(input);
                case "json" -> new JSONReader(input);
                case "csv" -> new CSVReader(input);
                default -> throw new IllegalArgumentException("A reader for FORMAT: " + format + " is not supported");
            };
            reader.setVerbose(verbose);
            return reader;
        }
        else throw new IllegalArgumentException("A reader for FORMAT: " + format + " is not supported");
    }
    public static Reader createNonRdfReader(String filePath, String format, boolean verbose) throws Exception {
        if (validInputFormat(format)) {
            File f = new File(filePath);
            Reader reader = switch (format) {
                case "xml" -> new XMLReader(f);
                case "json" -> new JSONReader(f);
                case "csv" -> new CSVReader(f);
                default -> throw new IllegalArgumentException("A reader for FORMAT: " + format + " is not supported");
            };
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
            return switch (format) {
                case "xml" -> new XMLFormatter();
                case "turtle" -> new RDFFormatter(RDFFormat.TURTLE);
                case "rdfxml" -> new RDFFormatter(RDFFormat.RDFXML);
                case "nt" -> new RDFFormatter(RDFFormat.NTRIPLES);
                case "jsonld" -> new RDFFormatter(RDFFormat.JSONLD);
                default -> throw new IllegalArgumentException("A FORMATTER for FORMAT: " + format + " is not supported");
            };
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
    public static String createOutputFileName(String destinationPath, int number) {
        if (number == 0)
            return destinationPath;
        else {
            // if destination path specifies an extension i.e "/src/test/result.txt"
            if (destinationPath.contains(".")) {
                String extension = destinationPath.substring(destinationPath.lastIndexOf(".") + 1);
                String filePath = destinationPath.substring(0, destinationPath.lastIndexOf("."));
                return filePath + "-" + number + "." + extension;
            }
            else
                return destinationPath + "-" + number;
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

    public static VelocityContext createVelocityContext(Reader reader, TemplateMap templateMap) {
        VelocityContext context = new VelocityContext();
        if(reader != null) {
            context.put("reader", reader);
        }
        context.put("functions", new TemplateUtils());

        if (templateMap != null)
            context.put("map", templateMap);

        return context;
    }


}
