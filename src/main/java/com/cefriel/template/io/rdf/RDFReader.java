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

import com.cefriel.template.io.Reader;
import com.cefriel.template.utils.TemplateFunctions;
import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.*;
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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class RDFReader implements Reader {

    private final Logger log = LoggerFactory.getLogger(RDFReader.class);
    private Repository repository;
    private IRI context;
    private String baseIRI;
    private String queryHeader;
    private String outputFormat;
    private boolean verbose;
    private boolean hashVariable;
    private boolean onlyDistinct;

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

    public RDFReader(String repositoryUrl, String repositoryId) {
        this.repository = new HTTPRepository(repositoryUrl, repositoryId);
    }

    public RDFReader(String repositoryUrl, String repositoryId, String context) {
        this.repository = new HTTPRepository(repositoryUrl, repositoryId);
        setContext(context);
    }

    public RDFReader(String repositoryUrl, String repositoryId, IRI contextIRI) {
        this.repository = new HTTPRepository(repositoryUrl, repositoryId);
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

    public RDFReader(Repository repository, String graphName, String baseIri) {
        this.repository = repository;
        this.setContext(graphName);
        this.setBaseIRI(baseIri);
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
            try (TupleQueryResult result = tupleQuery.evaluate()) {
                resultList = QueryResults.asList(result);
            }
            List<Map<String,Value>> results = new ArrayList<>(resultList.size());
            int columnCount = !resultList.isEmpty() ? resultList.get(0).size() : 0;
            for (BindingSet bindingSet : resultList) {
                Map<String,Value> result = new HashMap<>(columnCount);
                for (String bindingName : bindingSet.getBindingNames()) {
                    if (bindingSet.getValue(bindingName) != null)
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
        Collection<Map<String, String>> dataframe = onlyDistinct ? new HashSet<>() : new ArrayList<>();
        for(Map<String,Value> row : valueResults) {
            if (hashVariable)
                dataframe.add(row.entrySet().stream()
                    .collect(Collectors.toMap(
                            e -> TemplateFunctions.literalHash(e.getKey()),
                            e -> (e.getValue() != null) ? e.getValue().stringValue() : null)
                    ));
            else
                dataframe.add(row.entrySet().stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                e -> (e.getValue() != null) ? e.getValue().stringValue() : null)
                        ));
        }
        return new ArrayList<>(dataframe);
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

    private List<Map<String, String>> extractDataframe(String query) {
        return verbose ? executeQueryStringValueVerbose(query) : getQueryResultsStringValue(query);
    }

    /**
     * Executes a SPARQL query returning a list of rows as {@code List<Map<String,String>>}
     * and logging ({@code INFO} level) the query, the duration and the number of rows returned.
     * if the {@code verbose} option is enabled.
     * @param query SPARQL query to be executed
     * @return Result of the SPARQL query with {@code String} values
     */
    public List<Map<String, String>> getDataframe(String query) {
        List<Map<String, String>> results = extractDataframe(query);
        if ("xml".equalsIgnoreCase(this.outputFormat))
            return getDataframeXMLEscaped(results);
        return results;
    }

    @Override
    public List<Map<String, String>> getDataframe() throws Exception {
        return null;
    }

    /**
     * Takes as input a {@code List<Map<String,String>>} dataframe and escapes
     * XML special chars. It logs ({@code INFO} level) the query, the duration and
     * the number of rows returned if the {@code verbose} option is enabled.
     * @param dataframe Dataframe to be escaped
     * @return Escaped dataframe
     */
    private List<Map<String, String>> getDataframeXMLEscaped(List<Map<String, String>> dataframe) {
        for (Map<String, String> row : dataframe)
            row.replaceAll((k, v) -> StringEscapeUtils.escapeXml11(v));
        return dataframe;
    }

    /**
     * Executes the SPARQL query in the {@code query} file writing the results in the TSV
     * format in {@code destinationPath}.
     * @param query SPARQL query to be executed
     * @param destinationPath File to save the results of the SPARQL query
     * @throws IOException If an error occurs in handling the files
     */
    public void debugQuery(String query, Path destinationPath) throws IOException {
        SPARQLResultsTSVWriter writer = new SPARQLResultsTSVWriter(new BufferedOutputStream(Files.newOutputStream(destinationPath)));
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
        if (file.exists()) {
            try (RepositoryConnection con = repository.getConnection()) {
                con.add(file, baseIRI, rdfFormat);
            }
        }
    }

    /**
     * Add triples from a file in the repository. RDF format is inferred using the file extension.
     * @param triplesPath Path of the file to be added.
     * @throws Exception
     */
    public void addFile(String triplesPath) throws Exception {
        File file = new File(triplesPath);
        if (file.exists()) {
            RDFFormat rdfFormat = Rio.getParserFormatForFileName(triplesPath).orElse(RDFFormat.TURTLE);
            try (RepositoryConnection con = repository.getConnection()) {
                con.add(file, baseIRI, rdfFormat);
            }
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
            namespaces.stream().forEach(dumpModel::setNamespace);
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
     * Supported formats: XML.
     * @param outputFormat String identifying the output format
     */
    @Override
    public void setOutputFormat(String outputFormat) {
        this.outputFormat = outputFormat;
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

    @Override
    public void setHashVariable(boolean hashVariable) {
        this.hashVariable = hashVariable;
    }

    @Override
    public void setOnlyDistinct(boolean onlyDistinct) {
        this.onlyDistinct = onlyDistinct;
    }

}
