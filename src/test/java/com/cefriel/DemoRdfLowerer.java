package com.cefriel;

import com.cefriel.utils.LoweringUtils;
import com.cefriel.utils.rdf.RDFReader;
import com.cefriel.utils.rdf.RDFWriter;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class DemoRdfLowerer {

    public static void main(String ... argv) throws Exception {

        Repository repo;
        repo = new SailRepository(new MemoryStore());

        RDFWriter.baseIRI = "http://www.cefriel.com/data/";
        RDFWriter writer = new RDFWriter(repo, null);
        writer.addFile("demo/input.ttl");


        RDFReader reader = new RDFReader(repo);
        VelocityEngine velocityEngine = new VelocityEngine();
        velocityEngine.init();

        File template = new File("demo/template.vm");
        InputStream templateStream = new FileInputStream(template);
        Reader templateReader = new InputStreamReader(templateStream);

        VelocityContext context = new VelocityContext();
        context.put("reader", reader);
        context.put("functions", new LoweringUtils());

        Writer w;
        w = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream("demo/output.txt"), StandardCharsets.UTF_8));

        velocityEngine.evaluate(context, w, "test", templateReader);

        w.close();
        templateReader.close();
    }
}
