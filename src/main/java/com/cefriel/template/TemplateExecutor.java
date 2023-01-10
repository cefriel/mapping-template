/*
 * Copyright (c) 2019-2022 Cefriel.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.cefriel.template;

import com.cefriel.template.io.Formatter;
import com.cefriel.template.io.Reader;
import com.cefriel.template.io.rdf.RDFReader;
import com.cefriel.template.utils.TemplateUtils;
import com.cefriel.template.utils.Util;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.eclipse.rdf4j.repository.Repository;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TemplateExecutor {

	private final org.slf4j.Logger log = LoggerFactory.getLogger(TemplateExecutor.class);
	private static final String DEFAULT_KEY = "default";
	private boolean resourceTemplate; // true if in resource folder, search in classpath

	// Default parameters
	private VelocityEngine velocityEngine;

	// method called from cli
	public void executeMapping(Reader reader, TemplateMap map, String templatePath, boolean trimTemplate, String queryFile, Formatter formatter, String outputFile) throws Exception {
		VelocityContext velocityContext = initEngine(reader, map);
		log.info("Template path: " + templatePath);
		if (trimTemplate)
			templatePath = trimTemplate(templatePath);

		if(queryFile != null) {
			String query = Files.readString(Paths.get(queryFile));
			log.info("Parametric Template executed with query: " + queryFile);
			List<Map<String, String>> rows = reader.getDataframe(query);
			int count = 0;
			for (Map<String, String> row : rows) {
				executeTemplate(templatePath, outputFile, velocityContext, formatter, row, generateId(row, count));
				count++;
			}

		} else {
			executeTemplate(templatePath, outputFile, velocityContext, formatter);
		}
	}

	// method called when used as library
	public Map<String, String> executeMapping(Repository repository, String namedGraph, String baseIri, boolean verbose, InputStream template, InputStream templateMapStream, boolean isCsv, InputStream query, String formatterFormat) throws Exception {
		RDFReader rdfReader = Util.createRDFReader(namedGraph, baseIri, repository, verbose);
		TemplateMap templateMap = Util.createTemplateMap(templateMapStream, isCsv);
		VelocityContext velocityContext = initEngine(rdfReader, templateMap);
		Formatter formatter = Util.createFormatter(formatterFormat);

		return evaluateProcedure(rdfReader, template, query, velocityContext, formatter);
	}
	public String executeMapping(Repository repository, String namedGraph, String baseIri, boolean verbose,InputStream template, InputStream templateMapStream, boolean isCsv, String formatterFormat) throws Exception {
		RDFReader rdfReader = Util.createRDFReader(namedGraph, baseIri, repository, verbose);
		TemplateMap templateMap = Util.createTemplateMap(templateMapStream, isCsv);
		VelocityContext velocityContext = initEngine(rdfReader, templateMap);
		Formatter formatter = Util.createFormatter(formatterFormat);

		return evaluateProcedure(rdfReader, template, null, velocityContext, formatter).get(DEFAULT_KEY);
	}
	public Map<String, String> executeMapping(String input, String inputFormat, boolean verbose, InputStream template, InputStream templateMapStream, boolean isCsv, InputStream query, String formatterFormat) throws Exception {
		Reader reader = Util.createNonRdfReaderFromInput(input, inputFormat, verbose);
		TemplateMap templateMap = Util.createTemplateMap(templateMapStream, isCsv);
		VelocityContext velocityContext = initEngine(reader, templateMap);
		Formatter formatter = Util.createFormatter(formatterFormat);

		return evaluateProcedure(reader, template, query, velocityContext, formatter);
	}

	public String executeMapping(String input, String inputFormat, boolean verbose, InputStream template, InputStream templateMapStream, boolean isCsv, String formatterFormat) throws Exception {
		Reader reader = Util.createNonRdfReaderFromInput(input, inputFormat, verbose);
		TemplateMap templateMap = Util.createTemplateMap(templateMapStream, isCsv);
		VelocityContext velocityContext = initEngine(reader, templateMap);
		Formatter formatter = Util.createFormatter(formatterFormat);

		return evaluateProcedure(reader, template, null, velocityContext, formatter).get(DEFAULT_KEY);
	}


	private VelocityContext initEngine(Reader reader, TemplateMap templateMap) throws IOException {
		if(this.velocityEngine == null) {
			this.velocityEngine = new VelocityEngine();
			if (this.resourceTemplate) {
				this.velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
				this.velocityEngine.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
			}
			this.velocityEngine.init();
		}

		VelocityContext context = new VelocityContext();
		if(reader != null) {
			context.put("reader", reader);
		}

		context.put("functions", new TemplateUtils());

		if (templateMap != null)
			context.put("map", templateMap);

		return context;
	}

    private Map<String, String> evaluateProcedure(Reader reader, InputStream template, InputStream query, VelocityContext context, Formatter formatter) throws Exception {
		Map<String, String> output = new HashMap<>();
		if(query != null) {
			String queryString = inputStreamToString(query);
			log.info("Parametric Template executed with query: " + queryString);
			List<Map<String, String>> rows = reader.getDataframe(queryString);

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			template.transferTo(baos);

			int c = 0;
			for (Map<String, String> row : rows) {
				KV result = executeTemplateStream(new ByteArrayInputStream(baos.toByteArray()),
						context, formatter, row, generateId(row, c));
				c++;
				output.put(result.k(), result.v());
			}
		} else {
			KV result = executeTemplateStream(template, context, formatter);
			output.put(result.k(), result.v());
		}
		return output;
	}
	private void executeTemplate(String templatePath, String destinationPath, VelocityContext context, Formatter formatter) throws Exception {
		executeTemplate(templatePath, destinationPath, context, formatter, null, "");
	}
	private void executeTemplate(String templatePath, String destinationPath, VelocityContext context, Formatter formatter, Map<String, String> row, String templateId) throws Exception {
		if (row != null)
			context.put("x", row);

		log.info("Executing Template" + templateId);
		String pathId = getPathId(destinationPath, templateId);

		Writer writer;
		writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(pathId), StandardCharsets.UTF_8));
		Template t = velocityEngine.getTemplate(templatePath);
		t.merge(context, writer);
		writer.close();
		if (formatter != null) {
			formatter.formatFile(pathId);
		}
	}
	// simple key-value type
	private record KV (String k, String v) {}
	private KV executeTemplateStream(InputStream template, VelocityContext context,
									 Formatter formatter) throws Exception {
		return executeTemplateStream(template, context, formatter, null, "");
	}
	private KV executeTemplateStream(InputStream template, VelocityContext context,
									 Formatter formatter, Map<String, String> row, String templateId) throws Exception {
		String id = templateId.isEmpty() ? DEFAULT_KEY : templateId;
		if (row != null)
			context.put("x", row);

		log.info("Executing Template " + id);

		java.io.Reader templateReader = new InputStreamReader(template);
		StringWriter writer = new StringWriter();
		velocityEngine.evaluate(context, writer, "TemplateExecutor", templateReader);

		templateReader.close();

		String result = writer.toString();
		writer.close();

		return new KV(id, formatter.formatString(result));
	}

	public static String inputStreamToString(InputStream input) throws IOException {
		return new String(input.readAllBytes(), StandardCharsets.UTF_8);
	}
	private String generateId(Map<String, String> row, int number) {
		if (row != null)
			if (row.containsKey("id"))
				return row.get("id");
			else {
				return "T-id-" + number;
			}
		else
			return "";
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

	public String getPathId(String destinationPath, String id) {
		String pathId = destinationPath + id;
		if (destinationPath.contains(".") && !id.isEmpty()) {
			String extension = destinationPath.substring(destinationPath.lastIndexOf(".") + 1);
			pathId = destinationPath.substring(0, destinationPath.lastIndexOf(".")) + "-" + id + "." + extension;
		}
		return pathId;
	}
}
