package com.cefriel.io.xml;

import com.cefriel.io.Reader;
import net.sf.saxon.Configuration;
import net.sf.saxon.ma.map.KeyValuePair;
import net.sf.saxon.ma.map.MapItem;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.query.DynamicQueryContext;
import net.sf.saxon.query.StaticQueryContext;
import net.sf.saxon.query.XQueryExpression;
import net.sf.saxon.s9api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.FileOutputStream;
import java.io.StringReader;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XMLReader implements Reader {

    private final Logger log = LoggerFactory.getLogger(XMLReader.class);

    private Processor saxon;
    private Configuration config;
    private DynamicQueryContext dynamicContext;


    private String namespaces;

    private boolean verbose;

    public XMLReader(File file) throws Exception {
        this.saxon = new Processor(false);
        this.config = new Configuration();
        this.dynamicContext = new DynamicQueryContext(config);
        dynamicContext.setContextItem(config.buildDocumentTree(new StreamSource(file)).getRootNode());
    }

    public XMLReader(String xml) throws Exception {
        this.saxon = new Processor(false);
        this.config = new Configuration();
        this.dynamicContext = new DynamicQueryContext(config);
        dynamicContext.setContextItem(config.buildDocumentTree(new StreamSource(new StringReader(xml))).getRootNode());
      }

    private String addNamespaces(String query) {
        if (namespaces != null && !namespaces.trim().isEmpty())
            return namespaces + query;
        return query;
    }

    public List<Map<String, String>> executeQueryStringValueVerbose(String query) throws Exception {
        log.info("Query: " + query + "\n");
        Instant start = Instant.now();
        List<Map<String, String>> results = getQueryResultsStringValue(query);
        Instant end = Instant.now();
        log.info("Info query: [duration: " + Duration.between(start, end).toMillis() + ", num_rows: " + results.size() + "]");
        return results;
    }

    public List<Map<String, String>> getQueryResultsStringValue(String query) throws Exception {
        if (namespaces != null)
            query = namespaces + query;
        StaticQueryContext sqc = config.newStaticQueryContext();
        XQueryExpression exp = sqc.compileQuery(query);
        SequenceIterator iter = exp.iterator(dynamicContext);
        List<Map<String, String>> results = new ArrayList<>();

        while (true) {
            Item item = iter.next();
            if (item == null)
                break;

            Map<String,String> map = new HashMap<>();
            if (item instanceof MapItem) {
                MapItem mitem = (MapItem) item;
                Iterable<KeyValuePair> keyValuePairs = mitem.keyValuePairs();
                for (KeyValuePair pair : keyValuePairs)
                    map.put(pair.key.getStringValue(), pair.value.getStringValue());
            } else {
                map.put("value", item.getStringValue());
            }

            results.add(map);
        }

        return results;
    }

    @Override
    public List<Map<String, String>> executeQueryStringValue(String query) throws Exception {
        return verbose ? executeQueryStringValueVerbose(query) : getQueryResultsStringValue(query);
    }

    @Override
    public void debugQuery(String query, String destinationPath) throws Exception {
        XQueryCompiler compiler = saxon.newXQueryCompiler();
        XQueryExecutable exec = compiler.compile(query);
        XQueryEvaluator queryEval = exec.load();
        queryEval.run(saxon.newSerializer(new FileOutputStream(destinationPath)));
    }

    @Override
    public void shutDown() {
        this.saxon = null;
        this.config = null;
        this.dynamicContext = null;
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


    public String getNamespaces() {
        return namespaces;
    }

    public void setNamespaces(String namespaces) {
        this.namespaces = namespaces;
    }

}
