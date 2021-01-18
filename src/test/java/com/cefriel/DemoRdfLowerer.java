package com.cefriel;

import com.cefriel.lowerer.TemplateLowerer;
import com.cefriel.utils.LoweringUtils;
import com.cefriel.utils.rdf.RDFReader;
import com.cefriel.utils.rdf.RDFWriter;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

import java.io.*;

public class DemoRdfLowerer {

    public static void main(String ... argv) throws Exception {
        Repository repo;
        repo = new SailRepository(new MemoryStore());

        RDFWriter.baseIRI = "http://www.cefriel.com/data/";
        RDFWriter writer = new RDFWriter(repo, null);
        writer.addFile("demo/input.ttl");


        RDFReader reader = new RDFReader(repo);

        TemplateLowerer lowerer = new TemplateLowerer(reader, new LoweringUtils());

        File template = new File("demo/template.vm");
        InputStream templateStream = new FileInputStream(template);
        System.out.println(lowerer.lower(templateStream));
    }
}
