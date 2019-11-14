package it.cefriel.template;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
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
	@Parameter(names={"--ts-address","-ts"})
	private String DB_ADDRESS;
	@Parameter(names={"--repository","-r"})
	private String REPOSITORY_ID;
	@Parameter(names={"--in-memory","-m"})
	private boolean memory;
	@Parameter(names={"--query","-q"})
	private String queryFile;

	private VelocityEngine velocityEngine;
	private int count = 0;

    private org.slf4j.Logger log = LoggerFactory.getLogger(TemplateLowerer.class);

	public static void main(String ... argv) throws Exception {

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


	public void lower() throws Exception {
		Repository repo;
		boolean triplesStore = (DB_ADDRESS != null) && (REPOSITORY_ID != null);
		if (triplesStore)
			repo = new HTTPRepository(DB_ADDRESS, REPOSITORY_ID);
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

		velocityEngine = new VelocityEngine();
		velocityEngine.init();

		RDFReader reader = new RDFReader();
		reader.setRepository(repo);

		log.info("Template path: " + templatePath);
		templatePath = utils.trimTemplate(templatePath);

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

		if(queryFile != null) {
			String query = new String(Files.readAllBytes(Paths.get(queryFile)), StandardCharsets.UTF_8);
			log.info("Parametric Template executed with query: " + templatePath);
			List<Map<String, String>> rows = reader.executeQueryStringValueXML(query);

			for (Map<String, String> row : rows)
				executeTemplate(context, row);
		} else {
			executeTemplate(context);
		}

		repo.shutDown();

	}

	private void executeTemplate(VelocityContext context) throws Exception {
		executeTemplate(context, null);
	}

	private void executeTemplate(VelocityContext context, Map<String, String> row) throws Exception {
		String id;
		if (row != null) {
			context.put("x", row);
			if (row.containsKey("id"))
				id = "-" + row.get("id");
			else {
				id = "-" + count;
				count += 1;
			}
		} else
			id = "";

		log.info("Executing Template" + id);

		Writer writer;
		if(memory)
			writer = new StringWriter();
		else
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(destinationPath + id)));

		Template t = velocityEngine.getTemplate(templatePath);
		t.merge(context, writer);

		if(memory)
			utils.writeToFile(writer.toString(), destinationPath + id, formatXml);

		writer.close();

		if(!memory && formatXml) {
			Builder builder = new Builder();
			InputStream ins = new BufferedInputStream(new FileInputStream(destinationPath + id));
			Document doc = builder.build(ins);
			utils.writeToFileXml(doc, destinationPath + id);
		}
	}

}
