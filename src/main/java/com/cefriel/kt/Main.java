package com.cefriel.kt;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.cefriel.kt.utils.rdf.TripleStoreConfig;
import com.cefriel.kt.lowerer.TemplateLowerer;
import com.cefriel.kt.utils.TransmodelLoweringUtils;
import org.slf4j.LoggerFactory;

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

		TemplateLowerer tl;
		if (utils != null)
		switch (utils) {
			case "transmodel":
				tl = new TemplateLowerer(templatePath, destinationPath, new TransmodelLoweringUtils());
				break;
			default:
				tl = new TemplateLowerer(templatePath, destinationPath);
		} else {
			tl = new TemplateLowerer(templatePath, destinationPath);
		}

		if (keyValueCsvPath != null)
			tl.setKeyValueCsvPath(keyValueCsvPath);
		if (keyValuePairsPath != null)
			tl.setKeyValuePairsPath(keyValuePairsPath);
		if (format != null)
			tl.setFormat(format);

		boolean triplesStore = (DB_ADDRESS != null) && (REPOSITORY_ID != null);
		if (triplesStore)
			tl.lower(new TripleStoreConfig(DB_ADDRESS, REPOSITORY_ID), queryFile);
		else
			tl.lower(triplesPath, queryFile);

	}

}
