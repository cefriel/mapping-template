/*
 * Copyright (c) 2019-2023 Cefriel.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cefriel.template.io.rdf;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.cefriel.template.io.Reader;
import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.resultio.text.tsv.SPARQLResultsTSVWriter;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.contextaware.ContextAwareRepository;
import org.eclipse.rdf4j.repository.http.HTTPRepository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.inferencer.fc.SchemaCachingRDFSInferencer;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RDFReader implements Reader {

    private final Logger log = LoggerFactory.getLogger(RDFReader.class);

    private Repository repository;
    private IRI context;

    private String baseIRI;

    private String queryHeader;

    private boolean verbose;

    public RDFReader() {
        this(false);
    }

    public RDFReader(boolean rdfsInference) {
       if (rdfsInference)
           repository = new SailRepository(
                new SchemaCachingRDFSInferencer(
                        new MemoryStore()));
       else
           repository = new SailRepository(new MemoryStore());
    }

    public RDFReader(String address, String repositoryId) {
        this.repository = new HTTPRepository(address, repositoryId);
    }

    public RDFReader(String address, String repositoryId, String context) {
        this.repository = new HTTPRepository(address, repositoryId);
        setContext(context);
    }

    public RDFReader(String address, String repositoryId, IRI contextIRI) {
        this.repository = new HTTPRepository(address, repositoryId);
        setContext(contextIRI);
    }

    public RDFReader(Repository repository) {
        this.repository = repository;
    }

    public RDFReader(Repository repository, String context) {
        this.repository = repository;
        setContext(context);
    }

    public RDFReader(Repository repository, IRI contextIRI) {
        this.repository = repository;
        setContext(contextIRI);
    }

    /**
     * Prepend the prefixes (if set as queryHeader) with the {@code query} parameter.
     * @param query SPARQL query string
     * @return SPARQL query with prefixes
     */
    private String addQueryHeader(String query) {
        if (queryHeader != null && !queryHeader.trim().isEmpty())
            return queryHeader + query;
        return query;
    }

    /**
     * Executes a SPARQL query returning a list of rows as {@code List<Map<String,Value>>}.
     * @param query SPARQL query to be executed
     * @return Result of the SPARQL query
     */
    public List<Map<String,Value>> executeQuery(String query) {
        query = addQueryHeader(query);

        try (RepositoryConnection con = this.repository.getConnection()) {
            TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, query);
            List<BindingSet> resultList;
            List<Map<String,Value>> results = new ArrayList<Map<String,Value>>();
            try (TupleQueryResult result = tupleQuery.evaluate()) {
                resultList = QueryResults.asList(result);
            }
            for (BindingSet bindingSet : resultList) {
                Map<String,Value> result = new HashMap<String,Value>();
                for (String bindingName : bindingSet.getBindingNames()) {
                    result.put(bindingName, bindingSet.getValue(bindingName));
                }
                results.add(result);
            }
            return results;
        }
    }

    /**
     * Executes a SPARQL query returning a list of rows as {@code List<Map<String,String>>}.
     * @param query SPARQL query to be executed
     * @return Result of the SPARQL query with {@code String} values
     */
    private List<Map<String,String>> getQueryResultsStringValue(String query) {
        List<Map<String,Value>> valueResults = executeQuery(query);
        List<Map<String,String>> results = new ArrayList<>();
        for(Map<String,Value> row : valueResults)
            results.add(row.entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> (e.getValue() != null) ? e.getValue().stringValue() : null )
                    ));
        return results;
    }

    /**
     * Executes a SPARQL query returning a list of rows as {@code List<Map<String,String>>}
     * and logging ({@code INFO} level) the query, the duration and the number of rows returned.
     * @param query SPARQL query to be executed
     * @return Result of the SPARQL query with {@code String} values
     */
    public List<Map<String, String>> executeQueryStringValueVerbose(String query) {
        log.info("Query: " + addQueryHeader(query) + "\n");
        Instant start = Instant.now();
        List<Map<String, String>> results = getQueryResultsStringValue(query);
        Instant end = Instant.now();
        log.info("Info query: [duration: " + Duration.between(start, end).toMillis() + ", num_rows: " + results.size() + "]");
        return results;
    }

    /**
     * Executes a SPARQL query returning a list of rows as {@code List<Map<String,String>>}
     * and logging ({@code INFO} level) the query, the duration and the number of rows returned.
     * if the {@code verbose} option is enabled.
     * @param query SPARQL query to be executed
     * @return Result of the SPARQL query with {@code String} values
     */
    public List<Map<String, String>> getDataframe(String query) {
        return verbose ? executeQueryStringValueVerbose(query) : getQueryResultsStringValue(query);
    }

    @Override
    public List<Map<String, String>> getDataframe() throws Exception {
        return null;
    }

    /**
     * Executes a SPARQL query returning a list of rows as {@code List<Map<String,String>>}
     * escaping XML special chars. It logs ({@code INFO} level) the query, the duration and
     * the number of rows returned. if the {@code verbose} option is enabled.
     * @param query SPARQL query to be executed
     * @return Result of the SPARQL query with {@code String} values
     */
    public List<Map<String, String>> executeQueryStringValueXML(String query) {
        List<Map<String, String>> results = getDataframe(query);
        for (Map<String, String> result : results)
            result.replaceAll((k, v) -> StringEscapeUtils.escapeXml11(v));
        return results;
    }

    /**
     * Executes the SPARQL query in the {@code query} file writing the results in the TSV
     * format in {@code destinationPath}.
     * @param query SPARQL query to be executed
     * @param destinationPath File to save the results of the SPARQL query
     * @throws IOException If an error occurs in handling the files
     */
    public void debugQuery(String query, String destinationPath) throws IOException {
        SPARQLResultsTSVWriter writer = new SPARQLResultsTSVWriter(new FileOutputStream(destinationPath));
        try (RepositoryConnection con = this.repository.getConnection()) {
            con.prepareTupleQuery(query).evaluate(writer);
        }
    }

    /**
     * Add triples from a file in the repository.
     * @param triplesPath Path of the file to be added.
     * @throws Exception
     */
    public void addFile(String triplesPath, RDFFormat rdfFormat) throws Exception {
        File file = new File(triplesPath);
        try (RepositoryConnection con = repository.getConnection()) {
            con.add(file, baseIRI, rdfFormat);
        }
    }

    /**
     * Add triples from a file in the repository. RDF format is inferred using the file extension.
     * @param triplesPath Path of the file to be added.
     * @throws Exception
     */
    public void addFile(String triplesPath) throws Exception {
        File file = new File(triplesPath);
        RDFFormat rdfFormat = Rio.getParserFormatForFileName(triplesPath).orElse(RDFFormat.TURTLE);
        try (RepositoryConnection con = repository.getConnection()) {
            con.add(file, baseIRI, rdfFormat);
        }
    }

    /**
     * Add triples from a String in the repository.
     * @param triples String containing triples to be added.
     * @param rdfFormat RDFFormat to parse the String.
     * @throws Exception
     */
    public void addString(String triples, RDFFormat rdfFormat) throws Exception {
        try (RepositoryConnection con = repository.getConnection()) {
            con.add(new StringReader(triples), baseIRI, rdfFormat);
        }
    }

    /**
     * Get a {@link org.eclipse.rdf4j.model.Model} dump from the Repository targeted by the RDFReader.
     * @return {@link org.eclipse.rdf4j.model.Model} dump
     */
    public Model getDump() {
        try (RepositoryConnection con = repository.getConnection()) {
            RepositoryResult<Statement> dump;
            dump = con.getStatements(null, null, null);
            Model dumpModel = QueryResults.asModel(dump);

            RepositoryResult<Namespace> namespaces = con.getNamespaces();
            for (Namespace n : Iterations.asList(namespaces))
                dumpModel.setNamespace(n);
            return dumpModel;
        }
    }

    /**
     * Merge the content of a RDFReader into the reader.
     * @param sourceReader RDFReader to be merged.
     */
    public void mergeRdfReader(RDFReader sourceReader) {
        Repository sourceRepo = sourceReader.getRepository();
        try (RepositoryConnection conn = repository.getConnection()) {
            try (RepositoryConnection source = sourceRepo.getConnection()) {
                conn.add(source.getStatements(null, null, null, true));
            }
        }
    }

    public void shutDown() {
        repository.shutDown();
    }

    /**
     * Get the IRI of the context (named graph) used for read/write operations on the repository.
     * @return IRI of the context (named graph)
     */
    public IRI getContext() {
        return context;
    }

    /**
     * Set the IRI of the context (named graph) for read/write operations on the repository.
     * @param context IRI of the context (named graph)
     */
    public void setContext(IRI context) {
        this.context = context;
        if (repository != null) {
            ContextAwareRepository cRep = new ContextAwareRepository(repository);
            cRep.setReadContexts(context);
            repository = cRep;
        }
    }

    /**
     * Set the IRI of the context (named graph) for read/write operations on the repository.
     * @param context String representing the IRI of the context (named graph)
     */
    public void setContext(String context) {
        if (context != null && !context.equals("")) {
            ValueFactory vf = SimpleValueFactory.getInstance();
            setContext(vf.createIRI(context));
        }
    }

    /**
     * Get the base IRI set for the RDFReader.
     * @return base IRI set for the RDFReader
     */
    public String getBaseIRI() {
        return baseIRI;
    }

    /**
     * Set a base IRI for the RDFReader.
     * @param baseIRI String to be set as base IRI for the RDFReader
     */
    public void setBaseIRI(String baseIRI) {
        this.baseIRI = baseIRI;
    }

    public Repository getRepository() {
        return repository;
    }

    public boolean isVerbose() {
        return verbose;
    }

    /**
     * Set verbose option to enable logging on query executions.
     * @param verbose boolean option
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    /**
     * Get string containing header section (i.e., prefixes) for SPARQL queries.
     * @return String containing prefixes for SPARQL queries.
     */
    public String getQueryHeader() {
        return queryHeader;
    }

    @Override
    public void appendQueryHeader(String s) {
        this.queryHeader += s;
    }

    @Override
    public void setQueryHeader(String queryHeader) {
        this.queryHeader = queryHeader;
    }

    /**
     * Set string containing header section (i.e., prefixes) for SPARQL queries.
     * Legacy function signature (Deprecated, use {@link #setQueryHeader(String)}).
     * @param queryHeader String containing prefixes for SPARQL queries.
     */
    @Deprecated
    public void setPrefixes(String queryHeader) {
        this.queryHeader = queryHeader;
    }

}
