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

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.cefriel.template.io.Formatter;
import com.cefriel.template.io.Reader;
import com.cefriel.template.utils.RMLCompilerUtils;
import com.cefriel.template.utils.TemplateFunctions;
import com.cefriel.template.utils.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.FileWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {
	@Parameter(names={"--template","-t"})
	private Path templatePath = Path.of("template.vm");
	@Parameter(names={"--input","-i"},
			variableArity = true)
	private List<Path> inputFilesPaths = null;
	@Parameter(names={"--input-format","-if"})
	private String inputFormat = null;
	@Parameter(names={"--baseiri","-iri"})
	private String baseIri = "http://www.cefriel.com/data/";
	@Parameter(names={"--basepath","-b"})
	private Path basePath;
	@Parameter(names={"--output","-o"})
	private Path destinationPath = Path.of("output.txt");
	@Parameter(names={"--key-value","-kv"})
	private Path keyValuePairsPath;
	@Parameter(names={"--key-value-csv","-kvc"})
	private Path keyValueCsvPath;
	@Parameter(names={"--format","-f"})
	private String format;
	@Parameter(names={"--compile-rml","-rml"})
	private boolean compileRML;
	@Parameter(names={"--trim","-tr"})
	private boolean trimTemplate;
	@Parameter(names={"--template-resource","-trs"})
	private boolean templateInResources;
    	@Parameter(names={"--fail-invalid-ref","-fir"})
	private boolean failInvalidRef;
	@Parameter(names={"--username","-us"})
	private String username;
	@Parameter(names={"--password","-psw"})
	private String password;
	@Parameter(names={"--remote-url","-url"})
	private String dbAddress;
	@Parameter(names={"--remote-id","-id"})
	private String dbId;
	@Parameter(names={"--contextIRI","-c"})
	private String context;
	@Parameter(names={"--query","-q"})
	private Path queryPath;
	@Parameter(names={"--debug-query","-dq"})
	private boolean debugQuery;
	@Parameter(names={"--verbose","-v"})
	private boolean verbose;
	@Parameter(names={"--time","-tm"})
	private Path timePath;
	@Parameter(names={"--functions","-fun"})
	private Path functionsPath;

	private final Logger log = LoggerFactory.getLogger(Main.class);

	public static void main(String ... argv) throws Exception {

		Main main = new Main();

		JCommander.newBuilder()
				.addObject(main)
				.build()
				.parse(argv);

		main.updateBasePath();
		main.exec();
	}

	public void updateBasePath(){
		if (basePath != null) {
		    templatePath = basePath.resolve(templatePath);
			if (inputFilesPaths != null)
				for (int i = 0; i < inputFilesPaths.size(); i++)
					if(inputFilesPaths.get(i) != null)
						inputFilesPaths.set(i, basePath.resolve(inputFilesPaths.get(i)));
			destinationPath = basePath.resolve(destinationPath);
			if (queryPath != null)
				queryPath = basePath.resolve(queryPath);
			if (keyValueCsvPath != null)
				keyValueCsvPath = basePath.resolve(keyValueCsvPath);
			if (keyValuePairsPath != null)
				keyValuePairsPath = basePath.resolve(keyValuePairsPath);
			if (timePath != null)
				timePath = basePath.resolve(timePath);
			if (functionsPath != null)
				functionsPath = basePath.resolve(functionsPath);
		}
	}

	public void exec() throws Exception {
		Reader reader = Util.createReader(inputFormat, inputFilesPaths, dbAddress, dbId, context, baseIri, username, password);

		if (reader != null) {
			reader.setVerbose(verbose);

			if (debugQuery) {
				if (queryPath == null)
					log.error("Provide a query using the --query option");
				else {
					String debugQueryFromFile = Files.readString(queryPath);
					reader.debugQuery(debugQueryFromFile, destinationPath);
				}
			}
		}

		TemplateMap templateMap = null;
		if (keyValueCsvPath != null) {
			templateMap = new TemplateMap(keyValueCsvPath, true);
		}
		if (keyValuePairsPath != null) {
			templateMap = new TemplateMap(keyValuePairsPath, false);
		}

		Formatter formatter = null;
		if(format != null) {
			formatter = Util.createFormatter(format);
			if(reader != null)
				reader.setOutputFormat(format);
		}

		TemplateFunctions templateFunctions = new TemplateFunctions();
		if (functionsPath != null) {
			JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
			File utilsFile = functionsPath.toFile();
			compiler.run(null, null, null, utilsFile.getPath());
			File classDir = functionsPath.getParent() != null ?
					functionsPath.getParent().toFile() : Path.of("./").resolve(functionsPath).toFile();
			URLClassLoader classLoader = URLClassLoader.newInstance(new URL[]{(classDir).toURI().toURL()});

			// List all the files in the directory and identify the class file
			File[] classFiles = classDir.listFiles((dir, name) -> name.endsWith(".class"));
			if (classFiles != null) {
				for (File classFile : classFiles) {
					String className = classFile.getName().replace(".class", "");
					Class<?> loadedClass = Class.forName(className, true, classLoader);

					if ((TemplateFunctions.class).isAssignableFrom(loadedClass)) {
						templateFunctions = (TemplateFunctions) loadedClass.getDeclaredConstructor().newInstance();
						break;
					}
				}
			}
		}
		if(compileRML) {
			Util.validateRML(templatePath, verbose);

			Reader compilerReader = TemplateFunctions.getRDFReaderFromFile(templatePath.toString());

			Path rmlCompiler;
			if (trimTemplate) {
				rmlCompiler = Paths.get("rml/rml-compiler.vm.tmp.vm");
				// Avoid trim on the compiled template
				trimTemplate = false;
			} else
				rmlCompiler = Paths.get("rml/rml-compiler.vm");
			RMLCompilerUtils rmlCompilerUtils = new RMLCompilerUtils();

			Map<String,String> rmlMap = new HashMap<>();
			// Extract base IRI if provided
			String baseIriRML = rmlCompilerUtils.getBaseIRI(templatePath);
			baseIriRML = baseIriRML != null ? baseIriRML : baseIri;
			rmlMap.put("baseIRI", baseIriRML);
			rmlMap.put("basePath", basePath.toString() + "/");

			Path compiledTemplatePath = Paths.get(basePath + "template.rml.vm");
			TemplateExecutor templateExecutor = new TemplateExecutor(new RMLCompilerUtils(), failInvalidRef, trimTemplate, templateInResources, new TemplateMap(rmlMap), formatter);
			templateExecutor.executeMapping(compilerReader, rmlCompiler, compiledTemplatePath);
			templatePath = compiledTemplatePath;
		}

		try {
			TemplateExecutor templateExecutor = new TemplateExecutor(templateFunctions, failInvalidRef, trimTemplate, templateInResources, templateMap, formatter);
			if(timePath != null)
				try (FileWriter pw = new FileWriter(timePath.toFile(), true)) {
					long start = Instant.now().toEpochMilli();
					if(queryPath != null)
						templateExecutor.executeMappingParametric(reader, templatePath, queryPath, destinationPath);
					else
						templateExecutor.executeMapping(reader, templatePath, destinationPath);
					long duration = Instant.now().toEpochMilli() - start;
					pw.write(templatePath + "," + destinationPath + "," + duration + "\n");
				}
			else{
				if(queryPath != null)
					templateExecutor.executeMappingParametric(reader, templatePath, queryPath, destinationPath);
				else
					templateExecutor.executeMapping(reader, templatePath, destinationPath);
			}
		} catch (Exception e) {
			Files.deleteIfExists(destinationPath);
			throw e;
		}

		if(reader != null)
			reader.shutDown();
	}
}
