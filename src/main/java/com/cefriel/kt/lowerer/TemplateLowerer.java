package com.cefriel.kt.lowerer;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import com.cefriel.kt.utils.LoweringUtils;
import com.cefriel.kt.utils.rdf.RDFReader;
import com.cefriel.kt.utils.rdf.TripleStoreConfig;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Serializer;
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

public class TemplateLowerer {

	private org.slf4j.Logger log = LoggerFactory.getLogger(TemplateLowerer.class);

	// Constructor parameters
	private String templatePath;
	private String destinationPath;
	private LoweringUtils lu;

	// Configurable parameters
	private String keyValuePairsPath;
	private String keyValueCsvPath;
	private String format;

	private VelocityEngine velocityEngine;
	private RDFReader reader;
	private int count = 0;

    public TemplateLowerer(String templatePath, String destinationPath) {
    	this.templatePath = templatePath;
    	this.destinationPath = destinationPath;
    	this.lu = new LoweringUtils();
	}

	public TemplateLowerer(String templatePath, String destinationPath, LoweringUtils lu) {
		this.templatePath = templatePath;
		this.destinationPath = destinationPath;
		this.lu = lu;
	}

	public void lower(String triplesPath) throws Exception {
		lower(triplesPath, null);
	}

	public void lower(String triplesPath, String queryFile) throws Exception {
		Repository repo;
		repo = new SailRepository(new MemoryStore());
		repo.init();

		File file = new File(triplesPath);
		String baseURI = "";
		try (RepositoryConnection con = repo.getConnection()) {
			con.add(file, baseURI, RDFFormat.TURTLE);
		}

		lower(repo, queryFile);
	}

	public void lower(TripleStoreConfig tsc) throws Exception {
    	lower(tsc, null);
	}

	public void lower(TripleStoreConfig tsc, String queryFile) throws Exception {
		Repository repo;
		repo = new HTTPRepository(tsc.getAddress(), tsc.getRepositoryID());
		repo.init();

		lower(repo, queryFile);
	}

	private void lower(Repository repo, String queryFile) throws Exception {
		VelocityContext context = initEngine(repo);
		executeLowering(queryFile, context);
		repo.shutDown();
	}

	private VelocityContext initEngine(Repository repo) throws IOException {
		velocityEngine = new VelocityEngine();
		velocityEngine.init();

		reader = new RDFReader();
		reader.setRepository(repo);

		VelocityContext context = new VelocityContext();
		context.put("reader", reader);
		context.put("functions", lu);

		Map<String, String> map = new HashMap<>();
		if(keyValuePairsPath !=  null)
			map.putAll(parseMap(keyValuePairsPath));
		if(keyValueCsvPath !=  null)
			map.putAll(parseCsvMap(keyValueCsvPath));
		context.put("map", map);

		return context;
	}

	private void executeLowering(String queryFile, VelocityContext context) throws Exception {
		log.info("Template path: " + templatePath);
		templatePath = trimTemplate(templatePath);

		if(queryFile != null) {
			String query = new String(Files.readAllBytes(Paths.get(queryFile)), StandardCharsets.UTF_8);
			log.info("Parametric Template executed with query: " + templatePath);
			List<Map<String, String>> rows = reader.executeQueryStringValueXML(query);

			for (Map<String, String> row : rows)
				executeTemplate(context, row);
		} else {
			executeTemplate(context);
		}
	}

	private void executeTemplate(VelocityContext context) throws Exception {
		executeTemplate(context, null);
	}

	private void executeTemplate(VelocityContext context, Map<String, String> row) throws Exception {

    	String id = generateId(row);
		if (row != null)
			context.put("x", row);

		log.info("Executing Template" + id);
		String pathId = getPathId(destinationPath, id);

		Writer writer;
		writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(pathId)));
		Template t = velocityEngine.getTemplate(templatePath);
		t.merge(context, writer);
		writer.close();

		handleFormat(pathId);
	}

	private void handleFormat(String pathId) throws Exception {
    	if (format != null)
			switch (format) {
				case "xml":
					Builder builder = new Builder();
					InputStream ins = new BufferedInputStream(new FileInputStream(pathId));
					Document doc = builder.build(ins);
					writeToFileXml(doc, pathId);
					break;
				default:
					return;
			}
	}

	private String generateId(Map<String, String> row) {
		if (row != null)
			if (row.containsKey("id"))
				return "-" + row.get("id");
			else {
				String id = "-T-id-" + count;
				count += 1;
				return id;
			}
		else
			return "";
	}

	public void writeToFileXml(Document doc, String path) throws IOException {
		BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(new File(path)));
		Serializer serializer = new Serializer(bos, "ISO-8859-1");
		serializer.setIndent(4);
		serializer.setMaxLength(0);
		serializer.write(doc);
		bos.close();
	}

	public String trimTemplate(String templatePath) throws IOException {
		String newTemplatePath = templatePath + ".tmp.vm";
		List<String> newLines = new ArrayList<>();
		for (String line : Files.readAllLines(Paths.get(templatePath), StandardCharsets.UTF_8)) {
			newLines.add(line.trim().replace("\n", "").replace("\r",""));
		}
		String result = String.join(" ", newLines);
		try (PrintWriter out = new PrintWriter(newTemplatePath)) {
			out.println(result);
		}
		return newTemplatePath;
	}

	public Map<String,String> parseMap(String filePath) throws IOException {
		Path path = FileSystems.getDefault().getPath(filePath);
		Map<String, String> mapFromFile = Files.lines(path)
				.filter(s -> s.matches("^\\w+:.+"))
				.collect(Collectors.toMap(k -> k.split(":")[0], v -> v.substring(v.indexOf(":") + 1)));
		return mapFromFile;
	}

	public Map<String,String> parseCsvMap(String filePath) throws IOException {
		Path path = FileSystems.getDefault().getPath(filePath);
		List<String[]> fromCSV = Files.lines(path)
				.map(s -> s.split(","))
				.collect(Collectors.toList());
		Map<String,String> mapFromFile = new HashMap<>();
		String[] keys = fromCSV.get(0);
		String[] values = fromCSV.get(1);
		for(int i = 0; i < keys.length; i++)
			mapFromFile.put(keys[i], values[i]);
		return mapFromFile;
	}

	public String getPathId(String destinationPath, String id) {
		String pathId = destinationPath + id;
		if (destinationPath.contains(".")) {
			String extension = destinationPath.substring(destinationPath.lastIndexOf(".") + 1);
			pathId = destinationPath.substring(0, destinationPath.lastIndexOf(".")) + id + "." + extension;
		}
		return pathId;
	}

	public String getKeyValuePairsPath() {
		return keyValuePairsPath;
	}

	public void setKeyValuePairsPath(String keyValuePairsPath) {
		this.keyValuePairsPath = keyValuePairsPath;
	}

	public String getKeyValueCsvPath() {
		return keyValueCsvPath;
	}

	public void setKeyValueCsvPath(String keyValueCsvPath) {
		this.keyValueCsvPath = keyValueCsvPath;
	}

	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}

}