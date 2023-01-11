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

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.cefriel.template.io.Formatter;
import com.cefriel.template.io.Reader;
import com.cefriel.template.utils.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;

import static com.cefriel.template.utils.Util.validInputFormat;

public class Main {

	@Parameter(names={"--template","-t"})
	private String templatePath = "template.vm";
	// todo allow multiple input files but only support rdf, warn not supported for other formats
	@Parameter(names={"--input","-i"},
			variableArity = true)
	private List<String> inputFilesPaths;

	@Parameter(names={"--input-format","-if"})
	private String inputFormat;
	@Parameter(names={"--baseiri","-iri"})
	private String baseIri = "http://www.cefriel.com/data/";
	@Parameter(names={"--basepath","-b"})
	private String basePath = "./";
	@Parameter(names={"--output","-o"})
	private String destinationPath = "output.txt";

	@Parameter(names={"--key-value","-kv"})
	private String keyValuePairsPath;
	@Parameter(names={"--key-value-csv","-kvc"})
	private String keyValueCsvPath;

	@Parameter(names={"--format","-f"})
	private String format;
	@Parameter(names={"--trim","-tr"})
	private boolean trimTemplate;

	@Parameter(names={"--template-resource","-trs"})
	private boolean templateInResources;

	@Parameter(names={"--ts-address","-ts"})
	private String dbAddress;
	@Parameter(names={"--repository","-r"})
	private String repositoryId;
	@Parameter(names={"--contextIRI","-c"})
	private String context;

	@Parameter(names={"--query","-q"})
	private String queryPath;
	@Parameter(names={"--debug-query","-dq"})
	private boolean debugQuery;

	@Parameter(names={"--verbose","-v"})
	private boolean verbose;
	@Parameter(names={"--time","-tm"})
	private String timePath;

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
		basePath = basePath.endsWith("/") ? basePath : basePath + "/";
		templatePath = basePath + templatePath;
		if (inputFilesPaths != null)
			for (int i = 0; i < inputFilesPaths.size(); i++)
				if(inputFilesPaths.get(i) != null)
					inputFilesPaths.set(i, basePath + inputFilesPaths.get(i));


		destinationPath = basePath + destinationPath;
		if (queryPath != null)
			queryPath = basePath + queryPath;
		if (keyValueCsvPath != null)
			keyValueCsvPath = basePath + keyValueCsvPath;
		if (keyValuePairsPath != null)
			keyValuePairsPath = basePath + keyValuePairsPath;
		if (timePath != null)
			timePath = basePath + timePath;
	}


	public boolean validateInputFiles(List<String> inputFilesPaths, String format) {
		if (validInputFormat(format)){
			log.warn("FORMAT: " + format + " is not a supported input");
			return false;
		}

		if(inputFilesPaths.size() == 0) {
			log.warn("No input file is provided");
			return false;
		}

		if(inputFilesPaths.size() > 1 && !format.equals("rdf")) {
			log.warn("Multiple input files are supported only for rdf files");
			return false;
		}

		return true;

	}

	public void exec() throws Exception {

		Reader reader = null;
		if (validateInputFiles(inputFilesPaths, inputFormat)) {
			if (inputFormat.equals("rdf")) {
				reader = Util.createRDFReader(inputFilesPaths, inputFormat, repositoryId, context, baseIri, verbose);
			}
			else {
				String inputFilePath = inputFilesPaths.get(0);
				reader = Util.createNonRdfReader(inputFilePath, inputFormat, verbose);
			}
		}

		if (reader != null && debugQuery) {
			if (queryPath == null)
				log.error("Provide a query using the --query option");
			else {
				String debugQueryFromFile = Files.readString(Paths.get(queryPath));
				reader.debugQuery(debugQueryFromFile, destinationPath);
			}
		}

		TemplateExecutor tl = new TemplateExecutor();

		TemplateMap templateMap = new TemplateMap();
		if (keyValueCsvPath != null) {
			templateMap = Util.createTemplateMap(keyValueCsvPath, true);
			log.info(keyValueCsvPath + " parsed");
		}
		if (keyValuePairsPath != null) {
			templateMap = Util.createTemplateMap(keyValuePairsPath, false);
			log.info(keyValuePairsPath + " parsed");
		}
		if(templateMap.size() > 0)
			log.info("Parsed " + templateMap.size() + " key-value pairs");

		Formatter formatter = Util.createFormatter(format);

		if(timePath != null)
			try (FileWriter pw = new FileWriter(timePath, true)) {
				long start = Instant.now().toEpochMilli();
				if(queryPath != null)
					tl.executeMappingParametric(reader, templatePath, templateInResources, trimTemplate, queryPath, destinationPath, templateMap, formatter);
				else
					tl.executeMapping(reader, templatePath, templateInResources, trimTemplate, destinationPath, templateMap, formatter);
				long duration = Instant.now().toEpochMilli() - start;
				pw.write(templatePath + "," + destinationPath + "," + duration + "\n");
			}
		else{
			if(queryPath != null)
				tl.executeMappingParametric(reader, templatePath, templateInResources, trimTemplate, queryPath, destinationPath, templateMap, formatter);
			else
				tl.executeMapping(reader, templatePath, templateInResources, trimTemplate, destinationPath, templateMap, formatter);
		}

		if(reader != null)
			reader.shutDown();

	}

}
