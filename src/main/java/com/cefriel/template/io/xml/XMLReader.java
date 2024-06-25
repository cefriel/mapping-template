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

package com.cefriel.template.io.xml;

import com.cefriel.template.io.Reader;
import net.sf.saxon.Configuration;
import net.sf.saxon.ma.map.KeyValuePair;
import net.sf.saxon.ma.map.MapItem;
import net.sf.saxon.om.GroundedValue;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.query.DynamicQueryContext;
import net.sf.saxon.query.StaticQueryContext;
import net.sf.saxon.query.XQueryExpression;
import net.sf.saxon.s9api.*;
import net.sf.saxon.tree.tiny.TinyElementImpl;
import net.sf.saxon.value.SequenceExtent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

public class XMLReader implements Reader {

    private final Logger log = LoggerFactory.getLogger(XMLReader.class);

    public static final String DEFAULT_KEY = "value";

    private Processor saxon;
    private Configuration config;
    private DynamicQueryContext dynamicContext;

    private String queryHeader = "xquery version \"3.1\";\n" +
            "declare namespace map = \"http://www.w3.org/2005/xpath-functions/map\";";

    private boolean verbose;
    private boolean serializeElements;

    public XMLReader(File file) throws Exception {
        if (Files.exists(file.toPath())) {
            this.saxon = new Processor(false);
            this.config = new Configuration();
            this.dynamicContext = new DynamicQueryContext(config);
            dynamicContext.setContextItem(config.buildDocumentTree(new StreamSource(file)).getRootNode());
        }
        else
            throw new IllegalArgumentException("FILE: " + file.getPath() + " FOR XMLREADER DOES NOT EXIST");
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
                    if (isSerializeElements()) {
                        if (pair.value instanceof SequenceExtent.Of<?>) {
                            StringBuilder sb = new StringBuilder();
                            sb.append("<XMLLiteral>");
                            for (GroundedValue tei : ((SequenceExtent.Of<?>) pair.value))
                                if (!(tei instanceof TinyElementImpl)) {
                                    sb.append(tei.getStringValue());
                                    sb.append("\t");
                                }
                                else
                                    sb.append(serializeElement(tei));
                            sb.append("</XMLLiteral>");
                            value = sb.toString();
                        }
                        else
                            value = serializeElement(pair.value);
                    }

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

    private String serializeElement(GroundedValue o) throws Exception {
        if (o instanceof TinyElementImpl) {
            Serializer ser = saxon.newSerializer();
            ser.setOutputProperty(Serializer.Property.OMIT_XML_DECLARATION, "yes");
            XdmNode node = new XdmNode(((TinyElementImpl) o));
            String value = ser.serializeNodeToString(node);
            return value;
        } else
            return o.getStringValue();
    }

    @Override
    public List<Map<String, String>> getDataframe(String query) throws Exception {
        return verbose ? executeQueryStringValueVerbose(query) : getQueryResultsStringValue(query);
    }

    @Override
    public List<Map<String, String>> getDataframe() throws Exception {
        return null;
    }

    @Override
    public void debugQuery(String query, Path destinationPath) throws Exception {
        List<Map<String, String>> result = getDataframe(query);
        StringBuilder sb = new StringBuilder();

        if (result != null && !result.isEmpty()){
            Set<String> keys = new LinkedHashSet<>();
            for (Map<String, String> row : result)
                keys.addAll(row.keySet());

            sb.append(String.join("\t", keys));
            sb.append("\n");

            for(Map<String, String> row : result) {
                for (String key : keys) {
                    String value = row.get(key);
                    if(value != null)
                        sb.append(value);
                    sb.append("\t");
                }
                sb.setCharAt(sb.length() - 1, '\n');
            }
            sb.setLength(sb.length() - 1);

            try (PrintWriter out = new PrintWriter(destinationPath.toString())) {
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

    @Override
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    /**
     * Not implemented for XMLReader yet.
     * @param outputFormat String identifying the output format
     */
    @Override
    public void setOutputFormat(String outputFormat) { return;}

    public String getQueryHeader() {
        return queryHeader;
    }

    @Override
    public void setQueryHeader(String header) {
        this.queryHeader = header;
    }

    @Override
    public void appendQueryHeader(String s) {
        this.queryHeader += s;
    }

    public boolean isSerializeElements() {
        return serializeElements;
    }

    public void setSerializeElements(boolean serializeElements) {
        this.serializeElements = serializeElements;
    }

}
