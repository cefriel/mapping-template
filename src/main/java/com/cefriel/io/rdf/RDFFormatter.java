package com.cefriel.io.rdf;

import com.cefriel.io.Formatter;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

import java.io.FileOutputStream;
import java.io.StringWriter;

public class RDFFormatter implements Formatter {

    private RDFFormat rdfFormatInput;
    private RDFFormat rdfFormatOutput;

    public RDFFormatter() {
        this.rdfFormatInput = null;
        this.rdfFormatOutput = null;
    }

    public RDFFormatter(RDFFormat rdfFormat) {
        this.rdfFormatInput = rdfFormat;
        this.rdfFormatOutput = rdfFormat;
    }

    @Override
    public void formatFile(String filepath) throws Exception {
        Repository repo = new SailRepository(new MemoryStore());
        RDFReader reader = new RDFReader(repo);

        RDFFormat format = rdfFormatInput;
        if (format == null)
            format = Rio.getParserFormatForFileName(filepath).orElse(RDFFormat.TURTLE);

        reader.addFile(filepath, format);
        Model model = reader.getDump();

        if (rdfFormatOutput != null)
            format = rdfFormatOutput;

        try (FileOutputStream out = new FileOutputStream(filepath)) {
            Rio.write(model, out, format);
        }
    }

    @Override
    public String formatString(String s) throws Exception {
        Repository repo = new SailRepository(new MemoryStore());
        RDFReader reader = new RDFReader(repo);

        RDFFormat format = RDFFormat.TURTLE;
        if (rdfFormatInput != null)
            format = rdfFormatInput;
        reader.addString(s, rdfFormatInput);

        Model model = reader.getDump();
        StringWriter sw = new StringWriter();

        if (rdfFormatOutput != null)
            format = rdfFormatOutput;
        Rio.write(model, sw, format);
        return sw.toString();
    }

    public RDFFormat getRdfFormatInput() {
        return rdfFormatInput;
    }

    public void setRdfFormatInput(RDFFormat rdfFormatInput) {
        this.rdfFormatInput = rdfFormatInput;
    }

    public RDFFormat getRdfFormatOutput() {
        return rdfFormatOutput;
    }

    public void setRdfFormatOutput(RDFFormat rdfFormatOutput) {
        this.rdfFormatOutput = rdfFormatOutput;
    }

}
