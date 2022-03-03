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
package com.cefriel;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.cefriel.io.Formatter;
import com.cefriel.io.Reader;
import com.cefriel.io.rdf.RDFFormatter;
import com.cefriel.io.xml.XMLFormatter;
import com.cefriel.io.xml.XMLReader;
import com.cefriel.lowerer.MapConfigurator;
import com.cefriel.utils.LoweringUtils;
import com.cefriel.lowerer.TemplateLowerer;
import com.cefriel.utils.TransmodelLoweringUtils;
import com.cefriel.io.rdf.RDFReader;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.http.HTTPRepository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

public class Main {

	@Parameter(names={"--template","-t"})
	private String templatePath = "template.vm";
	@Parameter(names={"--rdf","-rdf"},
			variableArity = true)
	private List<String> triplesPaths;

	@Parameter(names={"--xml","-xml"})
	private String xmlPath;

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
		if (triplesPaths != null)
			for (int i = 0; i < triplesPaths.size(); i++)
				if(triplesPaths.get(i) != null)
					triplesPaths.set(i, basePath + triplesPaths.get(i));
		if (xmlPath != null)
			xmlPath = basePath + xmlPath;
		destinationPath = basePath + destinationPath;
		if (queryPath != null)
			queryPath = basePath + queryPath;
		if (keyValueCsvPath != null)
			keyValueCsvPath = basePath + keyValueCsvPath;
		if (keyValuePairsPath != null)
			keyValuePairsPath = basePath + keyValuePairsPath;
	}

	public void exec() throws Exception {

		LoweringUtils lu = new LoweringUtils();
		if (utils != null)
			switch (utils) {
				case "transmodel":
					lu = new TransmodelLoweringUtils();
					break;
			}

		Reader reader = null;
		if (triplesPaths != null) {
			Repository repo;
			boolean triplesStore = (DB_ADDRESS != null) && (REPOSITORY_ID != null);
			if (triplesStore)
				repo = new HTTPRepository(DB_ADDRESS, REPOSITORY_ID);
			else
				repo = new SailRepository(new MemoryStore());
			RDFReader rdfReader = new RDFReader(repo, context);
			rdfReader.setBaseIRI(baseIri);
			RDFFormat format;
			for (String triplesPath : triplesPaths)
				if ((new File(triplesPath)).exists()) {
					format = Rio.getParserFormatForFileName(triplesPath).orElse(RDFFormat.TURTLE);
					rdfReader.addFile(triplesPath, format);
				}
			reader = rdfReader;
		} else if (xmlPath != null) {
			reader = new XMLReader(new File(xmlPath));
		}

		if (reader != null) {
			if (verbose)
				reader.setVerbose(true);

			if (debugQuery) {
				if (queryPath == null)
					log.error("Provide a query using the --query option");
				else
					reader.debugQuery(queryPath, destinationPath);
			} else {
				TemplateLowerer tl = new TemplateLowerer(reader, lu);

				MapConfigurator mc = new MapConfigurator();
				if (keyValueCsvPath != null)
					mc.setKeyValueCsvPath(keyValueCsvPath);
				if (keyValuePairsPath != null)
					mc.setKeyValuePairsPath(keyValuePairsPath);
				tl.setMap(mc.getMap());

				Formatter f = null;
				if (format != null) {
					switch (format) {
						case "xml":
							f = new XMLFormatter();
							break;
						case "turtle":
							f = new RDFFormatter(RDFFormat.TURTLE);
							break;
						case "rdfxml":
							f = new RDFFormatter(RDFFormat.RDFXML);
							break;
						case "nt":
							f = new RDFFormatter(RDFFormat.NTRIPLES);
							break;
					}
					tl.setFormatter(f);
				}
				if (trimTemplate)
					tl.setTrimTemplate(true);

				tl.lower(templatePath, destinationPath, queryPath);
			}

			reader.shutDown();
		}

	}

}
