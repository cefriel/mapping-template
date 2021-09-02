/*
 * Copyright 2020 Cefriel.
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
package com.cefriel;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.cefriel.utils.LoweringUtils;
import com.cefriel.lowerer.TemplateLowerer;
import com.cefriel.utils.TransmodelLoweringUtils;
import com.cefriel.utils.rdf.RDFReader;
import com.cefriel.utils.rdf.RDFWriter;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.http.HTTPRepository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

public class Main {

	@Parameter(names={"--template","-t"})
	private String templatePath = "template.vm";
	@Parameter(names={"--input","-i"},
			variableArity = true)
	private List<String> triplesPaths;

	@Parameter(names={"--baseiri","-iri"})
	private String baseIri = "http://www.cefriel.com/data/";
	@Parameter(names={"--basepath","-b"})
	private String basePath = "./";
	@Parameter(names={"--output","-o"})
	private String destinationPath = "output.txt";

	@Parameter(names={"--utils","-u"})
	private String utils;

	@Parameter(names={"--key-value","-kv"})
	private String keyValuePairsPath;
	@Parameter(names={"--key-value-csv","-kvc"})
	private String keyValueCsvPath;

	@Parameter(names={"--format","-f"})
	private String format;
	@Parameter(names={"--trim","-tr"})
	private boolean trimTemplate;

	@Parameter(names={"--ts-address","-ts"})
	private String DB_ADDRESS;
	@Parameter(names={"--repository","-r"})
	private String REPOSITORY_ID;
	@Parameter(names={"--contextIRI","-c"})
	private String context;

	@Parameter(names={"--query","-q"})
	private String queryPath;
	@Parameter(names={"--debug-query","-dq"})
	private boolean debugQuery;

	@Parameter(names={"--verbose","-v"})
	private boolean verbose;

    private org.slf4j.Logger log = LoggerFactory.getLogger(Main.class);

	public static void main(String ... argv) throws Exception {

		Set<String> loggers = new HashSet<>(Arrays.asList("org.apache.http"));

		for(String log:loggers) {
			Logger logger = (Logger)LoggerFactory.getLogger(log);
			logger.setLevel(Level.INFO);
			logger.setAdditive(false);
		}

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
		for (int i = 0; i < triplesPaths.size(); i++)
			triplesPaths.set(i, basePath + triplesPaths.get(i));
		destinationPath = basePath + destinationPath;
		if (queryPath != null)
			queryPath = basePath + queryPath;
	}


	public void exec() throws Exception {

		LoweringUtils lu = new LoweringUtils();
		if (utils != null)
			switch (utils) {
				case "transmodel":
					lu = new TransmodelLoweringUtils();
					break;
			}

		Repository repo;
		boolean triplesStore = (DB_ADDRESS != null) && (REPOSITORY_ID != null);
		if (triplesStore)
			repo = new HTTPRepository(DB_ADDRESS, REPOSITORY_ID);
		else
			repo = new SailRepository(new MemoryStore());

		for (String triplesPath : triplesPaths)
			if ((new File(triplesPath)).exists()) {
				RDFWriter.baseIRI = baseIri;
				RDFWriter writer = new RDFWriter(repo, context);
				writer.addFile(triplesPath);
			}

		RDFReader reader = new RDFReader(repo, context);
		if (verbose)
			reader.setVerbose(true);

		if(debugQuery) {
			if (queryPath == null)
				log.error("Provide a query using the --query option");
			else
				reader.debugQuery(queryPath, destinationPath);
		}
		else {
			TemplateLowerer tl = new TemplateLowerer(reader, lu);

			if (keyValueCsvPath != null)
				tl.setKeyValueCsvPath(keyValueCsvPath);
			if (keyValuePairsPath != null)
				tl.setKeyValuePairsPath(keyValuePairsPath);
			if (format != null)
				tl.setFormat(format);
			if (trimTemplate)
				tl.setTrimTemplate(true);

			tl.lower(templatePath, destinationPath, queryPath);
		}

		reader.shutDown();

	}

}
