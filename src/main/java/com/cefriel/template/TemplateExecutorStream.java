package com.cefriel.template;

import com.cefriel.template.io.Formatter;
import com.cefriel.template.io.Reader;
import com.cefriel.template.io.rdf.RDFReader;
import com.cefriel.template.utils.TemplateUtils;
import com.cefriel.template.utils.Util;
import org.apache.lucene.analysis.util.ClasspathResourceLoader;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.eclipse.rdf4j.repository.Repository;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TemplateExecutorStream {

    // non-parametric return string result
    public String executeMapping(Reader reader, InputStream template, TemplateMap templateMap, Formatter formatter) throws Exception {
        VelocityContext velocityContext = Util.createVelocityContext(reader, templateMap);
        return applyTemplate(template, velocityContext,  formatter);
    }
    public String executeMapping(Reader reader, InputStream template, String outputFilePath, TemplateMap templateMap, Formatter formatter) throws Exception {
        VelocityContext velocityContext = Util.createVelocityContext(reader, templateMap);
        return applyTemplate(template, velocityContext,  formatter, outputFilePath);
    }
    public Map<String,String> executeMappingParametric(Reader reader, InputStream template, InputStream query, TemplateMap templateMap, Formatter formatter) throws Exception {
        VelocityContext velocityContext = Util.createVelocityContext(reader, templateMap);
        String queryString = Util.inputStreamToString(query);
        return applyTemplateParametric(template, queryString, velocityContext, formatter);
    }
    public List<String> executeMappingParametric(Reader reader, InputStream template, InputStream query, String outputFilePath, TemplateMap templateMap, Formatter formatter) throws Exception {
        VelocityContext velocityContext = Util.createVelocityContext(reader, templateMap);
        String queryString = Util.inputStreamToString(query);
        return applyTemplateParametric(template, queryString, velocityContext, formatter, outputFilePath);
    }
    // non parametric - return string result
    private String applyTemplate(InputStream template, VelocityContext context, Formatter formatter) throws Exception {
        java.io.Reader templateReader = new InputStreamReader(template);
        StringWriter writer = new StringWriter();

        VelocityEngine velocityEngine = Util.createVelocityEngine(false);
        velocityEngine.evaluate(context, writer, "TemplateExecutorStream", templateReader);

        String result = writer.toString();
        templateReader.close();
        writer.close();

        return formatter != null ? formatter.formatString(result) : result;
    }

    // non-parametric - write to file and return filePath
    private String applyTemplate(InputStream template, VelocityContext context, Formatter formatter, String outputFilePath) throws Exception {
        java.io.Reader templateReader = new InputStreamReader(template);
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(outputFilePath),
                StandardCharsets.UTF_8));

        VelocityEngine velocityEngine = Util.createVelocityEngine(false);
        velocityEngine.evaluate(context, writer, "TemplateExecutorStream", templateReader);

        templateReader.close();
        writer.close();

        if (formatter != null)
            formatter.formatFile(outputFilePath);

        return outputFilePath;
    }

    // parametric - return result
    private Map<String, String> applyTemplateParametric(InputStream template, String query,  VelocityContext context, Formatter formatter) throws Exception {
        if(query != null) {
            Reader reader = (Reader) context.get("reader");
            List<Map<String, String>> dataframe = reader.getDataframe(query);

            // copy templateStream which is otherwise consumed
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            template.transferTo(baos);
            ByteArrayInputStream templateStreamCopy = new ByteArrayInputStream(baos.toByteArray());

            HashMap<String, String> output = new HashMap<>();
            int counter = 0;
            for (Map<String, String> row : dataframe) {
                if (row != null)
                    context.put("x", row);

                String rowId = Util.generateRowId(row, counter);
                String result = applyTemplate(templateStreamCopy, context, formatter);
                output.put(rowId, result);
                counter++;
            }
            return output;
            }
            else {
                throw new IllegalArgumentException("Query parameter cannot be null");
            }
    }

    // parametric - return list of filePaths
    private List<String> applyTemplateParametric(InputStream template, String query, VelocityContext context, Formatter formatter, String outputFilePath) throws Exception {
        if(query != null) {
            List<String> resultFilePaths = new ArrayList<>();
            Reader reader = (Reader) context.get("reader");
            List<Map<String, String>> dataframe = reader.getDataframe(query);

            // copy templateStream which is otherwise consumed
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            template.transferTo(baos);
            ByteArrayInputStream templateStreamCopy = new ByteArrayInputStream(baos.toByteArray());

            int counter = 0;
            for (Map<String, String> row : dataframe) {
                if (row != null)
                    context.put("x", row);
                String resultFilePath = applyTemplate(templateStreamCopy, context, formatter,
                        Util.createOutputFileName(outputFilePath, counter));
                resultFilePaths.add(resultFilePath);
                counter++;
            }
            return resultFilePaths;
        }
        else {
            throw new IllegalArgumentException("Query parameter cannot be null");
        }
    }
}
