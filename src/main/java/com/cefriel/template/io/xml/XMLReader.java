package com.cefriel.template.io.xml;

import com.cefriel.template.io.Reader;
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
import java.io.PrintWriter;
import java.io.StringReader;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

public class XMLReader implements Reader {

    private final Logger log = LoggerFactory.getLogger(XMLReader.class);

    public static final String DEFAULT_KEY = "value";

    private Processor saxon;
    private Configuration config;
    private DynamicQueryContext dynamicContext;

    private String queryHeader;

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

    private String addQueryHeader(String query) {
        if (queryHeader != null && !queryHeader.trim().isEmpty())
            return queryHeader + query;
        return query;
    }

    public List<Map<String, String>> executeQueryStringValueVerbose(String query) throws Exception {
        log.info("Query: " + addQueryHeader(query) + "\n");
        Instant start = Instant.now();
        List<Map<String, String>> results = getQueryResultsStringValue(query);
        Instant end = Instant.now();
        log.info("Info query: [duration: " + Duration.between(start, end).toMillis() + ", num_rows: " + results.size() + "]");
        return results;
    }

    public List<Map<String, String>> getQueryResultsStringValue(String query) throws Exception {
        query = addQueryHeader(query);
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
                for (KeyValuePair pair : keyValuePairs) {
                    String value = pair.value.getStringValue();
                    if (value != null && !value.isEmpty())
                        map.put(pair.key.getStringValue(), value);
                }
            } else {
                String value = item.getStringValue();
                if (value != null && !value.isEmpty())
                    map.put(DEFAULT_KEY, value);
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
        List<Map<String, String>> result = executeQueryStringValue(query);
        StringBuilder sb = new StringBuilder();

        if (result != null && !result.isEmpty()){
            List<String> keys = new ArrayList<>(result.get(0).keySet());
            sb.append(String.join("\t", keys));
            sb.append("\n");

            for(Map<String, String> row : result) {
                for (String key : keys) {
                    sb.append(row.get(key));
                    sb.append("\t");
                }
                sb.setCharAt(sb.length() - 1, '\n');
            }
            sb.setLength(sb.length() - 1);

            try (PrintWriter out = new PrintWriter(destinationPath)) {
                out.println(sb);
            }
        }
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
    @Override
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }


    public String getQueryHeader() {
        return queryHeader;
    }

    @Override
    public void setQueryHeader(String header) {
        this.queryHeader = header;
    }

}
