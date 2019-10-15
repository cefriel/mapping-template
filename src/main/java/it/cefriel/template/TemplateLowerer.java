package it.cefriel.template;

import java.io.*;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TemplateLowerer {

	@Parameter(names={"--basepath","-b"})
	private String basePath = "./";
	@Parameter(names={"--template","-t"})
	private String templatePath = "template.vm";
	@Parameter(names={"--input","-i"})
	private String triplesPath = "input.ttl";
	@Parameter(names={"--output","-o"})
	private String destinationPath = "output.xml";
	@Parameter(names={"--version-output","-v"})
	private String versionOutput = "any";
	@Parameter(names={"--format-xml","-f"})
	private boolean formatXml;
	@Parameter(names={"--graphdb-address","-g"})
	private static String GRAPHDB_SERVER = "http://localhost:7200/";
	@Parameter(names={"--repository","-r"})
	private static String REPOSITORY_ID = "SNAP";
	@Parameter(names={"--skip-init","-s"})
	private boolean skip;

    private Logger log = LoggerFactory.getLogger(TemplateLowerer.class);

	public static void main(String ... argv) throws IOException, ParsingException {
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
		Repository repo = new HTTPRepository(GRAPHDB_SERVER, REPOSITORY_ID);
		repo.initialize();

		if(!skip) {
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
		Utils utils = new Utils();

		log.info("template path: " + templatePath);
		templatePath = utils.trimTemplate(templatePath);

		Template t = velocityEngine.getTemplate(templatePath);
		VelocityContext context = new VelocityContext();
		context.put("reader", reader);
		context.put("functions", utils);
		context.put("version", versionOutput);
		OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(destinationPath));
		t.merge(context, writer);

	}

}
