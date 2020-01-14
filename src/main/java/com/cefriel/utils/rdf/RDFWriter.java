package com.cefriel.utils.rdf;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.contextaware.ContextAwareRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;

import java.io.File;

public class RDFWriter {

    public static String baseIRI;

    private Repository repository;
    private IRI context;

    public RDFWriter(Repository repository) {
        this.repository = repository;
    }

    public RDFWriter(Repository repository, String context) {
        this.repository = repository;
        setContext(context);
    }

    public void addFile(String triplesPath) throws Exception {
        File file = new File(triplesPath);
        RDFFormat rdfFormat = Rio.getParserFormatForFileName(triplesPath).orElse(RDFFormat.TURTLE);
        try (RepositoryConnection con = repository.getConnection()) {
            con.add(file, baseIRI, rdfFormat);
        }
    }

    public IRI getContext() {
        return context;
    }

    public void setContext(IRI context) {
        this.context = context;
        if (repository != null) {
            ContextAwareRepository cRep = new ContextAwareRepository(repository);
            cRep.setInsertContext(context);
            repository = cRep;
        }
    }

    public void setContext(String context) {
        if (context != null && !context.equals("")) {
            ValueFactory vf = SimpleValueFactory.getInstance();
            setContext(vf.createIRI(context));
        }
    }

    public Repository getRepository() {
        return repository;
    }
}
