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
package com.cefriel.lowerer;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.cefriel.io.Reader;
import com.cefriel.io.rdf.RDFReader;
import com.cefriel.io.rdf.RDFUtils;
import com.cefriel.io.rdf.RDFWriter;
import com.cefriel.utils.LoweringUtils;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Serializer;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.slf4j.LoggerFactory;

public class TemplateLowerer {

	private org.slf4j.Logger log = LoggerFactory.getLogger(TemplateLowerer.class);

	// Constructor parameters
	private LoweringUtils lu;

	// Configurable parameters
	private String keyValuePairsPath;
	private String keyValueCsvPath;
	private String format;
	private boolean trimTemplate;
	private boolean resourceTemplate;

	private VelocityEngine velocityEngine;
	private Reader reader;

	private int count = 0;

	public TemplateLowerer(Reader reader) throws Exception {
		this.reader = reader;
		this.lu = new LoweringUtils();
	}

	public TemplateLowerer(Reader reader, LoweringUtils lu) throws Exception {
		this.reader = reader;
		this.lu = lu;
	}

	public void lower(String templatePath, String destinationPath) throws Exception {
		procedure(templatePath, destinationPath, null);
	}

	public void lower(String templatePath, String destinationPath, String queryFile) throws Exception {
		procedure(templatePath, destinationPath, queryFile);
	}

    public String lower(InputStream template) throws Exception {
        return evaluateProcedure(template);
    }

    private void procedure(String templatePath, String destinationPath, String queryFile) throws Exception {
		VelocityContext context = initEngine();
		executeLowering(templatePath, destinationPath, queryFile, context);
	}

    private String evaluateProcedure(InputStream template) throws Exception {
        VelocityContext context = initEngine();

        if (trimTemplate)
            log.warn("InputStream Template: trim option not supported!");

        java.io.Reader templateReader = new InputStreamReader(template);
        Writer writer = new StringWriter();
        velocityEngine.evaluate(context, writer, "TemplateLowerer", templateReader);
        templateReader.close();

        String result = writer.toString();
        writer.close();

        return handleStreamFormat(result);
    }

	private VelocityContext initEngine() throws IOException {
		if(velocityEngine == null) {
			velocityEngine = new VelocityEngine();
			if (resourceTemplate) {
				velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
				velocityEngine.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
			}
			velocityEngine.init();
		}

		VelocityContext context = new VelocityContext();
		context.put("reader", reader);
		context.put("functions", lu);

		Map<String, String> map = new HashMap<>();
		if(keyValuePairsPath !=  null)
			map.putAll(parseMap(keyValuePairsPath));
		if(keyValueCsvPath !=  null)
			map.putAll(parseCsvMap(keyValueCsvPath));
		context.put("map", map);

		return context;
	}

	private void executeLowering(String templatePath, String destinationPath, String queryFile, VelocityContext context) throws Exception {
		log.info("Template path: " + templatePath);

		if(trimTemplate)
			templatePath = trimTemplate(templatePath);

		if(queryFile != null) {
			String query = Files.readString(Paths.get(queryFile));
			log.info("Parametric Template executed with query: " + queryFile);
			List<Map<String, String>> rows = reader.executeQueryStringValue(query);

			for (Map<String, String> row : rows)
				executeTemplate(templatePath, destinationPath, context, row);
		} else {
			executeTemplate(templatePath, destinationPath, context);
		}
	}

	private void executeTemplate(String templatePath, String destinationPath, VelocityContext context) throws Exception {
		executeTemplate(templatePath, destinationPath, context, null);
	}

