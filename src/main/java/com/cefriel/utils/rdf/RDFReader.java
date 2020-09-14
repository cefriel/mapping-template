/*
 * Copyright 2020 Cefriel.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cefriel.utils.rdf;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cefriel.lowerer.TemplateLowerer;
import org.apache.commons.lang.StringEscapeUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import org.eclipse.rdf4j.repository.contextaware.ContextAwareRepository;
import org.eclipse.rdf4j.repository.http.HTTPRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RDFReader {

    private Logger log = LoggerFactory.getLogger(TemplateLowerer.class);

    private Repository repository;
    private IRI context;

    private boolean verbose;

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

    public List<Map<String,Value>> executeQuery(String query) {
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


    public List<Map<String,String>> getQueryResultsStringValue(String query) {
        try (RepositoryConnection con = this.repository.getConnection()) {
            TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, query);
            List<BindingSet> resultList;
            List<Map<String,String>> results = new ArrayList<>();
            try (TupleQueryResult result = tupleQuery.evaluate()) {
                resultList = QueryResults.asList(result);
            }
            for (BindingSet bindingSet : resultList) {
                Map<String,String> result = new HashMap<>();
                for (String bindingName : bindingSet.getBindingNames()) {
                    Value v = bindingSet.getValue(bindingName);
                    String value = (v != null) ? v.stringValue() : null ;
                    result.put(bindingName, value);
                }
                results.add(result);
            }
            return results;
        }
    }

    public List<Map<String, String>> executeQueryStringValueVerbose(String query) {
        Instant start = Instant.now();
        List<Map<String, String>> results = getQueryResultsStringValue(query);
        Instant end = Instant.now();
        if (results.size() < 1)
            log.info("Query: " + query + "\n");
        log.info("Info query: [duration: " + Duration.between(start, end).toMillis() + ", num_rows: " + results.size() + "]");
        return results;
    }

    public List<Map<String, String>> executeQueryStringValue(String query) {
        return verbose ? executeQueryStringValueVerbose(query) : getQueryResultsStringValue(query);
    }

    // Returns the string value escaping XML special chars
    public List<Map<String, String>> executeQueryStringValueXML(String query) {
        List<Map<String, String>> results = executeQueryStringValue(query);
        for (Map<String, String> result : results)
            result.replaceAll((k, v) -> StringEscapeUtils.escapeXml(v));
        return results;
    }

    public void shutDown() {
        repository.shutDown();
    }

    public IRI getContext() {
        return context;
    }

    public void setContext(IRI context) {
        this.context = context;
        if (repository != null) {
            ContextAwareRepository cRep = new ContextAwareRepository(repository);
            cRep.setReadContexts(context);
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

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }


}
