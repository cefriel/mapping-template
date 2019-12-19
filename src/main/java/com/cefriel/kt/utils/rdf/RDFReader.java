package com.cefriel.kt.utils.rdf;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cefriel.kt.lowerer.TemplateLowerer;
import org.apache.commons.lang.StringEscapeUtils;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RDFReader {

    private Logger log = LoggerFactory.getLogger(TemplateLowerer.class);

    private Repository repository;

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

    public List<Map<String, String>> executeQueryStringValue(String query) {
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

    // Returns the string value escaping XML special chars
    public List<Map<String, String>> executeQueryStringValueXML(String query) {
        return executeQueryStringValueXML(query, true);
    }

    public List<Map<String, String>> executeQueryStringValueXML(String query, boolean debug) {
        Instant start = Instant.now();
        List<Map<String, String>> results = executeQueryStringValue(query);
        Instant end = Instant.now();
        if (debug) {
            if(results.size() < 1)
                log.info("Query: " + query + "\n");
            log.info("Info query: [duration: " + Long.toString(Duration.between(start, end).toMillis()) + ", num_rows: " + results.size() + "]");
        }
        for (Map<String, String> result : results)
            result.replaceAll((k, v) -> StringEscapeUtils.escapeXml(v));
        return results;
    }

    public Repository getRepository() {
        return repository;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }
}
