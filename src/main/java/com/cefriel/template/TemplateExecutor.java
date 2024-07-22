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
	private Map<String, Reader> readers = null;
	private final TemplateFunctions templateFunctions;
	private final boolean failInvalidRef;
	private final boolean trimTemplate;
	private final boolean templateInResourcesFolder;
	private final TemplateMap templateMap;
	private final Formatter formatter;
	private final VelocityContext velocityContext;
	private final VelocityEngine velocityEngine;

	public TemplateExecutor(Map<String, Reader> readers, TemplateFunctions templateFunctions, boolean failInvalidRef, boolean trimTemplate, boolean templateInResourcesFolder, TemplateMap templateMap, Formatter formatter) {
		this.readers = readers;
		this.templateFunctions = templateFunctions;
		this.failInvalidRef = failInvalidRef;
		this.trimTemplate = trimTemplate;
		this.templateInResourcesFolder = templateInResourcesFolder;
		this.templateMap = templateMap;
		this.formatter = formatter;
		this.velocityContext = this.templateFunctions == null ?
				Util.createVelocityContext(readers, templateMap) : Util.createVelocityContext(readers, templateMap,templateFunctions);
		this.velocityEngine = Util.createVelocityEngine(this.templateInResourcesFolder,failInvalidRef);
	}

	public TemplateExecutor(Reader reader, TemplateFunctions templateFunctions, boolean failInvalidRef, boolean trimTemplate, boolean templateInResourcesFolder, TemplateMap templateMap, Formatter formatter) {
		this.readers = null;
		if(reader != null)
			this.readers = Map.of("reader", reader);

		this.templateFunctions = templateFunctions;
		this.failInvalidRef = failInvalidRef;
		this.trimTemplate = trimTemplate;
		this.templateInResourcesFolder = templateInResourcesFolder;
		this.templateMap = templateMap;
		this.formatter = formatter;
		this.velocityContext = this.templateFunctions == null ?
				Util.createVelocityContext(this.readers, templateMap) : Util.createVelocityContext(readers, templateMap,templateFunctions);
		this.velocityEngine = Util.createVelocityEngine(this.templateInResourcesFolder,failInvalidRef);
	}

	// methods called from cli
	public String executeMapping(Path templatePath) throws Exception {
		Path usedTemplatePath = this.trimTemplate ? trimTemplate(templatePath) : templatePath;
		return applyTemplate(usedTemplatePath);
	}
	public Path executeMapping(Path templatePath, Path outputFilePath) throws Exception {
		Path usedTemplatePath = this.trimTemplate ? trimTemplate(templatePath) : templatePath;
		return applyTemplate(usedTemplatePath, outputFilePath);
	}
	public Map<String,String> executeMappingParametric(Path templatePath, Path queryPath) throws Exception {
		Path usedTemplatePath = this.trimTemplate ? trimTemplate(templatePath) : templatePath;
		String query = Files.readString(queryPath);
		return applyTemplateParametric(usedTemplatePath, query);
	}
	public List<Path> executeMappingParametric(Path templatePath, Path queryPath, Path outputFilePath) throws Exception {
		Path usedTemplatePath = this.trimTemplate ? trimTemplate(templatePath) : templatePath;
		String query = Files.readString(queryPath);
		return applyTemplateParametric(usedTemplatePath, query, outputFilePath);
	}
	// non parametric - return string result
	private String applyTemplate(Path templatePath) throws Exception {
		StringWriter writer = new StringWriter();
		Template t = this.velocityEngine.getTemplate(templatePath.toString());
		t.merge(this.velocityContext, writer);
		String result = writer.toString();
		writer.close();

		return this.formatter != null ? this.formatter.formatString(result) : result;
	}
	// non-parametric - write to file and return filePath
	private Path applyTemplate(Path templatePath, Path outputFilePath) throws Exception {
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(outputFilePath.toFile()),
				StandardCharsets.UTF_8));
		Template t = velocityEngine.getTemplate(templatePath.toString());
		t.merge(this.velocityContext, writer);
		writer.close();

		if (this.formatter != null)
			this.formatter.formatFile(outputFilePath.toString());

		return outputFilePath.toAbsolutePath();
	}
	// parametric - return result
	private Map<String, String> applyTemplateParametric(Path templatePath, String query) throws Exception {
		if(query != null) {
			Reader reader = (Reader) this.velocityContext.get("reader");
			List<Map<String, String>> dataframe = reader.getDataframe(query);

			HashMap<String, String> output = new HashMap<>();
			int counter = 0;
			for (Map<String, String> row : dataframe) {
				if (row != null)
					this.velocityContext.put("x", row);

				String rowId = Util.generateRowId(row, counter);
				String result = applyTemplate(templatePath);
				output.put(rowId, result);
				counter++;
			}
			return output;
		}
		else {
			throw new IllegalArgumentException("QueryPath parameter cannot be null");
		}
	}
	private List<Path> applyTemplateParametric(Path templatePath, String query, Path outputFilePath) throws Exception {
		if(query != null) {
			List<Path> resultFilePaths = new ArrayList<>();
			Reader reader = (Reader) this.velocityContext.get("reader");
			List<Map<String, String>> dataframe = reader.getDataframe(query);

			int counter = 0;
			for (Map<String, String> row : dataframe) {
				if (row != null)
					this.velocityContext.put("x", row);
				Path resultFilePath = applyTemplate(templatePath, Util.createOutputFileName(outputFilePath, counter));
				resultFilePaths.add(resultFilePath.toAbsolutePath());
				counter++;
			}
			return resultFilePaths;
		}
		else {
			throw new IllegalArgumentException("QueryPath parameter cannot be null");
		}
	}

	// STREAM OPTIONS
	// non-parametric return string result
		public String executeMapping(InputStream template) throws Exception {
			return applyTemplate(template);
		}
		public Path executeMapping(InputStream template, Path outputFilePath) throws Exception {
			return applyTemplate(template, outputFilePath);
		}
		public Map<String,String> executeMappingParametric(InputStream template, InputStream query) throws Exception {
			String queryString = Util.inputStreamToString(query);
			return applyTemplateParametric(template, queryString);
		}

		public List<Path> executeMappingParametric(InputStream template, InputStream query, Path outputFilePath) throws Exception {
			String queryString = Util.inputStreamToString(query);
			return applyTemplateParametric(template, queryString, outputFilePath);
		}
		// non parametric - return string result
		private String applyTemplate(InputStream template) throws Exception {
			java.io.Reader templateReader = new InputStreamReader(template);
			StringWriter writer = new StringWriter();

			VelocityEngine velocityEngine = this.velocityEngine;
			velocityEngine.evaluate(this.velocityContext, writer, "TemplateExecutor", templateReader);

			String result = writer.toString();
			templateReader.close();
			writer.close();

			return this.formatter != null ? this.formatter.formatString(result) : result;
		}

		// non-parametric - write to file and return filePath
		private Path applyTemplate(InputStream template, Path outputFilePath) throws Exception {
			java.io.Reader templateReader = new InputStreamReader(template);
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(outputFilePath.toString()),
					StandardCharsets.UTF_8));

			VelocityEngine velocityEngine = this.velocityEngine;
			velocityEngine.evaluate(this.velocityContext, writer, "TemplateExecutor", templateReader);

			templateReader.close();
			writer.close();

			if (this.formatter != null)
				this.formatter.formatFile(outputFilePath.toString());

			return outputFilePath.toAbsolutePath();
		}

		// parametric - return result
		private Map<String, String> applyTemplateParametric(InputStream template, String query) throws Exception {
			if(query != null) {
				Reader reader = (Reader) this.velocityContext.get("reader");
				List<Map<String, String>> dataframe = reader.getDataframe(query);

				// copy templateStream which is otherwise consumed
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				template.transferTo(baos);


				HashMap<String, String> output = new HashMap<>();
				int counter = 0;
				for (Map<String, String> row : dataframe) {
					if (row != null)
						this.velocityContext.put("x", row);

					String rowId = Util.generateRowId(row, counter);
					String result = applyTemplate(new ByteArrayInputStream(baos.toByteArray()));
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
		private List<Path> applyTemplateParametric(InputStream template, String query, Path outputFilePath) throws Exception {
			if(query != null) {
				List<Path> resultFilePaths = new ArrayList<>();
				Reader reader = (Reader) this.velocityContext.get("reader");
				List<Map<String, String>> dataframe = reader.getDataframe(query);

				// copy templateStream which is otherwise consumed
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				template.transferTo(baos);

				int counter = 0;
				for (Map<String, String> row : dataframe) {
					if (row != null)
						this.velocityContext.put("x", row);
					Path resultFilePath = applyTemplate(new ByteArrayInputStream(baos.toByteArray()), Util.createOutputFileName(outputFilePath, counter));
					resultFilePaths.add(resultFilePath.toAbsolutePath());
					counter++;
				}
				return resultFilePaths;
			}
			else {
				throw new IllegalArgumentException("Query parameter cannot be null");
			}
		}
	public Path trimTemplate(Path templatePath) throws IOException {
		Path trimmedTempTemplatePath = Path.of(templatePath + ".tmp.vm");
		List<String> newLines = new ArrayList<>();
		for (String line : Files.readAllLines(templatePath, StandardCharsets.UTF_8)) {
			newLines.add(line.trim().replace("\n", "").replace("\r",""));
		}
		String result = String.join(" ", newLines);
		try (PrintWriter out = new PrintWriter(trimmedTempTemplatePath.toFile(), StandardCharsets.UTF_8)) {
			out.println(result);
		}

		return trimmedTempTemplatePath;
	}
}
