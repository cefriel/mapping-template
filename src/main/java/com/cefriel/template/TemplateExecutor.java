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
package com.cefriel.template;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cefriel.template.io.Formatter;
import com.cefriel.template.io.Reader;
import com.cefriel.template.utils.TemplateUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.slf4j.LoggerFactory;

public class TemplateExecutor {

	private final org.slf4j.Logger log = LoggerFactory.getLogger(TemplateExecutor.class);
	private static final String DEFAULT_KEY = "default";

	// Constructor parameters
	private final TemplateUtils lu;

	// Configurable parameters
	private Formatter formatter;
	private boolean trimTemplate;
	private boolean resourceTemplate;
	private Map<String, String> map;

	// Default parameters
	private VelocityEngine velocityEngine;
	private final Reader reader;

	private int count = 0;

	public TemplateExecutor(Reader reader) throws Exception {
		this.reader = reader;
		this.lu = new TemplateUtils();
	}

	public TemplateExecutor(Reader reader, TemplateUtils lu) throws Exception {
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
        return evaluateProcedure(template, null).get(DEFAULT_KEY);
    }

	public Map<String, String> lower(InputStream template, String queryFile) throws Exception {
		return evaluateProcedure(template, queryFile);
	}

    private void procedure(String templatePath, String destinationPath, String queryFile) throws Exception {
		VelocityContext context = initEngine();
		executeLowering(templatePath, destinationPath, queryFile, context);
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

		if (map != null)
			context.put("map", map);

		return context;
	}

    private Map<String, String> evaluateProcedure(InputStream template, String queryFile) throws Exception {
        VelocityContext context = initEngine();

        if (trimTemplate)
            log.warn("InputStream Template: trim option not supported!");

		Map<String, String> output = new HashMap<>();
		if(queryFile != null) {
			String query = Files.readString(Paths.get(queryFile));
			log.info("Parametric Template executed with query: " + queryFile);
			List<Map<String, String>> rows = reader.executeQueryStringValue(query);

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			template.transferTo(baos);
			for (Map<String, String> row : rows)
				executeTemplateStream(new ByteArrayInputStream(baos.toByteArray()),
						context, output, row);
		} else {
			executeTemplateStream(template, context, output);
		}
		return output;
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

	private Map<String, String> executeTemplateStream(InputStream template, VelocityContext context,
													  Map<String, String> output) throws Exception {
		return executeTemplateStream(template, context, output, null);
	}

	private Map<String, String> executeTemplateStream(InputStream template, VelocityContext context,
													  Map<String, String> output, Map<String, String> row)
			throws Exception {
		String id = generateId(row);
		if (id.isEmpty())
			id = DEFAULT_KEY;
		if (row != null)
			context.put("x", row);

		log.info("Executing Template " + id);

		java.io.Reader templateReader = new InputStreamReader(template);
		Writer writer = new StringWriter();
		velocityEngine.evaluate(context, writer, "TemplateExecutor", templateReader);
		templateReader.close();

		String result = writer.toString();
		writer.close();

		output.put(id, handleStreamFormat(result));
		return output;
	}

	private void handleFormat(String pathId) throws Exception {
		if (formatter != null)
			formatter.formatFile(pathId);
	}

	private String handleStreamFormat(String s) throws Exception {
		if (formatter != null)
			return formatter.formatString(s);
		return s;
	}

	private String generateId(Map<String, String> row) {
		if (row != null)
			if (row.containsKey("id"))
				return row.get("id");
			else {
				String id = "T-id-" + count;
				count += 1;
				return id;
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

	public Formatter getFormatter() {
		return formatter;
	}

	public void setFormatter(Formatter formatter) {
		this.formatter = formatter;
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

	public Map<String, String> getMap() {
		return map;
	}

	public void setMap(Map<String, String> map) {
		this.map = map;
	}

}