	private void executeTemplate(String templatePath, String destinationPath, VelocityContext context, Map<String, String> row) throws Exception {
    	String id = generateId(row);
		if (row != null)
			context.put("x", row);

		log.info("Executing Template" + id);
		String pathId = getPathId(destinationPath, id);

		Writer writer;
		writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(pathId), StandardCharsets.UTF_8));
		Template t = velocityEngine.getTemplate(templatePath);
		t.merge(context, writer);
		writer.close();

		handleFormat(pathId);
	}

	private void handleFormat(String pathId) throws Exception {
    	if (format != null)
			switch (format) {
				case "xml":
					Builder builder = new Builder();
					InputStream ins = new BufferedInputStream(new FileInputStream(pathId));
					Document doc = builder.build(ins);
					BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(new File(pathId)));
					formatXML(doc, bos);
					bos.close();
					break;
				case "turtle":
					RDFUtils.serializeFile(pathId, RDFFormat.TURTLE);
					break;
				case "rdfxml":
					RDFUtils.serializeFile(pathId, RDFFormat.RDFXML);
					break;
				case "nt":
					RDFUtils.serializeFile(pathId, RDFFormat.NQUADS);
					break;
			}
	}

	private String handleStreamFormat(String result) throws Exception {
		if (format != null)
			switch (format) {
				case "xml":
					Builder builder = new Builder();
					InputStream ins = new BufferedInputStream(new ByteArrayInputStream(result.getBytes()));
					Document doc = builder.build(ins);
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					formatXML(doc, baos);
					String formatted = baos.toString();
					baos.close();
					return formatted;
				// TODO Make input RDFFormat configurable
				case "turtle":
					return RDFUtils.serialize(result, RDFFormat.TURTLE, RDFFormat.TURTLE);
				case "rdfxml":
					return RDFUtils.serialize(result, RDFFormat.TURTLE, RDFFormat.RDFXML);
				case "nt":
					return RDFUtils.serialize(result, RDFFormat.TURTLE, RDFFormat.NQUADS);
			}
		return result;
	}

	private String generateId(Map<String, String> row) {
		if (row != null)
			if (row.containsKey("id"))
				return "-" + row.get("id");
			else {
				String id = "-T-id-" + count;
				count += 1;
				return id;
			}
		else
			return "";
	}

	public void formatXML(Document doc, OutputStream os) throws IOException {
		Serializer serializer = new Serializer(os, "utf-8");
		serializer.setIndent(2);
		serializer.setMaxLength(0);
		serializer.write(doc);
	}

	public String trimTemplate(String templatePath) throws IOException {
		String newTemplatePath = templatePath + ".tmp.vm";
		List<String> newLines = new ArrayList<>();
		for (String line : Files.readAllLines(Paths.get(templatePath), StandardCharsets.UTF_8)) {
			newLines.add(line.trim().replace("\n", "").replace("\r",""));
		}
		String result = String.join(" ", newLines);
		try (PrintWriter out = new PrintWriter(newTemplatePath, StandardCharsets.UTF_8)) {
			out.println(result);
		}
		return newTemplatePath;
	}

	public Map<String,String> parseMap(String filePath) throws IOException {
		Path path = FileSystems.getDefault().getPath(filePath);
		Map<String, String> mapFromFile = Files.lines(path)
				.filter(s -> s.matches("^\\w+:.+"))
				.collect(Collectors.toMap(k -> k.split(":")[0], v -> v.substring(v.indexOf(":") + 1)));
		return mapFromFile;
	}

	public Map<String,String> parseCsvMap(String filePath) throws IOException {
		Path path = FileSystems.getDefault().getPath(filePath);
		List<String[]> fromCSV = Files.lines(path)
				.map(s -> s.split(","))
				.collect(Collectors.toList());
		Map<String,String> mapFromFile = new HashMap<>();
		String[] keys = fromCSV.get(0);
		String[] values = fromCSV.get(1);
		for(int i = 0; i < keys.length; i++)
			mapFromFile.put(keys[i], values[i]);
		return mapFromFile;
	}

	public String getPathId(String destinationPath, String id) {
		String pathId = destinationPath + id;
		if (destinationPath.contains(".")) {
			String extension = destinationPath.substring(destinationPath.lastIndexOf(".") + 1);
			pathId = destinationPath.substring(0, destinationPath.lastIndexOf(".")) + id + "." + extension;
		}
		return pathId;
	}

	public String getKeyValuePairsPath() {
		return keyValuePairsPath;
	}

	public void setKeyValuePairsPath(String keyValuePairsPath) {
		this.keyValuePairsPath = keyValuePairsPath;
	}

	public String getKeyValueCsvPath() {
		return keyValueCsvPath;
	}

	public void setKeyValueCsvPath(String keyValueCsvPath) {
		this.keyValueCsvPath = keyValueCsvPath;
	}

	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	public boolean isTrimTemplate() {
		return trimTemplate;
	}

	public void setTrimTemplate(boolean trimTemplate) {
		this.trimTemplate = trimTemplate;
	}

	public boolean isResourceTemplate() {
		return resourceTemplate;
	}

	public void setResourceTemplate(boolean resourceTemplate) {
		this.resourceTemplate = resourceTemplate;
	}

}
