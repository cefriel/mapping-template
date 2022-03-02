package com.cefriel.io.rdf;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

import java.io.FileOutputStream;
import java.io.StringWriter;

public class RDFUtils {

    public static void serializeFile(String path, RDFFormat rdfFormat) throws Exception {
        Repository repo = new SailRepository(new MemoryStore());
        RDFWriter writer = new RDFWriter(repo);
        writer.addFile(path);
        Model model = writer.getDump();
        try (FileOutputStream out = new FileOutputStream(path)) {
            Rio.write(model, out, rdfFormat);
        }
    }

    public static String serialize(String triples, RDFFormat rdfFormatInput, RDFFormat rdfFormatOutput) throws Exception {
        Repository repo = new SailRepository(new MemoryStore());
        RDFWriter writer = new RDFWriter(repo);
        writer.addString(triples, rdfFormatInput);
        Model model = writer.getDump();
        StringWriter sw = new StringWriter();
        Rio.write(model, sw, rdfFormatOutput);
        return sw.toString();
    }
}
