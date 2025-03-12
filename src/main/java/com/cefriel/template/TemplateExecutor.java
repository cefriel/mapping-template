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

import com.cefriel.template.io.Formatter;
import com.cefriel.template.io.Reader;
import com.cefriel.template.utils.TemplateFunctions;
import com.cefriel.template.utils.Util;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TemplateExecutor {

    private final org.slf4j.Logger log = LoggerFactory.getLogger(TemplateExecutor.class);
    private final boolean failInvalidRef;
    private final boolean trimTemplate;
    private final boolean templateInResourcesFolder;
    private final Formatter formatter;
    private final VelocityEngine velocityEngine;

    public TemplateExecutor(boolean failInvalidRef, boolean trimTemplate, boolean templateInResourcesFolder, Formatter formatter) {
        this.failInvalidRef = failInvalidRef;
        this.trimTemplate = trimTemplate;
        this.templateInResourcesFolder = templateInResourcesFolder;
        this.formatter = formatter;
        this.velocityEngine = Util.createVelocityEngine(this.templateInResourcesFolder, failInvalidRef);
    }

    public String executeMapping(Map<String, Reader> readers, Path templatePath) throws Exception {
        return executeMapping(readers, templatePath, null, null);
    }

    public Path executeMapping(Map<String, Reader> readers, Path templatePath, Path outputFile) throws Exception {
        return executeMapping(readers, templatePath, outputFile,null, null);
    }

    // methods called from cli
    public String executeMapping(Map<String, Reader> readers, Path templatePath, TemplateFunctions templateFunctions, TemplateMap templateMap) throws Exception {
        Path usedTemplatePath = this.trimTemplate ? trimTemplate(templatePath) : templatePath;
        VelocityContext velocityContext = templateFunctions == null ? Util.createVelocityContext(readers, templateMap) : Util.createVelocityContext(readers, templateMap, templateFunctions);
        return applyTemplate(usedTemplatePath, velocityContext);
    }
    public Path executeMapping(Map<String, Reader> readers, Path templatePath, Path outputFilePath, TemplateFunctions templateFunctions, TemplateMap templateMap) throws Exception {
        Path usedTemplatePath = this.trimTemplate ? trimTemplate(templatePath) : templatePath;
        VelocityContext velocityContext = templateFunctions == null ? Util.createVelocityContext(readers, templateMap) : Util.createVelocityContext(readers, templateMap, templateFunctions);
        return applyTemplate(usedTemplatePath, outputFilePath, velocityContext);
    }

    public Map<String, String> executeMappingParametric(Map<String, Reader> readers, Path templatePath, Path queryPath) throws Exception {
        return executeMappingParametric(readers, templatePath, queryPath, null, null);
    }

    public List<Path> executeMappingParametric(Map<String, Reader> readers, Path templatePath, Path queryPath, Path outputFilePath) throws Exception {
        return executeMappingParametric(readers, templatePath, queryPath, outputFilePath, null, null);
    }

    public Map<String, String> executeMappingParametric(Map<String, Reader> readers, Path templatePath, Path queryPath, TemplateFunctions templateFunctions, TemplateMap templateMap) throws Exception {
        Path usedTemplatePath = this.trimTemplate ? trimTemplate(templatePath) : templatePath;
        String query = Files.readString(queryPath);
        VelocityContext velocityContext = templateFunctions == null ? Util.createVelocityContext(readers, templateMap) : Util.createVelocityContext(readers, templateMap, templateFunctions);
        return applyTemplateParametric(usedTemplatePath, query, velocityContext);
    }
    public List<Path> executeMappingParametric(Map<String, Reader> readers, Path templatePath, Path queryPath, Path outputFilePath, TemplateFunctions templateFunctions, TemplateMap templateMap) throws Exception {
        Path usedTemplatePath = this.trimTemplate ? trimTemplate(templatePath) : templatePath;
        String query = Files.readString(queryPath);
        VelocityContext velocityContext = templateFunctions == null ? Util.createVelocityContext(readers, templateMap) : Util.createVelocityContext(readers, templateMap, templateFunctions);
        return applyTemplateParametric(usedTemplatePath, query, outputFilePath, velocityContext);
    }
    // non parametric - return string result
    private String applyTemplate(Path templatePath, VelocityContext velocityContext) throws Exception {
        StringWriter writer = new StringWriter();
        Template t = this.velocityEngine.getTemplate(templatePath.toString());
        t.merge(velocityContext, writer);
        String result = writer.toString();
        writer.close();

        return this.formatter != null ? this.formatter.formatString(result) : result;
    }

    // non-parametric - write to file and return filePath
    private Path applyTemplate(Path templatePath, Path outputFilePath, VelocityContext velocityContext) throws Exception {

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFilePath.toFile()), StandardCharsets.UTF_8))) {
            Template t = velocityEngine.getTemplate(templatePath.toString());
            t.merge(velocityContext, writer);
        }

        if (this.formatter != null) this.formatter.formatFile(outputFilePath.toString());

        return outputFilePath.toAbsolutePath();
    }

    // parametric - return result
    private Map<String, String> applyTemplateParametric(Path templatePath, String query, VelocityContext velocityContext) throws Exception {
        if (query != null) {
            Reader reader = (Reader) velocityContext.get("reader");
            List<Map<String, String>> dataframe = reader.getDataframe(query);

            HashMap<String, String> output = new HashMap<>();
            int counter = 0;
            for (Map<String, String> row : dataframe) {
                if (row != null) velocityContext.put("x", row);

                String rowId = Util.generateRowId(row, counter);
                String result = applyTemplate(templatePath, velocityContext);
                output.put(rowId, result);
                counter++;
            }
            return output;
        } else {
            throw new IllegalArgumentException("QueryPath parameter cannot be null");
        }
    }

    private List<Path> applyTemplateParametric(Path templatePath, String query, Path outputFilePath, VelocityContext velocityContext) throws Exception {
        if (query != null) {
            List<Path> resultFilePaths = new ArrayList<>();
            Reader reader = (Reader) velocityContext.get("reader");
            List<Map<String, String>> dataframe = reader.getDataframe(query);

            int counter = 0;
            for (Map<String, String> row : dataframe) {
                if (row != null) velocityContext.put("x", row);
                Path resultFilePath = applyTemplate(templatePath, Util.createOutputFileName(outputFilePath, counter), velocityContext);
                resultFilePaths.add(resultFilePath.toAbsolutePath());
                counter++;
            }
            return resultFilePaths;
        } else {
            throw new IllegalArgumentException("QueryPath parameter cannot be null");
        }
    }

    // STREAM OPTIONS
    // non-parametric return string result

    public String executeMapping(Map<String, Reader> readers, InputStream template) throws Exception {
        return executeMapping(readers, template, null, null);
    }

    public Path executeMapping(Map<String, Reader> readers, InputStream template, Path outputFile) throws Exception {
        return executeMapping(readers, template, outputFile,null, null);
    }

    public String executeMapping(Map<String, Reader> readers, InputStream template, TemplateFunctions templateFunctions, TemplateMap templateMap) throws Exception {
        VelocityContext velocityContext = templateFunctions == null ? Util.createVelocityContext(readers, templateMap) : Util.createVelocityContext(readers, templateMap, templateFunctions);
        return applyTemplate(template, velocityContext);
    }
    public Path executeMapping(Map<String, Reader> readers, InputStream template, Path outputFile, TemplateFunctions templateFunctions, TemplateMap templateMap) throws Exception {
        VelocityContext velocityContext = templateFunctions == null ? Util.createVelocityContext(readers, templateMap) : Util.createVelocityContext(readers, templateMap, templateFunctions);
        return applyTemplate(template, outputFile, velocityContext);
    }

    public Map<String, String> executeMappingParametric(Map<String, Reader> readers, InputStream template, InputStream query) throws Exception {
        return executeMappingParametric(readers, template, query, null, null);
    }

    public List<Path> executeMappingParametric(Map<String, Reader> readers, InputStream template, InputStream query, Path outputFilePath) throws Exception {
        return executeMappingParametric(readers, template, query, outputFilePath, null, null);
    }

    public Map<String, String> executeMappingParametric(Map<String, Reader> readers, InputStream template, InputStream query, TemplateFunctions templateFunctions, TemplateMap templateMap) throws Exception {
        String queryString = Util.inputStreamToString(query);
        VelocityContext velocityContext = templateFunctions == null ? Util.createVelocityContext(readers, templateMap) : Util.createVelocityContext(readers, templateMap, templateFunctions);
        return applyTemplateParametric(template, queryString, velocityContext);
    }
    public List<Path> executeMappingParametric(Map<String, Reader> readers, InputStream template, InputStream query, Path outputFilePath, TemplateFunctions templateFunctions, TemplateMap templateMap) throws Exception {
        String queryString = Util.inputStreamToString(query);
        VelocityContext velocityContext = templateFunctions == null ? Util.createVelocityContext(readers, templateMap) : Util.createVelocityContext(readers, templateMap, templateFunctions);
        return applyTemplateParametric(template, queryString, outputFilePath, velocityContext);
    }
    // non parametric - return string result
    private String applyTemplate(InputStream template, VelocityContext velocityContext) throws Exception {
        java.io.Reader templateReader = new InputStreamReader(template);
        StringWriter writer = new StringWriter();

        VelocityEngine velocityEngine = this.velocityEngine;
        velocityEngine.evaluate(velocityContext, writer, "TemplateExecutor", templateReader);

        String result = writer.toString();
        templateReader.close();
        writer.close();

        return this.formatter != null ? this.formatter.formatString(result) : result;
    }

    // non-parametric - write to file and return filePath
    private Path applyTemplate(InputStream template, Path outputFilePath, VelocityContext velocityContext) throws Exception {
        java.io.Reader templateReader = new InputStreamReader(template);
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFilePath.toString()), StandardCharsets.UTF_8));

        VelocityEngine velocityEngine = this.velocityEngine;
        velocityEngine.evaluate(velocityContext, writer, "TemplateExecutor", templateReader);

        templateReader.close();
        writer.close();

        if (this.formatter != null) this.formatter.formatFile(outputFilePath.toString());

        return outputFilePath.toAbsolutePath();
    }

    // parametric - return result
    private Map<String, String> applyTemplateParametric(InputStream template, String query, VelocityContext velocityContext) throws Exception {
        if (query != null) {
            Reader reader = (Reader) velocityContext.get("reader");
            List<Map<String, String>> dataframe = reader.getDataframe(query);

            // copy templateStream which is otherwise consumed
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            template.transferTo(baos);

            HashMap<String, String> output = new HashMap<>();
            int counter = 0;
            for (Map<String, String> row : dataframe) {
                if (row != null) velocityContext.put("x", row);

                String rowId = Util.generateRowId(row, counter);
                String result = applyTemplate(new ByteArrayInputStream(baos.toByteArray()), velocityContext);
                output.put(rowId, result);
                counter++;
            }
            return output;
        } else {
            throw new IllegalArgumentException("Query parameter cannot be null");
        }
    }

    // parametric - return list of filePaths
    private List<Path> applyTemplateParametric(InputStream template, String query, Path outputFilePath, VelocityContext velocityContext) throws Exception {
        if (query != null) {
            List<Path> resultFilePaths = new ArrayList<>();
            Reader reader = (Reader) velocityContext.get("reader");
            List<Map<String, String>> dataframe = reader.getDataframe(query);

            // copy templateStream which is otherwise consumed
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            template.transferTo(baos);

            int counter = 0;
            for (Map<String, String> row : dataframe) {
                if (row != null) velocityContext.put("x", row);
                Path resultFilePath = applyTemplate(new ByteArrayInputStream(baos.toByteArray()), Util.createOutputFileName(outputFilePath, counter), velocityContext);
                resultFilePaths.add(resultFilePath.toAbsolutePath());
                counter++;
            }
            return resultFilePaths;
        } else {
            throw new IllegalArgumentException("Query parameter cannot be null");
        }
    }

    public Path trimTemplate(Path templatePath) throws IOException {
        Path trimmedTempTemplatePath = Path.of(templatePath + ".tmp.vm");
        List<String> newLines = new ArrayList<>();
        for (String line : Files.readAllLines(templatePath, StandardCharsets.UTF_8)) {
            newLines.add(line.trim().replace("\n", "").replace("\r", ""));
        }
        String result = String.join(" ", newLines);
        try (PrintWriter out = new PrintWriter(trimmedTempTemplatePath.toFile(), StandardCharsets.UTF_8)) {
            out.println(result);
        }

        return trimmedTempTemplatePath;
    }
}
