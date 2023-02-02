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
import com.cefriel.template.utils.TemplateFunctions;
import com.cefriel.template.utils.Util;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
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
	// methods called from cli
	public String executeMapping(Reader reader, String templatePath, boolean templateInResourcesFolder, boolean trimTemplate, TemplateMap templateMap, Formatter formatter, TemplateFunctions templateFunctions) throws Exception {
		VelocityContext velocityContext;
		velocityContext = templateFunctions == null ? Util.createVelocityContext(reader, templateMap) : Util.createVelocityContext(reader, templateMap,templateFunctions);
		VelocityEngine velocityEngine = Util.createVelocityEngine(templateInResourcesFolder);
		String usedTemplatePath = trimTemplate ? trimTemplate(templatePath) : templatePath;
		return applyTemplate(usedTemplatePath, velocityContext, velocityEngine, formatter);
	}
	public String executeMapping(Reader reader, String templatePath, boolean templateInResourcesFolder, boolean trimTemplate, String outputFilePath, TemplateMap templateMap, Formatter formatter, TemplateFunctions templateFunctions) throws Exception {
		VelocityContext velocityContext;
		velocityContext = templateFunctions == null ? Util.createVelocityContext(reader, templateMap) : Util.createVelocityContext(reader, templateMap, templateFunctions);
		VelocityEngine velocityEngine = Util.createVelocityEngine(templateInResourcesFolder);
		String usedTemplatePath = trimTemplate ? trimTemplate(templatePath) : templatePath;
		return applyTemplate(usedTemplatePath, velocityContext, velocityEngine, formatter, outputFilePath);
	}
	public Map<String,String> executeMappingParametric(Reader reader, String templatePath, boolean templateInResourcesFolder, boolean trimTemplate, String queryPath, TemplateMap templateMap, Formatter formatter, TemplateFunctions templateFunctions) throws Exception {
		VelocityContext velocityContext;
		velocityContext = templateFunctions == null ? Util.createVelocityContext(reader, templateMap) : Util.createVelocityContext(reader, templateMap, templateFunctions);
		VelocityEngine velocityEngine = Util.createVelocityEngine(templateInResourcesFolder);
		String usedTemplatePath = trimTemplate ? trimTemplate(templatePath) : templatePath;
		String query = Files.readString(Paths.get(queryPath));
		return applyTemplateParametric(usedTemplatePath, query, velocityContext, velocityEngine, formatter);
	}
	public List<String> executeMappingParametric(Reader reader, String templatePath, boolean templateInResourcesFolder, boolean trimTemplate, String queryPath, String outputFilePath, TemplateMap templateMap, Formatter formatter, TemplateFunctions templateFunctions) throws Exception {
		VelocityContext velocityContext;
		velocityContext = templateFunctions == null ? Util.createVelocityContext(reader, templateMap) : Util.createVelocityContext(reader, templateMap, templateFunctions);
		VelocityEngine velocityEngine = Util.createVelocityEngine(templateInResourcesFolder);
		String usedTemplatePath = trimTemplate ? trimTemplate(templatePath) : templatePath;
		String query = Files.readString(Paths.get(queryPath));
		return applyTemplateParametric(usedTemplatePath, query, velocityContext, velocityEngine, formatter, outputFilePath);
	}
	// non parametric - return string result
	private String applyTemplate(String templatePath, VelocityContext context, VelocityEngine velocityEngine, Formatter formatter) throws Exception {
		StringWriter writer = new StringWriter();
		Template t = velocityEngine.getTemplate(templatePath);
		t.merge(context, writer);
		String result = writer.toString();
		writer.close();

		return formatter != null ? formatter.formatString(result) : result;
	}
	// non-parametric - write to file and return filePath
	private String applyTemplate(String templatePath, VelocityContext context, VelocityEngine velocityEngine, Formatter formatter, String outputFilePath) throws Exception {
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(outputFilePath),
				StandardCharsets.UTF_8));
		Template t = velocityEngine.getTemplate(templatePath);
		t.merge(context, writer);
		writer.close();

		if (formatter != null)
			formatter.formatFile(outputFilePath);

		return outputFilePath;
	}
	// parametric - return result
	private Map<String, String> applyTemplateParametric(String templatePath, String query,  VelocityContext context, VelocityEngine velocityEngine, Formatter formatter) throws Exception {
		if(query != null) {
			Reader reader = (Reader) context.get("reader");
			List<Map<String, String>> dataframe = reader.getDataframe(query);

			HashMap<String, String> output = new HashMap<>();
			int counter = 0;
			for (Map<String, String> row : dataframe) {
				if (row != null)
					context.put("x", row);

				String rowId = Util.generateRowId(row, counter);
				String result = applyTemplate(templatePath, context, velocityEngine, formatter);
				output.put(rowId, result);
				counter++;
			}
			return output;
		}
		else {
			throw new IllegalArgumentException("QueryPath parameter cannot be null");
		}
	}
	private List<String> applyTemplateParametric(String templatePath, String query, VelocityContext context, VelocityEngine velocityEngine, Formatter formatter, String outputFilePath) throws Exception {
		if(query != null) {
			List<String> resultFilePaths = new ArrayList<>();
			Reader reader = (Reader) context.get("reader");
			List<Map<String, String>> dataframe = reader.getDataframe(query);

			int counter = 0;
			for (Map<String, String> row : dataframe) {
				if (row != null)
					context.put("x", row);
				String resultFilePath = applyTemplate(templatePath, context, velocityEngine, formatter,
						Util.createOutputFileName(outputFilePath, counter));
				resultFilePaths.add(resultFilePath);
				counter++;
			}
			return resultFilePaths;
		}
		else {
			throw new IllegalArgumentException("QueryPath parameter cannot be null");
		}
	}

	// STREAM OPTIONS
	// todo add templateFunctions param
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
			velocityEngine.evaluate(context, writer, "TemplateExecutor", templateReader);

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
			velocityEngine.evaluate(context, writer, "TemplateExecutor", templateReader);

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
	public String trimTemplate(String templatePath) throws IOException {
		String trimmedTempTemplatePath = templatePath + ".tmp.vm";
		List<String> newLines = new ArrayList<>();
		for (String line : Files.readAllLines(Paths.get(templatePath), StandardCharsets.UTF_8)) {
			newLines.add(line.trim().replace("\n", "").replace("\r",""));
		}
		String result = String.join(" ", newLines);
		try (PrintWriter out = new PrintWriter(trimmedTempTemplatePath, StandardCharsets.UTF_8)) {
			out.println(result);
		}
		return trimmedTempTemplatePath;
	}
}
