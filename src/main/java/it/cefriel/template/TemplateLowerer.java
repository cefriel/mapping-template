package it.cefriel.template;

import java.io.*;
import java.util.*;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.ParsingException;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.http.HTTPRepository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;

import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

public class TemplateLowerer {

	private Utils utils = new Utils();

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
	@Parameter(names={"--xml","-x"})
	private boolean formatXml;
	@Parameter(names={"--graphdb-address","-g"})
	private static String GRAPHDB_SERVER = "http://localhost:7200/";
	@Parameter(names={"--repository","-r"})
	private static String REPOSITORY_ID = "SNAP";
	//If --triples-store is set the triplesPath is ignored. Triples should be already in the specified remote repository.
	@Parameter(names={"--triples-store","-ts"})
	private boolean triplesStore;
	@Parameter(names={"--in-memory","-m"})
	private boolean memory;

    private org.slf4j.Logger log = LoggerFactory.getLogger(TemplateLowerer.class);

	public static void main(String ... argv) throws IOException, ParsingException {

		Set<String> loggers = new HashSet<>(Arrays.asList("org.apache.http"));

		for(String log:loggers) {
			Logger logger = (Logger)LoggerFactory.getLogger(log);
			logger.setLevel(Level.INFO);
			logger.setAdditive(false);
		}

		TemplateLowerer lowerer = new TemplateLowerer();

		JCommander.newBuilder()
				.addObject(lowerer)
				.build()
				.parse(argv);

		lowerer.updateBasePath();
		lowerer.lower();
	}

	public void updateBasePath(){
		templatePath = basePath + templatePath;
		triplesPath = basePath + triplesPath;
		destinationPath = basePath + destinationPath;
	}


	public void lower() throws IOException, ParsingException {
		Repository repo;
		if (triplesStore)
			repo = new HTTPRepository(GRAPHDB_SERVER, REPOSITORY_ID);
		else
			repo = new SailRepository(new MemoryStore());
		repo.init();

		if(!triplesStore) {
			File file = new File(triplesPath);
			String baseURI = "";
			try (RepositoryConnection con = repo.getConnection()) {
				con.add(file, baseURI, RDFFormat.TURTLE);
			}
		}

		VelocityEngine velocityEngine = new VelocityEngine();
		velocityEngine.init();

		RDFReader reader = new RDFReader();
		reader.setRepository(repo);

		log.info("Template path: " + templatePath);
		templatePath = utils.trimTemplate(templatePath);

		Template t = velocityEngine.getTemplate(templatePath);
		VelocityContext context = new VelocityContext();
		context.put("reader", reader);
		context.put("functions", utils);

		Map<String, String> map = new HashMap<>();
		if(keyValuePairsPath !=  null)
			map.putAll(utils.parseMap(keyValuePairsPath));
		if(keyValueCsvPath !=  null)
			map.putAll(utils.parseCsvMap(keyValueCsvPath));
		if(!map.containsKey("version"))
			map.put("version", "any");
		context.put("map", map);

		Writer writer;
		if(memory)
			writer = new StringWriter();
		else
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(destinationPath)));

		t.merge(context, writer);
		repo.shutDown();

		if(memory)
			utils.writeToFile(writer.toString(), destinationPath, formatXml);

		writer.close();

		if(!memory && formatXml) {
			Builder builder = new Builder();
			InputStream ins = new BufferedInputStream(new FileInputStream(destinationPath));
			Document doc = builder.build(ins);
			utils.writeToFileXml(doc, destinationPath);
		}

	}
}
