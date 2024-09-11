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

package com.cefriel.template.io.json;

import com.cefriel.template.io.Reader;
import com.cefriel.template.utils.TemplateFunctions;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.PathNotFoundException;
import net.minidev.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class JSONReader implements Reader {

    private final Logger log = LoggerFactory.getLogger(JSONReader.class);
    Object document;
    private boolean hashVariable;
    private boolean onlyDistinct;
    private boolean verbose;

    public JSONReader(String json) {
        Configuration conf = Configuration.defaultConfiguration()
                .addOptions(Option.ALWAYS_RETURN_LIST);

        document = conf.jsonProvider().parse(json);
    }

    public JSONReader(File file) throws IOException {
        if (Files.exists(file.toPath())) {
            Configuration conf = Configuration.defaultConfiguration()
                    .addOptions(Option.ALWAYS_RETURN_LIST);

            document = conf.jsonProvider().parse(Files.readString(Paths.get(file.getPath())));
        } else
            throw new IllegalArgumentException("FILE: " + file.getPath() + " FOR JSONREADER DOES NOT EXIST");

    }
    @Override
    public void setQueryHeader(String header) {}

    @Override
    public void appendQueryHeader(String s) {}

    @Override
    public List<Map<String, String>> getDataframe(String query) throws Exception {
        Object queryDoc = Configuration.defaultConfiguration().jsonProvider().parse(query);
        String iterator = JsonPath.read(queryDoc, "$.iterator");
        Set<String> keys = JsonPath.read(queryDoc, "$.paths.keys()");
        List<Map<String,String>> dataframe = new ArrayList<>();
        // config to get results as jsonPaths
        Configuration conf = Configuration.builder()
                .options(Option.AS_PATH_LIST, Option.DEFAULT_PATH_LEAF_TO_NULL, Option.SUPPRESS_EXCEPTIONS).build();
        try {
            // Extract paths for each node identified by the iterator
            List<String> results = JsonPath.using(conf).parse(document).read(iterator);
            dataframe = new ArrayList<>(results.size());

            if(!results.isEmpty()) {
                for (int i=0; i< results.size(); i++)
                    dataframe.add(new HashMap<>(keys.size()));
                for(String key : keys) {
                    String path = JsonPath.read(queryDoc, "$.paths." + key);
                    Object objects = JsonPath.read(document, iterator + "." + path);

                    String variable = key;
                    if (hashVariable)
                        variable = TemplateFunctions.literalHash(variable);

                    // objects can either be a list of objects or a single object(string)
                    // if it is a single object then in the output list I have only one item

                    if (!(objects instanceof JSONArray)) {
                        if(objects != null)
                            dataframe.get(0).put(variable, objects.toString());
                    } else {
                        JSONArray objectsList = (JSONArray) objects;
                        // CASE 1, all the nodes identified by the iterator have the key (sub field)
                        // A single query to extract all values for the key
                        if (objectsList.size() == results.size()) {
                            for (int i = 0; i < objectsList.size(); i++) {
                                if(objectsList.get(i) != null)
                                    dataframe.get(i).put(variable, objectsList.get(i).toString());
                            }
                        }
                        // CASE 2, not all nodes have the key (sub field)
                        // For each node a query is executed to get the value for the key (sub field)
                        // The subquery is composed of the jsonpath for the node and the specific key (sub field)
                        else {
                            for (int i = 0; i < results.size(); i++) {
                                String topPath = results.get(i);
                                try {
                                    var x = JsonPath.read(document, topPath + "." + path);
                                    if (x != null)
                                        dataframe.get(i).put(variable, x.toString());
                                } catch (PathNotFoundException pe) {
                                    // what happens when the path is not found? i.e. the item does not have the field the jsonPath is pointing to
                                    // for now we do not put the key in the map
                                    if (verbose) {
                                        log.warn("PATH NOT FOUND: " + topPath + "." + path);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Exception while accessing JSON with query: " + query, e);
            throw e;
        }
        return dataframe;
    }

    @Override
    public List<Map<String, String>> getDataframe() throws Exception {
        return null;
    }

    @Override
    public void debugQuery(String query, Path destinationPath) throws Exception {
        log.warn("Debug operation not implemented");
    }

    @Override
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    /**
     * Not implemented for JSONReader yet.
     * @param outputFormat String identifying the output format
     */
    @Override
    public void setOutputFormat(String outputFormat) { return;}

    @Override
    public void setHashVariable(boolean hashVariable) {
        this.hashVariable = hashVariable;
    }

    @Override
    public void setOnlyDistinct(boolean onlyDistinct) {
        log.warn("Not implemented for JSONReader");
    }

    @Override
    public void shutDown() {}
}
