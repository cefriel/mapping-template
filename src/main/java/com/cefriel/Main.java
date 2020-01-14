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

	@Parameter(names={"--basepath","-b"})
	private String basePath = "./";
	@Parameter(names={"--template","-t"})
	private String templatePath = "template.vm";
	@Parameter(names={"--input","-i"})
	private String triplesPath = "input.ttl";
	@Parameter(names={"--output","-o"})
	private String destinationPath = "output.xml";
	@Parameter(names={"--key-value","-kv"})
	private String keyValuePairsPath;
	@Parameter(names={"--key-value-csv","-kvc"})
	private String keyValueCsvPath;
	@Parameter(names={"--format","-f"})
	private String format;
	@Parameter(names={"--utils","-u"})
	private String utils;
	@Parameter(names={"--ts-address","-ts"})
	private String DB_ADDRESS;
	@Parameter(names={"--repository","-r"})
	private String REPOSITORY_ID;
	@Parameter(names={"--contextIRI","-c"})
	private String context;
	@Parameter(names={"--query","-q"})
	private String queryFile;

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
		templatePath = basePath + templatePath;
		triplesPath = basePath + triplesPath;
		destinationPath = basePath + destinationPath;
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

		if ((new File(triplesPath)).exists()) {
			RDFWriter.baseIRI = "http://www.cefriel.com/data/";
			RDFWriter writer = new RDFWriter(repo, context);
			writer.addFile(triplesPath);
		}

		RDFReader reader = new RDFReader(repo, context);
		TemplateLowerer tl = new TemplateLowerer(reader, lu);

		if (keyValueCsvPath != null)
			tl.setKeyValueCsvPath(keyValueCsvPath);
		if (keyValuePairsPath != null)
			tl.setKeyValuePairsPath(keyValuePairsPath);
		if (format != null)
			tl.setFormat(format);

		tl.lower(templatePath, destinationPath, queryFile);
		reader.shutDown();

	}

}
